package de.tum.i13.client;

import de.tum.i13.server.kv.KVMessage;
import de.tum.i13.server.kv.KVStore;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class CommunicationModule extends ActiveConnection implements KVStore {


    public CommunicationModule(Socket socket, PrintWriter output, BufferedReader input) {
        super(socket, output, input);
    }

    @Override
    public KVMessage put(String key, String value) throws Exception {

        if (value.equals("null")) {
            super.write("delete " + key);
        } else {
            super.write("put " + key + " " + value);
        }
        String input;
        input = super.readline();
        String[] message = input.split(" ");

        switch (message[0]) {
            case "PUT_SUCCESS": return new KVMessage() {
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
            };
            case "PUT_UPDATE": return new KVMessage() {
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
            };
            case "PUT_ERROR": return new KVMessage() {
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
            };
            case "DELETE_SUCCESS": return new KVMessage() {
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
            };
            case "DELETE_ERROR": return new KVMessage() {
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
            };
        }
        throw new RuntimeException("Error received from Server whle using PUT command!: " + input);
    }

    @Override
    public KVMessage get(String key) throws Exception {
        super.write("get " + key);
        String input;
        input = super.readline();
        String[] message = input.split(" ");

        switch (message[0]) {
            case "GET_SUCCESS":
                String value = "";
                for (int i = 2; i < message.length; i++) {
                    value = message[i] + " ";
                }
                value = value.substring(0, value.length() - 1);
                String finalValue = value;
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
            };
            case "GET_ERROR": return new KVMessage() {
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
            };
        }
        throw new RuntimeException("Error received from Server while using GET command!: " + input);
    }
}
