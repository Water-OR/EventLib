package net.llvg.eventlib;

import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A helper class for easier {@link ListenerWrapper ListenerWrapper} override creation.
 *
 * @param <E> Type of the events.
 * @param <L> Type of the listeners.
 */
@SuppressWarnings ("unused")
public abstract class ForwardingListenerWrapper<E, L extends Consumer<? super E>>
  implements ListenerWrapper<E, L>
{
    /**
     * Default constructor.
     */
    protected ForwardingListenerWrapper() { }
    
    /**
     * The delegated {@link EventBus EventBus}.
     *
     * @return The delegated {@link EventBus EventBus}.
     */
    protected abstract ListenerWrapper<E, L> delegatee();
    
    @Override
    public Optional<L> wrapStaticListener(
      final Class<? extends E> type,
      final UncheckedListener listener,
      final Method method
    ) {
        return delegatee().wrapStaticListener(type, listener, method);
    }
    
    @Override
    public Optional<L> wrapObjectListener(
      final Class<? extends E> type,
      final UncheckedListener listener,
      final Method method,
      final Object object,
      final ImmutableSet<Method> supers
    ) {
        return delegatee().wrapObjectListener(type, listener, method, object, supers);
    }
}
