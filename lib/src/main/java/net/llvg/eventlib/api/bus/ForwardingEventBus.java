package net.llvg.eventlib.api.bus;

import net.llvg.eventlib.api.phase.PhaseManager;
import org.jetbrains.annotations.Unmodifiable;

/**
 * An abstract decorator for {@link EventBus}.
 * <p>
 * This class simplifies the implementation of the Decorator pattern by forwarding all method calls
 * to a delegate {@code EventBus} instance. Users can override specific methods to add behavior
 * (e.g., logging, exception handling) without modifying the original implementation.
 *
 * @param <P> The phase type.
 */
public abstract class ForwardingEventBus<P>
  implements EventBus<P>
{
    /**
     * Returns the backing delegate instance that this bus forwards calls to.
     *
     * @return The delegate event bus.
     */
    protected abstract EventBus<P> delegate();
    
    @Override
    public PhaseManager<P> getPhases() {
        return delegate().getPhases();
    }
    
    @Override
    public <E> Registration<P> register(final Class<E> type, final P phase, final EventListener<? super E> listener) {
        return delegate().register(type, phase, listener);
    }
    
    @Override
    public <E> @Unmodifiable SnapshotList<P, E> getSnapshot(final Class<E> type) {
        return delegate().getSnapshot(type);
    }
    
    @Override
    public <E> E post(final E event) {
        return delegate().post(event);
    }
    
    @Override
    public String toString() {
        return "ForwardingEventBus{delegate=" + delegate() + "}";
    }
}
