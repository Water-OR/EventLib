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
    
    @CheckReturnValue
    public static <P> Builder<P> builder(final P defaultPhase) {
        return new Builder<>(defaultPhase);
    }
    
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
        Comparator<P> comparator;
        
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