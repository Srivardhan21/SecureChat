package securechat.crypto;


import java.security.*;
import java.util.Base64;

public class DSASignatureHandler {
    private KeyPair keyPair;

    public DSASignatureHandler() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA");
        gen.initialize(2048);
        this.keyPair = gen.generateKeyPair();
    }

    public PublicKey getPublicKey() { return keyPair.getPublic(); }

    // Sign the message hash using DSA private key
    public String sign(String data) throws Exception {
        Signature sig = Signature.getInstance("SHA256withDSA");
        sig.initSign(keyPair.getPrivate());
        sig.update(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(sig.sign());
    }

    // Verify the signature using sender's DSA public key
    public static boolean verify(String data, String signature, PublicKey pubKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withDSA");
        sig.initVerify(pubKey);
        sig.update(data.getBytes("UTF-8"));
        return sig.verify(Base64.getDecoder().decode(signature));
    }

    public static PublicKey decodeDSAPublicKey(String encoded) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(encoded);
        return KeyFactory.getInstance("DSA")
                .generatePublic(new java.security.spec.X509EncodedKeySpec(keyBytes));
    }
}