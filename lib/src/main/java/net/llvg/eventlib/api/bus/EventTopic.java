package net.llvg.eventlib.api.bus;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.var;
import net.llvg.eventlib.impl.Util;
import net.llvg.eventlib.impl.bus.ClassTopicFactory;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

/**
 * Represents an event topic (category) for the event bus system.
 *
 * <p>An EventTopic defines a category of events and can form hierarchies through
 * supertopics (parent topics). Event listeners can subscribe to topics to receive
 * events published to those topics or any of their supertopics.
 *
 * <p>The optional {@code name} argument passed to {@link #of(String)} is used in
 * {@link #toString()} to provide a readable representation:
 * {@code EventTopic[name]} when named, otherwise {@code EventTopic@<hashcode>}.
 *
 * @param <E> the event type bound to this topic
 */
@RequiredArgsConstructor (access = AccessLevel.PRIVATE)
@Getter
public final class EventTopic<E> {
    /**
     * -- GETTER --
     * Returns the optional name of this topic, used in {@link #toString()}.
     *
     * @return the name, or {@code null} if unnamed
     */
    private final @Nullable String name;
    
    /**
     * -- GETTER --
     * Returns the parent topics (supertopics) of this topic.
     *
     * @return the supertopics
     */
    private final Iterable<? extends EventTopic<? super E>> supertopics;
    
    @Override
    public String toString() {
        return name != null ? Util.format("EventTopic[{}]", name)
          : "EventTopic@" + Integer.toHexString(System.identityHashCode(this));
    }
    
    /**
     * Returns a string representation of this topic's hierarchy tree.
     *
     * <p>Uses Unicode box-drawing characters to visualize the topic hierarchy.
     * Example output:
     * <pre>{@code
     * EventTopic[parent]
     * ├─ EventTopic[child1]
     * │  ├─ EventTopic[grandchild1]
     * │  └─ EventTopic[grandchild2]
     * └─ EventTopic[child2]
     * }</pre>
     *
     * @return string representation of the topic hierarchy
     *
     * @see #dumpTreeAscii()
     */
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
    
    /**
     * Returns a string representation of this topic's hierarchy tree using ASCII characters.
     *
     * <p>Similar to {@link #dumpTree()} but uses ASCII characters instead of Unicode:
     * <pre>{@code
     * EventTopic[parent]
     * +- EventTopic[child1]
     * |  +- EventTopic[grandchild1]
     * |  \- EventTopic[grandchild2]
     * \- EventTopic[child2]
     * }</pre>
     *
     * @return string representation of the topic hierarchy
     *
     * @see #dumpTree()
     */
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
    
    /**
     * Creates an empty topic with no supertopics.
     *
     * @param <E> the event type
     *
     * @return empty topic
     */
    public static <E> EventTopic<E> of() {
        return of((String) null);
    }
    
    /**
     * Creates a named topic with no supertopics.
     *
     * @param name topic name
     * @param <E> the event type
     *
     * @return named empty topic
     */
    public static <E> EventTopic<E> of(final @Nullable String name) {
        return new EventTopic<>(name, Collections.emptyList());
    }
    
    /**
     * Creates a topic with a single supertopic.
     *
     * @param element parent topic
     * @param <E> the event type
     *
     * @return topic with one supertopic
     */
    public static <E> EventTopic<E> of(final EventTopic<? super E> element) {
        return of(null, element);
    }
    
    /**
     * Creates a named topic with a single supertopic.
     *
     * @param name topic name
     * @param element parent topic
     * @param <E> the event type
     *
     * @return named topic with one supertopic
     */
    public static <E> EventTopic<E> of(final @Nullable String name, final EventTopic<? super E> element) {
        return new EventTopic<>(name, Collections.singletonList(Util.argNotNull(element, "element")));
    }
    
    private static <E> EventTopic<E> ofTrusted(final @Nullable String name, final Set<EventTopic<? super E>> unique) {
        if (unique.size() == 1) {
            return new EventTopic<>(name, Collections.singletonList(unique.iterator().next()));
        }
        
        @SuppressWarnings ("unchecked")
        val array = (EventTopic<? super E>[]) unique.toArray(new EventTopic[0]);
        return new EventTopic<>(name, Util.asImmutableList(array));
    }
    
    @Contract ("null -> fail; !null -> param1")
    private static <T> T checkElementNotNull(final T element) {
        if (element == null) throw new NullPointerException("[elements] must not contain null.");
        return element;
    }
    
    /**
     * Creates a topic with supertopics from varargs.
     *
     * <p>Duplicate supertopics are automatically removed. The insertion order of
     * supertopics is preserved.
     *
     * @param elements parent topics
     * @param <E> the event type
     *
     * @return topic with supertopics
     *
     * @throws NullPointerException if {@code elements} is {@code null} or contains {@code null}
     */
    @SafeVarargs
    public static <E> EventTopic<E> of(final EventTopic<? super E>... elements) {
        return of(null, elements);
    }
    
    /**
     * Creates a named topic with supertopics from varargs.
     *
     * <p>Duplicate supertopics are automatically removed. The insertion order of
     * supertopics is preserved.
     *
     * @param name topic name
     * @param elements parent topics
     * @param <E> the event type
     *
     * @return named topic with supertopics
     *
     * @throws NullPointerException if {@code elements} is {@code null} or contains {@code null}
     */
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
                return ofTrusted(name, unique);
        }
    }
    
    /**
     * Creates a topic with supertopics from an iterable.
     *
     * <p>Duplicate supertopics are automatically removed. The insertion order of
     * supertopics is preserved.
     *
     * @param elements parent topics
     * @param <E> the event type
     *
     * @return topic with supertopics
     *
     * @throws NullPointerException if {@code elements} is {@code null} or contains {@code null}
     */
    public static <E> EventTopic<E> of(final Iterable<? extends EventTopic<? super E>> elements) {
        return of(null, elements);
    }
    
    /**
     * Creates a named topic with supertopics from an iterable.
     *
     * <p>Duplicate supertopics are automatically removed. The insertion order of
     * supertopics is preserved.
     *
     * @param name topic name
     * @param elements parent topics
     * @param <E> the event type
     *
     * @return named topic with supertopics
     *
     * @throws NullPointerException if {@code elements} is {@code null} or contains {@code null}
     */
    public static <E> EventTopic<E> of(final @Nullable String name, final Iterable<? extends EventTopic<? super E>> elements) {
        Util.argNotNull(elements, "elements");
        
        val it = elements.iterator();
        if (!it.hasNext()) return of(name);
        
        val first = it.next();
        if (!it.hasNext()) return of(name, first);
        
        val unique = new LinkedHashSet<EventTopic<? super E>>();
        unique.add(checkElementNotNull(first));
        while (it.hasNext()) unique.add(checkElementNotNull(it.next()));
        return ofTrusted(name, unique);
    }
    
    /**
     * Returns the EventTopic associated with the specified event class.
     *
     * <p>Creates a topic if none exists for the class. Result topics are cached.
     *
     * @param clazz the event class
     * @param <E> the event type
     *
     * @return topic for the specified class
     */
    public static <E> EventTopic<E> forClass(final Class<E> clazz) {
        return ClassTopicFactory.get(clazz);
    }
}
