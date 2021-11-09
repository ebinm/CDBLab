package de.tum.i13.server.echo;

import jdk.internal.net.http.common.Pair;

import java.util.Objects;

public class KVPair {

    private Pair<String, String> pair;
    private int counter;
    private int entry;

    public KVPair(String key, String value, int entry) {
        pair = new Pair<>(key, value);
        this.entry = entry;
        this.counter = 0;
    }

    public Pair<String, String> getPair() {
        return pair;
    }

    public void setPair(Pair<String, String> pair) {
        this.pair = pair;
    }

    public String getKey() {
        return pair.first;
    }

    public void setKey(String key) {
        pair = new Pair<>(key, pair.second);
    }

    public String getValue() {
        return pair.second;
    }

    public void setValue(String value) {
        pair = new Pair<>(pair.first, value);
    }

    public int getCounter() {
        return counter;
    }

    public int getEntry() {
        return entry;
    }

    public void increaseCounter() {
        this.counter++;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public void setEntry(int entry) {
        this.entry = entry;
    }

    @Override
    public boolean equals(Object o) {
        if (this.pair.first == o) return true;
        if (o == null || pair.first.getClass() != o.getClass()) return false;
        String key = (String) o;
        return Objects.equals(pair.first, key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pair);
    }
}
