package de.tum.i13.ecs;

import de.tum.i13.shared.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tum.i13.shared.Config.parseCommandlineArgs;
import static de.tum.i13.shared.LogSetup.setupLogging;

public class StartECSServer extends Thread{

    private Config cfg;
    private String oldData;

    public StartECSServer(Config cfg, String oldData) {
        this.cfg = cfg;
        this.oldData = oldData;
    }

    public final static Logger LOGGER = Logger.getLogger(StartECSServer.class.getName());

    public static void main(String[] args) throws IOException {
        Config cfg = parseCommandlineArgs(args);  //Do not change this

        startECS(cfg, null);
    }

    public static void startECS(Config cfg, String oldData) throws IOException {
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

                if (oldData != null) {

                    th.join();
                    LOGGER.info("Transferring old Data to new Server");

                    String serverInfo = ((ConnectionHandleThread) th).getServerInfo();

                    String host = serverInfo.split(":")[0];
                    String port = serverInfo.split(":")[1];

                    Socket s = new Socket(host, Integer.parseInt(port));
                    PrintWriter output = new PrintWriter(s.getOutputStream());
                    BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));

                    LOGGER.info(input.readLine());

                    String message = "server_stopped";
                    while (message.equals("server_stopped")) {
                        output.write(oldData);
                        output.flush();
                        message = input.readLine();
                    }

                    output.close();
                    input.close();
                    s.close();

                }
            }
        } catch (SocketException | InterruptedException s) {
            System.out.println("Closed ECSServer!");
        }
    }

    @Override
    public void run() {
        try {
            startECS(cfg, oldData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
