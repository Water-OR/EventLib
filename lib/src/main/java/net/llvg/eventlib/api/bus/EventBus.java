package net.llvg.eventlib.api.bus;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.llvg.eventlib.api.phase.PhaseManager;
import net.llvg.eventlib.impl.bus.EventBusImpl;
import org.jetbrains.annotations.ApiStatus;

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
    <E> Registration register(
      final Class<E> type,
      final P phase,
      final EventListener<? super E> listener
    );
    
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
    default <E> Registration register(
      final Class<E> type,
      final EventListener<? super E> listener
    ) {
        return register(type, getPhases().getDefaultPhase(), listener);
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
    <E> E post(final E event);
    
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
    interface Registration {
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
        private final Registration reg;
        
        /**
         * Unregisters the underlying listener.
         */
        @Override
        public void close() {
            reg.unregister();
        }
    }
}
