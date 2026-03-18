package net.llvg.eventlib.impl.bus;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;
import lombok.var;
import net.llvg.eventlib.api.bus.EventBus;
import net.llvg.eventlib.api.bus.EventError;
import net.llvg.eventlib.api.bus.EventListener;
import net.llvg.eventlib.api.bus.EventTopic;
import net.llvg.eventlib.api.phase.PhaseManager;
import net.llvg.eventlib.impl.Util;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

@ToString
@EqualsAndHashCode
public final class EventBusImpl<P>
  implements EventBus<P>
{
    private final ConcurrentHashMap<EventTopic<?>, ListenerList<P>> topic2list = new ConcurrentHashMap<>();
    
    private final PhaseManager<P> phases;
    
    private EventBusImpl(final PhaseManager.Builder<P> phaseManagerBuilder) {
        this.phases = phaseManagerBuilder
          .onDirty(() -> topic2list.values().forEach(ListenerList::makeDirty))
          .build();
    }
    
    public static <P> EventBusImpl<P> create(final PhaseManager.Builder<P> phaseManagerBuilder) {
        return new EventBusImpl<>(Util.argNotNull(phaseManagerBuilder, "phaseManagerBuilder"));
    }
    
    @Override
    public PhaseManager<P> getPhases() {
        return phases;
    }
    
    private ListenerList<P> makeListIfAbsent(final EventTopic<?> topic) {
        ListenerList<P> r;
        if ((r = topic2list.get(topic)) == null) {
            val builder = new HashSet<ListenerList<P>>();
            
            for (val it : topic.getSupertopics()) {
                val list = makeListIfAbsent(it);
                builder.add(list);
                builder.addAll(list.dependencies);
            }
            
            r = topic2list.computeIfAbsent(topic, $ -> new ListenerList<>(builder));
        }
        
        return r;
    }
    
    @Override
    public <E> EventBus.Registration<P> register(
      final EventTopic<E> topic,
      final P phase,
      final EventListener<? super E> listener
    ) {
        val result = new Registration<>(makeListIfAbsent(topic), phases.add(phase), listener);
        result.list.modify(result, true);
        return result;
    }
    
    @Override
    @SuppressWarnings ("unchecked")
    public <E> EventBus.@Unmodifiable SnapshotList<P, E> getSnapshot(final EventTopic<E> topic) {
        return (EventBus.SnapshotList<P, E>) makeListIfAbsent(topic).getSorted(phases);
    }
    
    @RequiredArgsConstructor
    @ToString
    @EqualsAndHashCode
    private static final class Registration<P, E>
      implements EventBus.Registration<P>
    {
        final ListenerList<P> list;
        
        @Getter
        final P phase;
        
        @Getter
        final EventListener<? super E> listener;
        
        volatile boolean active = true;
        
        @Override
        public boolean isRegistered() {
            return list.contains(this);
        }
        
        @Override
        public void unregister() {
            list.modify(this, false);
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
    
    private static final class ListenerList<P> {
        private static final Function<Object, ArrayList<Registration<?, ?>>> genArrayList = $ -> new ArrayList<>();
        
        final @Unmodifiable ArrayList<ListenerList<P>> dependencies;
        final ArrayList<ListenerList<P>> dependents = new ArrayList<>();
        
        final ConcurrentHashMap.KeySetView<Registration<P, ?>, Boolean> registry = ConcurrentHashMap.newKeySet();
        
        transient final StampedLock lock = new StampedLock();
        transient volatile @Nullable SnapshotList<P, ?> sorted = null;
        
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
            if (registry.contains(registration) == add) return;
            
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
        
        SnapshotList<P, ?> getSorted(final PhaseManager<P> manager) {
            SnapshotList<P, ?> r;
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
                        
                        @SuppressWarnings ("unchecked")
                        final Registration<P, ?>[] b = new Registration[size];
                        
                        int i = 0;
                        for (val phase : phases) {
                            val list = phase2actions.get(phase);
                            if (list != null && !list.isEmpty()) {
                                i = Util.copy(list, b, i);
                            }
                        }
                        
                        Util.check(i == size, "{} of {} registration are processed.", i, size);
                        
                        sorted = (r = new SnapshotList<>(b));
                    }
                } finally {
                    lock.unlockWrite(stamp);
                }
            }
            
            return r;
        }
    }
    
    @RequiredArgsConstructor
    private static final class SnapshotList<P, E>
      extends AbstractList<EventBus.Registration<P>>
      implements EventBus.SnapshotList<P, E>
    {
        final Registration<P, ?>[] regs;
        
        @Override
        public EventBus.Registration<P> get(final int index) {
            return regs[index];
        }
        
        @Override
        public int size() {
            return regs.length;
        }
        
        @Override
        public E post(final E event) {
            for (val reg : regs) reg.invoke(event);
            
            return event;
        }
        
        @Override
        public @Nullable EventError postAndCatch(E event) {
            int i = 0;
            
            try {
                for (; i < regs.length; ++i) {
                    regs[i].invoke(event);
                }
            } catch (Throwable e) {
                return new EventError(e, i, regs[i]);
            }
            
            return null;
        }
        
        @Override
        public Object[] toArray() {
            return Arrays.copyOf(regs, regs.length);
        }
        
        @SuppressWarnings ("all")
        @Override
        public <T extends @Nullable Object> T[] toArray(final T[] a) {
            if (a.length < regs.length) {
                return Arrays.copyOf(regs, regs.length, (Class<? extends T[]>) a.getClass());
            }
            
            System.arraycopy(regs, 0, a, 0, regs.length);
            
            if (a.length > regs.length) {
                a[regs.length] = null;
            }
            
            return a;
        }
    }
}
