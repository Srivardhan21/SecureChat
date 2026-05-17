import securechat.crypto.CryptoManager;
import securechat.crypto.EncryptedMessage;

// TestCrypto.java — run this to verify the pipeline before demo
public class TestCrypto {
    public static void main(String[] args) throws Exception {
        CryptoManager alice = new CryptoManager();
        CryptoManager bob = new CryptoManager();

        // DH Key Exchange
        alice.establishSessionKey(bob.getDH().getPublicKey());
        bob.establishSessionKey(alice.getDH().getPublicKey());

        // Alice sends
        EncryptedMessage msg = alice.encryptMessage("Hello Bob, this is secret!");
        System.out.println("Ciphertext: " + msg.getCiphertext());

        // Bob receives
        String decrypted = bob.decryptMessage(msg, alice.getDSA().getPublicKey(), "GENESIS");
        System.out.println("Decrypted: " + decrypted);

        // Tamper test
        EncryptedMessage tampered = new EncryptedMessage(
                msg.getCiphertext(), msg.getIv(), "FAKEHASH123", msg.getSignature()
        );
        try {
            bob.decryptMessage(tampered, alice.getDSA().getPublicKey(), "GENESIS");
        } catch (SecurityException e) {
            System.out.println("Tamper caught: " + e.getMessage()); // ✅ Expected
        }
    }
}
