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
            case "put_success": return new KVMessage() {
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
            case "put_update": return new KVMessage() {
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
            case "put_error": return new KVMessage() {
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
            case "delete_success": return new KVMessage() {
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
            case "delete_error": return new KVMessage() {
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
            };
            case "get_error": return new KVMessage() {
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
