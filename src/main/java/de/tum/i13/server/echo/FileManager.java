package de.tum.i13.server.echo;

import java.nio.file.Path;

public class FileManager {

    Path directionary;
    public FileManager(Path dataDir) {
        this.directionary = dataDir;
    }

    //put kv into file
    public String put(String key, String value) {
        return null;
    }

    //return only the value
    public String get(String key) {
        return null;
    }

    //delete the kv pair from the file
    public String delete(String key) {
        return null;
    }

    public boolean contains(String key) {
        return true;
    }
}
