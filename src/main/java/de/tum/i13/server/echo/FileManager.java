package de.tum.i13.server.echo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileManager {

    Path directionary;
    File file;

    public FileManager(Path dataDir) {

        this.directionary = dataDir;
        try {
            file = Files.createFile(directionary.resolve("data.txt")).toFile();
        } catch (IOException e) {
            file = directionary.resolve("data.txt").toFile();
        }
    }

    /*
   in this method we put the KV in the in the file
     */
    public void put(String key, String value) throws IOException {

        BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));

        bw.write(key + ";" + value);
        bw.newLine();
        bw.flush();
        bw.close();
    }

    /*
    In this methode we return the Value that corresponds to the key
     */
    public String get(String key) throws IOException {

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
        return value;
    }


    /*
    In this methode we delete the kv pair from the file
     */
    public String delete(String key) throws IOException {

        File out = new File(directionary.resolve("temp.txt").toString());


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

        BufferedReader cr = new BufferedReader(new FileReader(out));
        BufferedWriter cw = new BufferedWriter(new FileWriter(file));

        line = cr.readLine();


        while (line != null) {

            keyTemp = line.split(";")[0];
            cw.write(line);
            cw.newLine();
            cw.flush();


            line = cr.readLine();

        }

        bw.close();
        br.close();
        cw.close();
        cr.close();

        return value;

    }

    /*
    In this method we check if the Key is already stored in the File and returns a boolean
    */
    public boolean contains(String key) throws IOException {

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

        return exist;
    }
}
