package de.tum.i13.server.echo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileManager {

    Path directionary;
    File file;

    public FileManager(Path dataDir, String fileName) {

        this.directionary = dataDir;
        try {
            file = Files.createFile(directionary.resolve(fileName)).toFile();
        } catch (IOException e) {
            file = directionary.resolve(fileName).toFile();
        }
    }

    /*
   in this method we put the KV into the file
     */
    public synchronized void put(String key, String value) throws IOException {

        BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));

        bw.write(key + ";" + value);
        bw.newLine();
        bw.flush();
        bw.close();

    }

    /*
    In this methode we return the Value that corresponds to the key
     */
    public synchronized String get(String key) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();


        String value = "";
        String keyTemp;


        while (line != null) {

            keyTemp = line.split(";")[0];
            if (keyTemp.equals(key)) {

                String[] KVTab = line.split(";");

                value = KVTab[1];
                break;

            } else {

                line = br.readLine();

            }
        }
        br.close();
        return value;
    }


    /*
    In this methode we delete the kv pair from the file
     */
    public synchronized String delete(String key) throws IOException {

        File out = new File(directionary.resolve("temp_" + file.getName()).toString());


        BufferedReader br = new BufferedReader(new FileReader(file));
        BufferedWriter bw = new BufferedWriter(new FileWriter(out));


        String line = br.readLine();
        String keyTemp = "";
        String value = "";


        while (line != null) {

            keyTemp = line.split(";")[0];
            if (!keyTemp.equals(key)) {

                bw.write(line);
                bw.newLine();
                bw.flush();

            } else {
                value = line.split(";")[1];
            }

            line = br.readLine();

        }
        bw.close();
        br.close();

        Files.move(out.toPath(), file.toPath(), REPLACE_EXISTING, ATOMIC_MOVE);


//        BufferedReader cr = new BufferedReader(new FileReader(out));
//        BufferedWriter cw = new BufferedWriter(new FileWriter(file));
//
//        line = cr.readLine();
//
//
//        while (line != null) {
//
//            keyTemp = line.split(";")[0];
//            cw.write(line);
//            cw.newLine();
//            cw.flush();
//
//
//            line = cr.readLine();
//
//        }
//
//        cw.close();
//        cr.close();
//
//        out.delete();

        return value;

    }

    /*
    In this method we check if the Key is already stored in the File and returns a boolean
    */
    public synchronized boolean contains(String key) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();

        boolean exist = false;

        while (line != null && !exist) {
            line = line.split(";")[0];
            if (key.equals(line)) {
                exist = true;
                break;
            } else {
                line = br.readLine();
            }
        }

        br.close();
        return exist;
    }

    public synchronized String[] getData() {
        List<String> data = new LinkedList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();

            while (line != null) {
                data.add(line);
                line = br.readLine();
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data.toArray(new String[0]);
    }

    public synchronized void deleteAll() {
        try {
            PrintWriter writer = new PrintWriter(file);
            writer.print("");
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
//
//    public synchronized void deleteFile() {
//        file.delete();
//    }
}
