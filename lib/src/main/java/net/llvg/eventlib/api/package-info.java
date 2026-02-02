/**
 * Root package for the EventLib API.
 * <p>
 * This package serves as the namespace root and does not contain any functional classes directly.
 * The library functionality is organized into the following sub-packages:
 *
 * <ul>
 *   <li>{@link net.llvg.eventlib.api.bus} - <b>Core Event System</b>: Contains the {@code EventBus},
 *       {@code EventListener}, and registration logic. Start here!</li>
 *   <li>{@link net.llvg.eventlib.api.phase} - <b>Phase Management</b>: Contains the {@code PhaseManager}
 *       for defining topological execution order and handling cycle dependencies.</li>
 * </ul>
 *
 * <h2>Usage Entry Points</h2>
 * Most users will interact primarily with:
 * <ul>
 *   <li>{@link net.llvg.eventlib.api.bus.EventBus#create(java.lang.Comparable)} - To create a simple bus.</li>
 *   <li>{@link net.llvg.eventlib.api.bus.EventBus#create(net.llvg.eventlib.api.phase.PhaseManager.Builder)}
 *       - To create a bus with custom phase logic.</li>
 * </ul>
 */
@CheckReturnValue
@NullMarked
package net.llvg.eventlib.api;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;