package securechat.ui;

import securechat.client.Client;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private Client client;

    @FXML
    public void initialize() {
        try {
            client = new Client();
            client.connect();
        } catch (Exception e) {
            statusLabel.setText("❌ Cannot connect to server");
        }
    }

    // Called when coming back from register screen
    public void setExistingClient(Client client) {
        this.client = client;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("❌ Please enter username and password");
            return;
        }

        try {
            boolean success = client.login(username, password);
            if (success) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/chat.fxml")
                );
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(new Scene(loader.load(), 800, 550));
                stage.setResizable(true);
                ChatController controller = loader.getController();
                controller.setClient(client);
            } else {
                statusLabel.setText("❌ Invalid credentials");
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Login error: " + e.getMessage());
        }
    }

    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/register.fxml")
            );
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 400, 500));
            RegisterController controller = loader.getController();
            controller.setClient(client);
        } catch (Exception e) {
            statusLabel.setText("❌ " + e.getMessage());
        }
    }
}