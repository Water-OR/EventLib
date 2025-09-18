package net.llvg.eventlib;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullUnmarked;

/**
 * A factory creates {@link UncheckedListener UncheckedListener}.
 */
@FunctionalInterface
public interface ListenerFactory {
    /**
     * Creates a {@link UncheckedListener UncheckedListener} for the object.
     * For some implementation, they allow null value, others don't.
     * So the method is {@linkplain  NullUnmarked null-unmarked}.
     *
     * @param object The object for result built.
     *
     * @return A {@link UncheckedListener UncheckedListener} instance.
     */
    @NullUnmarked
    @NonNull UncheckedListener get(Object object);
    
    /**
     * Creates a {@link UncheckedListener UncheckedListener} instance for a constant.
     * No matter what value is passed, it always gives the same result.
     *
     * @param value The constant result value.
     *
     * @return A {@link UncheckedListener UncheckedListener} witch always provides the same result.
     *
     * @throws NullPointerException if {@code value} is {@code null}.
     */
    static ListenerFactory constant(final UncheckedListener value) {
        return new ListenerFactoryFromConstant(Objects.requireNonNull(value, "[value] must not be null"));
    }
}
