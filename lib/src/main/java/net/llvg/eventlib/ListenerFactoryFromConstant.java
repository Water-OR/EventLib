package net.llvg.eventlib;

import org.jspecify.annotations.Nullable;

final class ListenerFactoryFromConstant
  implements ListenerFactory
{
    final UncheckedListener value;
    
    ListenerFactoryFromConstant(final UncheckedListener value) {
        this.value = value;
    }
    
    @Override
    public UncheckedListener get(final @Nullable Object object) {
        return value;
    }
    
    @Override
    public String toString() {
        return String.format("ListenerFactor{ from: %s }", value);
    }
}
