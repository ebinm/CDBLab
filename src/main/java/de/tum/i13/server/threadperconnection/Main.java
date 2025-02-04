package de.tum.i13.server.threadperconnection;

import de.tum.i13.server.echo.CacheManager;
import de.tum.i13.server.echo.EchoLogic;
import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Config;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tum.i13.shared.Config.parseCommandlineArgs;
import static de.tum.i13.shared.LogSetup.setupLogging;

/**
 * Created by chris on 09.01.15.
 */
public class Main {


    public final static Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws IOException {
        Config cfg = parseCommandlineArgs(args);  //Do not change this
        setupLogging(cfg.logfile);

        LOGGER.setLevel(Level.parse(cfg.logLevel));

        if (!cfg.strategy.equals("FIFO") && !cfg.strategy.equals("LRU") && !cfg.strategy.equals("LFU")) {
            throw new RuntimeException("Cache display strategy is incorrect!");
        }


        final ServerSocket serverSocket = new ServerSocket();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Closing thread per connection kv server");
                try {
                    serverSocket.close();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        //bind to localhost only
        serverSocket.bind(new InetSocketAddress(cfg.listenaddr, cfg.port));

        //Replace with your Key value server logic.
        // If you use multithreading you need locking
        CommandProcessor logic = new EchoLogic(cfg);
        System.out.println("Starting KV Server");

        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();

                //When we accept a connection, we start a new Thread for this connection
                LOGGER.info("Creating a new Thread with new Connection");
                Thread th = new ConnectionHandleThread(logic, clientSocket);
                th.start();
            }
        } catch (SocketException s) {
            System.out.println("KVServer closed!");
        }
    }
}
