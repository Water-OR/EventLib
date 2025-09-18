package net.llvg.eventlib;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

import static com.google.common.base.Preconditions.*;

/**
 * The builder class of {@link EventBus EventBus}.
 *
 * @param <E> Type of the events.
 * @param <L> Type of the listeners.
 *
 * @see EventBus EventBus.
 */
public final class EventBusBuilder<E, L extends Consumer<? super E>> {
    private final Class<E> baseType;
    private @Nullable ListenerWrapper<E, L> listenerWrapper = null;
    private Consumer<? super ArrayList<L>> cacheProcessor = Utility.emptyConsumer;
    
    private @Nullable FactoryLoader factoryLoader;
    
    EventBusBuilder(final Class<E> baseType) {
        this.baseType = baseType;
    }
    
    Class<E> getBaseType() {
        return baseType;
    }
    
    /**
     * Specified the {@link ListenerWrapper listener-wrapper}.
     * <br>
     * This method should be invoked exactly once.
     *
     * @param value The specified value.
     * @param <L1> The type of actual listener.
     *
     * @return {@code this} for chainable invocations.
     *
     * @throws IllegalStateException if the {@link ListenerWrapper listener-wrapper} has already been specified.
     * @throws NullPointerException  if {@code value} is {@code null}.
     */
    public <L1 extends L> EventBusBuilder<E, L1> setListenerWrapper(final ListenerWrapper<E, L1> value) {
        checkState(this.listenerWrapper == null, "[listenerWrapper] have already been specified");
        
        @SuppressWarnings ({ "rawtypes", "unchecked" })
        final EventBusBuilder<E, L1> r = (EventBusBuilder) this;
        r.listenerWrapper = checkValue(value);
        return r;
    }
    
    ListenerWrapper<E, L> getListenerWrapper() {
        checkState(listenerWrapper != null, "[listenerWrapper] must be specified");
        return listenerWrapper;
    }
    
    /**
     * Specified the cache-processor.
     *
     * @param value The specified value.
     *
     * @return {@code this} for chainable invocations.
     *
     * @throws IllegalStateException if the {@link ListenerWrapper listener-wrapper} has not been specified yet.
     * @throws NullPointerException  if {@code value} is {@code null}.
     */
    public EventBusBuilder<E, L> setCacheProcessor(final Consumer<? super ArrayList<L>> value) {
        checkState(this.listenerWrapper != null, "[listenerWrapper] must be specified first");
        this.cacheProcessor = checkValue(value);
        return this;
    }
    
    Consumer<? super ArrayList<L>> getCacheProcessor() {
        return cacheProcessor;
    }
    
    /**
     * Specified the {@link FactoryLoader factory-loader}.
     *
     * @param value The specified value.
     *
     * @return {@code this} for chainable invocations.
     *
     * @throws NullPointerException if {@code value} is {@code null}.
     */
    public EventBusBuilder<E, L> setFactoryLoader(final FactoryLoader value) {
        this.factoryLoader = checkValue(value);
        return this;
    }
    
    FactoryLoader getFactoryLoader() {
        checkState(factoryLoader != null, "[factoryLoader] must be specified");
        return factoryLoader;
    }
    
    /**
     * Creates a new {@link EventBus EventBus} instance.
     * <br>
     * Though it's not suggested, this method can be called multiple times to create multiple instances.
     *
     * @return A new {@link EventBus EventBus} instance.
     *
     * @throws IllegalStateException If some necessary properties are not specified.
     */
    public EventBus<E, L> build() {
        return new EventBusImpl<>(this);
    }
    
    private static <T> T checkValue(T value) {
        return Objects.requireNonNull(value, "[value] must not be null");
    }
}
