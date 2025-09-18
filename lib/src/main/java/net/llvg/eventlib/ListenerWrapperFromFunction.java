package net.llvg.eventlib;

import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

final class ListenerWrapperFromFunction<E, L extends Consumer<? super E>>
  implements ListenerWrapper<E, L>
{
    private final Function<UncheckedListener, L> function;
    
    ListenerWrapperFromFunction(final Function<UncheckedListener, L> function) {
        this.function = function;
    }
    
    @Override
    public Optional<L> wrapStaticListener(
      final Class<? extends E> type,
      final UncheckedListener listener,
      final Method method
    ) {
        return Optional.of(function.apply(listener));
    }
    
    @Override
    public Optional<L> wrapObjectListener(
      final Class<? extends E> type,
      final UncheckedListener listener,
      final Method method,
      final Object object,
      final ImmutableSet<Method> supers
    ) {
        return Optional.of(function.apply(listener));
    }
    
    @Override
    public String toString() {
        return String.format("ListenerWrapper{ from: %s }", function);
    }
}
