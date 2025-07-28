package main.java.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import main.java.client.ClientConnection;
import main.java.user.Database;
import main.java.user.Session;
import main.java.user.User;
import main.java.util.UserInterface;
import main.java.util.sceneChange;

import java.io.File;
import java.util.Set;

public class FriendRequestsController implements UserInterface, ClientConnection.FriendListener {

    @FXML private VBox requestsBox;
    @FXML private Button backBtn;

    private User user;

    @Override
    public void setUser(User user) {
        this.user = user;
        Session.setUser(user);
        refresh();
    }

    @FXML
    public void initialize() {
        ClientConnection.getInstance().registerFriendListener(this);
        ClientConnection.getInstance().setCurrentController(this);

        if (user == null) {
            user = Session.getCurrentUser();
        }

        backBtn.setOnAction(e ->
        {
            ClientConnection.getInstance().removeFriendListener(this);
            sceneChange.changeScene("Dashboard.fxml", backBtn, user);
        });

        if (user != null) {
            refresh();
        }
    }

    public void refresh() {
        Session.refreshCurrentUser();
        user = Database.loadUser(user.getUsername());
        System.out.println("Refreshing for " + user.getUsername() + ", incoming requests: " + user.getFriendRequestsReceived());

        requestsBox.getChildren().clear();
        Set<String> incoming = user.getFriendRequestsReceived();
        if (incoming == null || incoming.isEmpty()) {
            System.out.println("No incoming friend requests for " + user.getUsername());
        }

        for (String requester : incoming) {
            User sender = Database.loadUser(requester);
            if (sender == null) {
                System.out.println("Failed to load user: " + requester);
                continue;
            }
            System.out.println("Adding friend request card for " + requester);

            HBox card = new HBox(10);
            card.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
            card.setPrefWidth(500);
            card.setAlignment(Pos.CENTER_LEFT);

            ImageView imageView = new ImageView();
            File imgFile = new File("users/" + sender.getUsername() + "/profile.jpg");

            if (imgFile.exists()) {
                imageView.setImage(new Image(imgFile.toURI().toString()));
            } else {
                imageView.setImage(new Image(getClass().getResource("/images/default.jpeg").toString()));
            }
            imageView.setFitHeight(40);
            imageView.setFitWidth(40);
            imageView.setClip(new Circle(20, 20, 20));

            Label name = new Label(sender.getUsername());
            name.setStyle("-fx-font-size: 16px; -fx-text-fill: #444;");
            HBox.setHgrow(name, Priority.ALWAYS);

            Button accept = new Button("Accept");
            accept.setStyle("-fx-background-color: skyblue; -fx-text-fill: white; -fx-background-radius: 5;");
            accept.setOnAction(e -> {
                System.out.println("Accepting friend request from " + sender.getUsername());
                ClientConnection.getInstance().acceptRequest(sender.getUsername());
                refresh();
            });

            Button decline = new Button("Decline");
            decline.setStyle("-fx-background-color: pink; -fx-text-fill: white; -fx-background-radius: 5;");
            decline.setOnAction(e -> {
                System.out.println("Declining friend request from " + sender.getUsername());
                ClientConnection.getInstance().declineRequest(sender.getUsername());
                refresh();
            });

            accept.setOnMouseEntered(e -> accept.setStyle("-fx-background-color: azure; -fx-text-fill: white; -fx-background-radius: 5;"));
            accept.setOnMouseExited(e -> accept.setStyle("-fx-background-color: skyblue; -fx-text-fill: white; -fx-background-radius: 5;"));
            decline.setOnMouseEntered(e -> decline.setStyle("-fx-background-color: pink; -fx-text-fill: white; -fx-background-radius: 5;"));
            decline.setOnMouseExited(e -> decline.setStyle("-fx-background-color: salmon; -fx-text-fill: white; -fx-background-radius: 5;"));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            card.getChildren().addAll(imageView, name, spacer, accept, decline);
            requestsBox.getChildren().add(card);
        }
    }

//    @Override
//    public void handlingRequests(String from, String body, String to) {
//        if (body.startsWith("ACCEPTED_REQUEST_FROM") || body.startsWith("DECLINED_REQUEST_FROM") || body.startsWith("GOT_FRIEND_REQUEST_FROM")) {
//            System.out.println("FriendRequestsController received: " + body + " for user " + user.getUsername());
//            Session.refreshCurrentUser();
//            refresh();
//        }
//    }


    @Override
    public void onFriendRequestReceived(String from) {
        Platform.runLater(this::refresh);
    }

    @Override
    public void onFriendRequestAccepted(String from) {
        Platform.runLater(this::refresh);
    }

    @Override
    public void onFriendRequestDeclined(String from) {
        Platform.runLater(this::refresh);
    }
}