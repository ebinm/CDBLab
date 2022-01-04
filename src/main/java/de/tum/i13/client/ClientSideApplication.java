package de.tum.i13.client;

import de.tum.i13.server.kv.KVMessage;
import de.tum.i13.server.nio.StartSimpleNioServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;


public class ClientSideApplication {

    public static Logger logger = Logger.getLogger(StartSimpleNioServer.class.getName());

    public static void main(String[] args)  {
        setupLogging(Path.of("client.log"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        CommunicationModule communicationModule = null;
        logger.info("Started Client Application");

        for(;;) {
            try {

                System.out.print("EchoClient> ");
                String line = reader.readLine();
                String[] command = line.trim().split(" ");

                logger.info("Progressing command " + line);

                //System.out.print("command:");
                //System.out.println(line);
                switch (command[0]) {
                    case "connect":
                        communicationModule = buildconnection(command);
                        communicationModule.keyRange();
                        break;
                    //case "send": sendmessage(communicationModule, command, line); break;
                    case "put":
                        put(communicationModule, command);
                        break;
                    case "get":
                        get(communicationModule, command);
                        break;
                    case "disconnect":
                        closeConnection(communicationModule);
                        break;
                    case "help":
                        printHelp();
                        break;
                    case "keyrange_read":
                        keyRangeRead(communicationModule);
                        break;
                    case "keyrange":
                        keyRange(communicationModule);
                        break;
                    case "quit":
                        printEchoLine("Application exit!");
                        closeConnection(communicationModule);
                        logger.info("Closing Application");
                        return;
                    default:
                        printEchoLine("Unknown command");
                }
            } catch (Exception e) {
                printEchoLine("ERROR " + e.getMessage());
            }
        }
    }

    private static void put(CommunicationModule communicationModule, String[] command) {

        if (command.length < 3) {
            printEchoLine("PUT_ERROR Invalid use of put command!");
        }

        String key = command[1];
        StringBuilder value = new StringBuilder();

        for (int i = 2; i < command.length; i++) {
            value.append(command[i]).append(" ");
        }
        value = new StringBuilder(value.substring(0, value.length() - 1));

        KVMessage kvMessage;
        try {
            kvMessage = communicationModule.put(key, value.toString());
        } catch (Exception e) {
            printEchoLine("PUT_ERROR Put command could not be executed!");
            return;
        }

        if (kvMessage.getStatus().equals(KVMessage.StatusType.SERVER_WRITE_LOCK)) {
            printEchoLine(kvMessage.getStatus().toString());
        } else {
            printEchoLine(kvMessage.getStatus() + " " + kvMessage.getKey());
        }
    }

    private static void get(CommunicationModule communicationModule, String[] command) {

        if (command.length != 2) {
            printEchoLine("GET_ERROR Invalid use of get command!");
        }

        String key = command[1];
        KVMessage kvMessage;
        try {
            kvMessage = communicationModule.get(key);
        } catch (Exception e) {
            printEchoLine("GET_ERROR Get command could not be executed!");
            return;
        }

        switch (kvMessage.getStatus()) {
            case GET_SUCCESS: printEchoLine(kvMessage.getStatus() + " " + kvMessage.getKey() + " " +
                    kvMessage.getValue()); return;
            case GET_ERROR: printEchoLine(kvMessage.getStatus() + " " + kvMessage.getKey());
        }
    }

    private static void keyRangeRead(CommunicationModule communicationModule) {
        KVMessage kvMessage;

        try {
            kvMessage = communicationModule.keyRangeRead();
        } catch (Exception e) {
            printEchoLine("KEYRANGE_READ_ERROR KeyRange_read command could not be executed!");
            return;
        }

        if (kvMessage.getStatus().equals(KVMessage.StatusType.KEYRANGE_READ_SUCCESS)) {
            printEchoLine(kvMessage.getStatus() + " " + kvMessage.getKeyRange());
        }
    }

    private static void keyRange(CommunicationModule communicationModule) {
        KVMessage kvMessage;

        try {
            kvMessage = communicationModule.keyRange();
        } catch (Exception e) {
            printEchoLine("KEYRANGE_ERROR KeyRange command could not be executed!");
            return;
        }

        if (kvMessage.getStatus().equals(KVMessage.StatusType.KEYRANGE_SUCCESS)) {
            printEchoLine(kvMessage.getStatus() + " " + kvMessage.getKeyRange());
        }
    }

    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("connect <address> <port> - Tries to establish a TCP- connection to the echo server based on the given server address and the port number of the echo service.");
        System.out.println("disconnect - Tries to disconnect from the connected server.");
        System.out.println("send <message> - Sends a text message to the echo server according to the communication protocol.");
        System.out.println("logLevel <level> - Sets the logger to the specified log level (ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF)");
        System.out.println("help - Display this help");
        System.out.println("quit - Tears down the active connection to the server and exits the program execution.");
    }

    private static void printEchoLine(String msg) {
        System.out.println("EchoClient> " + msg);
    }

    private static void closeConnection(CommunicationModule communicationModule) {
        if(communicationModule != null) {
            try {
                communicationModule.close();
            } catch (Exception e) {
                //e.printStackTrace();
                //TODO: handle gracefully
                communicationModule = null;
            }
        }
    }

    private static void sendmessage(ActiveConnection activeConnection, String[] command, String line) {
        if(activeConnection == null) {
            printEchoLine("Error! Not connected!");
            return;
        }
        int firstSpace = line.indexOf(" ");
        if(firstSpace == -1 || firstSpace + 1 >= line.length()) {
            printEchoLine("Error! Nothing to send!");
            return;
        }

        String cmd = line.substring(firstSpace + 1);
        activeConnection.write(cmd);

        try {
            printEchoLine(activeConnection.readline());
        } catch (IOException e) {
            printEchoLine("Error! Not connected!");
        }
    }

    private static CommunicationModule buildconnection(String[] command) {
        if(command.length == 3){
            try {
                EchoConnectionBuilder kvcb = new EchoConnectionBuilder(command[1], Integer.parseInt(command[2]));
                ActiveConnection ac = kvcb.connect();
                CommunicationModule cm = new CommunicationModule(ac);
                String confirmation = ac.readline();
                printEchoLine(confirmation);
                return cm;
            } catch (UnknownHostException e) {
                printEchoLine("Host " + command[1] + " is unknown");
            } catch (NumberFormatException e) {
                printEchoLine("Port is invalid");
            } catch (Exception e) {
                //Todo: separate between could not connect, unknown host and invalid port
                printEchoLine("Could not connect to server");
            }
        }
        return null;
    }

}
