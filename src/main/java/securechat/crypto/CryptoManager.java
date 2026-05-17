package securechat.crypto;


import javax.crypto.SecretKey;
import java.security.PublicKey;

public class CryptoManager {
    private RSAKeyManager rsa;
    private DHKeyExchange dh;
    private DSASignatureHandler dsa;
    private SecretKey sessionKey;
    private String lastMessageHash = "GENESIS"; // Seed for hash chain

    public CryptoManager() throws Exception {
        this.rsa = new RSAKeyManager();
        this.dh = new DHKeyExchange();
        this.dsa = new DSASignatureHandler();
    }

    // Call after DH handshake with the other party
    public void establishSessionKey(PublicKey otherDHPublicKey) throws Exception {
        dh.generateSharedSecret(otherDHPublicKey);
        this.sessionKey = dh.getAESSessionKey();
    }

    // Full send pipeline: Hash → Chain → Sign → Encrypt
    public EncryptedMessage encryptMessage(String plaintext) throws Exception {
        String chainedHash = HashVerifier.chainHash(plaintext, lastMessageHash);
        String signature = dsa.sign(chainedHash);
        String[] encrypted = AESEncryptor.encrypt(plaintext, sessionKey);
        lastMessageHash = chainedHash;

        return new EncryptedMessage(
                encrypted[0],  // ciphertext
                encrypted[1],  // iv
                chainedHash,   // hash
                signature      // DSA signature
        );
    }

    // Full receive pipeline: Decrypt → Verify Hash → Verify Signature
    public String decryptMessage(EncryptedMessage msg, PublicKey senderDSAPublicKey,
                                 String expectedPrevHash) throws Exception {
        String plaintext = AESEncryptor.decrypt(msg.getCiphertext(), msg.getIv(), sessionKey);
        String expectedHash = HashVerifier.chainHash(plaintext, expectedPrevHash);

        if (!expectedHash.equals(msg.getHash()))
            throw new SecurityException("⚠️ TAMPER DETECTED: Message hash mismatch!");

        if (!DSASignatureHandler.verify(msg.getHash(), msg.getSignature(), senderDSAPublicKey))
            throw new SecurityException("⚠️ IDENTITY SPOOFING DETECTED: Invalid signature!");

        lastMessageHash = msg.getHash();
        return plaintext;
    }

    public RSAKeyManager getRSA() { return rsa; }
    public DHKeyExchange getDH() { return dh; }
    public DSASignatureHandler getDSA() { return dsa; }
}