package net.llvg.eventlib;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

final class LambdaFactoryLoader
  extends AbstractFactoryLoader
{
    private static final MethodHandles.Lookup defaultLookup = MethodHandles.lookup();
    
    private static final String classUncheckedListenerSAMName;
    private static final MethodType classUncheckedListenerSAMType;
    
    static {
        final Method m = Utility.findSAM(UncheckedListener.class);
        classUncheckedListenerSAMName = m.getName();
        classUncheckedListenerSAMType = Utility.methodTypeFromMethod(m);
    }
    
    private final MethodHandles.Lookup lookup;
    
    LambdaFactoryLoader(final MethodHandles.Lookup lookup) {
        this.lookup = lookup;
    }
    
    LambdaFactoryLoader() {
        this(defaultLookup);
    }
    
    @Override
    protected MethodHandle buildStatic(final Method method) {
        return buildFactory(
          method,
          typeUncheckedListenerStaticNew
        ).getTarget();
    }
    
    @Override
    protected MethodHandle buildObject(final Method method) {
        return buildFactory(
          method,
          typeUncheckedListenerObjectNew.changeParameterType(0, method.getDeclaringClass())
        ).getTarget();
    }
    
    private CallSite buildFactory(
      final Method method,
      final MethodType factoryNewType
    ) {
        final MethodHandle handle;
        method.setAccessible(true);
        try {
            handle = lookup.unreflect(method);
        } catch (IllegalAccessException e1) {
            throw new AssertionError("Method should be accessible", e1);
        }
        
        final MethodType handleType = Utility.methodTypeFromMethod(method);
        try {
            return LambdaMetafactory.metafactory(
              lookup,
              classUncheckedListenerSAMName,
              factoryNewType,
              classUncheckedListenerSAMType,
              handle,
              handleType
            );
        } catch (LambdaConversionException e) {
            throw new IllegalStateException("Failure occur while building Function", e);
        }
    }
}
