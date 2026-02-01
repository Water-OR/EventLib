/**
 * Contains the core interfaces and types for the EventLib system.
 * <p>
 * The main entry point is the {@link net.llvg.eventlib.api.bus.EventBus} interface, which allows
 * registering {@link net.llvg.eventlib.api.bus.EventListener}s and posting events.
 *
 * <h2>Nullability &amp; Return Values</h2>
 * This package is annotated with {@link org.jspecify.annotations.NullMarked}.
 * All parameters and return values are considered <b>non-null</b> by default unless explicitly
 * annotated with {@link org.jspecify.annotations.Nullable}.
 * <p>
 * Additionally, methods in this package are annotated with {@link com.google.errorprone.annotations.CheckReturnValue}
 * by default, meaning the result of a method call should not be ignored (e.g., when creating a new EventBus or checking status).
 *
 * @see net.llvg.eventlib.api.bus.EventBus
 */
@NullMarked
@CheckReturnValue
package net.llvg.eventlib.api.bus;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;