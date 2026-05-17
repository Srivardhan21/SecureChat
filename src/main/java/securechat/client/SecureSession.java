package securechat.client;

import java.security.PublicKey;

public class SecureSession {
    private PublicKey senderDSAKey;
    private String lastHash;

    public SecureSession(PublicKey senderDSAKey, String lastHash) {
        this.senderDSAKey = senderDSAKey;
        this.lastHash = lastHash;
    }

    public PublicKey getSenderDSAKey() { return senderDSAKey; }
    public void setSenderDSAKey(PublicKey key) { this.senderDSAKey = key; }
    public String getLastHash() { return lastHash; }
    public void setLastHash(String hash) { this.lastHash = hash; }
}