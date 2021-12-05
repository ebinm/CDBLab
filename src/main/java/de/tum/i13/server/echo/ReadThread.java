package de.tum.i13.server.echo;

import de.tum.i13.ecs.ConnectionHandleThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

public class ReadThread extends Thread {
    private BufferedReader reader;
    private Socket socket;
    private ECSManager ecsManager;

    public ReadThread(BufferedReader bufferedReader, ECSManager ecsManager) {
        this.socket = socket;
        this.ecsManager = ecsManager;
        this.reader = bufferedReader;
    }

    public void run() {

        boolean run = true;
        while (run) {
            try {
                String response = reader.readLine();
                run = ecsManager.process(response);
            } catch (IOException ex) {
                System.out.println("Error reading from server: " + ex.getMessage());
                ex.printStackTrace();
                break;
            }
        }
    }
}
