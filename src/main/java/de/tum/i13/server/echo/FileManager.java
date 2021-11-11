package de.tum.i13.server.echo;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Path;

public class FileManager {

    Path directionary;

    public FileManager(Path dataDir) {

        this.directionary = dataDir;
    }

    //put kv into file
    public void put(String key, String value) throws IOException {

        File out = new File("temp.txt");
        File in = new File (String.valueOf(directionary.getFileName()));
        directionary.resolve("temp.txt");

        BufferedReader br = new BufferedReader(new FileReader(in));
        BufferedWriter bw = new BufferedWriter(new FileWriter(out));

        String line = br.readLine();

        while (line != null ){

            if (line.startsWith(key)){
                bw.write(key + ";" + value);
                bw.flush();
            } else {
                bw.write(key + ";" + value);
                bw.flush();
            }

        }
        bw.close();
        br.close();

        in.renameTo(new File("trash"));
        out.renameTo(new File(String.valueOf(directionary.getFileName())));
        in.delete();

    }

    //return only the value
    public String get(String key) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(String.valueOf(directionary.getFileName())));
        String line = br.readLine();

  String value="";


        while (line != null){

            if (line.startsWith(key)){

                String[] KVTab = line.split(";");

                    value = KVTab[1];

            }
        }
        return value;
    }



    //delete the kv pair from the file
    public String delete(String key) throws IOException {

        File out = new File("temp.txt");
        File in = new File (String.valueOf(directionary.getFileName()));
        directionary.resolve("temp.txt");

        BufferedReader br = new BufferedReader(new FileReader(in));
        PrintWriter pw = new  PrintWriter (new FileWriter(out));


        String line = br.readLine();


        while (line != null ){

            if (line.trim().startsWith(key)){

              pw.println(line);
              pw.flush();
            }

            pw.close();
              br.close();
            }


        in.renameTo(new File("trash"));
        out.renameTo(new File(String.valueOf(directionary.getFileName())));
        in.delete();

        return  key;

        }




    public boolean contains(String key) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(String.valueOf(directionary.getFileName())));
        String line = br.readLine();

        boolean exist = false;

        while (line != null && !exist) {
            if (key.startsWith(line)) {
                exist = true;
            } else {
                line = br.readLine();
            }
        }

        return exist;
    }
}
