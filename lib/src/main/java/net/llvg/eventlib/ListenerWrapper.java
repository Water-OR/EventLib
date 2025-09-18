package net.llvg.eventlib;

import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An interface for wrapping and filtering generated
 * {@link UncheckedListener UncheckedListener} to actual listeners.
 *
 * @param <E> Type of the events.
 * @param <L> Type of the listeners.
 */
public interface ListenerWrapper<E, L extends Consumer<? super E>> {
    /**
     * Wraps listeners generated from static method.
     *
     * @param type Type of the listener
     * @param listener The generated listener..
     * @param method The method where the listener generated from.
     *
     * @return An {@link Optional Optional} instance witch may contain a listener or be empty.
     */
    Optional<L> wrapStaticListener(
      Class<? extends E> type,
      UncheckedListener listener,
      Method method
    );
    
    /**
     * Wraps listeners generated from object method.
     *
     * @param type Type of the listener.
     * @param listener The generated listener.
     * @param method The method where the listener generated from.
     * @param object The object instance.
     * @param supers The similar methods in the supertypes of the object type.
     *
     * @return An {@link Optional Optional} instance witch may contain a listener or be empty.
     */
    Optional<L> wrapObjectListener(
      Class<? extends E> type,
      UncheckedListener listener,
      Method method,
      Object object,
      ImmutableSet<Method> supers
    );
    
    /**
     * Creates a {@link ListenerWrapper ListenerWrapper} instance from a mapper {@link Function Function}.
     *
     * @param mapper The mapper {@link Function}.
     * @param <E> Type of the events.
     * @param <L> Type of the listeners.
     *
     * @return A {@link ListenerWrapper ListenerWrapper} instance which wraps the listeners through {@code mapper}.
     *
     * @throws NullPointerException if {@code mapper} is {@code null}.
     */
    @SuppressWarnings ("unused")
    static <E, L extends Consumer<E>> ListenerWrapper<E, L> from(final Function<UncheckedListener, L> mapper) {
        return new ListenerWrapperFromFunction<>(Objects.requireNonNull(mapper, "[mapper] must not be null"));
    }
}
