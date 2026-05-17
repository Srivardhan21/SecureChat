package securechat.ui;

import securechat.client.Client;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ChatController implements Client.MessageListener {
    @FXML private ListView<String> contactList;
    @FXML private VBox messageBox;
    @FXML private TextField messageInput;
    @FXML private TextField newContactField;
    @FXML private ScrollPane scrollPane;
    @FXML private Label warningLabel;
    @FXML private Label chatWithLabel;

    private Client client;

    public void setClient(Client client) {
        this.client = client;
        client.setMessageListener(this);
        chatWithLabel.setText("Welcome, " + client.getUsername() + " 🔒");
    }

    @FXML
    private void handleAddContact() {
        String contact = newContactField.getText().trim();
        if (!contact.isEmpty() && !contactList.getItems().contains(contact)) {
            contactList.getItems().add(contact);
            newContactField.clear();
        }
    }

    @FXML
    private void handleSend() {
        String target = contactList.getSelectionModel().getSelectedItem();
        String text   = messageInput.getText().trim();

        if (target == null) {
            warningLabel.setText("⚠️ Select a contact first");
            return;
        }
        if (text.isEmpty()) return;

        try {
            client.sendMessage(target, text);
            addMessageBubble("You: " + text, true);
            messageInput.clear();
            warningLabel.setText("");
        } catch (Exception e) {
            warningLabel.setText("❌ Send failed: " + e.getMessage());
        }
    }

    @Override
    public void onMessage(String sender, String plaintext) {
        Platform.runLater(() -> {
            if (!contactList.getItems().contains(sender))
                contactList.getItems().add(sender);
            addMessageBubble(sender + ": " + plaintext, false);
        });
    }

    @Override
    public void onTamperAlert(String sender, String warning) {
        Platform.runLater(() -> {
            warningLabel.setText(warning);
            warningLabel.setStyle("-fx-text-fill: #f38ba8; -fx-font-weight: bold;");
            addTamperAlert(warning);
        });
    }

    private void addMessageBubble(String text, boolean isSent) {
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(350);
        bubble.setStyle(isSent
                ? "-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e;" +
                "-fx-padding: 8 12; -fx-background-radius: 12;"
                : "-fx-background-color: #313244; -fx-text-fill: #cdd6f4;" +
                "-fx-padding: 8 12; -fx-background-radius: 12;");

        HBox row = new HBox(bubble);
        row.setStyle(isSent
                ? "-fx-alignment: CENTER_RIGHT; -fx-padding: 2 4;"
                : "-fx-alignment: CENTER_LEFT;  -fx-padding: 2 4;");

        messageBox.getChildren().add(row);
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void addTamperAlert(String warning) {
        Label alert = new Label("🚨 " + warning);
        alert.setStyle(
                "-fx-background-color: #f38ba8; -fx-text-fill: #1e1e2e;" +
                        "-fx-padding: 6 12; -fx-background-radius: 8;" +
                        "-fx-font-weight: bold;"
        );
        HBox row = new HBox(alert);
        row.setStyle("-fx-alignment: CENTER;");
        messageBox.getChildren().add(row);
    }
}