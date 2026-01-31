/**
 * Provides the infrastructure for managing event execution phases.
 * <p>
 * The core component of this package is the {@link net.llvg.eventlib.api.phase.PhaseManager},
 * which implements a thread-safe, topological sort algorithm to determine the order in which
 * event listeners are invoked.
 * <p>
 * Phases are nodes in a dependency graph. Users can define arbitrary phases (using Strings,
 * Enums, or custom objects) and define "happens-before" relationships between them using
 * {@link net.llvg.eventlib.api.phase.PhaseManager#link(Object, Object)}.
 *
 * <h2>Nullability & Return Values</h2>
 * This package is annotated with {@link org.jspecify.annotations.NullMarked}.
 * All parameters and return values are considered <b>non-null</b> by default unless explicitly
 * annotated with {@link org.jspecify.annotations.Nullable}.
 * <p>
 * Additionally, methods are annotated with {@link com.google.errorprone.annotations.CheckReturnValue}
 * by default to prevent accidental misuse of the API (e.g., creating a builder without building it).
 *
 * @see net.llvg.eventlib.api.phase.PhaseManager
 */
@NullMarked
@CheckReturnValue
package net.llvg.eventlib.api.phase;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;