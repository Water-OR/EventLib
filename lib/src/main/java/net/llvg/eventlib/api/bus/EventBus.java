package net.llvg.eventlib.api.bus;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.llvg.eventlib.api.phase.PhaseManager;
import net.llvg.eventlib.impl.bus.EventBusImpl;
import org.jetbrains.annotations.ApiStatus;

public interface EventBus<P> {
    @CheckReturnValue
    PhaseManager<P> getPhases();
    
    @CanIgnoreReturnValue
    <E> Registration register(
      final Class<E> type,
      final P phase,
      final EventListener<? super E> listener
    );
    
    @CanIgnoreReturnValue
    @ApiStatus.NonExtendable
    default <E> Registration register(
      final Class<E> type,
      final EventListener<? super E> listener
    ) {
        return register(type, getPhases().getDefaultPhase(), listener);
    }
    
    @CanIgnoreReturnValue
    <E> E post(final E event);
    
    @CheckReturnValue
    static <P> EventBus<P> create(final PhaseManager.Builder<P> phaseManagerBuilder) {
        return new EventBusImpl<>(phaseManagerBuilder);
    }
    
    @CheckReturnValue
    static <P extends Comparable<? super P>> EventBus<P> create(final P defaultPhase) {
        return new EventBusImpl<>(PhaseManager.builderComparable(defaultPhase));
    }
    
    interface Registration {
        @CheckReturnValue
        boolean isRegistered();
        
        void unregister();
        
        void setActive(final boolean value);
        
        @CheckReturnValue
        boolean isActive();
        
        @CheckReturnValue
        @ApiStatus.NonExtendable
        default Resource asResource() {
            return new Resource(this);
        }
    }
    
    @RequiredArgsConstructor (access = AccessLevel.PRIVATE)
    @ToString
    @EqualsAndHashCode
    final class Resource
      implements AutoCloseable
    {
        private final Registration delegatee;
        
        @Override
        public void close() {
            delegatee.unregister();
        }
    }
}
