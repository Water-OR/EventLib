package net.llvg.eventlib;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * EventBus Api.
 * <br>
 * Use {@link #builder(Class) builder(...)} to build instance
 *
 * @param <E> Type of the events.
 * @param <L> Type of the listeners.
 *
 * @see EventBusBuilder EventBusBuilder
 */
public interface EventBus<E, L extends Consumer<? super E>> {
    /**
     * Gets the {@link ListenerList} object for {@code clazz}.
     *
     * @param clazz The queried listener type.
     *
     * @return A {@link ListenerList} object.
     */
    ListenerList<E, L> getList(Class<? extends E> clazz);
    
    /**
     * Registers {@code target} to the bus.
     * <br>
     * If the target is a {@link java.lang.reflect.Method Method} and it is static,
     * it will be processed.
     * <br>
     * If the target is a {@link Class Class}, all public static methods will be processed.
     * <br>
     * Otherwise, all public non-static methods of its class will be processed with it as the instance.
     *
     * @param target The object being processed.
     *
     * @see Class#getMethods() Class.getMethods()
     * @see FactoryLoader FactoryLoader
     */
    void register(Object target);
    
    /**
     * Unregisters all listeners provided by the {@code target}.
     *
     * @param target The object being processed.
     *
     * @see #register(Object) register(...)
     */
    void unregister(Object target);
    
    /**
     * Fire the {@code event} as type {@code type}.
     * Usually, it is an alias of {@code getList(type).getCache().forEach(it -> it.accept(event));}
     *
     * @param type Type of the event being fired as.
     * @param event The event being fired. Must be convertible to {@code type}.
     *
     * @throws IllegalArgumentException if {@code event} is inconvertible to {@code type}.
     * @see Class#isInstance(Object) Class.isInstance(...)
     * @see #getList(Class) getList(...)
     * @see ListenerList#getCache() ListenerList.getCache()
     */
    default void fire(Class<? extends E> type, E event) {
        Preconditions.checkArgument(type.isInstance(event), "The event firing must be instance of %s", type);
        
        final ImmutableList<L> cache = getList(type).getCache();
        for (final L it : cache) it.accept(event);
    }
    
    /**
     * Creates an {@link EventBusBuilder EventBusBuilder} instance with {@code baseType} specified.
     *
     * @param baseType The base type {@link Class Class} of the {@link EventBus EventBus} being built,
     * all event type being fired and being listened must be convertible to it.
     * @param <E> The type of the event type of the {@link EventBus EventBus} being built.
     *
     * @return A new {@link EventBusBuilder} instance.
     *
     * @throws NullPointerException if {@code baseType} is {@code null}.
     */
    @SuppressWarnings ("unused")
    static <E> EventBusBuilder<E, Consumer<E>> builder(final Class<E> baseType) {
        return new EventBusBuilder<>(Objects.requireNonNull(baseType, "[baseType] must not be null"));
    }
}
