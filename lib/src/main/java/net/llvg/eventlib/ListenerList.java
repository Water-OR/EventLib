package net.llvg.eventlib;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A "list" for recording and caching listeners.
 *
 * @param <E> Type of the events.
 * @param <L> Type of the listeners.
 *
 * @see EventBus EventBus
 */
public final class ListenerList<E, L extends Consumer<? super E>> {
    private final ImmutableSet<ListenerList<E, L>> supers;
    private final Set<ListenerList<E, L>> knownExtenders = new HashSet<>();
    
    private final List<L> listeners = new ArrayList<>();
    private volatile @Nullable ImmutableList<L> cache = null;
    private final Consumer<? super ArrayList<L>> cacheProcessor;
    private final Object cacheMutex = new Object();
    
    ListenerList(
      final Stream<ListenerList<E, L>> supers,
      final Consumer<? super ArrayList<L>> cacheProcessor
    ) {
        final ImmutableList<ListenerList<E, L>> v = ImmutableList.copyOf(supers.iterator());
        final ImmutableSet.Builder<ListenerList<E, L>> builder = ImmutableSet.builder();
        
        builder.addAll(v);
        for (final ListenerList<E, L> it : v) builder.addAll(it.supers);
        
        this.supers = builder.build();
        
        for (final ListenerList<E, L> it : this.supers) {
            synchronized (it.knownExtenders) {
                it.knownExtenders.add(this);
            }
        }
        
        this.cacheProcessor = cacheProcessor;
    }
    
    private void deprecateCaches() {
        synchronized (cacheMutex) {
            cache = null;
        }
        
        for (final ListenerList<E, L> it : knownExtenders) {
            synchronized (it.cacheMutex) {
                it.cache = null;
            }
        }
    }
    
    /**
     * Adds the {@code listener} to the list.
     *
     * @param listener The listener being added.
     */
    public void add(L listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
        
        deprecateCaches();
    }
    
    /**
     * Remove all listeners matches the {@code filter}.
     *
     * @param filter The {@link Predicate} to test if a listener should be removed or not.
     *
     * @return {@code true} if any listeners were removed, otherwise {@code false}.
     */
    @SuppressWarnings ("UnusedReturnValue")
    public boolean removeIf(Predicate<? super L> filter) {
        final boolean modified;
        
        synchronized (listeners) {
            modified = listeners.removeIf(filter);
        }
        
        if (modified) deprecateCaches();
        return modified;
    }
    
    /**
     * Build cache if it's not valid. Then returns the valid cache.
     *
     * @return The cached listeners.
     */
    public ImmutableList<L> getCache() {
        ImmutableList<L> r;
        if ((r = cache) == null) synchronized (cacheMutex) {
            if ((r = cache) == null) {
                final ArrayList<L> builder;
                
                synchronized (listeners) {
                    builder = new ArrayList<>(listeners);
                }
                
                for (final ListenerList<E, L> it : supers) {
                    synchronized (it.listeners) {
                        builder.addAll(it.listeners);
                    }
                }
                
                cacheProcessor.accept(builder);
                return cache = ImmutableList.copyOf(builder);
            }
        }
        
        return r;
    }
}
