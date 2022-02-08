package de.tum.i13.ecs;


import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import static de.tum.i13.hashing.Hashing.getHash;
import static de.tum.i13.ecs.StartECSServer.LOGGER;

public class ECSLogic {

    //Map Range to Serverinfo
    Map<String, String> metaData = new HashMap<>();
    //Map Serverinfo to CommunicationThread
    Map<String, ConnectionHandleThread> connections = new HashMap<>();
    private Queue<ConnectionHandleThread> requests = new LinkedList<>();
    private volatile boolean busy = false;
    private volatile int updated;

//    public ECSLogic() {
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            @Override
//            public void run() {
//                if (metaData.size() > 0) {
//                    closeAllServers();
//                }
//            }
//        });
//    }

    public synchronized void queue(ConnectionHandleThread c) {
        if(busy) {
            LOGGER.info("Queue request");
            c.setReady(false);
            requests.add(c);
        } else {
            LOGGER.info("Start request");
            setBusy(true);
            c.setReady(true);
        }

//        do {
//            while (busy) {
//                Thread.onSpinWait();
//            }
//
//            LOGGER.info("Deque request");
//            ConnectionHandleThread y = requests.remove();
//            y.setReady(true);
//
//            if (requests.isEmpty()) {
//                this.busy = false;
//            } else {
//                this.busy = true;
//            }
//        } while (busy);
    }

    public synchronized void add(String serverInfo, ConnectionHandleThread connectionHandleThread) throws InterruptedException {
        setAddingServer(true);
        LOGGER.info("Adding new Server " + serverInfo + " to the storage system");

        connections.put(serverInfo, connectionHandleThread);
        String transferFrom = "";
        String metaDataMessage = "";
        String rangeCurrent = "";

        if (metaData.isEmpty()) {
            String serverHash = hash(serverInfo);
            String to = serverHash;
            String from = (new BigInteger(serverHash, 16)).add(BigInteger.ONE).
                    mod(new BigInteger("100000000000000000000000000000000", 16)).toString(16);

            if (from.length() < 32) {
                while (from.length() < 32) {
                    from = "0" + from;
                }
            }
            rangeCurrent = from + "-" + to;
            metaData.put(rangeCurrent, serverInfo);
            connectionHandleThread.write(getMetaData());
            try {
                connectionHandleThread.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            setAddingServer(false);
        } else {
            String serverHash = hash(serverInfo);
            String rangeTemp = rangeOf(serverInfo);
            String from = rangeTemp.split("-")[0];
            String to = rangeTemp.split("-")[1];

            String predecessorFrom = (new BigInteger(serverHash, 16)).add(BigInteger.ONE).
                    mod(new BigInteger("100000000000000000000000000000000", 16)).toString(16);

            if (predecessorFrom.length() < 32) {
                while (predecessorFrom.length() < 32) {
                    predecessorFrom = "0" + predecessorFrom;
                }
            }
            String predecessorTo = to;
            String rangePredecessor = predecessorFrom + "-" + predecessorTo;
            rangeCurrent = from + "-" + serverHash;

            String serverInfoPredecessor = metaData.remove(from + "-" + to);
            metaData.put(rangePredecessor, serverInfoPredecessor);
            metaData.put(rangeCurrent, serverInfo);
            transferFrom = serverInfoPredecessor;

            connectionHandleThread.write(getMetaData());
            try {
                connectionHandleThread.readLine();
                LOGGER.info("Server ready for transfer");
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Thread.sleep(200); //A little buffer to set up the new EchoServer
            transfer(transferFrom, serverInfo, rangeCurrent);
        }
        connectionHandleThread.write("start");
    }

    public synchronized void transfer(String fromServer, String toServer, String range) {
        LOGGER.info("Initializing transfer from Server " + fromServer + " to Server " + toServer + " for range " +
                range);

        ConnectionHandleThread connectionHandleThread = connections.get(fromServer);
        try {
            connectionHandleThread.transfer(fromServer, toServer, range);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void bruteForceShutDown(String serverInfo) {
        setRemovingServer(true);
        LOGGER.info("Shutting down server ungracefully " + serverInfo);


        if (metaData.size() > 1) {
            String serverHash = hash(serverInfo);
            String currentRange = rangeOf(serverInfo);
            String predecessorUpdatedRange = currentRange.split("-")[0] + "-";

            String predecessorFrom = (new BigInteger(serverHash, 16)).add(BigInteger.ONE).
                    mod(new BigInteger("100000000000000000000000000000000", 16)).toString(16);

            if (predecessorFrom.length() < 32) {
                while (predecessorFrom.length() < 32) {
                    predecessorFrom = "0" + predecessorFrom;
                }
            }
            String predecessorRange = rangeOfHash(predecessorFrom);
            predecessorUpdatedRange += predecessorRange.split("-")[1];
            String predecessorServerInfo = metaData.remove(predecessorRange);
            metaData.put(predecessorUpdatedRange, predecessorServerInfo);

            metaData.remove(currentRange);
            connections.remove(serverInfo);

            if (metaData.size() < 3) {
                sendMetaDataToAll();
            } else {
                ConnectionHandleThread connectionHandleThread = connections.get(predecessorServerInfo);
                connectionHandleThread.write("update_metaData " + getMetaData());

                LOGGER.info("Reconstructing data on server " + predecessorServerInfo);
                connectionHandleThread.write("reconstruct_data");
            }
        } else {
            String currentRange = rangeOf(serverInfo);
            metaData.remove(currentRange);
            connections.remove(serverInfo);
            setBusy(false);
        }
    }

    public synchronized String shutDown(String serverInfo) {
        setRemovingServer(true);
        LOGGER.info("Shutting down server " + serverInfo);

        if (metaData.size() > 1) {
            String serverHash = hash(serverInfo);
            String currentRange = rangeOf(serverInfo);
            String predecessorUpdatedRange = currentRange.split("-")[0] + "-";

            String predecessorFrom = (new BigInteger(serverHash, 16)).add(BigInteger.ONE).
                    mod(new BigInteger("100000000000000000000000000000000", 16)).toString(16);

            if (predecessorFrom.length() < 32) {
                while (predecessorFrom.length() < 32) {
                    predecessorFrom = "0" + predecessorFrom;
                }
            }
            String predecessorRange = rangeOfHash(predecessorFrom);
            predecessorUpdatedRange += predecessorRange.split("-")[1];
            String predecessorServerInfo = metaData.remove(predecessorRange);
            metaData.put(predecessorUpdatedRange, predecessorServerInfo);

            metaData.remove(currentRange);
            connections.remove(serverInfo);

            ConnectionHandleThread connectionHandleThread = connections.get(predecessorServerInfo);
            connectionHandleThread.write("update_metaData " + getMetaData());

            LOGGER.info("Transfering all data from " + serverInfo + " to " + predecessorServerInfo);
            return "transfer_all " + serverInfo + " " + predecessorServerInfo;
        } else {
            String currentRange = rangeOf(serverInfo);
            metaData.remove(currentRange);
            connections.remove(serverInfo);
            setBusy(false);
            return "shutDown";
        }
    }


    public synchronized String getMetaData() {
        String metaDataMessage = "";
        for(String range: metaData.keySet()) {
            metaDataMessage += range + "," + metaData.get(range) + ";";
        }
        return metaDataMessage;
    }

    public synchronized void writeAllServers(String message) {
        for(String server: connections.keySet()) {
            connections.get(server).write(message);
        }
    }

    public synchronized void sendMetaDataToAll() {
        LOGGER.info("Updating Metadata on all servers");

        updated = metaData.size();
//        LOGGER.info("Updating " + updated + " KV Servers");
        writeAllServers("update_metaData " + getMetaData());

    }

    private String rangeOfHash(String inputHash) {

        String from = "";
        String to = "";
        for(String range: metaData.keySet()) {
            String[] rangeArray = range.split("-");
            from = rangeArray[0];
            to = rangeArray[1];

            if (from.compareTo(to) < 0) {
                if ((from.compareTo(inputHash) <= 0) && (inputHash.compareTo(to) <= 0)) {
                    break;
                }
            } else {
                if (((from.compareTo(inputHash) <= 0) && (inputHash.compareTo("ffffffffffffffffffffffffffffffff") <= 0))
                        || ((inputHash.compareTo("00000000000000000000000000000000")
                        >= 0) && (inputHash.compareTo(to) <= 0))) {
                    break;
                }
            }
        }

        return from + "-" + to;
    }

    private String rangeOf(String input) {

        String inputHash = hash(input);

        return rangeOfHash(inputHash);
    }

    private String hash(String input) {
        return getHash(input);
    }

    public void setRemovingServer(boolean removingServer) {
        setBusy(removingServer);
    }

    public void closeAllServers() {
        for(String server: connections.keySet()) {
            ConnectionHandleThread connectionHandleThread = connections.get(server);
            connectionHandleThread.write("revive_ECS");
            connectionHandleThread.shutDown();
        }
    }

    public synchronized void inform() {
        this.updated--;
//        LOGGER.info(String.valueOf(updated) + " updated");

        if(updated == 0) {
            setBusy(false);
        }
    }

    public void setAddingServer(boolean addingServer) {
        setBusy(addingServer);
    }

    public void setBusy(boolean busy) {
        this.busy = busy;

        if (!busy) {
            if(!requests.isEmpty()) {
                this.busy = true;
                LOGGER.info("Deque request");
                requests.remove().setReady(true);
            }
        }
    }
}
