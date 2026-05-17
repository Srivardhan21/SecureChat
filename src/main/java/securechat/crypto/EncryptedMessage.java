package securechat.crypto;

public class EncryptedMessage {
    private String ciphertext, iv, hash, signature;

    public EncryptedMessage(String ciphertext, String iv, String hash, String signature) {
        this.ciphertext = ciphertext;
        this.iv = iv;
        this.hash = hash;
        this.signature = signature;
    }

    public String getCiphertext() { return ciphertext; }
    public String getIv() { return iv; }
    public String getHash() { return hash; }
    public String getSignature() { return signature; }
}