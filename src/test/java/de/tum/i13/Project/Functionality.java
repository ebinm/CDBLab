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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Functionality {

    private StartECSServer ecs;
    private int port  = 5153;

    @BeforeAll
    static void setUpDirectory() throws IOException {
        try {
            Files.createDirectory(Path.of("FunctionalityTest"));
        } catch (FileAlreadyExistsException ignored) {

        }
    }

    @BeforeEach
    public void setUpECS() {
        ecs = new StartECSServer();
        Thread thread = new Thread() {
            public void run() {
                try {
                    ecs.main(new String[]{"-b 127.0.0.1:" + port, "-p"+port});
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
        try {
            ecs.stopECS();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    /**
     * Method tests whether one of the KV Servers converts to the new ECS Server, after removing the original ECS.
     * Therefore, the new method ecs_address is used, to identify the current ECS Server.
     */
    public void testEcsFailure() throws IOException {

        Thread nio1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:"+port, "-p"+5259, "-dFunctionalityTest/"+5259});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        nio1.start();

        Thread nio2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:"+port, "-p"+5260, "-dFunctionalityTest/"+5260});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        nio2.start();

        Socket s = new Socket("127.0.0.1", 5260);
        PrintWriter output = new PrintWriter(s.getOutputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));

        ActiveConnection activeConnection = new ActiveConnection(s, output, input);

        System.out.println(activeConnection.readline());

//        activeConnection.write("put apple juice");
//        System.out.println(activeConnection.readline());
//
//        activeConnection.write("put school bus");
//        System.out.println(activeConnection.readline());
//
//        activeConnection.write("put university student");
//        System.out.println(activeConnection.readline());

        activeConnection.write("ecs_address");
        String in = activeConnection.readline();
        System.out.println(in);
        assertEquals("ecs_address_success 127.0.0.1:"+port, in);

        System.out.println("Removing ESC Server!");
        stopECS();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        activeConnection.write("ecs_address");
        in = activeConnection.readline();
        System.out.println(in);
        assertEquals("ecs_address_success 127.0.0.1:5259", in);

        try {
            activeConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    /**
     * Method tests whether the reallocation after removing the ECS Server works with more KV Servers in a limited
     * timeframe.
     */
    public void testECSfailureWithMoreKVs() throws IOException {

        Thread nio1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:"+port, "-p"+5159, "-dFunctionalityTest/"+5159});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        nio1.start();

        Thread nio2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:"+port, "-p"+5160, "-dFunctionalityTest/"+5160});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        nio2.start();

        Thread nio3 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:"+port, "-p"+5161, "-dFunctionalityTest/"+5161});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        nio3.start();

        Thread nio4 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:"+port, "-p"+5162, "-dFunctionalityTest/"+5162});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        nio4.start();

        Socket s1 = new Socket("127.0.0.1", 5159);
        PrintWriter output1 = new PrintWriter(s1.getOutputStream());
        BufferedReader input1 = new BufferedReader(new InputStreamReader(s1.getInputStream()));
        ActiveConnection activeConnection = new ActiveConnection(s1, output1, input1);
        System.out.println(activeConnection.readline());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        activeConnection.write("keyrange");
        String in = activeConnection.readline();
        System.out.println(in);

        String[] numberKVs = in.split(";");
        assertEquals(4, numberKVs.length);

//        activeConnection.write("put apple juice");
//        System.out.println(activeConnection.readline());
//
//        activeConnection.write("put school bus");
//        System.out.println(activeConnection.readline());
//
//        activeConnection.write("put university student");
//        System.out.println(activeConnection.readline());

        System.out.println("Removing ESC Server!");
        stopECS();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        activeConnection.write("keyrange");
        in = activeConnection.readline();
        System.out.println(in);

        numberKVs = in.split(";");
        assertEquals(3, numberKVs.length);

        try {
            activeConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    /**
     * Method tests whether the data of the old KV Server is transferred to another KV Server
     * after transforming into the ECS
     */
    public void testEcsFailureWithData() throws IOException {

        Thread nio1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:"+port, "-p"+5359, "-dFunctionalityTest/"+5359});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        nio1.start();

        Thread nio2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:"+port, "-p"+5360, "-dFunctionalityTest/"+5360});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        nio2.start();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Putting all data into the KV Server, that is going to become the new ECS
        Socket s = new Socket("127.0.0.1", 5360);
        PrintWriter output = new PrintWriter(s.getOutputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));

        ActiveConnection activeConnection = new ActiveConnection(s, output, input);

        System.out.println(activeConnection.readline());

        activeConnection.write("put apple juice");
        System.out.println(activeConnection.readline());

        activeConnection.write("put school bus");
        System.out.println(activeConnection.readline());

        activeConnection.write("put university student");
        System.out.println(activeConnection.readline());

        activeConnection.write("ecs_address");
        String in = activeConnection.readline();
        System.out.println(in);

        try {
            activeConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Removing ESC Server!");
        stopECS();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Connecting to the only KV Server left
        s = new Socket("127.0.0.1", 5359);
        output = new PrintWriter(s.getOutputStream());
        input = new BufferedReader(new InputStreamReader(s.getInputStream()));

        activeConnection = new ActiveConnection(s, output, input);
        System.out.println(activeConnection.readline());

        activeConnection.write("ecs_address");
        in = activeConnection.readline();
        System.out.println(in);

        activeConnection.write("delete apple");
        in = activeConnection.readline();
        assertEquals("delete_success apple", in);
        System.out.println(in);

        activeConnection.write("delete school");
        in = activeConnection.readline();
        assertEquals("delete_success school", in);
        System.out.println(in);

        activeConnection.write("delete university");
        in = activeConnection.readline();
        assertEquals("delete_success university", in);
        System.out.println(in);

        try {
            activeConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    /**
     * Method tests whether the data of the old KV Server is transferred to another KV Server
     * after transforming into the new ECS
     */
    public void testAddingKVtoNewECS() throws IOException {

        Thread nio1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:"+port, "-p"+5459, "-dFunctionalityTest/"+5459});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        nio1.start();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Putting all data into the KV Server, that is going to become the new ECS
        Socket s = new Socket("127.0.0.1", 5459);
        PrintWriter output = new PrintWriter(s.getOutputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));

        ActiveConnection activeConnection = new ActiveConnection(s, output, input);

        System.out.println(activeConnection.readline());

        activeConnection.write("put apple juice");
        System.out.println(activeConnection.readline());

        activeConnection.write("put school bus");
        System.out.println(activeConnection.readline());

        activeConnection.write("put university student");
        System.out.println(activeConnection.readline());

        activeConnection.write("ecs_address");
        String in = activeConnection.readline();
        System.out.println(in);

        try {
            activeConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Removing ESC Server!");
        stopECS();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Adding new KV Server to old KV Server as its ECS
        Thread nio2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StartSimpleNioServer.main(new String[]{"-b 127.0.0.1:5459", "-p"+5460, "-dFunctionalityTest/"+5460});
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

        //Connecting to the new KV Server to check for the old data
        s = new Socket("127.0.0.1", 5460);
        output = new PrintWriter(s.getOutputStream());
        input = new BufferedReader(new InputStreamReader(s.getInputStream()));

        activeConnection = new ActiveConnection(s, output, input);
        System.out.println(activeConnection.readline());

        activeConnection.write("ecs_address");
        in = activeConnection.readline();
        System.out.println(in);

        activeConnection.write("delete apple");
        in = activeConnection.readline();
        assertEquals("delete_success apple", in);
        System.out.println(in);

        activeConnection.write("delete school");
        in = activeConnection.readline();
        assertEquals("delete_success school", in);
        System.out.println(in);

        activeConnection.write("delete university");
        in = activeConnection.readline();
        assertEquals("delete_success university", in);
        System.out.println(in);

        try {
            activeConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Test
//    public void testEcsShutDown() {
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        stopECS();
//        System.out.println("Stopping ECS!");
//
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        System.out.println("Is it closed?");
//    }
}
