package de.tum.i13.Project;

import de.tum.i13.ecs.ActiveConnection;
import de.tum.i13.ecs.StartECSServer;
import de.tum.i13.server.nio.StartSimpleNioServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Performance {

    private StartECSServer ecs;
    private int port = 6153;


    @BeforeAll
    static void setUpDirectory() throws IOException {
        try {
            Files.createDirectory(Path.of("PerformanceTest"));
        } catch (FileAlreadyExistsException ignored) {

        }
    }

    @BeforeEach
    public void setUpECS() {
        ecs = new StartECSServer();
        System.out.println("--------------Starting ECS on " + System.currentTimeMillis() + "-------------------------");
        Thread thread = new Thread() {
            public void run() {
                try {
                    ecs.main(new String[]{"-b 127.0.0.1:" + port, "-p" + port});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Thread closed");
            }
        };
        thread.start();
        port += 100;

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void stopECS() {
        System.out.println("---------Shutting down ECS on " + System.currentTimeMillis() + " ------------------------");
        try {
            ecs.stopECS();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    /**
     * Method tests the performance of the ECS replacement with another KV Server to add on different
     * size of data
     */
    public void testVariousDataSize() throws IOException {

        Thread nio1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:" + port, "-p" + 6359, "-dPerformanceTest/" +6359});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        nio1.start();

        Socket s = new Socket("127.0.0.1", 6359);
        PrintWriter output = new PrintWriter(s.getOutputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));

        ActiveConnection activeConnection = new ActiveConnection(s, output, input);

        System.out.println(activeConnection.readline());

        List<String> keys = new LinkedList<>();
        for (int i = 1; i <= 500; i++) {
            String key = randomString(30);
            String value = randomString(30);

            activeConnection.write("put " + key + " " + value);
            String message = activeConnection.readline();
            if (!message.equals("put_success " + key)) {
                i = i - 1;
            } else {
                keys.add(key);
            }
        }

        Thread nio2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:" + port, "-p" + 6360,
                            "-dPerformanceTest/" +6360});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        nio2.start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        activeConnection.write("ecs_address");
        String in = activeConnection.readline();
        System.out.println(in);
        assertEquals("ecs_address_success 127.0.0.1:" + port, in);

        System.out.println("Removing ESC Server!");
        stopECS();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(String key: keys) {
            activeConnection.write("delete " + key);
            String message = activeConnection.readline();
            assertEquals("delete_success " + key, message);
        }

        try {
            activeConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    /**
     * Method tests the performance of the ECS replacement with another KV Server to add on different
     * size of data
     */
    public void testVariousKVs() throws IOException {

//        int firstPort = 6359;
        int firstPort = getRandomNumber(49152, 65535);
        Thread nio1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:" + port, "-p" + firstPort,
                            "-dPerformanceTest/" +firstPort, "-ll=OFF"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        nio1.start();

        Socket s = new Socket("127.0.0.1", firstPort);
        PrintWriter output = new PrintWriter(s.getOutputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));

        ActiveConnection activeConnection = new ActiveConnection(s, output, input);

        System.out.println(activeConnection.readline());

        System.out.println("Putting in data to the first kv server");
        List<String> keys = new LinkedList<>();
        for (int i = 1; i <= 500; i++) {
            String key = randomString(30);
            String value = randomString(30);

            activeConnection.write("put " + key + " " + value);
            String message = activeConnection.readline();
            if (!message.equals("put_success " + key)) {
                i = i - 1;
            } else {
                keys.add(key);
            }
        }

        System.out.println("Starting more kv servers");
        List<Integer> ports = new LinkedList<>();
        ports.add(firstPort);
        for(int i = 1; i <=5; i++) {

            int id;
            do {
                id = getRandomNumber(49152, 65535);
            } while (ports.contains(id));
            ports.add(id);

            int finalId = id;

            Thread nio = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:" + port, "-p" + finalId,
                                "-dPerformanceTest/" + finalId, "-ll=OFF"});
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            nio.start();

//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Removing ESC Server!");
        stopECS();

        System.out.println("Finished");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to generate a random String with a maximum length of the value bound
     */
    public String randomString(int bound) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = (new Random()).nextInt(bound) + 1;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString;
    }

    public int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }
}
