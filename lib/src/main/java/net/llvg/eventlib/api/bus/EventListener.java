package net.llvg.eventlib.api.bus;

@FunctionalInterface
public interface EventListener<E> {
    void invoke(final E event);
}
