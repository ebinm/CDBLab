package de.tum.i13;


import de.tum.i13.server.echo.ECSManager;
import de.tum.i13.server.echo.EchoLogic;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestsMilestone3 {

    private PrintWriter output;
    private BufferedReader input;
    public static Integer port = 5153;
    private Socket Socket;


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

    //@BeforeAll
    static void setUpECS() {
        Thread th = new Thread() {
            public void run() {
                try {
                    de.tum.i13.ecs.StartECSServer.main(new String[]{});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
    }

    //@Test
    public void testHash() throws IOException {

        String input = "Hello World";

        String Expected = "b10a8db164e0754105b7a99be72e3fe5";

        String value = de.tum.i13.hashing.Hashing.getHash(input);

        assertEquals(Expected, value);


    }

    //@Test
    public void testUpdate_metaData() throws InterruptedException, IOException {

//        Thread th = new Thread() {
//            public void run() {
//                try {
//                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:5153"});
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//        th.start();
//        Thread.sleep(2000);

        InetSocketAddress IntSocket = new InetSocketAddress("127.0.0.1", port);

        Config con = Config.parseCommandlineArgs(new String[]{"-b 127.0.0.1:5153"});

        System.out.println(con);

        EchoLogic eco = new EchoLogic(con);

        ECSManager ecs = new ECSManager(IntSocket, eco);

        String command = "update_metaData 1-2,127.0.0.1:5532;3-4,127.0.0.2:5531";
        ecs.process(command);
        assertEquals("1,2,127.0.0.1:5532;3,4,127.0.0.2:5531;", ecs.getMetaDataString());


    }

    //@Test
    public void testTransfer() throws InterruptedException, IOException {

//        Thread th = new Thread() {
//            public void run() {
//                try {
//                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{-b 127.0.0.1:5153});
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//        th.start();
//        Thread.sleep(2000);

        InetSocketAddress IntSocket = new InetSocketAddress("127.0.0.1", port);

        Config con = Config.parseCommandlineArgs(new String[]{"-b 127.0.0.1:5153"});

        EchoLogic eco = new EchoLogic(con);

        ECSManager ecs = new ECSManager(IntSocket, eco);

        eco.setInitialization(false);


        String command = "transfer Hello World";
        eco.process(command);
        assertEquals("get_success Hello World", eco.get("Hello"));
    }

    //@Test
    public void testTransfer_all() throws InterruptedException, IOException {

        Thread th = new Thread() {
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:5153", "-p5155", "-d5155"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
        //Thread.sleep(2000);

        InetSocketAddress IntSocket = new InetSocketAddress("127.0.0.1", port);

        Config con = Config.parseCommandlineArgs(new String[]{"-b 127.0.0.1:5153"});

        EchoLogic eco = new EchoLogic(con);

        eco.setInitialization(false);

        ECSManager ecs = new ECSManager(IntSocket, eco);

        eco.put("Test", "Transfer");

        String command = "transfer_all x 127.0.0.1:5155 ALL";
        ecs.process(command);

        Socket socket = new Socket("127.0.0.1", 5155);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Constants.TELNET_ENCODING));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Constants.TELNET_ENCODING));
        out.write("get Test\r\n");
        out.flush();
        assertEquals("Connection to MSRG Echo server established: /127.0.0.1:5155", in.readLine());
        String input = in.readLine();
        assertEquals("get_success Test Transfer", input);

        th.stop();
    }

    //@Test
    public void testShutDown() throws InterruptedException, IOException {

        Thread th = new Thread() {
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
        Thread.sleep(2000);

        InetSocketAddress IntSocket = new InetSocketAddress("127.0.0.1", port);

        Config con = new Config();

        EchoLogic eco = new EchoLogic(con);

        ECSManager ecs = new ECSManager(IntSocket, eco);

        String command = "shutDown";
        ecs.process(command);
        assertTrue(ecs.process(command));

    }

}




