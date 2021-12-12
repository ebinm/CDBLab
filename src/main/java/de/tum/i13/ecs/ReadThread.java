package de.tum.i13.ecs;

import java.io.BufferedReader;
import java.io.IOException;

public class ReadThread extends Thread {
    private BufferedReader reader;
    private ConnectionHandleThread connectionHandleThread;
    private boolean isRunning;

    public ReadThread(BufferedReader bufferedReader, ConnectionHandleThread connectionHandleThread) {
        this.connectionHandleThread = connectionHandleThread;
        this.reader = bufferedReader;
    }

    public void run() {

        isRunning = true;
            try {
                String input;
                while (isRunning && (input = reader.readLine()) != null) {
                    connectionHandleThread.process(input);
                }
            } catch (IOException ex) {
                System.out.println("Error reading from server: " + ex.getMessage());
                ex.printStackTrace();
            }
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public void close() {
        setRunning(false);
    }
}
