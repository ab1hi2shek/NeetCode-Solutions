package lld.others;

import java.util.*;

public class URLShortener {

    private Map<Integer, String> map = new HashMap<>();
    private int id = 1; // Auto-incrementing ID for URLs
    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    // Encodes an integer ID to a base62 string
    private String toBase62(int num) {
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(BASE62.charAt(num % 62));
            num /= 62;
        }
        return sb.reverse().toString();
    }

    // Decodes a base62 string to an integer ID
    private int toBase10(String str) {
        int num = 0;
        for (char c : str.toCharArray()) {
            num = num * 62 + BASE62.indexOf(c);
        }
        return num;
    }

    // Encodes a URL to a shortened URL.
    public String encode(String longUrl) {
        map.put(id, longUrl);
        String shortKey = toBase62(id);
        id++;
        return "http://short.url/" + shortKey;
    }

    // Decodes a shortened URL to its original URL.
    public String decode(String shortUrl) {
        String shortKey = shortUrl.replace("http://short.url/", "");
        int key = toBase10(shortKey);
        return map.get(key);
    }
}
