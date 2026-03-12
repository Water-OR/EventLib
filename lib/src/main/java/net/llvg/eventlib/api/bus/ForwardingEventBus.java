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
    public <E> Registration<P> register(
      final EventTopic<E> topic,
      final P phase,
      final EventListener<? super E> listener
    ) {
        return delegate().register(topic, phase, listener);
    }
    
    @Override
    public @Unmodifiable <E> SnapshotList<P, E> getSnapshot(final EventTopic<E> topic) {
        return delegate().getSnapshot(topic);
    }
    
    @Override
    public String toString() {
        return "ForwardingEventBus{delegate=" + delegate() + "}";
    }
}
