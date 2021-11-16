package de.tum.i13.server.echo;

import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Logger;



public class EchoLogic implements CommandProcessor {

    public static Logger logger = Logger.getLogger(EchoLogic.class.getName());

    private Config cfg;
    private CacheManager cacheManager;

    public EchoLogic(Config config) {
        this.cfg = config;
        cacheManager = new CacheManager(cfg.cacheSize, cfg.strategy, cfg.dataDir);
    }

    public String process(String command) {


        logger.info("received command: " + command.trim());

        //Let the magic happen here
        String[] input = command.trim().split(" ");

        switch (input[0]) {
            case "put":
                String value = "";

                if (input.length < 3) {
                    return "PUT_ERROR key and/or value is missing\n";
                }

                for (int i = 2; i < input.length; i++) {
                    value = input[i] + " ";
                }
                value = value.substring(0, value.length());
                return put(input[1], value) + "\n";

            case "get":
                if (input.length < 2) {
                    return "GET_ERROR key is missing\n";
                }
                return get(input[1]) + "\n";

            case "delete":
                if (input.length < 2) {
                    return "DELETE_ERROR: key is missing\n";
                }
                return delete(input[1]) + "\n";

            default:
                return "ERROR unknown command\n";
        }
    }

    private String put(String key, String value){
        try {
            return cacheManager.put(key, value);
        } catch (IOException e) {
            return "PUT_ERROR " + e.getMessage();
        }
    }

    private String get(String key) {
        try {
            return cacheManager.get(key);
        } catch (IOException e) {
            return "GET_ERROR " + e.getMessage();
        }
    }

    private String delete(String key){
        try {
            return cacheManager.delete(key);
        } catch (IOException e) {
            return "DELETE_ERROR " + e.getMessage();
        }
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
