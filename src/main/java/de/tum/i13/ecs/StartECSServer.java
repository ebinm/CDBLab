package de.tum.i13.ecs;

import de.tum.i13.shared.Config;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tum.i13.shared.Config.parseCommandlineArgs;
import static de.tum.i13.shared.LogSetup.setupLogging;

public class StartECSServer {


    public final static Logger LOGGER = Logger.getLogger(StartECSServer.class.getName());

    public static void main(String[] args) throws IOException {
        Config cfg = parseCommandlineArgs(args);  //Do not change this
        setupLogging(cfg.logfile);

        LOGGER.setLevel(Level.parse(cfg.logLevel));
        LOGGER.info("Config: " + cfg.toString());

        final ServerSocket serverSocket = new ServerSocket();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Closing thread per connection ecs server");
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        //TODO change port
        serverSocket.bind(new InetSocketAddress(cfg.listenaddr, cfg.port));

        ECSLogic logic = new ECSLogic();
        System.out.println("ECS Server started");

        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();

                //When we accept a connection, we start a new Thread for this connection
                LOGGER.info("Handling new Server in new Thread");
                Thread th = new ConnectionHandleThread(logic, clientSocket);
                th.start();
            }
        } catch (SocketException s) {
            System.out.println("Closed ECSServer!");
        }
    }
}
