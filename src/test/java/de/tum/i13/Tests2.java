package de.tum.i13;


import de.tum.i13.server.echo.ECSManager;
import de.tum.i13.server.echo.EchoLogic;
import de.tum.i13.shared.Config;
import org.junit.jupiter.api.Test;



import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Tests2 {

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

    @Test
public void testHash () throws IOException{

String input= "Hello World";

String Expected = "b10a8db164e0754105b7a99be72e3fe5" ;

    String value = de.tum.i13.hashing.Hashing.getHash(input);

    assertEquals(Expected, value );


    }
@Test
public void testUpdate_metaData () throws InterruptedException, IOException {

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

    String command = "update_metaData 1,127.0.0.1:5532;2,127.0.0.2:5531";
    ecs.process(command);
    assertTrue(ecs.process(command));
}
@Test
    public void testTransfer () throws InterruptedException, IOException {

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

        String command = "transfer:127.0.0.1:5532; Hello; World";
        ecs.process(command);
        assertTrue(ecs.process(command));
    }
    @Test
    public void testTransfer_all() throws InterruptedException, IOException {

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

        String command = "transfer_all:127.0.0.1:5532; ALL";
        ecs.process(command);
        assertTrue(ecs.process(command));
    }
@Test
    public void testShutDown () throws InterruptedException, IOException {

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

    String command= "shutDown" ;
    ecs.process(command);
    assertTrue(ecs.process(command));

    }

}




