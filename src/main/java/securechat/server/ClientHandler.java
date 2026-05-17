package securechat.server;

import securechat.db.DBManager;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            String line;
            while ((line = in.readLine()) != null) {
                handleCommand(line);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + username);
        } finally {
            if (username != null) Server.onlineUsers.remove(username);
        }
    }

    private void handleCommand(String line) {
        String[] parts = line.split("\\|", -1);
        switch (parts[0]) {

            case "REGISTER":
                boolean ok = DBManager.registerUser(
                        parts[1], parts[2], parts[3], parts[4], parts[5]
                );
                out.println(ok ? "REGISTER_OK" : "REGISTER_FAIL");
                break;

            case "LOGIN":
                String stored = DBManager.getPasswordHash(parts[1]);
                if (stored != null && stored.equals(parts[2])) {
                    this.username = parts[1];
                    Server.onlineUsers.put(username, this);
                    out.println("LOGIN_OK");
                } else {
                    out.println("LOGIN_FAIL");
                }
                break;

            case "GET_KEY":
                String dh  = DBManager.getDHPublicKey(parts[1]);
                String dsa = DBManager.getDSAPublicKey(parts[1]);
                out.println("KEY|" + dh + "|" + dsa);
                break;

            case "UPDATE_DH":
                // UPDATE_DH|username|freshDHPubKey
                DBManager.updateDHKey(parts[1], parts[2]);
                System.out.println("DH key updated for: " + parts[1]);
                break;

            case "MESSAGE":
                // MESSAGE|receiver|ciphertext|iv|hash|prevHash|signature|senderDHPub|senderDSAPub
                String receiver = parts[1];
                DBManager.saveMessage(username, receiver,
                        parts[2], parts[4], parts[5], parts[6], parts[3]);
                ClientHandler receiverHandler = Server.onlineUsers.get(receiver);
                if (receiverHandler != null) {
                    receiverHandler.sendMessage(
                            "INCOMING|" + username
                                    + "|" + parts[2]  // ciphertext
                                    + "|" + parts[3]  // iv
                                    + "|" + parts[4]  // hash
                                    + "|" + parts[5]  // prevHash
                                    + "|" + parts[6]  // signature
                                    + "|" + parts[7]  // senderDHPub
                                    + "|" + parts[8]  // senderDSAPub
                    );
                }
                break;
        }
    }
    public void sendMessage(String msg) { out.println(msg); }
}