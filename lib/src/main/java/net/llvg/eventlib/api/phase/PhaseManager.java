package net.llvg.eventlib.api.phase;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.StampedLock;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.val;
import net.llvg.eventlib.impl.Util;
import net.llvg.eventlib.impl.graph.Node;
import net.llvg.eventlib.impl.graph.TopoSorter;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

/**
 * Manages the execution order of event phases using a topological sort.
 * <p>
 * A {@code PhaseManager} maintains a dependency graph of phases (nodes) and their relationships (edges).
 * It produces a linearized, topologically sorted list of phases that determines the order in which
 * event listeners are invoked.
 * <p>
 * Key features:
 * <ul>
 *     <li><b>Thread-Safe:</b> Uses {@link StampedLock} and {@link ConcurrentHashMap} to allow concurrent
 *     reads and writes. Writes (adding phases/links) do not block other writes, but will block the
 *     sorting process.</li>
 *     <li><b>Cycle Handling:</b> If a cycle is detected in the dependency graph, the manager uses the
 *     provided {@link Comparator} to deterministically sort the phases involved in the cycle (Strongly Connected Component).</li>
 *     <li><b>Reactive Caching:</b> The sorted result is cached. The {@code onDirty} callback is triggered
 *     only when the graph structure actually changes, notifying downstream systems (like {@code EventBus})
 *     to invalidate their caches.</li>
 * </ul>
 *
 * @param <P> The type of the phase identifier (e.g., String, Identifier, or Enum).
 */
@ToString
@EqualsAndHashCode
@FieldDefaults (
  level = AccessLevel.PRIVATE,
  makeFinal = true
)
public final class PhaseManager<P> {
    ConcurrentHashMap<P, Wrapper<P>> phase2wrapper = new ConcurrentHashMap<>();
    Runnable onDirty;
    
    transient StampedLock lock = new StampedLock();
    transient AtomicReference<@Nullable @Unmodifiable List<P>> sorted = new AtomicReference<>();
    
    /**
     * The default phase used when no specific phase is requested during registration.
     * <p>
     * This phase is guaranteed to exist in the manager.
     */
    @Getter
    P defaultPhase;
    
    Comparator<Wrapper<P>> comparator;
    
    private PhaseManager(
      final P defaultPhase,
      final Runnable onDirty,
      final Comparator<P> comparator
    ) {
        this.defaultPhase = defaultPhase;
        this.onDirty = onDirty;
        this.comparator = Comparator.comparing(it -> it.value, comparator);
        
        phase2wrapper.put(this.defaultPhase, new Wrapper<>(this.defaultPhase));
    }
    
    /**
     * Creates a builder for a {@code PhaseManager}.
     *
     * @param defaultPhase The default phase identifier. Must not be null.
     * @param <P> The phase type.
     *
     * @return A new Builder instance.
     */
    @CheckReturnValue
    public static <P> Builder<P> builder(final P defaultPhase) {
        return new Builder<>(defaultPhase);
    }
    
    /**
     * Creates a builder for a {@code PhaseManager} where the phase type implements {@link Comparable}.
     * <p>
     * This builder automatically sets the comparator to {@link Comparator#naturalOrder()}.
     *
     * @param defaultPhase The default phase identifier.
     * @param <P> The phase type (must be Comparable).
     *
     * @return A new Builder instance configured with natural order.
     */
    @CheckReturnValue
    public static <P extends Comparable<? super P>> Builder<P> builderComparable(final P defaultPhase) {
        return new Builder<>(defaultPhase).comparator(Comparator.naturalOrder());
    }
    
    private Wrapper<P> makeWrapperIfAbsent(final P phase) {
        return phase2wrapper.computeIfAbsent(
          phase, it -> {
              if (sorted.getAndSet(null) != null) onDirty.run();
              return new Wrapper<>(it);
          }
        );
    }
    
    /**
     * Registers a new phase node without establishing any connections.
     *
     * @param phase The phase to add.
     *
     * @return The canonical instance of the phase identifier stored in the manager.
     */
    @CanIgnoreReturnValue
    public P add(final P phase) {
        Wrapper<P> r;
        if ((r = phase2wrapper.get(phase)) == null) {
            val stamp = lock.readLock(); // shared lock
            try {
                r = makeWrapperIfAbsent(phase);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        return r.value;
    }
    
    /**
     * Establishes a happens-before relationship between two phases.
     * <p>
     * This method ensures that {@code earlier} will appear before {@code later} in the sorted list.
     * If the phases do not exist, they are automatically added.
     *
     * @param earlier The phase that should run first.
     * @param later The phase that should run after.
     */
    public void link(final P earlier, final P later) {
        val stamp = lock.readLock();
        try {
            val from = makeWrapperIfAbsent(earlier);
            val to = makeWrapperIfAbsent(later);
            
            final boolean modified;
            synchronized (from) {
                modified = from.linkTo(to);
            }
            
            if (modified && sorted.getAndSet(null) != null) onDirty.run();
        } finally {
            lock.unlockRead(stamp);
        }
    }
    
    /**
     * Returns the topologically sorted list of phases.
     * <p>
     * The result is cached. If the graph structure has changed since the last call,
     * the sort is re-computed. This operation requires an exclusive lock and will block
     * any concurrent {@link #add} or {@link #link} operations.
     *
     * @return An unmodifiable, sorted list of phases.
     */
    @CheckReturnValue
    public @Unmodifiable List<P> getSorted() {
        List<P> r;
        if ((r = sorted.get()) == null) {
            val stamp = lock.writeLock(); // exclusive lock
            try {
                if ((r = sorted.get()) == null) {
                    val builder = new ArrayList<P>(phase2wrapper.size());
                    
                    TopoSorter.sort(
                      phase2wrapper.values(),
                      wrappers -> {
                          for (val it : wrappers) builder.add(it.value);
                      },
                      comparator
                    );
                    
                    builder.trimToSize();
                    sorted.set(r = Collections.unmodifiableList(builder));
                }
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        return r;
    }
    
    private static final class Wrapper<P>
      extends Node<Wrapper<P>>
    {
        final P value;
        
        Wrapper(final P value) {
            super(new HashSet<>());
            this.value = value;
        }
    }
    
    /**
     * Builder for {@link PhaseManager}.
     *
     * @param <P> The phase type.
     */
    @RequiredArgsConstructor (access = AccessLevel.PRIVATE)
    @Setter
    @Accessors (
      fluent = true,
      chain = true
    )
    @FieldDefaults (level = AccessLevel.PRIVATE)
    public static final class Builder<P> {
        final P defaultPhase;
        Runnable onDirty = () -> { };
        
        /**
         * Sets the callback to be executed when the phase graph changes.
         * Defaults to a no-op.
         */
        Comparator<P> comparator;
        
        /**
         * Builds the {@link PhaseManager}.
         *
         * @return A new PhaseManager instance.
         *
         * @throws IllegalStateException if {@code comparator} is not set and P is not Comparable.
         */
        @CheckReturnValue
        public PhaseManager<P> build() {
            return new PhaseManager<>(
              defaultPhase,
              onDirty,
              Util.checkNotNull(comparator, "[comparator] must be specified")
            );
        }
    }
}