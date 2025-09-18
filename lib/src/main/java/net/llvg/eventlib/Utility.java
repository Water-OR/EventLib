package net.llvg.eventlib;

import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

import static com.google.common.base.Preconditions.*;

final class Utility {
    private Utility() {
        throw new UnsupportedOperationException();
    }
    
    @SuppressWarnings ("SameParameterValue")
    static Method findSAM(final Class<?> clazz) {
        checkArgument(clazz.isInterface(), "[clazz] (%s) must be an interface", clazz.getName());
        
        Method result = null;
        for (final Method method : clazz.getMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) continue;
            checkState(result == null, "[clazz] (%s) has more than one abstract methods", clazz.getName());
            result = method;
        }
        
        checkState(result != null, "[clazz] (%s) has no abstract methods", clazz.getName());
        return result;
    }
    
    static MethodType methodTypeFromMethod(final Method method) {
        return MethodType.methodType(method.getReturnType(), method.getParameterTypes());
    }
    
    static final Consumer<@Nullable Object> emptyConsumer = ignored -> { };
}
