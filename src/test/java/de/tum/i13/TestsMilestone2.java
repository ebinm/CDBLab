package de.tum.i13;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestsMilestone2 {

    private PrintWriter output;
    private BufferedReader input;
    public static Integer port = 5153;

    public String doRequest(Socket s, String req) throws IOException {

        output.write(req + "\r\n");
        output.flush();

        return input.readLine();
    }

    public String doRequest(String req) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress("127.0.0.1", port));
        String res = doRequest(s, req);
        s.close();

        return res;
    }

    @BeforeAll
    static void setUpECS() {
        Thread th = new Thread() {
            public void run() {
                try {
                    de.tum.i13.ecs.StartECSServer.main(new String[]{"-b 127.0.0.1:5153"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
    }

    @Test
    public void connectTest() throws InterruptedException, IOException {
        Thread th = new Thread() {
            @Override
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:5153", "-p3154"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start(); // started the server
        Thread.sleep(2000);

        Socket s = new Socket();
        s.connect(new InetSocketAddress("127.0.0.1", 3154));
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
        String line = input.readLine();
        String shouldBe = "Connection to MSRG Echo server established: /127.0.0.1:3154";
        s.close();
        assertEquals(line, shouldBe);
    }

    @Test
    public void putGetDeleteTest() throws IOException, InterruptedException {
        Thread th = new Thread() {
            @Override
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:5153", "-p3155"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start(); // started the server
        Thread.sleep(2000);

        Socket s = new Socket();
        s.connect(new InetSocketAddress("127.0.0.1", 3155));
        this.output = new PrintWriter(s.getOutputStream());
        this.input = new BufferedReader(new InputStreamReader(s.getInputStream()));
        input.readLine();

        String request = "put test value";
        String shouldBe = "put_success test";
        assertEquals(doRequest(s, request), shouldBe);

        request = "get test";
        shouldBe = "get_success test value";
        assertEquals(doRequest(s, request), shouldBe);

        request = "delete test";
        shouldBe = "delete_success test";
        assertEquals(doRequest(s, request), shouldBe);
        s.close();

    }

    @Test
    public void persistentTest() throws IOException, InterruptedException {
        Thread th = new Thread() {
            @Override
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:5153", "-p3156"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start(); // started the server
        Thread.sleep(2000);

        Socket s = new Socket();
        s.connect(new InetSocketAddress("127.0.0.1", 3156));
        this.output = new PrintWriter(s.getOutputStream());
        this.input = new BufferedReader(new InputStreamReader(s.getInputStream()));
        input.readLine();

        String request = "put test value";
        doRequest(s, request);


        Thread th2 = new Thread() {
            @Override
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:5153", "-p3157"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th2.start(); // started the server
        Thread.sleep(2000);

        request = "get test";
        String shouldBe = "get_success test value";
        assertEquals(doRequest(s, request), shouldBe);

        doRequest(s, "delete test");
        s.close();
    }
}
