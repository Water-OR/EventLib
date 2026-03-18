package net.llvg.eventlib.impl.bus;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;
import lombok.val;
import lombok.var;
import net.llvg.eventlib.api.bus.EventTopic;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@UtilityClass
public final class ClassTopicFactory {
    private final ConcurrentHashMap<Class<?>, EventTopic<Object>> cache = new ConcurrentHashMap<>();
    
    private EventTopic<Object> getUnchecked(final Class<?> clazz) {
        var r = cache.get(clazz);
        if (r != null) return r;
        
        val builder = new HashSet<EventTopic<Object>>();
        
        val superclass = clazz.getSuperclass();
        if (superclass != null) builder.add(getUnchecked(superclass));
        
        val interfaces = clazz.getInterfaces();
        for (val it : interfaces) builder.add(getUnchecked(it));
        
        r = EventTopic.of(builder);
        val o = cache.putIfAbsent(clazz, r);
        return o != null ? o : r;
    }
    
    @SuppressWarnings ("unchecked")
    public <E> EventTopic<E> get(final Class<E> clazz) {
        return (EventTopic<E>) getUnchecked(clazz);
    }
}
