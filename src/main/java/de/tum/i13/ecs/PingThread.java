package de.tum.i13.ecs;

import java.io.IOException;
import java.net.InetAddress;

public class PingThread extends Thread {

    private InetAddress inetAddress;
    private ConnectionHandleThread connectionHandleThread;
    private boolean isRunning;

    public PingThread(InetAddress inetAddress, ConnectionHandleThread connectionHandleThread) {
        this.inetAddress = inetAddress;
        this.connectionHandleThread = connectionHandleThread;
    }

    @Override
    public void run() {
        boolean isReachable = true;
        isRunning = true;
        try {
            while (isReachable && isRunning) {
                sleep(1000);
                isReachable = inetAddress.isReachable(700);
                //System.out.println("KVServer still active");
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        if (!isReachable) {
            //connectionHandleThread.bruteForceShutDown();
        }
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public void close() {
        setRunning(false);
    }
}
