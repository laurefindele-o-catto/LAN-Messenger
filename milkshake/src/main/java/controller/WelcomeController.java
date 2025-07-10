package main.java.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import main.java.util.sceneChange;


public class WelcomeController {
    @FXML
    private Button loginBtn;

    @FXML
    private Button signupBtn;

    @FXML
    private ImageView imageView;

    @FXML
    public void initialize(){
        //load the image
        try{
            Image img = new Image(getClass().getResourceAsStream("/images/three cats on a bench.jpeg"));
            imageView.setImage(img);
        }
        catch(Exception e){
            System.out.println("Could not load image: " + e.getMessage());
        }

        //hover effect
        String initial = "-fx-background-color: black; " +
                "-fx-text-fill: white; " +
                "-fx-border-color: white; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8;";

        String hover = "-fx-background-color: #555555; " +
                "-fx-text-fill: lightblue; " +
                "-fx-border-color: white; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8;";

        loginBtn.setStyle(initial);
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(hover));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle(initial));

        signupBtn.setStyle(initial);
        signupBtn.setOnMouseEntered(e -> signupBtn.setStyle(hover));
        signupBtn.setOnMouseExited(e -> signupBtn.setStyle(initial));

        loginBtn.setOnAction(e -> sceneChange.changeScene("LoginPage.fxml", loginBtn));
        signupBtn.setOnAction(e -> sceneChange.changeScene("SignupPage.fxml", signupBtn));
    }
}

