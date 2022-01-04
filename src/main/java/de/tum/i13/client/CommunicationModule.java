package de.tum.i13.client;

import de.tum.i13.hashing.Hashing;
import de.tum.i13.server.kv.KVMessage;
import de.tum.i13.server.kv.KVStore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static de.tum.i13.client.ClientSideApplication.logger;

public class CommunicationModule implements KVStore {

    private Map<String, String> metaData;
    private ActiveConnection activeConnection;
    private int attempts;


    public CommunicationModule(ActiveConnection activeConnection) {
        this.activeConnection = activeConnection;

//        KVMessage kvMessage = new KVMessage() {
//            @Override
//            public String getKey() {
//                return null;
//            }
//
//            @Override
//            public String getValue() {
//                return null;
//            }
//
//            @Override
//            public StatusType getStatus() {
//                return null;
//            }
//
//            @Override
//            public String getKeyRange() {
//                return null;
//            }
//        };

//        try {
//            /*kvMessage = */
//            keyRange();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        this.metaData = convertMetaData(kvMessage.getKeyRange());
    }

    @Override
    public KVMessage put(String key, String value) throws Exception {

        checkConnection(key);

        if (value.equals("null")) {
            activeConnection.write("delete " + key);
        } else {
            activeConnection.write("put " + key + " " + value);
        }
        String input;
        input = activeConnection.readline();
        String[] message = input.split(" ");

        switch (message[0]) {
            case "put_success":
                return new KVMessage() {
                    @Override
                    public String getKey() {
                        return key;
                    }

                    @Override
                    public String getValue() {
                        return value;
                    }

                    @Override
                    public StatusType getStatus() {
                        return StatusType.PUT_SUCCESS;
                    }

                    @Override
                    public String getKeyRange() {
                        return null;
                    }
                };
            case "put_update":
                return new KVMessage() {
                    @Override
                    public String getKey() {
                        return key;
                    }

                    @Override
                    public String getValue() {
                        return value;
                    }

                    @Override
                    public StatusType getStatus() {
                        return StatusType.PUT_UPDATE;
                    }

                    @Override
                    public String getKeyRange() {
                        return null;
                    }
                };
            case "put_error":
                return new KVMessage() {
                    @Override
                    public String getKey() {
                        return key;
                    }

                    @Override
                    public String getValue() {
                        return value;
                    }

                    @Override
                    public StatusType getStatus() {
                        return StatusType.PUT_ERROR;
                    }

                    @Override
                    public String getKeyRange() {
                        return null;
                    }
                };
            case "delete_success":
                return new KVMessage() {
                    @Override
                    public String getKey() {
                        return key;
                    }

                    @Override
                    public String getValue() {
                        return null;
                    }

                    @Override
                    public StatusType getStatus() {
                        return StatusType.DELETE_SUCCESS;
                    }

                    @Override
                    public String getKeyRange() {
                        return null;
                    }
                };
            case "delete_error":
                return new KVMessage() {
                    @Override
                    public String getKey() {
                        return key;
                    }

                    @Override
                    public String getValue() {
                        return null;
                    }

                    @Override
                    public StatusType getStatus() {
                        return StatusType.DELETE_ERROR;
                    }

                    @Override
                    public String getKeyRange() {
                        return null;
                    }
                };
            case "server_not_responsible":
                /*KVMessage kvMessage = */
                keyRange();
                return put(key, value);
            case "server_stopped":
                backOff(this.attempts++);
                KVMessage result = put(key, value);
                this.attempts = 0;
                return result;
            case "server_write_lock":
                return new KVMessage() {
                    @Override
                    public String getKey() {
                        return null;
                    }

                    @Override
                    public String getValue() {
                        return null;
                    }

                    @Override
                    public StatusType getStatus() {
                        return StatusType.SERVER_WRITE_LOCK;
                    }

                    @Override
                    public String getKeyRange() {
                        return null;
                    }
                };
        }
        throw new RuntimeException("Error received from Server while using PUT command!: " + input);
    }

    @Override
    public KVMessage get(String key) throws Exception {

        //checkConnection(key);

        activeConnection.write("get " + key);
        String input;
        input = activeConnection.readline();
        String[] message = input.split(" ");

        switch (message[0]) {
            case "get_success":
                StringBuilder value = new StringBuilder();
                for (int i = 2; i < message.length; i++) {
                    value.append(message[i]).append(" ");
                }
                value = new StringBuilder(value.substring(0, value.length() - 1));
                String finalValue = value.toString();
                return new KVMessage() {
                    @Override
                    public String getKey() {
                        return key;
                    }

                    @Override
                    public String getValue() {
                        return finalValue;
                    }

                    @Override
                    public StatusType getStatus() {
                        return StatusType.GET_SUCCESS;
                    }

                    @Override
                    public String getKeyRange() {
                        return null;
                    }
                };
            case "get_error":
                return new KVMessage() {
                    @Override
                    public String getKey() {
                        return key;
                    }

                    @Override
                    public String getValue() {
                        return null;
                    }

                    @Override
                    public StatusType getStatus() {
                        return StatusType.GET_ERROR;
                    }

                    @Override
                    public String getKeyRange() {
                        return null;
                    }
                };
            case "server_not_responsible":
                /*KVMessage kvMessage = */
                keyRange();
                checkConnection(key);
                return get(key);
            case "server_stopped":
                backOff(this.attempts++);
                KVMessage result = get(key);
                this.attempts = 0;
                return result;
        }
        throw new RuntimeException("Error received from Server while using GET command!: " + input);
    }

    @Override
    public KVMessage keyRange() throws Exception {
        activeConnection.write("keyrange");
        String input;
        input = activeConnection.readline();

        String[] message = input.split(" ");

        switch (message[0]) {
            case "keyrange_success":
                this.metaData = convertMetaData(message[1]);

                return new KVMessage() {
                    @Override
                    public String getKey() {
                        return null;
                    }

                    @Override
                    public String getValue() {
                        return null;
                    }

                    @Override
                    public StatusType getStatus() {
                        return StatusType.KEYRANGE_SUCCESS;
                    }

                    @Override
                    public String getKeyRange() {
                        return message[1];
                    }
                };
            case "server_stopped":
                backOff(this.attempts++);
                KVMessage result =keyRange();
                this.attempts = 0;
                return result;
        }
        throw new RuntimeException("Error received from Server while using keyrange command!");
    }

    public KVMessage keyRangeRead() throws Exception {
        activeConnection.write("keyrange_read");
        String input;
        input = activeConnection.readline();

        String[] message = input.split(" ");

        switch (message[0]) {
            case "keyrange_read_success":

                return new KVMessage() {
                    @Override
                    public String getKey() {
                        return null;
                    }

                    @Override
                    public String getValue() {
                        return null;
                    }

                    @Override
                    public StatusType getStatus() {
                        return StatusType.KEYRANGE_READ_SUCCESS;
                    }

                    @Override
                    public String getKeyRange() {
                        return message[1];
                    }
                };
            case "server_stopped":
                backOff(this.attempts++);
                KVMessage result =keyRangeRead();
                this.attempts = 0;
                return result;
        }
        throw new RuntimeException("Error received from Server while using keyrange_read command!");
    }

    private Map<String, String> convertMetaData(String metaDataString) {
        String[] serverData = metaDataString.split(";");
        Map<String, String> result = new HashMap<>();

        for (int i = 0; i < serverData.length; i++) {
            String[] temp = serverData[i].split(",");
            String range = temp[0] + "-" + temp[1];
            String serverInfo = temp[2];
            result.put(range, serverInfo);
        }

        return result;
    }

    private void updateConnection(String host, int port) throws Exception {
        this.activeConnection.close();
        EchoConnectionBuilder echoConnectionBuilder = new EchoConnectionBuilder(host, port);
        this.activeConnection = echoConnectionBuilder.connect();
        logger.info(activeConnection.readline());
    }

    public void close() throws Exception {
        activeConnection.close();
    }

    private String rangeOf(String input) {

        String inputHash = hash(input);

        String from = "";
        String to = "";
        for (String range : metaData.keySet()) {
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

    private void checkConnection(String key) throws Exception {
        String range = rangeOf(key);
        String serverInfo = metaData.get(range);

        if (!serverInfo.equals(activeConnection.getServerInfo())) {
            String host = serverInfo.split(":")[0];
            String port = serverInfo.split(":")[1];
            updateConnection(host, Integer.parseInt(port));
        }
    }

    private String hash(String input) {
        return Hashing.getHash(input);
    }

    private void backOff(int attempts) throws InterruptedException {
        Random r = new Random();
        int sleep = r.nextInt(Math.min(10000, (int) Math.pow(2, attempts) * 2));
        Thread.sleep(sleep);
    }
}
