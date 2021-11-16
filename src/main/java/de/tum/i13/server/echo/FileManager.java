package de.tum.i13.server.echo;

import java.io.*;
import java.lang.reflect.Array;
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

    //put kv into file
    public void put(String key, String value) throws IOException {

        //File out = new File("temp.txt");
        //File in = new File (String.valueOf(directionary.getFileName()));
        //directionary.resolve("temp.txt");

        //BufferedReader br = new BufferedReader(new FileReader(in));
        BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));

        //String line = br.readLine();



//        while (line != null ){
//
//
//
//            if (line.startsWith(key)){
//                bw.write(key + ";" + value);
//                bw.flush();
//            } else {
//                bw.write(key + ";" + value);
//                bw.flush();
//            }
//            //line = br.readLine();
//
//        }

        bw.write(key + ";" + value);
        bw.newLine();
        bw.flush();
        bw.close();
        //br.close();

        //in.delete();
        //out.renameTo(new File(String.valueOf(directionary.getFileName())));


    }

    //return only the value
    public String get(String key) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();


        String value="";


        while (line != null){

            line = line.split(";")[0];
            if (line.equals(key)){

                String[] KVTab = line.split(";");

                value = KVTab[1];
                break;

            } else {

                //Change by Ebin!
                line = br.readLine();

            }
        }
        return value;
    }



    //delete the kv pair from the file
    public String delete(String key) throws IOException {

        //Change by Ebin
        File out = new File(directionary.resolve("temp.txt").toString());
        //File in = new File (String.valueOf(directionary.getFileName()));


        BufferedReader br = new BufferedReader(new FileReader(file));
        BufferedWriter bw = new BufferedWriter(new FileWriter(out));


        String line = br.readLine();
        String keyTemp = "";
        String value = "";


        while (line != null ){

            keyTemp = line.split(";")[0];
            if (!keyTemp.equals(key)){

                bw.write(line);
                bw.newLine();
                bw.flush();

            } else {
                value = line.split(";")[1];
            }

            //Change by Ebin
            line = br.readLine();

        }

        BufferedReader cr = new BufferedReader(new FileReader(out));
        BufferedWriter cw = new BufferedWriter(new FileWriter(file));

        line = cr.readLine();


        while (line != null ){

            keyTemp = line.split(";")[0];
                cw.write(line);
                cw.newLine();
                cw.flush();


            //Change by Ebin
            line = cr.readLine();

        }

        //Change by Ebin
        bw.close();
        br.close();
        cw.close();
        cr.close();

        return value;

    }



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
