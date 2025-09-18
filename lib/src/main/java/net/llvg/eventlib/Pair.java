package net.llvg.eventlib;

final class Pair<K, V> {
    private final K key;
    private final V value;
    
    private Pair(final K key, final V value) {
        this.key = key;
        this.value = value;
    }
    
    static <K, V> Pair<K, V> of(final K key, final V value) {
        return new Pair<>(key, value);
    }
    
    K getKey() {
        return key;
    }
    
    V getValue() {
        return value;
    }
}
