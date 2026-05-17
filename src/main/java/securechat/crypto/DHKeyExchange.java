package securechat.crypto;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Base64;

public class DHKeyExchange {
    private KeyPair dhKeyPair;
    private byte[] sharedSecret;

    public DHKeyExchange() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("DH");
        gen.initialize(2048);
        this.dhKeyPair = gen.generateKeyPair();
    }

    public PublicKey getPublicKey() { return dhKeyPair.getPublic(); }

    // Generate shared secret using other party's DH public key
    public void generateSharedSecret(PublicKey otherPublicKey) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("DH");
        ka.init(dhKeyPair.getPrivate());
        ka.doPhase(otherPublicKey, true);
        byte[] raw = ka.generateSecret();
        // Derive a 256-bit AES key from shared secret using SHA-256
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        this.sharedSecret = sha.digest(raw);
    }

    // Returns a 256-bit AES session key derived from the DH shared secret
    public SecretKeySpec getAESSessionKey() {
        return new SecretKeySpec(sharedSecret, "AES");
    }

    public static PublicKey decodeDHPublicKey(String encoded) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(encoded);
        return KeyFactory.getInstance("DH")
                .generatePublic(new java.security.spec.X509EncodedKeySpec(keyBytes));
    }
}