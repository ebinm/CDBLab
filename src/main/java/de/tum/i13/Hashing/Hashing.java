package de.tum.i13.Hashing;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;

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

    public static void main(String[] args) {
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

        //System.out.println(getHash("127.0.0.1:5155"));
        //System.out.println(new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16));
        //byte[] bytes = (new BigInteger("240282366920938463463374607431768211455")).toByteArray();
        byte[] bytes = new byte[]{1, -1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        //byte[] bytes = new byte[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
        long test = ByteBuffer.wrap(bytes).getLong();
        System.out.println(test);

//        System.out.println(test);
//        String hex = Long.toUnsignedString(test, 10);
//        System.out.println(hex);
//        System.out.println(Long.parseUnsignedLong(hex, 16));
//        System.out.println("75BC709C2D0E508".compareTo("fffffffffffffff"));
//        Byte.toUnsignedInt(bytes[0]);

        BigInteger bigInteger = new BigInteger(1, bytes);
        System.out.println(bigInteger);
        //bigInteger = BigInteger.valueOf(test);
        byte[] b1 = bigInteger.toByteArray();
        for (int i = 0; i  < b1.length; i++) {
            System.out.format("0x%02X\t", b1[i]);
        }

        System.out.println();
        String hex = bigInteger.toString(16);
        if (hex.length() < 32) {
            while (hex.length() < 32) {
                hex = "0" + hex;
            }
        }
        System.out.println(hex);
    }
}
