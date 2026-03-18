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
 * The core event dispatching system.
 * <p>
 * An {@code EventBus} manages the registration and execution of event listeners based on
 * topological phases and class inheritance hierarchies.
 *
 * @param <P> The type used to identify execution phases (e.g., {@link String}).
 */
public interface EventBus<P> {
    /**
     * Gets the {@link PhaseManager} associated with this bus.
     * <p>
     * The manager can be used to register new phases, define ordering rules (cycles are handled automatically),
     * or inspect the current topology.
     *
     * @return The phase manager instance.
     *
     * @see PhaseManager
     */
    PhaseManager<P> getPhases();
    
    @CanIgnoreReturnValue
    <E> Registration<P> register(
      final EventTopic<E> topic,
      final P phase,
      final EventListener<? super E> listener
    );
    
    @CanIgnoreReturnValue
    @ApiStatus.NonExtendable
    default <E> Registration<P> register(
      final EventTopic<E> topic,
      final EventListener<? super E> listener
    ) {
        return register(topic, getPhases().getDefaultPhase(), listener);
    }
    
    @CanIgnoreReturnValue
    <E> @Unmodifiable SnapshotList<P, E> getSnapshot(final EventTopic<E> topic);
    
    @CanIgnoreReturnValue
    default <E> E post(final EventTopic<E> topic, final E event) {
        return getSnapshot(topic).post(event);
    }
    
    /**
     * Registers a listener for a specific event type at a specified phase.
     * <p>
     * The listener will be triggered when an event of class {@code type} (or any of its subclasses)
     * is posted.
     *
     * @param type The class of the event to listen for.
     * @param phase The phase identifier determining when this listener executes relative to others.
     * @param listener The callback to execute.
     * @param <E> The event type.
     *
     * @return A {@link Registration} handle to control or unregister the listener.
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
     * Registers a listener for a specific event type using the {@link PhaseManager#getDefaultPhase()}.
     *
     * @param type The class of the event to listen for.
     * @param listener The callback to execute.
     * @param <E> The event type.
     *
     * @return A {@link Registration} handle to control or unregister the listener.
     *
     * @see #register(Class, Object, EventListener)
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
     * Retrieves an immutable snapshot of all active registrations for the specified event type.
     * <p>
     * This method is an advanced API primarily designed for framework integrations
     * (e.g., wrapping Forge or Fabric event buses) that require index-based iteration over
     * listeners or custom exception-handling logic.
     * <p>
     * The returned {@link SnapshotList} represents a frozen state of the topological sort.
     * It is heavily optimized for both random access and direct dispatching via {@link SnapshotList#post(Object)}.
     *
     * @param type The class of the event.
     * @param <E> The event type.
     *
     * @return A read-only snapshot containing the sorted registrations.
     */
    @ApiStatus.NonExtendable
    default <E> @Unmodifiable SnapshotList<P, E> getSnapshot(final Class<E> type) {
        return getSnapshot(EventTopic.forClass(type));
    }
    
    /**
     * Dispatches an event to all registered listeners.
     * <p>
     * Execution order is determined by:
     * <ol>
     *     <li>Phase topology (sorted by {@link PhaseManager}).</li>
     *     <li>Within the same phase, the order is undefined but consistent.</li>
     * </ol>
     * This method also triggers listeners registered for superclasses or interfaces of the event type.
     *
     * @param event The event instance to dispatch.
     * @param <E> The event type.
     *
     * @return The same event instance (useful for chaining).
     */
    @CanIgnoreReturnValue
    @SuppressWarnings ("unchecked")
    default <E> E post(final E event) {
        return post(EventTopic.forClass((Class<E>) event.getClass()), event);
    }
    
    /**
     * Posts an event and catches any exception thrown by a listener.
     * <p>
     * This method is highly optimized. It returns {@code null} on a successful dispatch
     * to avoid object allocation on the hot path.
     *
     * @param event The event to dispatch.
     * @param <E> The event type.
     *
     * @return An {@link EventError} if a listener failed, or {@code null} if successful.
     */
    @CanIgnoreReturnValue
    @SuppressWarnings ("unchecked")
    default <E> @Nullable EventError postAndCatch(final E event) {
        return getSnapshot((Class<E>) event.getClass()).postAndCatch(event);
    }
    
    /**
     * Creates a new {@code EventBus} with a custom {@link PhaseManager} configuration.
     * <p>
     * Note: The EventBus implementation will attach its own cache-invalidation logic to the builder's
     * {@code onDirty} callback.
     *
     * @param phaseManagerBuilder The builder for the underlying phase manager.
     * @param <P> The phase type.
     *
     * @return A new EventBus instance.
     */
    static <P> EventBus<P> create(final PhaseManager.Builder<P> phaseManagerBuilder) {
        return EventBusImpl.create(phaseManagerBuilder);
    }
    
    /**
     * Creates a new {@code EventBus} using the natural order of the phase type.
     * <p>
     * This is a convenience factory for phases that implement {@link Comparable}.
     *
     * @param defaultPhase The default phase used when no phase is specified during registration.
     * @param <P> The phase type (must be Comparable).
     *
     * @return A new EventBus instance.
     */
    static <P extends Comparable<? super P>> EventBus<P> create(final P defaultPhase) {
        return EventBusImpl.create(PhaseManager.builderComparable(defaultPhase));
    }
    
    /**
     * A control handle for a registered listener.
     * <p>
     * This handle allows checking the registration status, toggling activity, or unregistering the listener.
     * It is <b>not</b> {@link AutoCloseable} by default to avoid IDE warnings for long-lived listeners.
     * Use {@link #asResource()} for try-with-resources support.
     */
    interface Registration<P> {
        /**
         * Gets the underlying listener associated with this registration.
         *
         * @return The event listener.
         */
        EventListener<?> getListener();
        
        /**
         * Gets the execution phase this listener is registered to.
         *
         * @return The phase identifier.
         */
        P getPhase();
        
        /**
         * Checks if the listener is still registered in the bus.
         *
         * @return {@code true} if registered, {@code false} if {@link #unregister()} has been called.
         */
        boolean isRegistered();
        
        /**
         * Permanently removes the listener from the event bus.
         * <p>
         * Once unregistered, the listener cannot be added back using this handle.
         */
        void unregister();
        
        /**
         * Temporarily enables or disables the listener without unregistering it.
         *
         * @param value {@code true} to enable, {@code false} to disable.
         */
        void setActive(final boolean value);
        
        /**
         * Checks if the listener is currently active.
         *
         * @return {@code true} if active, {@code false} if paused.
         */
        boolean isActive();
        
        /**
         * Converts this registration into an {@link AutoCloseable} resource.
         * <p>
         * Useful for temporary listeners within a {@code try-with-resources} block.
         *
         * @return A wrapper that calls {@link #unregister()} on close.
         */
        @ApiStatus.NonExtendable
        default Resource asResource() {
            return new Resource(this);
        }
    }
    
    /**
     * An {@link AutoCloseable} wrapper for {@link Registration}.
     * <p>
     * This class ensures the registration is unregistered when the resource is closed.
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
     * A specialized, unmodifiable list of event registrations representing a frozen state
     * of the event bus for a specific event type.
     * <p>
     * Besides standard {@link List} operations, it provides an optimized {@link #post(Object)}
     * method to directly dispatch an event to this specific pre-computed chain of listeners.
     *
     * @param <P> The phase type.
     * @param <E> The event type.
     */
    interface SnapshotList<P, E>
      extends List<Registration<P>>
    {
        /**
         * Dispatches the given event sequentially to all listeners within this snapshot.
         *
         * @param event The event instance to dispatch.
         *
         * @return The same event instance.
         */
        E post(final E event);
        
        /**
         * Dispatches the event and catches the first exception thrown.
         *
         * @param event The event to dispatch.
         *
         * @return An {@link EventError} containing the failure details, or {@code null}.
         */
        @Nullable EventError postAndCatch(final E event);
    }
}
