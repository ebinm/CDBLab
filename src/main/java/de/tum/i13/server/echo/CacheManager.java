package de.tum.i13.server.echo;


import java.io.FileNotFoundException;
import java.io.IOException;
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

    public synchronized String put(String key, String value) throws IOException {
        if(dataCache.contains(new KVPair(key, value, 0))) {
            updateCache(key, value);
            return "PUT_UPDATE " + key;

        } else {
            //TODO they kv is looked up in the file and if it is there it will be updated otherwise it will be added to the list
            if (file.contains(key)) {
                file.delete(key);
                add(key, value);
                return "PUT_UPDATE " + key;

            } else {
                //add kv to cache
                add(key, value);
                return "PUT_SUCCESS " + key;
            }
        }
    }

    public synchronized String get(String key) throws IOException {
        if (dataCache.contains(new KVPair(key, "", 0))) {
            int index = dataCache.indexOf(new KVPair(key, "", 0));
            KVPair kvPair = dataCache.remove(index);
            kvPair.increaseCounter();
            dataCache.add(kvPair);
            return "GET_SUCCESS " + kvPair.getKey() + " " + kvPair.getValue();
        } else if (file.contains(key)){
            KVPair kvPair = new KVPair(key, file.delete(key), timeline++);

            add(kvPair.getKey(), kvPair.getValue());
            return "GET_SUCCESS " + kvPair.getKey() + " " + kvPair.getValue();

        } else {
            return "get_error " + key;
        }
    }

    public synchronized String delete(String key) throws IOException {
        if (dataCache.contains(new KVPair(key, "", 0))) {
            dataCache.remove(new KVPair(key, "", 0));
            return "DELETE_SUCCESS " + key;

        } else if (file.contains(key)) {
            file.delete(key);
            return "DELETE_SUCCESS " + key;
        } else {
            return "DELETE_ERROR " + key;
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
        int index = dataCache.indexOf(new KVPair(key, "", 0));
        KVPair kvPair = dataCache.remove(index);
        kvPair.setValue(value);
        kvPair.increaseCounter();
        dataCache.add(kvPair);
    }

    private void add(String key, String value) throws IOException {
        if (dataCache.size() < size) {
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
    }
}
