package de.tum.i13;


import de.tum.i13.server.echo.ECSManager;
import de.tum.i13.server.echo.EchoLogic;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import org.junit.jupiter.api.*;


import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestsMilestone3 {

    private PrintWriter output;
    private BufferedReader input;
    public static Integer port = 6153;
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

    //@BeforeEach
    public void setUpECS() {
        Thread th = new Thread() {
            public void run() {
                try {
                    de.tum.i13.ecs.StartECSServer.main(new String[]{"-p"+port});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
        port++;
    }

    //@Test
    @Order(1)
    public void testHash() throws IOException {

        String input = "Hello World";

        String Expected = "b10a8db164e0754105b7a99be72e3fe5";

        String value = de.tum.i13.hashing.Hashing.getHash(input);

        assertEquals(Expected, value);


    }

    //@Test
    @Order(4)
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

        //InetSocketAddress IntSocket = new InetSocketAddress("127.0.0.1", port);

        Config con = Config.parseCommandlineArgs(new String[]{"-b 127.0.0.1:"+port});

        System.out.println(con);

        EchoLogic eco = new EchoLogic(con);
        ECSManager ecs = new ECSManager(con.bootstrap, eco);

        String command = "update_metaData 1-2,127.0.0.1:5532;3-4,127.0.0.2:5531";
        ecs.process(command);
        assertEquals("1,2,127.0.0.1:5532;3,4,127.0.0.2:5531;", ecs.getMetaDataString());


    }

    //@Test
    @Order(3)
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

        //InetSocketAddress IntSocket = new InetSocketAddress("127.0.0.1", port);

        Config con = Config.parseCommandlineArgs(new String[]{"-b 127.0.0.1:"+port});

        EchoLogic eco = new EchoLogic(con);

        //ECSManager ecs = new ECSManager(con.bootstrap, eco);

        eco.setInitialization(false);


        String command = "transfer Hello World";
        eco.process(command);
        String actual = eco.get("Hello");
        assertEquals("get_success Hello World", actual);
    }

    //@Test
    @Order(2)
    public void testTransfer_all() throws InterruptedException, IOException {

        Thread th = new Thread() {
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:"+port, "-p5159", "-d5159"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
        Thread.sleep(2000);

        Socket socket = new Socket("127.0.0.1", 5159);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Constants.TELNET_ENCODING));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Constants.TELNET_ENCODING));

        out.write("put Test Transfer\r\n");
        out.flush();
        String test = in.readLine();
        test = in.readLine();

        //InetSocketAddress IntSocket = new InetSocketAddress("127.0.0.1", port);

        Thread th2 = new Thread() {
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:"+port, "-p5155", "-d5155"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th2.start();
        Thread.sleep(2000);

//        Config con = Config.parseCommandlineArgs(new String[]{"-b 127.0.0.1:5153"});
//
//        EchoLogic eco = new EchoLogic(con);
//
//        eco.setInitialization(false);
//
//        ECSManager ecs = eco.getEcsManager();

//        out.write("transfer_all x 127.0.0.1:5153 ALL\r\n");
//        out.flush();

        socket.close();
        in.close();
        out.close();

        socket = new Socket("127.0.0.1", 5155);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Constants.TELNET_ENCODING));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Constants.TELNET_ENCODING));

        out.write("get Test\r\n");
        out.flush();

        in.readLine();
        String actual = in.readLine();
        assertEquals("get_success Test Transfer", actual);
    }

    //@Test
    @Order(4)
    public void testRetry() throws InterruptedException, IOException {

        Thread th = new Thread() {
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:"+port, "-p5160", "-d5160"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
        Thread.sleep(2000);

        Socket socket = new Socket("127.0.0.1", 5160);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Constants.TELNET_ENCODING));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Constants.TELNET_ENCODING));

        out.write("put Test Retry\r\n");
        out.flush();

        Thread th1 = new Thread() {
            public void run() {
                try {
                    de.tum.i13.server.nio.StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:"+port, "-p5161", "-d5161"});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th1.start();
        Thread.sleep(2000);

        socket.close();
        in.close();
        out.close();

        socket = new Socket("127.0.0.1", 5161);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Constants.TELNET_ENCODING));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Constants.TELNET_ENCODING));

        out.write("get Test Retry\r\n");
        out.flush();

        in.readLine();
        String input = in.readLine();

        assertEquals("server_not_responsible", input);
    }


}




