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
    private ECSManager ecsManager;
    private boolean writerLock;


    public EchoLogic(Config config) {
        this.cfg = config;
        this.cacheManager = new CacheManager(cfg.cacheSize, cfg.strategy, cfg.dataDir);
        this.ecsManager = new ECSManager(cfg.bootstrap, this);
        this.writerLock = false;
    }

    public String process(String command) {


        logger.info("received command: " + command.trim());

        //Let the magic happen here
        String[] input = command.trim().split(" ");

        switch (input[0]) {
            case "put":
                logger.info("starting put operation");
                StringBuilder value = new StringBuilder();

                if (input.length < 3) {
                    return "put_error key and/or value is missing\n";
                }

                for (int i = 2; i < input.length; i++) {
                    value.append(input[i]).append(" ");
                }
                value = new StringBuilder(value.substring(0, value.length() - 1));
                return put(input[1], value.toString()) + "\n";

            case "get":
                logger.info("starting get operation");
                if (input.length < 2) {
                    return "get_error key is missing\n";
                }
                return get(input[1]) + "\n";

            case "delete":
                logger.info("starting delete operation");
                if (input.length < 2) {
                    return "delete_error key is missing\n";
                }
                return delete(input[1]) + "\n";

            default:
                return "error unknown command\n";
        }
    }

    private String put(String key, String value){
        try {
            return cacheManager.put(key, value);
        } catch (IOException e) {
            return "put_error " + e.getMessage();
        }
    }

    private String get(String key) {
        try {
            return cacheManager.get(key);
        } catch (IOException e) {
            return "get_error " + e.getMessage();
        }
    }

    private String delete(String key){
        try {
            return cacheManager.delete(key);
        } catch (IOException e) {
            return "delete_error " + e.getMessage();
        }
    }

    public String[] getData() throws IOException {
        return cacheManager.getData();
    }

    public void setWriterLock(boolean lock) {
        writerLock = lock;
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

    public String getClientPort() {
        return cfg.listenaddr + ":" + cfg.port;
    }
}
