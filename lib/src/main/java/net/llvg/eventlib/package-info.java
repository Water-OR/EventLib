/**
 * <b>EventLib</b>: A high-performance, thread-safe event dispatching library for Java 8+.
 * <p>
 * EventLib is designed with a focus on low latency, minimal GC pressure, and flexible
 * execution ordering. It provides a robust alternative to traditional event buses by
 * incorporating topological phase sorting and class-hierarchy-based dispatching.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Topological Ordering:</b> Precise control over listener execution order using
 *       {@link net.llvg.eventlib.api.phase.PhaseManager}.</li>
 *   <li><b>High Concurrency:</b> Utilizes {@link java.util.concurrent.locks.StampedLock}
 *       with a reverse-locking strategy for maximum throughput.</li>
 *   <li><b>Zero-Allocation Dispatch:</b> Optimized internal structures ensure that event
 *       posting is nearly garbage-free on the hot path.</li>
 *   <li><b>Resource Management:</b> Seamless integration with {@code try-with-resources}
 *       via {@link net.llvg.eventlib.api.bus.EventBus.Resource}.</li>
 * </ul>
 *
 * <h2>Getting Started</h2>
 * To get started, refer to the {@link net.llvg.eventlib.api.bus.EventBus} interface to
 * create your first bus instance.
 *
 * <h2>Nullability &amp; Return Values</h2>
 * This package and its subpackages are annotated with {@link org.jspecify.annotations.NullMarked}.
 * All parameters and return values are considered <b>non-null</b> by default unless
 * explicitly annotated with {@link org.jspecify.annotations.Nullable}.
 * <p>
 * Methods are annotated with {@link com.google.errorprone.annotations.CheckReturnValue} by default,
 * meaning the result should not be ignored (e.g., when creating a new EventBus or checking status).
 */
@CheckReturnValue
@NullMarked
package net.llvg.eventlib;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;