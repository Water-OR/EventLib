/**
 * Contains the core interfaces and types for the EventLib system.
 *
 * <p>The main entry point is the {@link net.llvg.eventlib.api.bus.EventBus} interface, which allows
 * registering {@link net.llvg.eventlib.api.bus.EventListener}s and posting events. Related types include
 * {@link net.llvg.eventlib.api.bus.EventTopic} for topic definition and
 * {@link net.llvg.eventlib.api.bus.EventBus.Registration} for
 * listener lifecycle management.
 *
 * <h2>Nullability &amp; Return Values</h2>
 * See {@link net.llvg.eventlib package} for common annotation documentation.
 *
 * @see net.llvg.eventlib.api.bus.EventBus
 * @see net.llvg.eventlib.api.bus.EventTopic
 * @see net.llvg.eventlib.api.bus.EventBus.Registration
 */
@NullMarked
@CheckReturnValue
package net.llvg.eventlib.api.bus;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;