package main.java.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import main.java.client.ClientConnection;
import main.java.user.Database;
import main.java.user.Session;
import main.java.user.User;
import main.java.util.UserInterface;
import main.java.util.sceneChange;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shows another user's public profile and lets the current user send a
 * friend-request (or view its state).
 */
public class ViewProfileController implements UserInterface,
        ClientConnection.MessageListener {

    /* ─────────────────────  FXML  ───────────────────── */
    @FXML private ImageView profilePic;
    @FXML private Label     usernameLabel;
    @FXML private Label     bioLabel;
    @FXML private Button    sendFriendRequestBtn;
    @FXML private Button    backBtn;

    /* ─────────────────────  STATE ───────────────────── */
    private User currentUser;   // logged-in user
    private User targetUser;    // profile we are viewing

    /* ───────────────────  INITIALISE ────────────────── */
    @FXML
    private void initialize() {
        ClientConnection conn = ClientConnection.getInstance();
        conn.registerListener(this);
        conn.setCurrentController(this);

        backBtn.setOnAction(e ->
                sceneChange.changeScene("Dashboard.fxml", backBtn, currentUser));
    }

    /** Called immediately after the scene is loaded. */
    @Override
    public void setUser(User ignored) {
        Session.refreshCurrentUser();
        currentUser = Session.getCurrentUser();

        String selected = Session.getSelectedUser();
        if (selected == null) return;

        targetUser = Database.loadUser(selected);
        if (targetUser == null) return;

        updateProfileView();
    }

    /* ──────────────────  RENDERING  ────────────────── */
    private void updateProfileView() {
        usernameLabel.setText(targetUser.getUsername());
        bioLabel.setText(targetUser.getBio());

        loadAndClipAvatar();
        updateFriendButton();
    }

    /** Load avatar from disk, packaged fallback, or external config. */
    private void loadAndClipAvatar() {

        // 1️⃣ user-specific photo on disk
        File userPhoto = new File("users/" + targetUser.getUsername() + "/profile.jpg");
        if (userPhoto.exists()) {
            setImage(new Image(userPhoto.toURI().toString(), false));
            return;
        }

        // 2️⃣ packaged default inside resources/images
        URL packaged = getClass().getResource("/images/default.jpeg");
        if (packaged != null) {
            setImage(new Image(packaged.toString(), false));
            return;
        }

        // 3️⃣ external configurable default
        Path external = Path.of("config/default-avatar.jpg");
        if (Files.exists(external)) {
            setImage(new Image(external.toUri().toString(), false));
            return;
        }

        // 4️⃣ nothing found – leave previous image (log it once)
        System.err.println("[ViewProfile] No avatar found for "
                + targetUser.getUsername()
                + "  disk=" + userPhoto.getAbsolutePath()
                + "  packaged=/images/default.jpeg"
                + "  external=" + external.toAbsolutePath());
    }

    /** Helper to set the image and apply a circular clip once sized. */
    private void setImage(Image img) {
        profilePic.setImage(img);
        profilePic.imageProperty().addListener(new ChangeListener<>() {
            @Override public void changed(ObservableValue<? extends Image> obs,
                                          Image oldImg, Image newImg) {
                double r = Math.min(profilePic.getBoundsInLocal().getWidth(),
                        profilePic.getBoundsInLocal().getHeight()) / 2;
                profilePic.setClip(new Circle(r, r, r));
                profilePic.imageProperty().removeListener(this);
            }
        });
    }

    /* ─────────────  FRIEND REQUEST BUTTON  ───────────── */
    private void updateFriendButton() {

        if (currentUser.getUsername().equals(targetUser.getUsername())) {
            sendFriendRequestBtn.setVisible(false);
            return;
        }

        sendFriendRequestBtn.setVisible(true);

        if (currentUser.getFriends().contains(targetUser.getUsername())) {
            sendFriendRequestBtn.setText("Friends");
            sendFriendRequestBtn.setDisable(true);

        } else if (currentUser.getFriendRequestsSent()
                .contains(targetUser.getUsername())) {
            sendFriendRequestBtn.setText("Request Sent");
            sendFriendRequestBtn.setDisable(true);

        } else if (currentUser.getFriendRequestsReceived()
                .contains(targetUser.getUsername())) {
            sendFriendRequestBtn.setText("Sent you friend request");
            sendFriendRequestBtn.setDisable(true);

        } else {
            sendFriendRequestBtn.setText("Send Friend Request");
            sendFriendRequestBtn.setDisable(false);
            sendFriendRequestBtn.setOnAction(e -> {
                ClientConnection.getInstance()
                        .sendRequest(targetUser.getUsername());

                sendFriendRequestBtn.setText("Request Sent");
                sendFriendRequestBtn.setDisable(true);
            });
        }
    }

    /* ───────────────────  LIVE REFRESH  ─────────────────── */
    public void refreshProfile() {
        Platform.runLater(() -> {
            Session.refreshCurrentUser();
            currentUser = Session.getCurrentUser();
            targetUser  = Database.loadUser(Session.getSelectedUser());
            if (targetUser != null) updateFriendButton();
        });
    }

    /* ────────────────  SERVER CALLBACK  ──────────────── */
    @Override
    public void onMessageReceived(String from, String body) {
        if (body.startsWith("ACCEPTED_REQUEST_FROM")
                || body.startsWith("DECLINED_REQUEST_FROM")) {

            Session.refreshCurrentUser();
            currentUser = Session.getCurrentUser();
            targetUser  = Database.loadUser(Session.getSelectedUser());

            if (targetUser != null) Platform.runLater(this::updateProfileView);
        }
    }
}
