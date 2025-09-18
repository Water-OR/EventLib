package net.llvg.eventlib;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

abstract class AbstractFactoryLoader
  implements FactoryLoader
{
    protected static final MethodType typeUncheckedListenerStaticNew = MethodType.methodType(UncheckedListener.class);
    protected static final MethodType typeUncheckedListenerObjectNew = typeUncheckedListenerStaticNew.appendParameterTypes(Object.class);
    
    private final Map<Method, ListenerFactory> cache = new ConcurrentHashMap<>();
    private final Function<? super Method, ? extends ListenerFactory> builder = method -> {
        if (Modifier.isStatic(method.getModifiers())) {
            final MethodHandle handle = buildStatic(method).asType(typeUncheckedListenerStaticNew);
            
            final UncheckedListener resultTypeless;
            try {
                resultTypeless = (UncheckedListener) handle.invokeExact();
            } catch (Throwable e) {
                throw new IllegalStateException("Failure occur while constructing Consumer", e);
            }
            
            return ListenerFactory.constant(resultTypeless);
        } else {
            return new ObjectListenerFactory(buildObject(method).asType(typeUncheckedListenerObjectNew));
        }
    };
    
    @Override
    public ListenerFactory get(final Method method) {
        return cache.computeIfAbsent(method, builder);
    }
    
    protected abstract MethodHandle buildStatic(Method method);
    
    protected abstract MethodHandle buildObject(Method method);
    
    private static final class ObjectListenerFactory
      implements ListenerFactory
    {
        final MethodHandle handle;
        
        private ObjectListenerFactory(final MethodHandle handle) {
            this.handle = handle;
        }
        
        @Override
        public UncheckedListener get(@Nullable Object target) {
            try {
                return (UncheckedListener) handle.invokeExact(target);
            } catch (Throwable e) {
                throw new RuntimeException("Failure occur while instantiating listener", e);
            }
        }
    }
}
