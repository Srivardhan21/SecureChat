import securechat.client.Client;

public class TerminalChatTest {
    public static void main(String[] args) throws Exception {

        // Start Alice
        Client alice = new Client();
        alice.connect();
        alice.register("alice", "password123");
        alice.login("alice", "password123");

        // Start Bob
        Client bob = new Client();
        bob.connect();
        bob.register("bob", "password456");
        bob.login("bob", "password456");

        // Bob listens for messages
        bob.setMessageListener(new Client.MessageListener() {
            @Override
            public void onMessage(String sender, String plaintext) {
                System.out.println("✅ Bob received from " + sender + ": " + plaintext);
            }
            @Override
            public void onTamperAlert(String sender, String warning) {
                System.out.println("🚨 ALERT: " + warning);
            }
        });

        Thread.sleep(500); // Wait for Bob to be ready
        alice.sendMessage("bob", "Hey Bob! This is encrypted!");
        alice.sendMessage("bob", "Second message — hash chaining active!");

        Thread.sleep(1000);
        System.out.println("✅ Day 3 Terminal Test Complete!");
    }
}