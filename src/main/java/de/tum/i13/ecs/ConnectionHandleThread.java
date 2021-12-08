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
    private String serverInfo;
    private ActiveConnection activeConnection;

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
            ecsLogic.add(serverInfo, this);

            this.readThread = new ReadThread(in, this);
            readThread.start();

            System.out.println("Added Server " + serverInfo + " to the storage service");

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean process(String command) throws IOException {
        logger.info("received command: " + command.trim());

        String[] input = command.trim().split(" ");

        switch (input[0]) {
            case "transfer_successful":
                ecsLogic.sendMetaDataToAll();
                return true;
            case "initialize_shutdown":
                String output = ecsLogic.shutDown(serverInfo);
                write(output);
                return true;
            case "shutDown_successful":
                ecsLogic.setRemovingServer(false);
                shutDown();
                return false;
            case "server_ready":
                ecsLogic.setServerReady(true);
                return true;
            default:
                throw new RuntimeException("unknown command");
        }
    }

    private void shutDown() {
        try {
            activeConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

}
