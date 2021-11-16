package de.tum.i13.server.threadperconnection;

import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Constants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import static de.tum.i13.server.threadperconnection.Main.LOGGER;

public class ConnectionHandleThread extends Thread {
    private CommandProcessor cp;
    private Socket clientSocket;

    public ConnectionHandleThread(CommandProcessor commandProcessor, Socket clientSocket) {
        this.cp = commandProcessor;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            LOGGER.info("New Thread " + Thread.currentThread().getName() + " is running");
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), Constants.TELNET_ENCODING));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), Constants.TELNET_ENCODING));

            out.write("Connection with Server successful\r\n");
            out.flush();

            String firstLine;
            while ((firstLine = in.readLine()) != null) {
                LOGGER.info("Processing new input of Thread " + Thread.currentThread().getName());
                String res = cp.process(firstLine);
                out.write(res + "\r\n");
                out.flush();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
