package de.tum.i13.server.echo;

import de.tum.i13.shared.Constants;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static de.tum.i13.hashing.Hashing.getHash;
import static de.tum.i13.server.echo.EchoLogic.logger;

public class ECSManager {

    private final EchoLogic echoLogic;
    private BufferedReader reader;
    private PrintWriter writer;
    private Socket ecsSocket;
    private Map<String, String> metaData;
    private final String serverInfo;
    private String currentRange;
    private boolean isRunning = false;
    private ReplicationManager replicationManager;


    public ECSManager(InetSocketAddress bootstrap, EchoLogic echoLogic) {
        this.echoLogic = echoLogic;
        try {
            this.metaData = configureServer(bootstrap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.serverInfo = echoLogic.getClientPort();
        this.currentRange = rangeOf(serverInfo);

        Thread readThread = new ReadThread(reader, this);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if(isRunning) {
                    System.out.print("Shutting down server");
                    write("initialize_shutdown");
                    int x = 0;
                    while (isRunning) {
                        System.out.print(".");
                        if (x > 20000) {
                            throw new RuntimeException("Could not close KVServer properly");
                        }
                        x++;
                    }
                    System.out.println(".");
                    System.out.println("Shut down completed!");
                }
            }
        });

        System.out.println("Range of the server " + currentRange);
        this.isRunning = true;
        readThread.start();
        write("server_ready");
    }

    private Map<String, String>  configureServer(InetSocketAddress bootstrap) throws IOException {
        logger.info("Setting up connection to ECS");

        try {
            this.ecsSocket = new Socket(bootstrap.getAddress(), bootstrap.getPort());
            this.reader = new BufferedReader(new InputStreamReader(ecsSocket.getInputStream(), Constants.TELNET_ENCODING));
            this.writer = new PrintWriter(new OutputStreamWriter(ecsSocket.getOutputStream(), Constants.TELNET_ENCODING));
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info(readLine());
        write(echoLogic.getClientPort());

        return convertMetaData(readLine());
    }

    public boolean process(String command) {
        logger.info("Received command from ECS: " + command.trim());

        String[] input = command.trim().split(" ");

        switch (input[0]) {
            case "start":
                echoLogic.setInitialization(false);
                return isRunning;
            case "transfer":
                transfer(input[2], input[3]);
                logger.info("Transfer completed");
                return isRunning;
            case "update_metaData":
                this.metaData = convertMetaData(input[1]);
                updateRange();
                replicationManager.updateConnections(true);
                return isRunning;
            case "transfer_all":
                transfer(input[2], "ALL");
                shutDown();
                return isRunning;
            case "reconstruct_data":
                reconstructData();
                write("transfer_successful");
                return isRunning;
            case "shutDown":
                shutDown();
                return isRunning;
            default:
                throw new RuntimeException("unknown command: " + command);
        }
    }

    private void transfer(String toServer, String range) {

        String[] serverInfo = toServer.split(":");
        try {

            Socket socket = null;
            boolean connected = false;
            while (!connected) {
                try {
                    socket = new Socket(serverInfo[0], Integer.parseInt(serverInfo[1]));
                    connected = true;
                } catch (ConnectException c) {
                    connected = false;
                }
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Constants.TELNET_ENCODING));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Constants.TELNET_ENCODING));

            ActiveConnection ac = new ActiveConnection(socket, out, in);


            echoLogic.setWriterLock(true);
            String[] data = echoLogic.getData();
            List<String> toBeDeleted = new LinkedList<>();
            logger.info(ac.readline());

            if (data.length != 0) {
                if (range.equals("ALL")) {
                    logger.info("Transferring all data to server " + toServer);

                    StringBuilder output = new StringBuilder();
                    for (String line : data) {
                        String key = line.split(";")[0];
                        String value = line.split(";")[1];

                        output.append("transfer ").append(key).append(" ").append(value).append("\r\n");
                        //out.flush();
                        //in.readLine();
                        toBeDeleted.add(key);
                    }

                    if (output.length() != 0) {
                        ac.writeWithoutCarriageReturn(output.toString());
                        logger.info(ac.readline());
                    }
                    replicationManager.deleteBoth();

                } else {
                    logger.info("Transferring data in range " + range + " to server " + toServer);

                    StringBuilder output = new StringBuilder();
                    for (String line : data) {
                        String key = line.split(";")[0];
                        String value = line.split(";")[1];

                        if (inRange(key, range)) {
                            output.append("transfer ").append(key).append(" ").append(value).append("\r\n");
                            //out.flush();
                            //in.readLine();
                            toBeDeleted.add(key);
                        }
                    }

                    if (output.length() != 0) {
                        ac.writeWithoutCarriageReturn(output.toString());
                        logger.info(ac.readline());
                        //replicationManager.transferBoth();
                    }
                }
            }
            write("transfer_successful");

            for(String key: toBeDeleted) {
                echoLogic.delete(key);
            }
            echoLogic.setWriterLock(false);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reconstructData() {
        replicationManager.reconstructData();
        logger.info("Reconstruction completed");
    }

    private void shutDown() {
        write("shutDown_successful");
        try {
            reader.close();
            writer.close();
            ecsSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isRunning = false;
    }

    private void write(String message) {
        writer.write(message + "\r\n");
        writer.flush();
    }

    private String readLine() throws IOException {
        return reader.readLine();
    }

    public String getMetaDataString() {
        String metaDataMessage = "";
        for(String range: metaData.keySet()) {
            String[] fromTo = range.split("-");
            String from = fromTo[0];
            String to = fromTo[1];
            metaDataMessage += from + "," + to + "," + metaData.get(range) + ";";
        }
        return metaDataMessage;
    }

    private Map<String, String> convertMetaData(String metaDataString) {
        String[] serverData = metaDataString.split(";");
        Map<String, String> result = new HashMap<>();

        for(int i = 0; i < serverData.length; i++) {
            String[] temp = serverData[i].split(",");
            String range = temp[0];
            String serverInfo = temp[1];
            result.put(range, serverInfo);
        }

        return result;
    }

    public boolean inRange(String input) {
        return inRange(input, currentRange);
    }

    public boolean inRange(String input, String range) {
        String inputHash = hash(input);
        String[] rangeArray = range.split("-");
        String from = rangeArray[0];
        String to = rangeArray[1];

        if (from.compareTo(to) < 0) {
            if ((from.compareTo(inputHash) <= 0) && (inputHash.compareTo(to) <= 0)) {
                return true;
            }
        } else {
            if (((from.compareTo(inputHash) <= 0) && (inputHash.compareTo("ffffffffffffffffffffffffffffffff") <= 0))
                    || ((inputHash.compareTo("00000000000000000000000000000000")
                    >= 0) && (inputHash.compareTo(to) <= 0))) {
                return true;
            }
        }
        return false;
    }

    private String rangeOf(String input) {

        String inputHash = hash(input);

        return rangeOfNoHash(inputHash);
    }

    public String rangeOfNoHash(String input) {
        String from = "";
        String to = "";
        for(String range: metaData.keySet()) {
            String[] rangeArray = range.split("-");
            from = rangeArray[0];
            to = rangeArray[1];

            if (from.compareTo(to) < 0) {
                if ((from.compareTo(input) <= 0) && (input.compareTo(to) <= 0)) {
                    break;
                }
            } else {
                if (((from.compareTo(input) <= 0) && (input.compareTo("ffffffffffffffffffffffffffffffff") <= 0))
                        || ((input.compareTo("00000000000000000000000000000000")
                        >= 0) && (input.compareTo(to) <= 0))) {
                    break;
                }
            }
        }

        return from + "-" + to;
    }

    private void updateRange(){
        this.currentRange = rangeOf(serverInfo);
        System.out.println("New range of server " + currentRange);
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }

    public String getCurrentRange() {
        return currentRange;
    }

    private String hash(String input) {
        return getHash(input);
    }

    public void setReplicationManager(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
    }
}
