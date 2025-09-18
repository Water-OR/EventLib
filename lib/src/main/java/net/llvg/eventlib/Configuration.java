package net.llvg.eventlib;

import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Configuration of eventlib.
 *
 * @param <T> Type of the property value
 */
public final class Configuration<T> {
    /**
     * A {@link String} property {@code net.llvg.eventlib.asmGenExportLocation}
     */
    public static final Configuration<String> asmGenExportLocation = new Configuration<>("net.llvg.eventlib.asmGenExportLocation", Init.string);
    
    /**
     * The property key
     */
    public final String property;
    
    private volatile @Nullable T value;
    
    private Configuration(final String property, final Init<? extends T> init) {
        this.property = property;
        this.value = init.apply(property);
    }
    
    /**
     * Sets the property value to {@code value}.
     *
     * @param value The object being set.
     */
    public void set(final @Nullable T value) {
        this.value = value;
    }
    
    /**
     * Gets the property value.
     *
     * @return The property value.
     */
    public @Nullable T get() {
        return value;
    }
    
    /**
     * Gets the property value, or fallback to {@code fallback} if the property value is not specified.
     *
     * @param fallback The fallback value being returned when no property value is specified.
     *
     * @return The property value, or {@code fallback} if no property value is specified.
     */
    @SuppressWarnings ("unused")
    public T get(T fallback) {
        final T r;
        if ((r = value) != null) return r;
        return fallback;
    }
    
    private interface Init<T>
      extends Function<String, T>
    {
        Init<String> string = System::getProperty;
    }
}
