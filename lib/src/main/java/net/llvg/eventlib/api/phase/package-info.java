/**
 * Provides the infrastructure for managing event execution phases.
 *
 * <p>The core component of this package is the {@link net.llvg.eventlib.api.phase.PhaseManager},
 * which implements a thread-safe, topological sort algorithm to determine the order in which
 * event listeners are invoked.
 *
 * <p>Phases are nodes in a dependency graph. Users can define arbitrary phases (using Strings,
 * Enums, or custom objects) and define ordering relationships between them using
 * {@link net.llvg.eventlib.api.phase.PhaseManager#link(Object, Object)}.
 *
 * <h2>Nullability &amp; Return Values</h2>
 * See {@link net.llvg.eventlib package} for common annotation documentation.
 *
 * @see net.llvg.eventlib.api.phase.PhaseManager
 */
@NullMarked
@CheckReturnValue
package net.llvg.eventlib.api.phase;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;