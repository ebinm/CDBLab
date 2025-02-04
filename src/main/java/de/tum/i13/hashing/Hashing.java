package de.tum.i13.hashing;

import de.tum.i13.ecs.StartECSServer;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketImpl;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

public class Hashing {

    public static String getHash(String input) {
        byte[] bytes = input.getBytes();
        String value = "";

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(bytes);
            byte[] digested = messageDigest.digest();
            BigInteger bigInteger = new BigInteger(1, digested);
            value =  bigInteger.toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (value.length() < 32) {
            while (value.length() < 32) {
                value = "0" + value;
            }
        }
        return value;
    }

    public static void main(String[] args) throws IOException {

////        String number1 = getHash("Hello World");
////        System.out.println(number1);
////        String number2 = getHash("Hello Ebin");
////        System.out.println(number2);
////        System.out.println("FFFFFE".compareTo("FFFFFF"));
//        BigInteger test = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE", 16);
//        System.out.println(test.toString());
//        test = test.add(new BigInteger(String.valueOf(2))).mod(new BigInteger("100000000000000000000000000000000", 16));
//        System.out.println(test.toString(16).toUpperCase());
//        System.out.println(test.toString());

//        //System.out.println(getHash("127.0.0.1:5155"));
//        //System.out.println(new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16));
//        //byte[] bytes = (new BigInteger("240282366920938463463374607431768211455")).toByteArray();
//        byte[] bytes = new byte[]{1, -1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
//        //byte[] bytes = new byte[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
//        long test = ByteBuffer.wrap(bytes).getLong();
//        System.out.println(test);
//
////        System.out.println(test);
////        String hex = Long.toUnsignedString(test, 10);
////        System.out.println(hex);
////        System.out.println(Long.parseUnsignedLong(hex, 16));
////        System.out.println("75BC709C2D0E508".compareTo("fffffffffffffff"));
////        Byte.toUnsignedInt(bytes[0]);
//
//        BigInteger bigInteger = new BigInteger(1, bytes);
//        System.out.println(bigInteger);
//        //bigInteger = BigInteger.valueOf(test);
//        byte[] b1 = bigInteger.toByteArray();
//        for (int i = 0; i  < b1.length; i++) {
//            System.out.format("0x%02X\t", b1[i]);
//        }
//
//        System.out.println();
//        String hex = bigInteger.toString(16);
//        if (hex.length() < 32) {
//            while (hex.length() < 32) {
//                hex = "0" + hex;
//            }
//        }
//        System.out.println(hex);
//
//        String predecessorFrom = (new BigInteger("0")).subtract(BigInteger.ONE).
//                mod(new BigInteger("100000000000000000000000000000000", 16)).toString(16);
//        System.out.println(predecessorFrom);

//        String string = "transfer\r\n" + "put Ebin Madan\r\n" + "put Leon Madan\r\n" + "put Linti Madan\r\n" + "put Benny Madan\r\n";
//        System.out.println(string);
//        String[] strings = string.trim().split(" ");
//        System.out.println(string.substring(10));

//        String test1 = "replica1_put";
//        String test2 = "put";
//
//        String[] output1 = test1.split("_");
//        for(String s: output1) {
//            System.out.println(s);
//        }
//        String[] output2 = test2.split("_");
//        for(String s: output2) {
//            System.out.println(s);
//        }

//        ServerSocket serverSocket = new ServerSocket();
//        serverSocket.setReuseAddress(true);
//        serverSocket.bind(new InetSocketAddress("127.0.0.1", 5199));
//        Socket socket = serverSocket.accept();
//        System.out.println(socket.getInetAddress()+":"+socket.getPort());
//        serverSocket.close();
//
//        ServerSocket serverSocket1 = new ServerSocket();
//        serverSocket1.bind(new InetSocketAddress("127.0.0.1", 5199));

//        StringBuilder output = new StringBuilder();
//        output.append("transfer ").append("Ebin").append(" ").append("Madan").append("\r\n");
//        output.delete(output.length()-2, output.length());
//        System.out.println(output.toString());

//        StartECSServer startECSServer = new StartECSServer();
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    StartECSServer.main(new String[]{"-b 127.0.0.1:5153"});
//                    } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        thread.start();
//
//        try {
//            Thread.sleep(20000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
////        thread.interrupt();
//        startECSServer.stopECS();
//
//        System.out.println("Thread has been interupted");

        for (int i = 0; i < 100; i++) {
            givenUsingJava8_whenGeneratingRandomAlphanumericString_thenCorrect();
        }
    }

    public static void givenUsingJava8_whenGeneratingRandomAlphanumericString_thenCorrect() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = (new Random()).nextInt(20) + 1;
        Random random = new Random();

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        System.out.println(generatedString);
    }
}
