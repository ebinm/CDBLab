package de.tum.i13.ecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class ReadThread extends Thread {
    private BufferedReader reader;
    private Socket socket;
    private ConnectionHandleThread connectionHandleThread;

    public ReadThread(BufferedReader bufferedReader, ConnectionHandleThread connectionHandleThread) {
        this.socket = socket;
        this.connectionHandleThread = connectionHandleThread;
        this.reader = bufferedReader;
    }

    public void run() {

        boolean run = true;
        while (run) {
            try {
                String response = reader.readLine();
                run = connectionHandleThread.process(response);
            } catch (IOException ex) {
                System.out.println("Error reading from server: " + ex.getMessage());
                ex.printStackTrace();
                break;
            }
        }
    }
}
