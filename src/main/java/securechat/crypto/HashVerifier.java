package securechat.crypto;


import java.security.MessageDigest;
import java.util.Base64;

public class HashVerifier {

    // Generate SHA-256 hash of the message content
    public static String hash(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    // Verify that the hash matches the message
    public static boolean verify(String data, String expectedHash) throws Exception {
        return hash(data).equals(expectedHash);
    }

    // Hash chaining: hash(currentMessage + previousHash)
    public static String chainHash(String message, String prevHash) throws Exception {
        return hash(message + prevHash);
    }
}