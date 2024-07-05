/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mideaac.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.jose4j.base64url.Base64;

import com.google.gson.JsonObject;

/**
 * Utilities.
 *
 * @author Jacek Dobrowolski - Initial contribution
 */
@NonNullByDefault
public class Utils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static final char[] HEX_ARRAY_LOWERCASE = "0123456789abcdef".toCharArray();
    static byte[] empty = new byte[0];

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String bytesToBinary(byte[] bytes) {
        String s1 = "";
        for (int j = 0; j < bytes.length; j++) {

            s1 = s1.concat(Integer.toBinaryString(bytes[j] & 255 | 256).substring(1));
            s1 = s1.concat(" ");

        }

        return s1;
    }

    public static String bytesToHexLowercase(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY_LOWERCASE[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY_LOWERCASE[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static boolean validateIP(final String ip) {
        String pattern = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

        return ip.matches(pattern);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static byte[] concatenateArrays(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public static byte[] toBytes(short i) {
        ByteBuffer b = ByteBuffer.allocate(2);
        b.order(ByteOrder.BIG_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.
        b.putShort(i);
        return b.array();
    }

    public static byte[] strxor(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length];
        int i = 0;
        for (byte b : array1) {
            result[i] = (byte) (b ^ array2[i++]);
        }
        return result;
    }

    public static String tokenHex(int nbytes) {
        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        for (int n = 0; n < nbytes; n++) {
            sb.append(Integer.toHexString(r.nextInt()));
        }

        return sb.toString().substring(0, nbytes);
    }

    public static String tokenUrlsafe(int nbytes) {
        Random r = new Random();
        byte[] bytes = new byte[nbytes];
        r.nextBytes(bytes);
        return Base64.encode(bytes);
    }

    public static byte[] toIntTo6ByteArray(long i, ByteOrder order) {
        final ByteBuffer bb = ByteBuffer.allocate(8);
        bb.order(order);

        bb.putLong(i);

        if (order == ByteOrder.BIG_ENDIAN) {
            return Arrays.copyOfRange(bb.array(), 2, 8);
        }

        if (order == ByteOrder.LITTLE_ENDIAN) {
            return Arrays.copyOfRange(bb.array(), 0, 6);
        }

        return empty;
    }

    public static String getQueryString(JsonObject json) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> keys = json.keySet().stream().sorted().iterator();
        // sb.append("?"); // start of query args
        while (keys.hasNext()) {
            @SuppressWarnings("null")
            String key = keys.next();
            sb.append(key);
            sb.append("=");
            sb.append(json.get(key).getAsString());
            if (keys.hasNext()) {
                sb.append("&"); // To allow for another argument.
            }
        }

        return sb.toString();
    }
}
