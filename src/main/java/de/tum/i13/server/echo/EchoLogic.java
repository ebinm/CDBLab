package de.tum.i13.server.echo;

import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class EchoLogic implements CommandProcessor {

    public static Logger logger = Logger.getLogger(EchoLogic.class.getName());

    private Config cfg;
    private CacheManager cacheManager;
    private ECSManager ecsManager;
    private boolean writerLock;
    private boolean initialization;


    public EchoLogic(Config config) {
        this.initialization = true;
        this.cfg = config;
        this.cacheManager = new CacheManager(cfg.cacheSize, cfg.strategy, cfg.dataDir);
        this.ecsManager = new ECSManager(cfg.bootstrap, this);
        this.writerLock = false;
    }

    public String process(String command) {

        if (initialization) {
            return "server_stopped\n";
        }
        logger.info("received command: " + command.trim());

        //Let the magic happen here
        String[] input = command.trim().split(" ");

        switch (input[0]) {
            case "put":
                if (writerLock) {
                    return "server_write_lock\n";
                }

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
                if (writerLock) {
                    return "server_write_lock\n";
                }

                logger.info("starting delete operation");
                if (input.length < 2) {
                    return "delete_error key is missing\n";
                }
                return delete(input[1]) + "\n";

            case "transfer":
                initialization = true;
                logger.info("handling incoming transfer");

                String[] commands = command.lines().toArray(String[]::new);

                for (int j = 0; j < commands.length; j++) {

                    input = commands[j].trim().split(" ");
                    StringBuilder value1 = new StringBuilder();

                    if (input.length < 3) {
                        return "put_error key and/or value is missing\n";
                    }

                    for (int i = 2; i < input.length; i++) {
                        value1.append(input[i]).append(" ");
                    }
                    value1 = new StringBuilder(value1.substring(0, value1.length() - 1));
                    put(input[1], value1.toString());

                }
                initialization = false;
                return "transfer_success\n";

            case "keyrange":
                return "keyrange_success " + getMetaData() + "\n";

            default:
                return "error unknown command\n";
        }
    }

    public String put(String key, String value){
        try {
            if (ecsManager.inRange(key)) {
                return cacheManager.put(key, value);
            } else return "server_not_responsible";
        } catch (IOException e) {
            return "put_error " + e.getMessage();
        }
    }

    public String get(String key) {
        try {
            if (ecsManager.inRange(key)) {
                return cacheManager.get(key);
            } else return "server_not_responsible";
        } catch (IOException e) {
            return "get_error " + e.getMessage();
        }
    }

    public String delete(String key){
        try {
            if (ecsManager.inRange(key)) {
                return cacheManager.delete(key);
            } else return "server_not_responsible";
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

    private String getMetaData() {
        return ecsManager.getMetaDataString();
    }

    public void setInitialization(boolean initialization) {
        this.initialization = initialization;
    }
}
