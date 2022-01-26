package de.tum.i13.server.echo;

import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static de.tum.i13.server.echo.EchoLogic.logger;

public class ReplicationManager {

    private final EchoLogic echoLogic;
    private final ECSManager ecsManager;
    private FileManager replica_1;
    private FileManager replica_2;

    private ActiveConnection replicationServer_1;
    private ActiveConnection replicationServer_2;

    private String rangeReplica_1;
    private String rangeReplica_2;

    public ReplicationManager(EchoLogic echoLogic, ECSManager ecsManager, boolean transfer) {
        this.echoLogic = echoLogic;
        this.ecsManager = ecsManager;
        ecsManager.setReplicationManager(this);
        Path path = echoLogic.getCfg().dataDir;

        this.replica_1 = new FileManager(path, "replica_1.txt");
        this.replica_2 = new FileManager(path, "replica_2.txt");

        updateConnections(transfer);
    }

    public synchronized void updateConnections(boolean transfer) {
        Map<String, String> metaData = ecsManager.getMetaData();

        if (metaData.size() < 3) {
            if (replicationServer_1 != null && replicationServer_1.isConnected()) {
                try {
                    replicationServer_1.close();
                    replicationServer_1 = null;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (replicationServer_2 != null && replicationServer_2.isConnected()) {
                try {
                    replicationServer_2.close();
                    replicationServer_2 = null;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            replica_1.deleteAll();
            replica_2.deleteAll();
        } else {

            String rangeCurrent = ecsManager.getCurrentRange();

            String rangeCurrentEnd = rangeCurrent.split("-")[1];

            String rangeReplication1Start = (new BigInteger(rangeCurrentEnd, 16)).add(BigInteger.ONE).
                    mod(new BigInteger("100000000000000000000000000000000", 16)).toString(16);

            if (rangeReplication1Start.length() < 32) {
                while (rangeReplication1Start.length() < 32) {
                    rangeReplication1Start = "0" + rangeReplication1Start;
                }
            }

            String rangeReplication1 = ecsManager.rangeOfNoHash(rangeReplication1Start);

            String rangeReplication1End = rangeReplication1.split("-")[1];

            String rangeReplication2Start = (new BigInteger(rangeReplication1End, 16)).add(BigInteger.ONE).
                    mod(new BigInteger("100000000000000000000000000000000", 16)).toString(16);

            if (rangeReplication2Start.length() < 32) {
                while (rangeReplication2Start.length() < 32) {
                    rangeReplication2Start = "0" + rangeReplication2Start;
                }
            }

            String rangeReplication2 = ecsManager.rangeOfNoHash(rangeReplication2Start);

            String[] rangesStored = getReplicaRanges(rangeCurrent);
            rangeReplica_1 = rangesStored[0];
            rangeReplica_2 = rangesStored[1];

            establishConnections(rangeReplication1, rangeReplication2, transfer);
        }
    }

    private synchronized void establishConnections(String range1, String range2, boolean transfer) {

        logger.info("Setting up Replication Servers");

        Map<String, String> metaData = ecsManager.getMetaData();

        String server1 = metaData.get(range1);
        if (replicationServer_1 == null || !server1.equals(replicationServer_1.getServerInfo())) {
            try {
                if (replicationServer_1 != null && replicationServer_1.isConnected()) {
                    String[] data = echoLogic.getData();

                    for (String s: data) {
                        String key = s.split(";")[0];
                        replicationServer_1.write("replica1_delete " + key);
                        replicationServer_1.readline();
                    }

                    this.replicationServer_1.close();
                }

                String host = server1.split(":")[0];
                String port = server1.split(":")[1];
                this.replicationServer_1 = connect(host, Integer.parseInt(port));

                logger.info("Replica1: " + replicationServer_1.readline());

                if (transfer) {
                    transfer(replicationServer_1, "replica1");
                }



            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String server2 = metaData.get(range2);
        if (replicationServer_2 == null || !server2.equals(replicationServer_2.getServerInfo())) {
            try {
                if (replicationServer_2 != null && replicationServer_2.isConnected()) {
                    String[] data = echoLogic.getData();

                    for (String s: data) {
                        String key = s.split(";")[0];
                        replicationServer_2.write("replica2_delete " + key);
                        replicationServer_2.readline();
                    }

                    this.replicationServer_2.close();
                }

                String host = server2.split(":")[0];
                String port = server2.split(":")[1];
                this.replicationServer_2 = connect(host, Integer.parseInt(port));

                logger.info("Replica2: " + replicationServer_2.readline());

                if(transfer) {
                    transfer(replicationServer_2, "replica2");
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//        if (metaData.size() == 3) {
//            transferAll(replicationServer_1);
//            transferAll(replicationServer_2);
//        }
    }

    private synchronized ActiveConnection connect(String host, int port) throws IOException {
        Socket s = new Socket(host, port);

        PrintWriter output = new PrintWriter(s.getOutputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));

        return new ActiveConnection(s, output, input);
    }

    public String process(String inputs) {

        String[] lines = inputs.lines().toArray(String[]::new);
        String output = "Error: Could not find lines";

        for (String command: lines) {

            FileManager fileManager;
            if (command.startsWith("replica1")) {
                fileManager = replica_1;
            } else {
                fileManager = replica_2;
            }

            String[] input = command.trim().split(" ");
            input[0] = input[0].split("_")[1];

            switch (input[0]) {
                case "put":

                    String key = input[1];
                    StringBuilder value = new StringBuilder();

                    for (int i = 2; i < input.length; i++) {
                        value.append(input[i]).append(" ");
                    }
                    value = new StringBuilder(value.substring(0, value.length() - 1));

                    try {
                        if (fileManager.contains(key)) {
                            fileManager.delete(key);
                            fileManager.put(key, value.toString());
                            output = "replica_update_success";
                        } else {
                            fileManager.put(key, value.toString());
                            output = "replica_put_success";
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        output = "replica_put_error";
                    }

                    //            case "get":
//                logger.info("starting get operation");
//                if (input.length < 2) {
//                    return "get_error key is missing\n";
//                }
//                return get(input[1]) + "\n";
                    break;

                case "delete":

                    String key2 = input[1];

                    try {
                        fileManager.delete(key2);
                        output = "replica_delete_success";
                    } catch (IOException e) {
                        e.printStackTrace();
                        output = "replica_delete_error";
                    }

                    break;

                case "transfer":

//                    String[] commands = command.lines().toArray(String[]::new);
//                    //fileManager.deleteAll();
//
//                    for (int j = 0; j < commands.length; j++) {

//                        input = commands[j].trim().split(" ");
                        StringBuilder value1 = new StringBuilder();

                        if (input.length < 3) {
                            output = "put_error key and/or value is missing";
                        }

                        for (int i = 2; i < input.length; i++) {
                            value1.append(input[i]).append(" ");
                        }
                        value1 = new StringBuilder(value1.substring(0, value1.length() - 1));

                        try {
                            if (fileManager.contains(input[1])) {
                                fileManager.delete(input[1]);
                                fileManager.put(input[1], value1.toString());
                            } else {
                                fileManager.put(input[1], value1.toString());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
//                    }
                    output = "replica_transfer_success";

                        break;

//            case "keyrange":
//                return "keyrange_success " + getMetaData() + "\n";

                default:
                    output = "error unknown command";
            }
        }
        return output;
    }

    public synchronized void put(String key, String value) {
        if (ecsManager.getMetaData().size() < 3) {
            return;
        }

        replicationServer_1.write("replica1_put " + key + " " + value);
        replicationServer_1.readline();

        replicationServer_2.write("replica2_put " + key + " " + value);
        replicationServer_2.readline();

    }

    public synchronized void delete(String key) {
        if (ecsManager.getMetaData().size() < 3) {
            return;
        }

        replicationServer_1.write("replica1_delete " + key);
        replicationServer_1.readline();

        replicationServer_2.write("replica2_delete " + key);
        replicationServer_2.readline();
    }

    public synchronized void transfer(ActiveConnection ac, String replica) {

        String[] data = new String[0];
        data = echoLogic.getData();

        if (data.length != 0) {

            StringBuilder output = new StringBuilder();
            for (String line : data) {
                String key = line.split(";")[0];
                String value = line.split(";")[1];

                output.append(replica).append("_transfer ").append(key).append(" ").append(value).append("\r\n");
//            toBeDeleted.add(key);
            }
            logger.info("Transfering data to " + replica);

            if (output.length() != 0) {
                ac.writeWithoutCarriageReturn(output.toString());
                logger.info(ac.readline());
            }

        }
    }

    public synchronized void transferBoth() {

        Map<String, String> metaData = ecsManager.getMetaData();

        if (metaData.size() > 2) {
            if (replicationServer_1 != null && replicationServer_2 != null) {
                transfer(replicationServer_1, "replica1");
                transfer(replicationServer_2, "replica2");
            }
        }
    }

    private String[] getReplicaRanges(String range) {
        String rangeCurrent = range;

        String rangeCurrentStart = rangeCurrent.split("-")[0];

        String rangeReplication1End = (new BigInteger(rangeCurrentStart, 16)).subtract(BigInteger.ONE).
                mod(new BigInteger("100000000000000000000000000000000", 16)).toString(16);

        if (rangeReplication1End.length() < 32) {
            while (rangeReplication1End.length() < 32) {
                rangeReplication1End = "0" + rangeReplication1End;
            }
        }

        String rangeReplication1 = ecsManager.rangeOfNoHash(rangeReplication1End);

        String rangeReplication1Start = rangeReplication1.split("-")[0];

        String rangeReplication2End = (new BigInteger(rangeReplication1Start, 16)).subtract(BigInteger.ONE).
                mod(new BigInteger("100000000000000000000000000000000", 16)).toString(16);

        if (rangeReplication2End.length() < 32) {
            while (rangeReplication2End.length() < 32) {
                rangeReplication2End = "0" + rangeReplication2End;
            }
        }

        String rangeReplication2 = ecsManager.rangeOfNoHash(rangeReplication2End);

        String[] result = new String[]{rangeReplication1, rangeReplication2};

        return result;
    }

    public synchronized void deleteBoth() {
        replica_1.deleteAll();
        replica_2.deleteAll();
    }

    public String getKeyRangesRead() {
        Map<String, String > metaData = ecsManager.getMetaData();

        if (metaData.size() < 3) {
            return ecsManager.getMetaDataString();
        } else {
            String metaDataMessage = "";
            for(String range: metaData.keySet()) {
                String[] fromTo = range.split("-");
                String to = fromTo[1];
                String from = getReplicaRanges(range)[1].split("-")[0];
                metaDataMessage += from + "," + to + "," + metaData.get(range) + ";";
            }
            return metaDataMessage;
        }
    }

    public boolean inRange(String input) {
        if (ecsManager.getMetaData().size() < 3) {
            return false;
        } else {
            return ecsManager.inRange(input, rangeReplica_1) || ecsManager.inRange(input, rangeReplica_2);
        }
    }

    public String getOfReplica(String key) throws IOException {

            if (replica_1.contains(key)) {
                return "get_success " + key + " " + replica_1.get(key);
            } else if (replica_2.contains(key)) {
                return "get_success " + key + " " + replica_2.get(key);
            } else {
                return "server_not_responsible";
            }

    }

    public void reconstructData() {

        String[] data = new String[0];
        data = replica_1.getData();

        for (String line : data) {
            String key = line.split(";")[0];
            String value = line.split(";")[1];

            echoLogic.put(key, value);

            try {
                replica_1.delete(key);
                replicationServer_1.write("replica2_delete " + key);
                replicationServer_1.readline();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    public void transferAll(ActiveConnection ac) {
//
//        if (ecsManager.getMetaData().size() < 3) {
//            return;
//        }
//
////        List<String> toBeDeleted = new LinkedList<>();
//        String[] replica1 = new String[0];
//        String[] replica2 = new String[0];
//
//        try {
//            replica1 = replica_1.getData();
//            replica2 = replica_2.getData();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        StringBuilder output = new StringBuilder();
//        for (String line : replica1) {
//            String key = line.split(";")[0];
//            String value = line.split(";")[1];
//
//            output.append("replica1_transfer ").append(key).append(" ").append(value).append("\r\n");
////            toBeDeleted.add(key);
//        }
//        logger.info("Transfering replica_1 to server");
//        ac.writeWithoutCarriageReturn(output.toString());
//        try {
//            ac.readline();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
////        for(String key: toBeDeleted) {
////            try {
////                replica_1.delete(key);
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
////        }
//
////        toBeDeleted.clear();
//
//        StringBuilder output2 = new StringBuilder();
//        for (String line : replica2) {
//            String key = line.split(";")[0];
//            String value = line.split(";")[1];
//
//            output2.append("replica2_transfer ").append(key).append(" ").append(value).append("\r\n");
////            toBeDeleted.add(key);
//        }
//        logger.info("Transfering replica_2 to server");
//        ac.writeWithoutCarriageReturn(output2.toString());
//        try {
//            ac.readline();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
////        for(String key: toBeDeleted) {
////            try {
////                replica_2.delete(key);
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
////        }
//
//        replica_1.deleteAll();
//        replica_2.deleteAll();
//
//        try {
//            replicationServer_1.close();
//            replicationServer_2.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

//    public void transfer(ActiveConnection ac) {
//
//        if (ecsManager.getMetaData().size() < 3) {
//            return;
//        }
//
//        List<String> toBeMoved = new LinkedList<>();
//        String[] replica1 = new String[0];
//        String[] replica2 = new String[0];
//
//        try {
//            replica1 = replica_1.getData();
//            replica2 = replica_2.getData();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        StringBuilder output = new StringBuilder();
//        for (String line : replica1) {
//            String key = line.split(";")[0];
//            String value = line.split(";")[1];
//
//            output.append("replica1_transfer ").append(key).append(" ").append(value).append("\r\n");
//            toBeMoved.add(line);
//        }
//        ac.writeWithoutCarriageReturn(output.toString());
//        try {
//            ac.readline();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        StringBuilder output2 = new StringBuilder();
//        for (String line : replica2) {
//            String key = line.split(";")[0];
//            String value = line.split(";")[1];
//
//            output2.append("replica2_transfer ").append(key).append(" ").append(value).append("\r\n");
//        }
//        ac.writeWithoutCarriageReturn(output2.toString());
//        try {
//            ac.readline();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        replica_1.deleteAll();
//        replica_2.deleteAll();
//
//        for(String line: toBeMoved) {
//            String key = line.split(";")[0];
//            String value = line.split(";")[1];
//
//            try {
//                replica_2.put(key, value);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
