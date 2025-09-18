package net.llvg.eventlib;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * A factory creates {@link ListenerFactory ListenerFactory} with {@link Method Method} as its key.
 * <br>
 * Default implementations will cache the result to improve performance.
 * And the result of passing method not exactly one {@linkplain Object object-type} argument is undefined.
 *
 * @see #asmGen() asmGen()
 * @see #lambda() lambda()
 * @see ListenerFactory
 */
@FunctionalInterface
public interface FactoryLoader {
    /**
     * Creates a {@link ListenerFactory ListenerFactory} for {@link Method method}.
     *
     * @param method The method for {@link ListenerFactory ListenerFactory} built.
     *
     * @return A {@link ListenerFactory ListenerFactory} instance.
     */
    ListenerFactory get(Method method);
    
    /**
     * Creates a {@link ListenerFactory ListenerFactory},
     * which uses <a href="https://asm.ow2.io/">ASM</a> generation in built.
     *
     * @return a {@link FactoryLoader FactoryLoader} instance.
     *
     * @see ASMGenFactoryLoader ASMGenFactoryLoader
     * @see ASMGenFactoryLoader#ASMGenFactoryLoader() ASMGenFactoryLoader.new()
     */
    @SuppressWarnings ("unused")
    static FactoryLoader asmGen() {
        return new ASMGenFactoryLoader();
    }
    
    /**
     * Creates a {@link ListenerFactory ListenerFactory},
     * which uses <a href="https://asm.ow2.io/">ASM</a> generation in built.
     *
     * @param lookup The {@link MethodHandles.Lookup Lookup} object passing to the instance.
     *
     * @return a {@link FactoryLoader FactoryLoader} instance.
     *
     * @see ASMGenFactoryLoader ASMGenFactoryLoader
     * @see ASMGenFactoryLoader#ASMGenFactoryLoader(MethodHandles.Lookup) ASMGenFactoryLoader.new(...)
     */
    @SuppressWarnings ("unused")
    static FactoryLoader asmGen(final MethodHandles.Lookup lookup) {
        return new ASMGenFactoryLoader(Objects.requireNonNull(lookup, "[lookup] must not be null"));
    }
    
    /**
     * Creates a {@link ListenerFactory ListenerFactory},
     * which uses {@link java.lang.invoke.LambdaMetafactory LambdaMetafactory} in built.
     *
     * @return a {@link FactoryLoader FactoryLoader} instance.
     *
     * @throws NullPointerException if {@code lookup} is {@code null}.
     * @see LambdaFactoryLoader LambdaFactoryLoader
     * @see LambdaFactoryLoader#LambdaFactoryLoader() LambdaFactoryLoader.new()
     */
    @SuppressWarnings ("unused")
    static FactoryLoader lambda() {
        return new LambdaFactoryLoader();
    }
    
    /**
     * Creates a {@link ListenerFactory ListenerFactory},
     * which uses {@link java.lang.invoke.LambdaMetafactory LambdaMetafactory} in built.
     *
     * @param lookup The {@link MethodHandles.Lookup Lookup} object passing to the instance.
     *
     * @return a {@link FactoryLoader FactoryLoader} instance.
     *
     * @throws NullPointerException if {@code lookup} is {@code null}.
     * @see LambdaFactoryLoader LambdaFactoryLoader
     * @see LambdaFactoryLoader#LambdaFactoryLoader() LambdaFactoryLoader.new()
     */
    @SuppressWarnings ("unused")
    static FactoryLoader lambda(final MethodHandles.Lookup lookup) {
        return new LambdaFactoryLoader(Objects.requireNonNull(lookup, "[lookup] must not be null"));
    }
}
