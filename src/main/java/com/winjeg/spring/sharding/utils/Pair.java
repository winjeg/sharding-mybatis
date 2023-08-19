package com.winjeg.spring.sharding.utils;

/**
 * @param <K> 类型1
 * @param <V> 类型2
 * @author winjeg
 */
public class Pair<K, V> {

    private final K k;
    private final V v;

    public Pair(K k, V v) {
        this.k = k;
        this.v = v;
    }

    public K left() {
        return k;
    }

    public V right() {
        return v;
    }

    public static <K, V> Pair<K, V> of(K k, V v) {
        return new Pair<>(k, v);
    }
}
