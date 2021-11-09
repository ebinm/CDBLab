package de.tum.i13.server.echo;

import jdk.internal.net.http.common.Pair;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class CacheManager {

    private List<KVPair> dataCache;
    private int size;
    private String strategy;
    private int timeline;
    private FileManager file;

    public CacheManager(int size, String strategy, Path dataDir) {

        if (!strategy.equals("FIFO") && !strategy.equals("LRU") && !strategy.equals("LFU")) {
            throw new RuntimeException("Cache display strategy is incorrect!");
        }

        this.size = size;
        this.strategy = strategy;
        dataCache = new LinkedList<>();
        this.timeline = 0;
        this.file = new FileManager(dataDir);
    }

    public synchronized String put(String key, String value) {
        if(dataCache.contains(key)) {
            updateCache(key, value);
            return "put_update " + key;

        } else {
            //TODO they kv is looked up in the file and if it is there it will be updated otherwise it will be added to the list
            if (file.contains(key)) {
                file.delete(key);
                dataCache.add(new KVPair(key, value, timeline++));
                return "put_success " + key;

            } else {
                //add kv to cache
                if (dataCache.size() <= size) {
                    dataCache.add(new KVPair(key, value, timeline++));

                } else {
                    int indexToRemove;
                    switch (strategy) {
                        case "FIFO":
                            indexToRemove = removeFIFO();
                            break;
                        case "LRU":
                            indexToRemove = removeLRU();
                            break;
                        case "LFU":
                            indexToRemove = removeLFU();
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + strategy);
                    }
                    KVPair kvPair = dataCache.remove(indexToRemove);
                    file.put(kvPair.getKey(), kvPair.getValue());
                    dataCache.add(new KVPair(key, value, timeline++));
                }
                return "put_success " + key;
            }
        }
    }

    public synchronized String get(String key) {
        if (dataCache.contains(key)) {
            int index = dataCache.indexOf(key);
            KVPair kvPair = dataCache.remove(index);
            kvPair.increaseCounter();
            dataCache.add(kvPair);
            return "get_success " + kvPair.getKey() + " " + kvPair.getValue();
        } else if (file.contains(key)){
            KVPair kvPair = new KVPair(key, file.delete(key), timeline++);

            if (dataCache.size() <= size) {
                dataCache.add(kvPair);
                return "get_success " + kvPair.getKey() + " " + kvPair.getValue();
            } else {

                int indexToRemove;
                switch (strategy) {
                    case "FIFO":
                        indexToRemove = removeFIFO();
                        break;
                    case "LRU":
                        indexToRemove = removeLRU();
                        break;
                    case "LFU":
                        indexToRemove = removeLFU();
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + strategy);
                }
                KVPair kvPairOld = dataCache.remove(indexToRemove);
                file.put(kvPairOld.getKey(), kvPairOld.getValue());
                dataCache.add(kvPair);
                return "get_success " + kvPair.getKey() + " " + kvPair.getValue();
            }

        } else {
            return "get_error " + key;
        }
    }

    public synchronized String delete(String key) {
        if (dataCache.contains(key)) {
            dataCache.remove(key);
            return "delete_success " + key;

        } else if (file.contains(key)) {
            file.delete(key);
            return "delete_success " + key;
        } else {
            return "delete_error " + key;
        }
    }

    private int removeFIFO() {
        return dataCache.indexOf(dataCache.stream().min((o1, o2) -> {
            if (o1.getEntry() == o2.getEntry()) {
                return 0;
            } else if (o1.getEntry() < o2.getEntry()) {
                return -1;
            } else if (o1.getEntry() > o2.getEntry()) {
                return 1;
            } else throw new RuntimeException("Error in removeFIFO!");
        }).get());
    }

    private int removeLRU() {
        return size-1;
    }

    private int removeLFU() {
        return dataCache.indexOf(dataCache.stream().min((o1, o2) -> {
            if (o1.getCounter() == o2.getCounter()) {
                if (o1.getEntry() < o2.getEntry()) {
                    return -1;
                } else {
                    return 1;
                }
            } else if (o1.getCounter() < o2.getCounter()) {
                return -1;
            } else if (o1.getCounter() > o2.getCounter()) {
                return 1;
            } else throw new RuntimeException("Error in removeLFU!");
        }).get());
    }

    private void updateCache(String key, String value) {
        int index = dataCache.indexOf(key);
        KVPair kvPair = dataCache.remove(index);
        kvPair.setValue(value);
        kvPair.increaseCounter();
        dataCache.add(kvPair);
    }
}
