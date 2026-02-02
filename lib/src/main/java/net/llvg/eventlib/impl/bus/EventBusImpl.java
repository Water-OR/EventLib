package net.llvg.eventlib.impl.bus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import lombok.var;
import net.llvg.eventlib.api.bus.EventBus;
import net.llvg.eventlib.api.bus.EventListener;
import net.llvg.eventlib.api.phase.PhaseManager;
import net.llvg.eventlib.impl.Util;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

@ToString
@EqualsAndHashCode
public final class EventBusImpl<P>
  implements EventBus<P>
{
    private final ConcurrentHashMap<Class<?>, ListenerList<P>> type2list = new ConcurrentHashMap<>();
    
    private final PhaseManager<P> phases;
    
    private EventBusImpl(final PhaseManager.Builder<P> phaseManagerBuilder) {
        this.phases = phaseManagerBuilder
          .onDirty(() -> type2list.values().forEach(ListenerList::makeDirty))
          .build();
    }
    
    public static <P> EventBusImpl<P> create(final PhaseManager.Builder<P> phaseManagerBuilder) {
        return new EventBusImpl<>(Objects.requireNonNull(phaseManagerBuilder, "[phaseManagerBuilder] must not be null."));
    }
    
    @Override
    public PhaseManager<P> getPhases() {
        return phases;
    }
    
    private ListenerList<P> makeListIfAbsent(final Class<?> type) {
        ListenerList<P> r;
        if ((r = type2list.get(type)) == null) {
            val builder = new HashSet<ListenerList<P>>();
            
            val superclass = type.getSuperclass();
            if (superclass != null) {
                val list = makeListIfAbsent(superclass);
                builder.add(list);
                builder.addAll(list.dependencies);
            }
            
            for (val it : type.getInterfaces()) {
                val list = makeListIfAbsent(it);
                builder.add(list);
                builder.addAll(list.dependencies);
            }
            
            r = type2list.computeIfAbsent(type, $ -> new ListenerList<>(builder));
        }
        
        return r;
    }
    
    @Override
    public <E> EventBus.Registration register(
      final Class<E> type,
      final P phase,
      final EventListener<? super E> listener
    ) {
        val result = new Registration<>(makeListIfAbsent(type), phases.add(phase), listener);
        result.list.add(result);
        return result;
    }
    
    @Override
    public <E> E post(final E event) {
        val sorted = makeListIfAbsent(event.getClass()).getSorted(phases);
        
        for (val registration : sorted) registration.invoke(event);
        
        return event;
    }
    
    @RequiredArgsConstructor
    @ToString
    @EqualsAndHashCode
    static final class Registration<P, E>
      implements EventBus.Registration
    {
        final ListenerList<P> list;
        final P phase;
        final EventListener<? super E> listener;
        
        volatile boolean active = true;
        
        @Override
        public boolean isRegistered() {
            return list.contains(this);
        }
        
        @Override
        public void unregister() {
            list.rem(this);
        }
        
        @SuppressWarnings ("unchecked")
        void invoke(final Object event) {
            if (active) listener.invoke((E) event);
        }
        
        @Override
        public void setActive(final boolean active) {
            this.active = active;
        }
        
        @Override
        public boolean isActive() {
            return active;
        }
    }
    
    static final class ListenerList<P> {
        static final Function<Object, ArrayList<Registration<?, ?>>> genArrayList = $ -> new ArrayList<>();
        
        final @Unmodifiable ArrayList<ListenerList<P>> dependencies;
        final ArrayList<ListenerList<P>> dependents = new ArrayList<>();
        
        final ConcurrentHashMap.KeySetView<Registration<P, ?>, Boolean> registry = ConcurrentHashMap.newKeySet();
        
        transient final StampedLock lock = new StampedLock();
        transient volatile Registration<?, ?> @Unmodifiable @Nullable [] sorted = null;
        
        ListenerList(final @Unmodifiable HashSet<ListenerList<P>> dependencies) {
            this.dependencies = new ArrayList<>(dependencies);
            this.dependencies.trimToSize();
            
            for (val it : this.dependencies) {
                val stamp = it.lock.writeLock(); // exclusive lock
                try {
                    it.dependents.add(this);
                } finally {
                    it.lock.unlockWrite(stamp);
                }
            }
        }
        
        void makeDirty() {
            val stamp = lock.readLock();
            try {
                sorted = null;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        boolean contains(final Registration<P, ?> registration) {
            return registry.contains(registration);
        }
        
        void modify(final Registration<P, ?> registration, final boolean add) {
            val stamp = lock.readLock(); // shared lock
            try {
                sorted = null;
                val count = dependents.size();
                
                val stamps = new long[count];
                var i = 0;
                for (; i < count; ++i) {
                    val list = dependents.get(i);
                    stamps[i] = list.lock.readLock();
                    list.sorted = null;
                }
                
                try {
                    if (add) {
                        registry.add(registration);
                    } else {
                        registry.remove(registration);
                    }
                } finally {
                    while (i-- > 0) dependents.get(i).lock.unlockRead(stamps[i]);
                }
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        void add(final Registration<P, ?> registration) {
            if (!registry.contains(registration)) modify(registration, true);
        }
        
        void rem(final Registration<P, ?> registration) {
            if (registry.contains(registration)) modify(registration, false);
        }
        
        Registration<?, ?> @Unmodifiable [] getSorted(final PhaseManager<P> manager) {
            Registration<?, ?>[] r;
            if ((r = sorted) == null) {
                val phases = manager.getSorted();
                
                val stamp = lock.writeLock(); // exclusive
                try {
                    if ((r = sorted) == null) {
                        val phase2actions = new HashMap<P, ArrayList<Registration<?, ?>>>(phases.size());
                        var size = registry.size();
                        
                        for (val reg : registry) {
                            phase2actions.computeIfAbsent(reg.phase, genArrayList).add(reg);
                        }
                        
                        for (val it : dependencies) {
                            size += it.registry.size();
                            
                            for (val reg : it.registry) {
                                phase2actions.computeIfAbsent(reg.phase, genArrayList).add(reg);
                            }
                        }
                        
                        r = new Registration<?, ?>[size];
                        
                        int i = 0;
                        for (val phase : phases) {
                            val list = phase2actions.get(phase);
                            if (list != null) {
                                for (val it : list) r[i++] = it;
                            }
                        }
                        
                        Util.check(i == size, "{} of {} registration are processed.", i, size);
                        
                        sorted = r;
                    }
                } finally {
                    lock.unlockWrite(stamp);
                }
            }
            
            return r;
        }
    }
}
