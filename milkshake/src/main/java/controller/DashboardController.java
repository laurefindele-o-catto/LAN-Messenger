////
////package main.java.controller;
////
////import javafx.fxml.FXML;
////import javafx.scene.control.*;
////import javafx.scene.image.Image;
////import javafx.scene.image.ImageView;
////import javafx.scene.layout.VBox;
////import javafx.scene.shape.Circle;
////import main.java.client.ClientConnection;
////import main.java.user.Session;
////import main.java.user.User;
////import main.java.util.UserInterface;
////import main.java.util.sceneChange;
////
////import java.io.File;
////
////public class DashboardController implements UserInterface {
////    @FXML private ImageView photo;
////    @FXML private Label     usernameLabel;
////    @FXML private TextField friendSearch;
////    @FXML private Button    searchBtn;
////    @FXML private ScrollPane resultsPane;
////    @FXML private VBox      resultsBox;
////    @FXML private Label     noUserLabel;
////    @FXML private Button    chatBtn;
////    @FXML private Button    editProfileBtn;
////    @FXML private Button    logoutBtn;
////    @FXML private Button friendRequestsBtn;
////
////    private User user;
////
////    @Override
////    public void setUser(User user) {
////        this.user = user;
////        usernameLabel.setText(user.getUsername());
////        loadProfilePhoto();
////
////        // open persistent socket if not yet connected (protect against double‑connect)
////        ClientConnection conn = ClientConnection.getInstance();
////        if (conn != null && Session.getCurrentUser() != null && !user.getUsername().equals("")) {
////            conn.connect("localhost", 12345, user.getUsername()); // 5555 dewa chilo, changing to 12345
////        }
////    }
////
////    public void initialize() {
////        ClientConnection.getInstance().setCurrentController(this);
////
////        resultsPane.setVisible(false);
////        noUserLabel.setVisible(false);
////        styleButtons();
////
////        friendSearch.textProperty().addListener((obs, oldText, newText) -> {
////            if (newText.trim().isEmpty()) {
////                resultsPane.setVisible(false);
////                noUserLabel.setVisible(false);
////            } else {
////                performSearch(newText.trim());
////            }
////        });
////
////        chatBtn.setOnAction(e -> sceneChange.changeScene("ChatBox.fxml", chatBtn, user));
////        editProfileBtn.setOnAction(e -> sceneChange.changeScene("ProfilePage.fxml", editProfileBtn, user));
////        //logoutBtn.setOnAction(e -> sceneChange.changeScene("Welcome.fxml", logoutBtn));
////        logoutBtn.setOnAction(e -> {
////            // Notify server you're logging out
////            ClientConnection.getInstance().logout(Session.getCurrentUser().getUsername());
////
////            // Clear client-side session
////            ClientConnection.getInstance().clearCurrentUser();
////            Session.clear();
////
////            // Go to welcome screen
////            sceneChange.changeScene("Welcome.fxml", logoutBtn);
////        });
////
////        friendRequestsBtn.setOnAction(e ->
////                sceneChange.changeScene("FriendRequests.fxml", friendRequestsBtn, user)
////        );
////        //searchBtn.setOnAction(e->performSearch(friendSearch.getText()));
////    }
////
////    private void styleButtons() {
////        String initial = "-fx-background-color:black;-fx-text-fill:white;-fx-border-color:white;-fx-border-radius:8;-fx-background-radius:8;";
////        String hover   = "-fx-background-color:#555555;-fx-text-fill:lightblue;-fx-border-color:white;-fx-border-radius:8;-fx-background-radius:8;";
////
////        for (Button b : new Button[]{editProfileBtn, chatBtn, logoutBtn, friendRequestsBtn}) {
////            b.setStyle(initial);
////            b.setOnMouseEntered(e -> b.setStyle(hover));
////            b.setOnMouseExited(e -> b.setStyle(initial));
////        }
////    }
////
////    private void loadProfilePhoto() {
////        if (user.getPhotoPath() != null && !user.getPhotoPath().isEmpty()) {
////            File file = new File(user.getPhotoPath());
////            if (file.exists()) {
////                photo.setImage(new Image(file.toURI().toString()));
////                double radius = Math.min(photo.getFitWidth(), photo.getFitHeight()) / 2;
////                Circle clip = new Circle(
////                        photo.getFitWidth() / 2,
////                        photo.getFitHeight() / 2,
////                        radius
////                );
////                photo.setClip(clip);
////            }
////        }
////    }
////
////    private void performSearch(String query) {
////        resultsBox.getChildren().clear();
////
////        File usersFolder = new File("users");
////        if (!usersFolder.exists()) return;
////
////        File[] files = usersFolder.listFiles((dir, name) -> name.toLowerCase().contains(query.toLowerCase()) && name.endsWith(".ser"));
////
////        if (files == null || files.length == 0) {
////            resultsPane.setVisible(false);
////            noUserLabel.setVisible(true);
////            return;
////        }
////
////        resultsPane.setVisible(true);
////        noUserLabel.setVisible(false);
////
////        for (File f : files) {
////            String uname = f.getName().replace(".ser", "");
////            if (!uname.equals(user.getUsername())) {
////                Button userBtn = new Button(uname);
////                userBtn.setStyle("-fx-background-color: #1a73e8; " +
////                        "-fx-text-fill: white; " +
////                        "-fx-font-weight: bold; " +
////                        "-fx-background-radius: 8; " +
////                        "-fx-cursor: hand;");
////
////                //userBtn.setOnAction(e -> sceneChange.changeScene("ChatBox.fxml", userBtn, user));
////
////                userBtn.setOnAction(e -> {
////                    Session.setSelectedUser(uname);  // store it globally
////                    sceneChange.changeScene("ViewProfile.fxml", userBtn, user);
////                });
////                resultsBox.getChildren().add(userBtn);
////            }
////        }
////    }
////}
//
//
//package main.java.controller;
//
//import javafx.fxml.FXML;
//import javafx.scene.control.*;
//import javafx.scene.image.Image;
//import javafx.scene.image.ImageView;
//import javafx.scene.layout.VBox;
//import javafx.scene.shape.Circle;
//import main.java.client.ClientConnection;
//import main.java.user.Session;
//import main.java.user.User;
//import main.java.util.UserInterface;
//import main.java.util.sceneChange;
//
//import java.io.File;
//
//public class DashboardController implements UserInterface {
//    @FXML private ImageView photo;
//    @FXML private Label     usernameLabel;
//    @FXML private TextField friendSearch;
//    @FXML private Button    searchBtn;
//    @FXML private ScrollPane resultsPane;
//    @FXML private VBox      resultsBox;
//    @FXML private Label     noUserLabel;
//    @FXML private Button    chatBtn;
//    @FXML private Button    editProfileBtn;
//    @FXML private Button    logoutBtn;
//    @FXML private Button friendRequestsBtn;
//
//    private User user;
//
//    @Override
//    public void setUser(User user) {
//        this.user = user;
//        usernameLabel.setText(user.getUsername());
//        loadProfilePhoto();
//
//        // Open persistent socket if not yet connected (protect against double‑connect)
//        ClientConnection conn = ClientConnection.getInstance();
//        if (conn != null && Session.getCurrentUser() != null && !user.getUsername().isEmpty()) {
//            conn.connect("localhost", 12345, user.getUsername());
//        }
//    }
//
//    public void initialize() {
//        ClientConnection.getInstance().setCurrentController(this);
//
//        resultsPane.setVisible(false);
//        noUserLabel.setVisible(false);
//        styleButtons();
//
//        friendSearch.textProperty().addListener((obs, oldText, newText) -> {
//            if (newText.trim().isEmpty()) {
//                resultsPane.setVisible(false);
//                noUserLabel.setVisible(false);
//            } else {
//                performSearch(newText.trim());
//            }
//        });
//
//        chatBtn.setOnAction(e -> sceneChange.changeScene("/view/ChatBox.fxml", chatBtn, user));
//        editProfileBtn.setOnAction(e -> sceneChange.changeScene("ProfilePage.fxml", editProfileBtn, user));
//        logoutBtn.setOnAction(e -> {
//            // Notify server you're logging out
//            ClientConnection.getInstance().logout(Session.getCurrentUser().getUsername());
//
//            // Clear client-side session
//            ClientConnection.getInstance().clearCurrentUser();
//            Session.clear();
//
//            // Go to welcome screen
//            sceneChange.changeScene("Welcome.fxml", logoutBtn);
//        });
//
//        friendRequestsBtn.setOnAction(e ->
//                sceneChange.changeScene("FriendRequests.fxml", friendRequestsBtn, user)
//        );
//    }
//
//    private void styleButtons() {
//        String initial = "-fx-background-color:black;-fx-text-fill:white;-fx-border-color:white;-fx-border-radius:8;-fx-background-radius:8;";
//        String hover   = "-fx-background-color:#555555;-fx-text-fill:lightblue;-fx-border-color:white;-fx-border-radius:8;-fx-background-radius:8;";
//
//        for (Button b : new Button[]{editProfileBtn, chatBtn, logoutBtn, friendRequestsBtn}) {
//            b.setStyle(initial);
//            b.setOnMouseEntered(e -> b.setStyle(hover));
//            b.setOnMouseExited(e -> b.setStyle(initial));
//        }
//    }
//
//    private void loadProfilePhoto() {
//        if (user.getPhotoPath() != null && !user.getPhotoPath().isEmpty()) {
//            File file = new File(user.getPhotoPath());
//            if (file.exists()) {
//                try {
//                    photo.setImage(new Image(file.toURI().toString()));
//                    double radius = Math.min(photo.getFitWidth(), photo.getFitHeight()) / 2;
//                    Circle clip = new Circle(
//                            photo.getFitWidth() / 2,
//                            photo.getFitHeight() / 2,
//                            radius
//                    );
//                    photo.setClip(clip);
//                } catch (Exception e) {
//                    System.err.println("Failed to load profile photo: " + e.getMessage());
//                }
//            } else {
//                System.err.println("Profile photo file does not exist at: " + user.getPhotoPath());
//            }
//        } else {
//            System.err.println("Photo path is null or empty for user: " + user.getUsername());
//        }
//    }
//
//    private void performSearch(String query) {
//        resultsBox.getChildren().clear();
//
//        File usersFolder = new File("users");
//        if (!usersFolder.exists()) return;
//
//        File[] files = usersFolder.listFiles((dir, name) -> name.toLowerCase().contains(query.toLowerCase()) && name.endsWith(".ser"));
//
//        if (files == null || files.length == 0) {
//            resultsPane.setVisible(false);
//            noUserLabel.setVisible(true);
//            return;
//        }
//
//        resultsPane.setVisible(true);
//        noUserLabel.setVisible(false);
//
//        for (File f : files) {
//            String uname = f.getName().replace(".ser", "");
//            if (!uname.equals(user.getUsername())) {
//                Button userBtn = new Button(uname);
//                userBtn.setStyle("-fx-background-color: #1a73e8; " +
//                        "-fx-text-fill: white; " +
//                        "-fx-font-weight: bold; " +
//                        "-fx-background-radius: 8; " +
//                        "-fx-cursor: hand;");
//
//                userBtn.setOnAction(e -> {
//                    Session.setSelectedUser(uname);  // Store it globally
//                    sceneChange.changeScene("ViewProfile.fxml", userBtn, user);
//                });
//                resultsBox.getChildren().add(userBtn);
//            }
//        }
//    }
//}
package main.java.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import main.java.client.ClientConnection;
import main.java.user.Session;
import main.java.user.User;
import main.java.util.UserInterface;
import main.java.util.sceneChange;

import java.io.File;

public class DashboardController implements UserInterface {
    @FXML private ImageView photo;
    @FXML private Label     usernameLabel;
    @FXML private TextField friendSearch;
    @FXML private Button    searchBtn;
    @FXML private ScrollPane resultsPane;
    @FXML private VBox      resultsBox;
    @FXML private Label     noUserLabel;
    @FXML private Button    chatBtn;
    @FXML private Button    editProfileBtn;
    @FXML private Button    logoutBtn;
    @FXML private Button friendRequestsBtn;

    private User user;

    @Override
    public void setUser(User user) {
        this.user = user;
        usernameLabel.setText(user.getUsername());
        loadProfilePhoto();

        // Open persistent socket if not yet connected (protect against double‑connect)
        ClientConnection conn = ClientConnection.getInstance();
        if (conn != null && Session.getCurrentUser() != null && !user.getUsername().isEmpty()) {
            conn.connect("localhost", 12345, user.getUsername());
        }
    }

    public void initialize() {
        ClientConnection.getInstance().setCurrentController(this);

        resultsPane.setVisible(false);
        noUserLabel.setVisible(false);
        styleButtons();

        friendSearch.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.trim().isEmpty()) {
                resultsPane.setVisible(false);
                noUserLabel.setVisible(false);
            } else {
                performSearch(newText.trim());
            }
        });

        chatBtn.setOnAction(e -> sceneChange.changeScene("ChatBox.fxml", chatBtn, user));
        editProfileBtn.setOnAction(e -> sceneChange.changeScene("ProfilePage.fxml", editProfileBtn, user));
        logoutBtn.setOnAction(e -> {
            // Notify server you're logging out
            ClientConnection.getInstance().logout(Session.getCurrentUser().getUsername());

            // Clear client-side session
            ClientConnection.getInstance().clearCurrentUser();
            Session.clear();

            // Go to welcome screen
            sceneChange.changeScene("Welcome.fxml", logoutBtn);
        });

        friendRequestsBtn.setOnAction(e ->
                sceneChange.changeScene("FriendRequests.fxml", friendRequestsBtn, user)
        );
    }

    private void styleButtons() {
        String initial = "-fx-background-color:black;-fx-text-fill:white;-fx-border-color:white;-fx-border-radius:8;-fx-background-radius:8;";
        String hover   = "-fx-background-color:#555555;-fx-text-fill:lightblue;-fx-border-color:white;-fx-border-radius:8;-fx-background-radius:8;";

        for (Button b : new Button[]{editProfileBtn, chatBtn, logoutBtn, friendRequestsBtn}) {
            b.setStyle(initial);
            b.setOnMouseEntered(e -> b.setStyle(hover));
            b.setOnMouseExited(e -> b.setStyle(initial));
        }
    }

    private void loadProfilePhoto() {
        File imgFile = new File("users/" + user.getUsername() + "/profile.jpg");
        Image image;

        if (imgFile.exists()) {
            image = new Image(imgFile.toURI().toString(), false);
            if (image.isError()) {
                System.out.println("Error loading image: " + image.getException());
            }
        } else {
            image = new Image(getClass().getResource("/images/default.jpeg").toString());
        }

        photo.setImage(image);
        double radius = Math.min(photo.getFitWidth(), photo.getFitHeight()) / 2;
        Circle clip = new Circle(photo.getFitWidth() / 2, photo.getFitHeight() / 2, radius);
        photo.setClip(clip);

//        if (user.getPhotoPath() != null && !user.getPhotoPath().isEmpty()) {
//            File file = new File(user.getPhotoPath());
//            if (file.exists()) {
//                try {
//                    photo.setImage(new Image(file.toURI().toString()));
//                    double radius = Math.min(photo.getFitWidth(), photo.getFitHeight()) / 2;
//                    Circle clip = new Circle(
//                            photo.getFitWidth() / 2,
//                            photo.getFitHeight() / 2,
//                            radius
//                    );
//                    photo.setClip(clip);
//                } catch (Exception e) {
//                    System.err.println("Failed to load profile photo: " + e.getMessage());
//                }
//            } else {
//                System.err.println("Profile photo file does not exist at: " + user.getPhotoPath());
//            }
//        } else {
//            System.err.println("Photo path is null or empty for user: " + user.getUsername());
//        }
    }

    private void performSearch(String query) {
        resultsBox.getChildren().clear();

        File usersFolder = new File("users");
        if (!usersFolder.exists()) return;

        File[] files = usersFolder.listFiles((dir, name) -> name.toLowerCase().contains(query.toLowerCase()) && name.endsWith(".ser"));

        if (files == null || files.length == 0) {
            resultsPane.setVisible(false);
            noUserLabel.setVisible(true);
            return;
        }

        resultsPane.setVisible(true);
        noUserLabel.setVisible(false);

        for (File f : files) {
            String uname = f.getName().replace(".ser", "");
            if (!uname.equals(user.getUsername())) {
                Button userBtn = new Button(uname);
                userBtn.setStyle("-fx-background-color: #1a73e8; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;");

                userBtn.setOnAction(e -> {
                    Session.setSelectedUser(uname);  // Store it globally
                    sceneChange.changeScene("ViewProfile.fxml", userBtn, user);
                });
                resultsBox.getChildren().add(userBtn);
            }
        }
    }
}
