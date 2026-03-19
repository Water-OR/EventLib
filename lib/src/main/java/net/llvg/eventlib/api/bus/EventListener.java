package net.llvg.eventlib.api.bus;

/**
 * A functional interface for handling events registered to an {@link EventTopic}.
 *
 * @param <E> The type of event to handle.
 *
 * @see EventBus#post(Object)
 * @see EventBus#register(EventTopic, Object, EventListener)
 */
@FunctionalInterface
public interface EventListener<E> {
    /**
     * Called when an event matching the registered {@link EventTopic} is posted to the bus.
     *
     * @param event The event instance.
     */
    void invoke(E event);
}
