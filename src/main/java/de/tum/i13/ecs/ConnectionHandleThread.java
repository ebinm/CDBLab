package de.tum.i13.ecs;

import de.tum.i13.shared.Constants;

import java.io.*;
import java.net.Socket;

import static de.tum.i13.server.echo.EchoLogic.logger;
import static de.tum.i13.server.threadperconnection.Main.LOGGER;

public class ConnectionHandleThread extends Thread {
    private ECSLogic ecsLogic;
    private Socket clientSocket;
    private ReadThread readThread;
    private PingThread pingThread;
    private String serverInfo;
    private ActiveConnection activeConnection;
    private volatile boolean ready = true;

    public ConnectionHandleThread(ECSLogic ecsLogic, Socket clientSocket) {
        this.ecsLogic = ecsLogic;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            LOGGER.info("New Thread " + Thread.currentThread().getName() + " is running");
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), Constants.TELNET_ENCODING));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), Constants.TELNET_ENCODING));
            this.activeConnection = new ActiveConnection(clientSocket, out, in);
            activeConnection.write("Connection with ECS Server successful");

            serverInfo = readLine();

            queue();
            ecsLogic.add(serverInfo, this);

            this.readThread = new ReadThread(in, this);
            readThread.start();

            this.pingThread = new PingThread(clientSocket.getInetAddress(), this);
            pingThread.start();

            System.out.println("Added Server " + serverInfo + " to the storage service");
            System.out.println("--------------------Number " + ecsLogic.metaData.size() + " on " + System.currentTimeMillis() + "----------------");

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void process(String command) throws IOException {
        logger.info("Received command: " + command.trim());

        String[] input = command.trim().split(" ");

        switch (input[0]) {
//            case "transfer_successful":
//                ecsLogic.sendMetaDataToAll();
//                return;
            case "initialize_shutdown":
                queue();
                String output = ecsLogic.shutDown(serverInfo);
                write(output);
                return;
            case "integration_successful":
                ecsLogic.sendMetaDataToAll();
//                ecsLogic.setAddingServer(false);
                return;
            case "shutDown_successful":
                ecsLogic.sendMetaDataToAll();
//                ecsLogic.setRemovingServer(false);
                shutDown();
                return;
            case "reconstruction_successful":
                ecsLogic.sendMetaDataToAll();
//                ecsLogic.setRemovingServer(false);
                return;
            case "update_successful":
                ecsLogic.inform();
                return;
            default:
                throw new RuntimeException("unknown command");
        }
    }

    public void shutDown() {
        try {
            activeConnection.close();
            readThread.close();
            pingThread.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void bruteForceShutDown() {
        queue();
        ecsLogic.bruteForceShutDown(serverInfo);
        shutDown();
    }

    public void transfer(String fromServer, String toServer, String range) throws IOException {
        write("transfer " + fromServer + " " + toServer + " " + range);
    }

    public void transferAll(String fromServer, String toServer) throws IOException {
        write("transfer_all " + fromServer + " " + toServer);
    }

    public void write(String message) {
        activeConnection.write(message);
    }

    public String readLine() throws IOException {
        return activeConnection.readline();
    }

    public String getServerInfo() {
        return serverInfo;
    }

    private void queue() {
        ecsLogic.queue(this);

        while (!ready) {
            Thread.onSpinWait();

        }
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
