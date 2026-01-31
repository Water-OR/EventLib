package net.llvg.eventlib.api.bus;

/**
 * A functional interface for handling events of a specific type.
 * <p>
 * Implementations of this interface are registered with an {@link EventBus} to receive
 * notifications when matching events are posted.
 * <p>
 * Since this is a {@link FunctionalInterface}, it can be implemented using a lambda expression
 * or method reference.
 *
 * @param <E> The type of event to handle.
 *
 * @see EventBus#register(Class, Object, EventListener)
 */
@FunctionalInterface
public interface EventListener<E> {
    /**
     * Invoked when an event of type {@code E} (or a subclass) is posted to the bus.
     *
     * @param event The event instance.
     */
    void invoke(final E event);
}
