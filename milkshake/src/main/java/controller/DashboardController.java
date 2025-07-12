//package main.java.controller;
//
//
//import javafx.fxml.FXML;
//import javafx.scene.control.*;
//import javafx.scene.image.Image;
//import javafx.scene.image.ImageView;
//import javafx.scene.layout.VBox;
//import main.java.user.User;
//import main.java.util.UserInterface;
//import main.java.util.sceneChange;
//
//import java.io.File;
//
//public class DashboardController implements UserInterface {
//    @FXML private ImageView photo;
//    @FXML private Label username;
//    @FXML private TextField friendSearch;
//    @FXML private Button searchBtn;
//    @FXML private ScrollPane resultsPane;
//    @FXML private VBox resultsBox;
//    @FXML private Label noUserLabel;
//    @FXML private Button chatBtn;
//    @FXML private Button editProfileBtn;
//    @FXML private Button logoutBtn;
//
//    private User user;
//
//    @Override
//    public void setUser(User user){
//        this.user = user;
//        username.setText(user.getUsername()); //label e show korbe
//
//        //load profile photo
//        if(user.getPhotoPath() != null && !user.getPhotoPath().isEmpty()){
//            File file = new File(user.getPhotoPath());
//            if(file.exists()){
//                photo.setImage(new Image(file.toURI().toString()));
//            }
//        }
//    }
//
//    public void initialize(){
//        //hide search results until activated
//        resultsPane.setVisible(false);
//        noUserLabel.setVisible(false);
//
//        String initial = "-fx-background-color: black; " +
//                "-fx-text-fill: white; " +
//                "-fx-border-color: white; " +
//                "-fx-border-radius: 8; " +
//                "-fx-background-radius: 8;";
//
//        String hover = "-fx-background-color: #555555; " +
//                "-fx-text-fill: lightblue; " +
//                "-fx-border-color: white; " +
//                "-fx-border-radius: 8; " +
//                "-fx-background-radius: 8;";
//
//        editProfileBtn.setStyle(initial);
//        editProfileBtn.setOnMouseEntered(e -> editProfileBtn.setStyle(hover));
//        editProfileBtn.setOnMouseExited(e -> editProfileBtn.setStyle(initial));
//
//        chatBtn.setStyle(initial);
//        chatBtn.setOnMouseEntered(e -> chatBtn.setStyle(hover));
//        chatBtn.setOnMouseExited(e -> chatBtn.setStyle(initial));
//
//        logoutBtn.setStyle(initial);
//        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle(hover));
//        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle(initial));
//
//
//        //type korle show dropdown
//
//        friendSearch.textProperty().addListener((obs, oldText, newText)->{
//            if(newText.trim().isEmpty()){
//                //if kicchu na thake, pane gula hide kore dibo
//                resultsPane.setVisible(false);
//                noUserLabel.setVisible(false);
//            }
//            else{
//                performSearch(newText.trim());
//                //trim hocche whitespaces remove kore dei
//            }
//        });
//
//        chatBtn.setOnAction(e->sceneChange.changeScene("ChatBox.fxml", chatBtn, user));
//        editProfileBtn.setOnAction(e -> sceneChange.changeScene("ProfilePage.fxml", editProfileBtn, user));
//        logoutBtn.setOnAction(e -> sceneChange.changeScene("Welcome.fxml", logoutBtn));
//    }
//
//    private void performSearch(String query) {
//        resultsBox.getChildren().clear(); //clear everything
//
//        File usersFolder = new File("users");
//        if (!usersFolder.exists())
//            return;
//
//        File[] files = usersFolder.listFiles((dir, name) ->
//                name.toLowerCase().contains(query.toLowerCase()) && name.endsWith(".ser"));
//
//        if(files == null || files.length == 0){
//            resultsPane.setVisible(false);
//            noUserLabel.setVisible(true);
//        }
//
//        for(File f: files){
//            String username = f.getName().replace(".ser", "");
//
//            if(!username.equals(user.getUsername())){
//                Button userBtn = new Button(username);
//                userBtn.setStyle("");
//
//                userBtn.setOnAction(e->{
//                    //open chat with this user
//                    System.out.println("Opened char with " + username);
//                });
//
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

    private User user;

    @Override
    public void setUser(User user) {
        this.user = user;
        usernameLabel.setText(user.getUsername());
        loadProfilePhoto();

        // open persistent socket if not yet connected (protect against double‑connect)
        ClientConnection conn = ClientConnection.getInstance();
        if (conn != null && Session.getCurrentUser() != null && !user.getUsername().equals("")) {
            conn.connect("localhost", 12345, user.getUsername()); // 5555 dewa chilo, changing to 12345
        }
    }

    public void initialize() {
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
        logoutBtn.setOnAction(e -> sceneChange.changeScene("Welcome.fxml", logoutBtn));
    }

    private void styleButtons() {
        String initial = "-fx-background-color:black;-fx-text-fill:white;-fx-border-color:white;-fx-border-radius:8;-fx-background-radius:8;";
        String hover   = "-fx-background-color:#555555;-fx-text-fill:lightblue;-fx-border-color:white;-fx-border-radius:8;-fx-background-radius:8;";

        for (Button b : new Button[]{editProfileBtn, chatBtn, logoutBtn}) {
            b.setStyle(initial);
            b.setOnMouseEntered(e -> b.setStyle(hover));
            b.setOnMouseExited(e -> b.setStyle(initial));
        }
    }

    private void loadProfilePhoto() {
        if (user.getPhotoPath() != null && !user.getPhotoPath().isEmpty()) {
            File file = new File(user.getPhotoPath());
            if (file.exists()) {
                photo.setImage(new Image(file.toURI().toString()));
            }
        }
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
                userBtn.setOnAction(e -> sceneChange.changeScene("ChatBox.fxml", userBtn, user));
                resultsBox.getChildren().add(userBtn);
            }
        }
    }
}
