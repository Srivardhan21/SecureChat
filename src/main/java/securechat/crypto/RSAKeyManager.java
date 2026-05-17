package securechat.crypto;

import javax.crypto.Cipher;
import java.security.*;
import java.util.Base64;

public class RSAKeyManager {
    private KeyPair keyPair;

    public RSAKeyManager() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        this.keyPair = gen.generateKeyPair();
    }

    public PublicKey getPublicKey() { return keyPair.getPublic(); }
    public PrivateKey getPrivateKey() { return keyPair.getPrivate(); }

    // Encrypt a session key using recipient's RSA public key
    public static String encrypt(byte[] data, PublicKey pubKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(data));
    }

    // Decrypt using own RSA private key
    public static byte[] decrypt(String data, PrivateKey privKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privKey);
        return cipher.doFinal(Base64.getDecoder().decode(data));
    }

    // Serialize public key to Base64 string for DB storage
    public static String encodeKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // Deserialize public key from Base64 string
    public static PublicKey decodeRSAPublicKey(String encoded) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(encoded);
        return KeyFactory.getInstance("RSA").generatePublic(new java.security.spec.X509EncodedKeySpec(keyBytes));
    }
}
