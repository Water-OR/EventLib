package net.llvg.eventlib.impl;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.ImmutableTypeParameter;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.RandomAccess;
import java.util.Spliterator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import lombok.var;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

@UtilityClass
public final class Util {
    @Contract ("false, _, _ -> fail")
    public void check(final boolean condition, final String message, final @Nullable Object... args) {
        if (!condition) throw new IllegalStateException(format(message, args));
    }
    
    @CanIgnoreReturnValue
    @Contract ("null, _, _ -> fail; !null, _, _ -> param1")
    public <T> T checkNotNull(final @Nullable T value, final String message, final @Nullable Object... args) {
        if (value == null) throw new IllegalStateException(format(message, args));
        return value;
    }
    
    @CanIgnoreReturnValue
    @Contract ("null, _ -> fail; !null, _ -> param1")
    public <T> T argNotNull(final @Nullable T value, final String name) {
        if (value == null) throw new NullPointerException(format("[{}] must not be null.", name));
        return value;
    }
    
    @CheckReturnValue
    public String format(final String format, final @Nullable Object... args) {
        if (format.isEmpty()) return format;
        
        val size = format.length();
        val builder = new StringBuilder(size);
        
        var prevIndex = 0;
        var currIndex = 1;
        
        val it = Arrays.asList(args).iterator();
        
        if (it.hasNext()) for (; currIndex < size; ++currIndex) {
            if (
              currIndex + 2 < size &&
              format.charAt(currIndex) == '{' &&
              format.charAt(currIndex - 1) == '{' &&
              format.charAt(currIndex + 1) == '}' &&
              format.charAt(currIndex + 2) == '}'
            ) {
                builder.append(format, prevIndex, currIndex - 1).append("{}");
                prevIndex = (currIndex += 3);
                continue;
            }
            
            if (format.charAt(currIndex) == '}') {
                if (format.charAt(currIndex - 1) == '{') {
                    builder.append(format, prevIndex, currIndex - 1).append(it.next());
                    prevIndex = (++currIndex);
                    
                    if (it.hasNext()) continue;
                    
                    ++currIndex;
                    break;
                } else {
                    ++currIndex;
                }
            }
        }
        
        for (; currIndex < size; ++currIndex) {
            if (
              currIndex + 2 < size &&
              format.charAt(currIndex) == '{' &&
              format.charAt(currIndex - 1) == '{' &&
              format.charAt(currIndex + 1) == '}' &&
              format.charAt(currIndex + 2) == '}'
            ) {
                builder.append(format, prevIndex, currIndex - 1).append("{}");
                prevIndex = (currIndex += 3);
            }
        }
        
        return builder.append(format, prevIndex, size).toString();
    }
    
    private final Field arrayOfArrayList;
    
    static {
        val classArrayList = ArrayList.class;
        try {
            arrayOfArrayList = classArrayList.getDeclaredField("elementData");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        arrayOfArrayList.setAccessible(true);
    }
    
    @SneakyThrows (Throwable.class)
    public int copy(final ArrayList<?> src, final Object[] dest, final int offset) {
        val size = src.size();
        val array = (Object[]) arrayOfArrayList.get(src);
        System.arraycopy(array, 0, dest, offset, size);
        return offset + size;
    }
    
    @SafeVarargs
    public <E> @UnmodifiableView List<E> asImmutableList(final E... elements) {
        return new ImmutableArrayAsList<>(argNotNull(elements, "elements"));
    }
}

@Immutable
@RequiredArgsConstructor
final class ImmutableArrayAsList<@ImmutableTypeParameter E>
  extends AbstractList<E>
  implements List<E>, RandomAccess, Serializable
{
    private static final long serialVersionUID = 0L;
    
    private final E[] array;
    
    @Override
    public boolean isEmpty() {
        return array.length == 0;
    }
    
    @Override
    public E get(final int index) {
        return array[index];
    }
    
    @Override
    public int size() {
        return array.length;
    }
    
    @NullUnmarked
    @Override
    public Object @NotNull [] toArray() {
        return Arrays.copyOf(array, array.length);
    }
    
    @NullUnmarked
    @SuppressWarnings ("all")
    @Override
    public <T> T @NotNull [] toArray(final T @NotNull [] a) {
        val size = array.length;
        if (a.length < size) {
            return (T[]) Arrays.copyOf(array, size, a.getClass());
        }
        
        System.arraycopy(array, 0, a, 0, size);
        if (a.length > size) a[size] = null;
        
        return a;
    }
    
    @NullUnmarked
    @Override
    public int indexOf(final Object o) {
        if (o != null) {
            val a = array;
            for (int i = 0, size = a.length; i < size; ++i) {
                if (o.equals(a[i])) return i;
            }
        }
        
        return -1;
    }
    
    @NullUnmarked
    @Override
    public int lastIndexOf(final Object o) {
        if (o != null) {
            val a = array;
            for (int i = a.length - 1; i >= 0; --i) {
                if (o.equals(a[i])) return i;
            }
        }
        return -1;
    }
    
    @Override
    public Spliterator<E> spliterator() {
        return Arrays.spliterator(array);
    }
}

