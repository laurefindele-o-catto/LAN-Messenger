package main.java.controller;

import main.java.client.ClientConnection;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert.AlertType;
import main.java.user.Database;
import main.java.user.Session;
import main.java.user.User;
import main.java.util.UserInterface;
import main.java.util.sceneChange;
import java.io.*;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Button backBtn;
    @FXML private ImageView logoImage;
    @FXML private Label errorLabel;

    public void initialize() {
        String initial = "-fx-background-color:  #e6f7ff; -fx-text-fill: #595569; -fx-border-color: #3385ff; -fx-border-radius: 8; -fx-background-radius: 8;";
        String hover = "-fx-background-color:  grey; -fx-text-fill: white; -fx-border-color: black; -fx-border-radius: 8; -fx-background-radius: 8;";

        backBtn.setStyle(initial);
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(hover));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(initial));

        loginBtn.setStyle(initial);
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(hover));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle(initial));

        backBtn.setOnAction(e -> sceneChange.changeScene("Welcome.fxml", backBtn));
        loginBtn.setOnAction(e -> loginUser());
        passwordField.setOnAction(e->loginBtn.fire());
    }

    private void loginUser() {
        try {
            String username = usernameField.getText();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                showAlert("Unfilled boxes", "Please enter both username and password.");
                return;
            }

            if (username.contains(" ")) {
                errorLabel.setText("Username cannot contain spaces.");
                return;
            }

            String response = ClientConnection.send("LOGIN|" + username + "|" + password);

            if (response.equalsIgnoreCase("SUCCESS")) {
                usernameField.clear();
                passwordField.clear();

                User loggedInUser = Database.loadUser(username);
                Session.setUser(loggedInUser);

                sceneChange.changeScene("Dashboard.fxml", loginBtn, loggedInUser);
            } else if (response.startsWith("ERROR")) {
                String msg[] = response.split("\\|");
                errorLabel.setText(msg[1]);
            } else {
                errorLabel.setText("Unexpected server response.");
            }
        } catch (IOException e) {
            showAlert("Network Error", "Could not connect to server.");
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
