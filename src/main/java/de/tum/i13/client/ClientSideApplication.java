package de.tum.i13.client;

import de.tum.i13.server.kv.KVMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;


public class ClientSideApplication {
    public static void main(String[] args)  {

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        CommunicationModule communicationModule = null;
        for(;;) {
            try {

                System.out.print("EchoClient> ");
                String line = reader.readLine();
                String[] command = line.split(" ");
                //System.out.print("command:");
                //System.out.println(line);
                switch (command[0]) {
                    case "connect":
                        communicationModule = buildconnection(command);
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
                    case "quit":
                        printEchoLine("Application exit!");
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
        String value = "";

        for (int i = 2; i < command.length; i++) {
            value = command[i] + " ";
        }
        value = value.substring(0, value.length() - 1);

        KVMessage kvMessage;
        try {
            kvMessage = communicationModule.put(key, value);
        } catch (Exception e) {
            printEchoLine("PUT_ERROR Put command could not be executed!");
            return;
        }

        printEchoLine(kvMessage.getStatus() + " " + kvMessage.getKey());
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

    private static void closeConnection(ActiveConnection activeConnection) {
        if(activeConnection != null) {
            try {
                activeConnection.close();
            } catch (Exception e) {
                //e.printStackTrace();
                //TODO: handle gracefully
                activeConnection = null;
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
                CommunicationModule ac = kvcb.connect();
                String confirmation = ac.readline();
                printEchoLine(confirmation);
                return ac;
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
