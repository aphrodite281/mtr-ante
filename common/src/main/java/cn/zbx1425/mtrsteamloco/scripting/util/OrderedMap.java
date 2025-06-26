package cn.zbx1425.mtrsteamloco.scripting.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collection;

public class OrderedMap<K, V> implements Map<K, V> {
    private HashMap<K, V> valueMap;

    private List<K> upsideList;
    private List<K> midpointList;
    private List<K> downsideList;

    public OrderedMap() {
        valueMap = new LinkedHashMap<>();
        upsideList = new ArrayList<>();
        midpointList = new ArrayList<>();
        downsideList = new ArrayList<>();
    }

    public OrderedMap(Map<? extends K, ? extends V> m) {
        this();
        putAll(m);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        putAll(m, PlacementOrder.CENTRAL);
    }

    public void putAll(Map<? extends K, ? extends V> m, PlacementOrder order) {
        if (m instanceof OrderedMap) {
            OrderedMap<K, V> other = (OrderedMap<K, V>) m;
            for (K key : other.upsideList) {
                put(key, other.valueMap.get(key), PlacementOrder.UPPER);
            }
            for (K key : other.midpointList) {
                put(key, other.valueMap.get(key), PlacementOrder.CENTRAL);
            }
            for (K key : other.downsideList) {
                put(key, other.valueMap.get(key), PlacementOrder.LOWER);
            }
        } else {
            for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
                put(entry.getKey(), entry.getValue(), order);
            }
        }
    }

    @Override
    public V put(K key, V value) {
        return put(key, value, PlacementOrder.CENTRAL);
    }

    public V put(K key, V value, PlacementOrder order) {
        switch (order) {
            case UPPER:
                if (!upsideList.contains(key)) upsideList.add(key);
                break;
            case CENTRAL:
                if (!midpointList.contains(key)) midpointList.add(key);
                break;
            case LOWER:
                if (!downsideList.contains(key)) downsideList.add(key);
                break;
        }
        V oldValue = valueMap.get(key);
        valueMap.put(key, value);
        return oldValue;
    }

    @Override
    public int size() {
        return valueMap.size();
    }

    @Override
    public boolean isEmpty() {
        return valueMap.isEmpty();
    }
    
    @Override
    public boolean containsKey(Object key) {
        return valueMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return valueMap.containsValue(value);
    }

    @Override
    public V remove(Object key) {
        if (upsideList.contains(key)) {
            int index = upsideList.indexOf(key);
            upsideList.remove(index);
        }else if(midpointList.contains(key)) {
            int index = midpointList.indexOf(key);
            midpointList.remove(index);
        }else if(downsideList.contains(key)) {
            int index = downsideList.indexOf(key);
            downsideList.remove(index);
        }
        return valueMap.remove(key);
    }

    @Override
    public V get(Object key) {
        return valueMap.get(key);
    }

    @Override
    public void clear() {
        valueMap.clear();
        upsideList.clear();
        midpointList.clear();
        downsideList.clear();
    }

    public List<Map.Entry<K, V>> entryList() {
        List<Map.Entry<K, V>> entryList = new ArrayList<>();
        for (K key : upsideList) {
            entryList.add(new Entry<>(key, valueMap.get(key)));
        }
        for (K key : midpointList) {
            entryList.add(new Entry<>(key, valueMap.get(key)));
        }
        for (K key : downsideList) {
            entryList.add(new Entry<>(key, valueMap.get(key)));
        }
        return entryList;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        List<Map.Entry<K, V>> entryList = entryList();
        return new LinkedHashSet<>(entryList);
    }

    @Override
    public Set<K> keySet() {
        List<K> keyList = new ArrayList<>();
        keyList.addAll(upsideList);
        keyList.addAll(midpointList);
        keyList.addAll(downsideList);
        return new LinkedHashSet<>(keyList);
    }

    @Override
    public Collection<V> values() {
        return new ArrayList<>(valueMap.values());
    }

    public enum PlacementOrder {
        UPPER,
        CENTRAL,
        LOWER
    }

    public class Entry<K, V> implements Map.Entry<K, V> {
        private K key;
        private V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

    }
}
