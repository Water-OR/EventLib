package net.llvg.eventlib.api.bus;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collections;
import java.util.LinkedHashSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.var;
import net.llvg.eventlib.impl.Util;
import net.llvg.eventlib.impl.bus.ClassTopicFactory;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor (access = AccessLevel.PRIVATE)
@Getter
public final class EventTopic<E> {
    private final @Nullable String name;
    
    private final Iterable<? extends EventTopic<? super E>> supertopics;
    
    @Override
    public String toString() {
        return name != null ? Util.format("EventTopic[{}]", name)
          : "EventTopic@" + Integer.toHexString(System.identityHashCode(this));
    }
    
    @SuppressWarnings ("unused")
    public String dumpTree() {
        return dumpTree(new StringBuilder(), "").toString();
    }
    
    @CanIgnoreReturnValue
    public StringBuilder dumpTree(final StringBuilder builder, final String indent) {
        builder.append(this).append('\n');
        val it = supertopics.iterator();
        if (it.hasNext()) {
            var next = it.next();
            while (it.hasNext()) {
                next.dumpTree(builder.append(indent).append("├─ "), indent + "│  ");
                next = it.next();
            }
            next.dumpTree(builder.append(indent).append("└─ "), indent + "   ");
        }
        return builder;
    }
    
    @SuppressWarnings ("unused")
    public String dumpTreeAscii() {
        return dumpTreeAscii(new StringBuilder(), "").toString();
    }
    
    @CanIgnoreReturnValue
    public StringBuilder dumpTreeAscii(final StringBuilder builder, final String indent) {
        builder.append(this).append('\n');
        val it = supertopics.iterator();
        if (it.hasNext()) {
            var next = it.next();
            while (it.hasNext()) {
                next.dumpTreeAscii(builder.append(indent).append("+- "), indent + "|  ");
                next = it.next();
            }
            next.dumpTreeAscii(builder.append(indent).append("\\- "), indent + "   ");
        }
        return builder;
    }
    
    public static <E> EventTopic<E> of() {
        return of((String) null);
    }
    
    public static <E> EventTopic<E> of(final @Nullable String name) {
        return new EventTopic<>(name, Collections.emptyList());
    }
    
    public static <E> EventTopic<E> of(final EventTopic<? super E> element) {
        return of(null, element);
    }
    
    public static <E> EventTopic<E> of(final @Nullable String name, final EventTopic<? super E> element) {
        return new EventTopic<>(name, Collections.singletonList(Util.argNotNull(element, "element")));
    }
    
    @SafeVarargs
    public static <E> EventTopic<E> of(final EventTopic<? super E>... elements) {
        return of(null, elements);
    }
    
    @SafeVarargs
    public static <E> EventTopic<E> of(final @Nullable String name, final EventTopic<? super E>... elements) {
        switch (Util.argNotNull(elements, "elements").length) {
            case 0:
                return of(name);
            
            case 1:
                return of(name, elements[0]);
            
            default:
                val unique = new LinkedHashSet<EventTopic<? super E>>();
                for (val element : elements) unique.add(checkElementNotNull(element));
                
                if (unique.size() == 1) {
                    return new EventTopic<>(name, Collections.singletonList(unique.iterator().next()));
                }
                
                @SuppressWarnings ("unchecked")
                val array = (EventTopic<? super E>[]) unique.toArray(new EventTopic[0]);
                return new EventTopic<>(name, Util.asImmutableList(array));
        }
    }
    
    public static <E> EventTopic<E> of(final Iterable<? extends EventTopic<? super E>> elements) {
        return of(null, elements);
    }
    
    public static <E> EventTopic<E> of(final @Nullable String name, final Iterable<? extends EventTopic<? super E>> elements) {
        Util.argNotNull(elements, "elements");
        
        val it = elements.iterator();
        if (!it.hasNext()) return of(name);
        
        val first = it.next();
        if (!it.hasNext()) return of(name, first);
        
        val unique = new LinkedHashSet<EventTopic<? super E>>();
        unique.add(checkElementNotNull(first));
        while (it.hasNext()) unique.add(checkElementNotNull(it.next()));
        
        if (unique.size() == 1) {
            return new EventTopic<>(name, Collections.singletonList(unique.iterator().next()));
        }
        
        @SuppressWarnings ("unchecked")
        val array = (EventTopic<? super E>[]) unique.toArray(new EventTopic[0]);
        return new EventTopic<>(name, Util.asImmutableList(array));
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
