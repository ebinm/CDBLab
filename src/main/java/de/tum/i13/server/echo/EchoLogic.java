package de.tum.i13.server.echo;

import de.tum.i13.shared.CommandProcessor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

import static de.tum.i13.server.threadperconnection.Main.cacheManager;


public class EchoLogic implements CommandProcessor {
    public static Logger logger = Logger.getLogger(EchoLogic.class.getName());


    public String process(String command) throws IOException {

        logger.info("received command: " + command.trim());

        //Let the magic happen here
        String[] input = command.split(" ");

        switch (input[0]) {
            case "put":
                String value = "";

                if (input.length < 3) {
                    return "error: key and/or value is missing";
                }

                for (int i = 2; i < input.length; i++) {
                    value = input[i];
                }
                value = value.trim();
                return put(input[1], value);

            case "get":
                if (input.length < 2) {
                    return "error: key is missing";
                }
                return get(input[1]);

            case "delete":
                if (input.length < 2) {
                    return "error: key is missing";
                }
                return delete(input[1]);

            default:
                return "error: unknown command";
        }
    }

    private String put(String key, String value) throws IOException {
        return cacheManager.put(key, value);
    }

    private String get(String key) throws IOException {
        return cacheManager.get(key);
    }

    private String delete(String key) throws IOException {
        return cacheManager.delete(key);
    }

    @Override
    public String connectionAccepted(InetSocketAddress address, InetSocketAddress remoteAddress) {
        logger.info("new connection: " + remoteAddress.toString());

        return "Connection to MSRG Echo server established: " + address.toString() + "\r\n";
    }

    @Override
    public void connectionClosed(InetAddress remoteAddress) {
        logger.info("connection closed: " + remoteAddress.toString());
    }
}
