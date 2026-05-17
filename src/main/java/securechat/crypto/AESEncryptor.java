package securechat.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

public class AESEncryptor {

    // Encrypt plaintext using AES-256-CBC with a random IV
    public static String[] encrypt(String plaintext, SecretKey key) throws Exception {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // Returns [ciphertext, iv] both Base64 encoded
        return new String[]{
                Base64.getEncoder().encodeToString(encrypted),
                Base64.getEncoder().encodeToString(iv)
        };
    }

    // Decrypt ciphertext using AES-256-CBC with the same IV
    public static String decrypt(String ciphertext, String ivStr, SecretKey key) throws Exception {
        byte[] iv = Base64.getDecoder().decode(ivStr);
        byte[] encrypted = Base64.getDecoder().decode(ciphertext);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return new String(cipher.doFinal(encrypted), "UTF-8");
    }
}