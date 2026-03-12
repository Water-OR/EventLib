package net.llvg.eventlib.api.bus;

import java.util.Collections;
import java.util.LinkedHashSet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import net.llvg.eventlib.impl.Util;
import net.llvg.eventlib.impl.bus.ClassTopicFactory;
import org.jetbrains.annotations.Contract;

@RequiredArgsConstructor (access = AccessLevel.PRIVATE)
public @Value class EventTopic<E> {
    Iterable<? extends EventTopic<? super E>> superTopics;
    
    public static <E> EventTopic<E> of() {
        return new EventTopic<>(Collections.emptyList());
    }
    
    public static <E> EventTopic<E> of(final EventTopic<? super E> element) {
        return new EventTopic<>(Collections.singletonList(Util.argNotNull(element, "element")));
    }
    
    @SafeVarargs
    public static <E> EventTopic<E> of(final EventTopic<? super E>... elements) {
        switch (Util.argNotNull(elements, "elements").length) {
            case 0:
                return of();
            
            case 1:
                return of(elements[0]);
            
            default:
                val unique = new LinkedHashSet<EventTopic<? super E>>();
                for (val element : elements) unique.add(checkElementNotNull(element));
                
                if (unique.size() == 1) {
                    return new EventTopic<>(Collections.singletonList(unique.iterator().next()));
                }
                
                return new EventTopic<>(Util.asImmutableList(unique.toArray(new EventTopic[0])));
        }
    }
    
    public static <E> EventTopic<E> of(final Iterable<? extends EventTopic<? super E>> elements) {
        Util.argNotNull(elements, "elements");
        
        val it = elements.iterator();
        if (!it.hasNext()) return of();
        
        val first = it.next();
        if (!it.hasNext()) return of(first);
        
        val unique = new LinkedHashSet<EventTopic<? super E>>();
        unique.add(checkElementNotNull(first));
        while (it.hasNext()) unique.add(checkElementNotNull(it.next()));
        
        if (unique.size() == 1) {
            return new EventTopic<>(Collections.singletonList(unique.iterator().next()));
        }
        
        return new EventTopic<>(Util.asImmutableList(unique.toArray(new EventTopic[0])));
    }
    
    public static <E> EventTopic<E> ofClass(final Class<E> clazz) {
        return ClassTopicFactory.get(clazz);
    }
    
    @Contract ("null -> fail; !null -> param1")
    private static <T> T checkElementNotNull(final T element) {
        if (element == null) throw new NullPointerException("[elements] must not contain null.");
        return element;
    }
}
