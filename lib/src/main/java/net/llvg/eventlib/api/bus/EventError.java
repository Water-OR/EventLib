package net.llvg.eventlib.api.bus;

import lombok.Value;

/**
 * A data transfer object containing details about a failed event dispatch.
 * <p>
 * This class captures the exact state at the moment a listener threw an exception,
 * allowing external frameworks to report or handle the error gracefully without
 * crashing the entire event bus.
 */
public @Value class EventError {
    /**
     * The exception thrown by the failing listener.
     */
    Throwable exception;
    
    /**
     * The index of the failing listener within the snapshot.
     */
    int index;
    
    /**
     * The registration handle of the failing listener.
     */
    EventBus.Registration<?> registration;
}
