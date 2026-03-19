package net.llvg.eventlib.impl.bus;

import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import lombok.experimental.UtilityClass;
import lombok.val;
import lombok.var;
import net.llvg.eventlib.api.bus.EventTopic;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@UtilityClass
public final class ClassTopicFactory {
    // simply prevents ClassLoader memory leak with WeakHashMap
    private final Map<Class<?>, EventTopic<Object>> cache = new WeakHashMap<>();
    
    // do not call directly without using synchronized
    private EventTopic<Object> getUnchecked(final Class<?> clazz) {
        var r = cache.get(clazz);
        if (r != null) return r;
        
        val builder = new HashSet<EventTopic<Object>>();
        
        val superclass = clazz.getSuperclass();
        if (superclass != null) builder.add(getUnchecked(superclass));
        
        val interfaces = clazz.getInterfaces();
        for (val it : interfaces) builder.add(getUnchecked(it));
        
        r = EventTopic.of(builder);
        cache.put(clazz, r);
        return r;
    }
    
    @SuppressWarnings ("unchecked")
    public synchronized <E> EventTopic<E> get(final Class<E> clazz) {
        return (EventTopic<E>) getUnchecked(clazz);
    }
}
