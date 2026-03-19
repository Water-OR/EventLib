package net.llvg.eventlib.api.bus;

import lombok.Value;

/**
 * A data transfer object containing details about a failed event dispatch.
 *
 * <p>This is an immutable value object created by the event bus when a listener
 * throws an exception during event dispatch.
 *
 * @see net.llvg.eventlib.api.bus.EventBus
 */
public @Value class EventError {
    /**
     * The exception thrown by the failing listener.
     * -- GETTER --
     * Returns the exception thrown by the failing listener.
     *
     * @return the exception
     */
    Throwable exception;
    
    /**
     * The index of the failing listener within the dispatch snapshot
     * -- GETTER --
     * Returns the index of the failing listener within the dispatch snapshot.
     *
     * @return the index
     */
    int index;
    
    /**
     * The registration handle of the failing listener
     * -- GETTER --
     * Returns the registration handle of the failing listener.
     *
     * @return the registration handle
     */
    EventBus.Registration<?> registration;
}
