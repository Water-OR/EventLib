package net.llvg.eventlib.impl;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Arrays;
import lombok.experimental.UtilityClass;
import lombok.val;
import lombok.var;
import org.jetbrains.annotations.Contract;
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
}
