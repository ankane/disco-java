package org.ankane.disco;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class IdMap<T> {
    private Map<T, Integer> map;
    private List<T> vec;

    IdMap() {
        this.map = new HashMap<>();
        this.vec = new ArrayList<>();
    }

    public int add(T id) {
        int v = this.vec.size();
        Integer i = this.map.putIfAbsent(id, v);
        if (i == null) {
            this.vec.add(id);
            return v;
        } else {
            return i;
        }
    }

    public Optional<Integer> get(T id) {
        return Optional.ofNullable(this.map.get(id));
    }

    public T lookup(int index) {
        return this.vec.get(index);
    }

    public int size() {
        return this.vec.size();
    }

    public List<T> ids() {
        return Collections.unmodifiableList(this.vec);
    }
}
