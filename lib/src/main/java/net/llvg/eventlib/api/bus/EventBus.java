package net.llvg.eventlib.api.bus;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.List;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.llvg.eventlib.api.phase.PhaseManager;
import net.llvg.eventlib.impl.bus.EventBusImpl;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

/**
 * An event bus for dispatching events to registered listeners in phase order.
 *
 * <p>An event bus manages a collection of event topics and their registered listeners.
 * Events are posted to topics and delivered to all active listeners in ascending phase order.
 *
 * <p>Example usage:
 * <pre>{@code
 * EventBus<Phase> bus = EventBus.create("default");
 *
 * EventTopic<MyEvent> topic = EventTopic.of();
 *
 * // Register a listener
 * Registration<Phase> reg = bus.register(topic, "custom", event -> {
 *     System.out.println("Received: " + event);
 * });
 *
 * // Post an event
 * bus.post(topic, new MyEvent());
 *
 * // Unregister when done
 * reg.unregister();
 * }</pre>
 *
 * @param <P> the phase type used for event ordering
 *
 * @see EventTopic
 * @see PhaseManager
 * @see EventListener
 */
public interface EventBus<P> {
    /**
     * Returns the phase manager for this event bus.
     *
     * @return the phase manager
     */
    PhaseManager<P> getPhases();
    
    /**
     * Registers an event listener for a specific topic and phase.
     *
     * @param topic the event topic to listen to
     * @param phase the phase at which to receive events
     * @param listener the event listener
     * @param <E> the event type
     *
     * @return a registration handle for unregistering
     */
    @CanIgnoreReturnValue
    <E> Registration<P> register(
      final EventTopic<E> topic,
      final P phase,
      final EventListener<? super E> listener
    );
    
    /**
     * Registers an event listener for a specific topic using the default phase.
     *
     * <p>This is an alias for:
     * <pre>{@code register(topic, getPhases().getDefaultPhase(), listener)}</pre>
     *
     * @param topic the event topic to listen to
     * @param listener the event listener
     * @param <E> the event type
     *
     * @return a registration handle for unregistering
     *
     * @see #register(EventTopic, Object, EventListener)
     */
    @CanIgnoreReturnValue
    @ApiStatus.NonExtendable
    default <E> Registration<P> register(
      final EventTopic<E> topic,
      final EventListener<? super E> listener
    ) {
        return register(topic, getPhases().getDefaultPhase(), listener);
    }
    
    /**
     * Gets a snapshot of all registrations for a specific topic.
     *
     * <p>The returned list is a read-only snapshot of the current registrations.
     * New registrations added after this call are not reflected in the snapshot.
     *
     * @param topic the event topic
     * @param <E> the event type
     *
     * @return an unmodifiable list of registrations with posting methods
     */
    @CanIgnoreReturnValue
    <E> @Unmodifiable SnapshotList<P, E> getSnapshot(final EventTopic<E> topic);
    
    /**
     * Posts an event to all active registrations for a specific topic.
     *
     * <p>This is an alias for:
     * <pre>{@code getSnapshot(topic).post(event)}</pre>
     *
     * @param topic the event topic
     * @param event the event to post
     * @param <E> the event type
     *
     * @return the posted event
     *
     * @see SnapshotList#post(Object)
     */
    @CanIgnoreReturnValue
    default <E> E post(final EventTopic<E> topic, final E event) {
        return getSnapshot(topic).post(event);
    }
    
    /**
     * Posts an event and catches any exception thrown by listeners.
     *
     * <p>This is an alias for:
     * <pre>{@code getSnapshot(topic).postAndCatch(event)}</pre>
     *
     * @param topic the event topic
     * @param event the event to post
     * @param <E> the event type
     *
     * @return an error containing the exception if one occurred, {@code null} otherwise
     *
     * @see SnapshotList#postAndCatch(Object)
     */
    @CanIgnoreReturnValue
    default <E> @Nullable EventError postAndCatch(final EventTopic<E> topic, final E event) {
        return getSnapshot(topic).postAndCatch(event);
    }
    
    /**
     * Registers an event listener using a class type as the event topic.
     *
     * <p>This is an alias for:
     * <pre>{@code register(EventTopic.forClass(type), phase, listener)}</pre>
     *
     * @param type the event class
     * @param phase the phase at which to receive events
     * @param listener the event listener
     * @param <E> the event type
     *
     * @return a registration handle for unregistering
     *
     * @see EventTopic#forClass(Class)
     */
    @CanIgnoreReturnValue
    @ApiStatus.NonExtendable
    default <E> Registration<P> register(
      final Class<E> type,
      final P phase,
      final EventListener<? super E> listener
    ) {
        return register(EventTopic.forClass(type), phase, listener);
    }
    
    /**
     * Registers an event listener using a class type with the default phase.
     *
     * <p>This is an alias for:
     * <pre>{@code register(EventTopic.forClass(type), listener)}</pre>
     *
     * @param type the event class
     * @param listener the event listener
     * @param <E> the event type
     *
     * @return a registration handle for unregistering
     *
     * @see EventTopic#forClass(Class)
     */
    @CanIgnoreReturnValue
    @ApiStatus.NonExtendable
    default <E> Registration<P> register(
      final Class<E> type,
      final EventListener<? super E> listener
    ) {
        return register(EventTopic.forClass(type), listener);
    }
    
    /**
     * Gets a snapshot of all registrations for a topic created from a class.
     *
     * <p>This is an alias for:
     * <pre>{@code getSnapshot(EventTopic.forClass(type))}</pre>
     *
     * @param type the event class
     * @param <E> the event type
     *
     * @return an unmodifiable list of registrations with posting methods
     *
     * @see EventTopic#forClass(Class)
     */
    @ApiStatus.NonExtendable
    default <E> @Unmodifiable SnapshotList<P, E> getSnapshot(final Class<E> type) {
        return getSnapshot(EventTopic.forClass(type));
    }
    
    /**
     * Posts an event using its runtime class as the event topic.
     *
     * <p>This is an alias for:
     * <pre>{@code post(EventTopic.forClass(event.getClass()), event)}</pre>
     *
     * @param event the event to post
     * @param <E> the event type
     *
     * @return the posted event
     *
     * @see EventTopic#forClass(Class)
     */
    @CanIgnoreReturnValue
    @SuppressWarnings ("unchecked")
    default <E> E post(final E event) {
        return post(EventTopic.forClass((Class<E>) event.getClass()), event);
    }
    
    /**
     * Posts an event using its runtime class and catches any exception.
     *
     * <p>This is an alias for:
     * <pre>{@code postAndCatch(EventTopic.forClass(event.getClass()), event)}</pre>
     *
     * @param event the event to post
     * @param <E> the event type
     *
     * @return an error containing the exception if one occurred, {@code null} otherwise
     *
     * @see EventTopic#forClass(Class)
     */
    @CanIgnoreReturnValue
    @SuppressWarnings ("unchecked")
    default <E> @Nullable EventError postAndCatch(final E event) {
        return postAndCatch(EventTopic.forClass((Class<E>) event.getClass()), event);
    }
    
    /**
     * Creates an event bus with a custom phase manager builder.
     *
     * @param phaseManagerBuilder the phase manager builder
     * @param <P> the phase type
     *
     * @return a new event bus instance
     */
    static <P> EventBus<P> create(final PhaseManager.Builder<P> phaseManagerBuilder) {
        return EventBusImpl.create(phaseManagerBuilder);
    }
    
    /**
     * Creates an event bus with a single comparable default phase.
     *
     * <p>This is a convenience method for creating an event bus with a single phase.
     * The phase must implement {@code Comparable}.
     *
     * @param defaultPhase the default phase value
     * @param <P> the phase type
     *
     * @return a new event bus instance
     */
    static <P extends Comparable<? super P>> EventBus<P> create(final P defaultPhase) {
        return EventBusImpl.create(PhaseManager.builderComparable(defaultPhase));
    }
    
    /**
     * A handle for a registered event listener.
     *
     * <p>Use this to control the registration, such as unregistering or temporarily
     * disabling the listener.
     *
     * @param <P> the phase type
     *
     * @see EventBus#register(EventTopic, Object, EventListener)
     */
    interface Registration<P> {
        /**
         * Returns the registered event listener.
         *
         * @return the listener
         */
        EventListener<?> getListener();
        
        /**
         * Returns the phase at which this listener was registered.
         *
         * @return the registration phase
         */
        P getPhase();
        
        /**
         * Checks if this registration is still active.
         *
         * @return {@code true} if registered, {@code false} if unregistered
         */
        boolean isRegistered();
        
        /**
         * Unregisters the listener from the event bus.
         *
         * <p>After calling this method, the listener will no longer receive events.
         * This method is idempotent and safe to call multiple times.
         */
        void unregister();
        
        /**
         * Toggles the listener without unregistering it.
         *
         * @param value {@code true} to enable, {@code false} to disable
         */
        void setActive(final boolean value);
        
        /**
         * Checks if this listener is currently active.
         *
         * @return {@code true} if active, otherwise {@code false}
         */
        boolean isActive();
        
        /**
         * Wraps this registration as an {@link AutoCloseable} resource.
         *
         * <p>Calling {@code close()} on the returned resource is equivalent to
         * calling {@link #unregister()}.
         *
         * @return a resource that unregisters on close
         */
        @ApiStatus.NonExtendable
        default Resource asResource() {
            return new Resource(this);
        }
    }
    
    /**
     * An AutoCloseable wrapper for a Registration.
     *
     * <p>Useful for try-with-resources patterns:
     * <pre>{@code
     * try (EventBus.Resource ignored = bus.register(topic, phase, listener).asResource()) {
     *     // listener is active
     * } // listener is automatically unregistered
     * }</pre>
     *
     * @see Registration#asResource()
     */
    @RequiredArgsConstructor (access = AccessLevel.PRIVATE)
    @ToString
    @EqualsAndHashCode
    final class Resource
      implements AutoCloseable
    {
        private final Registration<?> reg;
        
        /**
         * Unregisters the underlying listener.
         */
        @Override
        public void close() {
            reg.unregister();
        }
    }
    
    /**
     * A read-only snapshot of registrations for a specific event topic.
     *
     * <p>A snapshot is taken at the time of creation and does not reflect subsequent
     * registration changes. It provides methods to post events to the captured
     * registrations.
     *
     * <p>Example usage:
     * <pre>{@code
     * SnapshotList<Phase, MyEvent> snapshot = bus.getSnapshot(myTopic);
     *
     * // Iterate over registrations (in phase order)
     * for (Registration<Phase> reg : snapshot) {
     *     System.out.println("Phase: " + reg.getPhase());
     * }
     *
     * // Post an event
     * snapshot.post(new MyEvent());
     * }</pre>
     *
     * @param <P> the phase type
     * @param <E> the event type
     *
     * @see EventBus#getSnapshot(EventTopic)
     * @see Registration
     */
    interface SnapshotList<P, E>
      extends List<Registration<P>>
    {
        /**
         * Posts the event to all active registrations in phase order.
         *
         * <p>Only registrations where {@link Registration#isActive()} returns {@code true}
         * receive the event. Events are delivered in ascending phase order.
         *
         * @param event the event to post
         *
         * @return the posted event
         */
        E post(final E event);
        
        /**
         * Posts the event and catches any exception thrown by listeners.
         *
         * <p>If any listener throws an exception, delivery stops and the error is returned.
         *
         * @param event the event to post
         *
         * @return an error containing the exception if one occurred, {@code null} otherwise
         */
        @Nullable EventError postAndCatch(final E event);
    }
}
