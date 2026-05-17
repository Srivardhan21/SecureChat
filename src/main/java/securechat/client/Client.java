package securechat.client;

import securechat.crypto.*;
import java.io.*;
import java.net.Socket;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.*;

public class Client {
    private static final String HOST = "centerbeam.proxy.rlwy.net";
    private static final int PORT = 47747;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private CryptoManager crypto;

    // Queue for direct responses (LOGIN_OK, REGISTER_OK, KEY|...)
    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

    // Per-contact session
    private Map<String, SecureSession> sessions = new HashMap<>();

    public interface MessageListener {
        void onMessage(String sender, String plaintext);
        void onTamperAlert(String sender, String warning);
    }

    private MessageListener messageListener;

    public Client() throws Exception {
        this.crypto = new CryptoManager();
    }

    public void connect() throws Exception {
        socket = new Socket(HOST, PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        new Thread(this::listenForMessages).start();
        System.out.println("✅ Connected to server at " + HOST + ":" + PORT);
    }

    // Single listener thread — routes messages to queue or UI callback
    private void listenForMessages() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("INCOMING|")) {
                    handleIncoming(line); // Route to UI
                } else {
                    responseQueue.put(line); // Route to waiting main thread
                }
            }
        } catch (Exception e) {
            System.out.println("🔌 Disconnected from server.");
        }
    }

    // Safe blocking read — waits for server response
    private String waitForResponse() throws Exception {
        return responseQueue.poll(5, TimeUnit.SECONDS);
    }

    // Replace only the register() method in Client.java
    public boolean register(String username, String password) throws Exception {
        String passwordHash = HashVerifier.hash(password);
        String rsaPub = Base64.getEncoder()
                .encodeToString(crypto.getRSA().getPublicKey().getEncoded());
        String dsaPub = Base64.getEncoder()
                .encodeToString(crypto.getDSA().getPublicKey().getEncoded());
        String dhPub  = Base64.getEncoder()
                .encodeToString(crypto.getDH().getPublicKey().getEncoded());

        // Now sends DH key too
        out.println("REGISTER|" + username + "|" + passwordHash
                + "|" + rsaPub + "|" + dsaPub + "|" + dhPub);

        String response = waitForResponse();
        System.out.println("📋 Register response: " + response);
        return "REGISTER_OK".equals(response);
    }

    public boolean login(String username, String password) throws Exception {
        String passwordHash = HashVerifier.hash(password);
        out.println("LOGIN|" + username + "|" + passwordHash);

        String response = waitForResponse();
        System.out.println("Login response: " + response);
        if ("LOGIN_OK".equals(response)) {
            this.username = username;

            // Update DH public key in DB with current session's fresh key
            String freshDHPub = Base64.getEncoder()
                    .encodeToString(crypto.getDH().getPublicKey().getEncoded());
            out.println("UPDATE_DH|" + username + "|" + freshDHPub);

            System.out.println("Logged in as: " + username);
            return true;
        }
        return false;
    }

    // Replace only initiateKeyExchange() in Client.java
    public void initiateKeyExchange(String targetUser) throws Exception {
        out.println("GET_KEY|" + targetUser);
        String response = waitForResponse();

        if (response == null || !response.startsWith("KEY|")) {
            throw new Exception("Failed to get keys for " + targetUser);
        }

        // KEY|dhPubKey|dsaPubKey  ← DH is now parts[1], DSA is parts[2]
        String[] parts = response.split("\\|", -1);
        PublicKey theirDH  = DHKeyExchange.decodeDHPublicKey(parts[1]);
        PublicKey theirDSA = DSASignatureHandler.decodeDSAPublicKey(parts[2]);

        crypto.establishSessionKey(theirDH);

        SecureSession session = new SecureSession(theirDSA, "GENESIS");
        sessions.put(targetUser, session);
        System.out.println("🔑 Key exchange complete with " + targetUser);
    }

    public void sendMessage(String targetUser, String plaintext) throws Exception {
        if (!sessions.containsKey(targetUser)) {
            initiateKeyExchange(targetUser);
        }

        SecureSession session = sessions.get(targetUser);
        EncryptedMessage msg = crypto.encryptMessage(plaintext);

        // Include own DH + DSA public keys so receiver can set up session directly
        String myDHPub  = Base64.getEncoder()
                .encodeToString(crypto.getDH().getPublicKey().getEncoded());
        String myDSAPub = Base64.getEncoder()
                .encodeToString(crypto.getDSA().getPublicKey().getEncoded());

        // MESSAGE|receiver|ciphertext|iv|hash|prevHash|signature|senderDHPub|senderDSAPub
        out.println("MESSAGE|" + targetUser
                + "|" + msg.getCiphertext()
                + "|" + msg.getIv()
                + "|" + msg.getHash()
                + "|" + session.getLastHash()
                + "|" + msg.getSignature()
                + "|" + myDHPub
                + "|" + myDSAPub);

        session.setLastHash(msg.getHash());
        System.out.println("📤 Sent to " + targetUser + ": " + plaintext);
    }

    private void handleIncoming(String line) {
        // INCOMING|sender|ciphertext|iv|hash|prevHash|signature|senderDHPub|senderDSAPub
        String[] parts = line.split("\\|", -1);
        if (parts.length < 9) return;

        String sender      = parts[1];
        String cipher      = parts[2];
        String iv          = parts[3];
        String hash        = parts[4];
        String prevHash    = parts[5];
        String sig         = parts[6];
        String senderDHPub = parts[7];
        String senderDSAPub= parts[8];

        EncryptedMessage msg = new EncryptedMessage(cipher, iv, hash, sig);

        try {
            // If no session, build one directly from keys in the message — no GET_KEY needed
            if (!sessions.containsKey(sender)) {
                PublicKey theirDH  = DHKeyExchange.decodeDHPublicKey(senderDHPub);
                PublicKey theirDSA = DSASignatureHandler.decodeDSAPublicKey(senderDSAPub);
                crypto.establishSessionKey(theirDH);
                SecureSession newSession = new SecureSession(theirDSA, prevHash);
                sessions.put(sender, newSession);
                System.out.println("🔑 Session established with: " + sender);
            }

            SecureSession session = sessions.get(sender);

            String plaintext = crypto.decryptMessage(
                    msg, session.getSenderDSAKey(), prevHash
            );
            session.setLastHash(hash);

            System.out.println("📨 [" + sender + "]: " + plaintext);
            if (messageListener != null)
                messageListener.onMessage(sender, plaintext);

        } catch (SecurityException e) {
            System.out.println(e.getMessage());
            if (messageListener != null)
                messageListener.onTamperAlert(sender, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            // Show error in UI
            if (messageListener != null) {
                messageListener.onTamperAlert(sender,
                        "Receive error: " + e.getMessage());
            }
        }
    }
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public String getUsername() { return username; }
}