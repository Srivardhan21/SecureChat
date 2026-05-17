package securechat.ui;

import securechat.client.Client;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    private Client client;

    public void setClient(Client client) {
        this.client = client;
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("❌ Username and password cannot be empty.");
            return;
        }
        if (!password.equals(confirm)) {
            statusLabel.setText("❌ Passwords do not match.");
            return;
        }

        try {
            boolean success = client.register(username, password);
            if (success) {
                statusLabel.setStyle("-fx-text-fill: #a6e3a1;");
                statusLabel.setText("✅ Registered! Redirecting to login...");
                new Thread(() -> {
                    try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(this::goToLogin);
                }).start();
            } else {
                statusLabel.setText("❌ Username already taken. Try another.");
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Registration error: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToLogin() {
        goToLogin();
    }

    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/login.fxml")
            );
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 400, 500));
            LoginController controller = loader.getController();
            controller.setExistingClient(client);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}