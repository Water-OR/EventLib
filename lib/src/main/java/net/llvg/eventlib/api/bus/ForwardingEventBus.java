package net.llvg.eventlib.api.bus;

import net.llvg.eventlib.api.phase.PhaseManager;
import org.jetbrains.annotations.Unmodifiable;

/**
 * An abstract decorator for {@link EventBus} that forwards all operations to a delegate instance.
 *
 * @param <P> The phase type.
 * @see EventBus
 */
public abstract class ForwardingEventBus<P> implements EventBus<P> {
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
      EventTopic<E> topic,
      P phase,
      EventListener<? super E> listener
    ) {
        return delegate().register(topic, phase, listener);
    }

    @Override
    public @Unmodifiable <E> SnapshotList<P, E> getSnapshot(EventTopic<E> topic) {
        return delegate().getSnapshot(topic);
    }

    @Override
    public String toString() {
        return "ForwardingEventBus{delegate=" + delegate() + "}";
    }
}
