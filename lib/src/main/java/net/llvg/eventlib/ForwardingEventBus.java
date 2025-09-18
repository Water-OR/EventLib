package net.llvg.eventlib;

import java.util.function.Consumer;

/**
 * A helper class for easier {@link EventBus EventBus} override creation.
 *
 * @param <E> Type of the events.
 * @param <L> Type of the listeners.
 */
@SuppressWarnings ("unused")
public abstract class ForwardingEventBus<E, L extends Consumer<? super E>>
  implements EventBus<E, L>
{
    /**
     * Default constructor.
     */
    protected ForwardingEventBus() { }
    
    /**
     * The delegated {@link EventBus EventBus}.
     *
     * @return The delegated {@link EventBus EventBus}.
     */
    protected abstract EventBus<E, L> delegatee();
    
    @Override
    public ListenerList<E, L> getList(final Class<? extends E> clazz) {
        return delegatee().getList(clazz);
    }
    
    @Override
    public void register(final Object target) {
        delegatee().register(target);
    }
    
    @Override
    public void unregister(final Object target) {
        delegatee().unregister(target);
    }
    
    @Override
    public void fire(final Class<? extends E> type, final E event) {
        delegatee().fire(type, event);
    }
}
