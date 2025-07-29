//package main.java.controller;
//
//import main.java.client.ClientConnection;
//import javafx.fxml.FXML;
//import javafx.scene.control.*;
//import javafx.scene.image.ImageView;
//import javafx.scene.control.Alert.AlertType;
//import main.java.user.Database;
//import main.java.user.Session;
//import main.java.user.User;
//import main.java.util.UserInterface;
//import main.java.util.sceneChange;
//import java.io.*;
//
//public class LoginController {
//    @FXML private TextField usernameField;
//    @FXML private PasswordField passwordField;
//    @FXML private Button loginBtn;
//    @FXML private Button backBtn;
//    @FXML private ImageView logoImage;
//    @FXML private Label errorLabel;
//
//    public void initialize() {
//        String initial = "-fx-background-color:  #e6f7ff; -fx-text-fill: #595569; -fx-border-color: #3385ff; -fx-border-radius: 8; -fx-background-radius: 8;";
//        String hover = "-fx-background-color:  grey; -fx-text-fill: white; -fx-border-color: black; -fx-border-radius: 8; -fx-background-radius: 8;";
//
//        backBtn.setStyle(initial);
//        backBtn.setOnMouseEntered(e -> backBtn.setStyle(hover));
//        backBtn.setOnMouseExited(e -> backBtn.setStyle(initial));
//
//        loginBtn.setStyle(initial);
//        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(hover));
//        loginBtn.setOnMouseExited(e -> loginBtn.setStyle(initial));
//
//        backBtn.setOnAction(e -> sceneChange.changeScene("Welcome.fxml", backBtn));
//        loginBtn.setOnAction(e -> loginUser());
//        passwordField.setOnAction(e->loginBtn.fire());
//    }
//
//    private void loginUser() {
//        try {
//            String username = usernameField.getText();
//            String password = passwordField.getText();
//
//            if (username.isEmpty() || password.isEmpty()) {
//                showAlert("Unfilled boxes", "Please enter both username and password.");
//                return;
//            }
//
//            if (username.contains(" ")) {
//                errorLabel.setText("Username cannot contain spaces.");
//                return;
//            }
//
//            String response = ClientConnection.send("LOGIN|" + username + "|" + password);
//
//            if (response.equalsIgnoreCase("SUCCESS")) {
//                usernameField.clear();
//                passwordField.clear();
//
//                User loggedInUser = Database.loadUser(username);
//                Session.setUser(loggedInUser);
//
//                sceneChange.changeScene("Dashboard.fxml", loginBtn, loggedInUser);
//            } else if (response.startsWith("ERROR")) {
//                String msg[] = response.split("\\|");
//                errorLabel.setText(msg[1]);
//            } else {
//                errorLabel.setText("Unexpected server response.");
//            }
//        } catch (Exception e) { //Exception IO is not thrown dekhacche so replacing it with Exception
//            showAlert("Network Error", "Could not connect to server.");
//            e.printStackTrace();
//        }
//    }
//
//    private void showAlert(String title, String content) {
//        Alert alert = new Alert(Alert.AlertType.WARNING);
//        alert.setTitle(title);
//        alert.setContentText(content);
//        alert.showAndWait();
//    }
//}



//New Version 11/7/25
package main.java.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import main.java.client.ClientConnection;
import main.java.user.Database;
import main.java.user.Session;
import main.java.user.User;
import main.java.util.sceneChange;

import java.io.*;
import java.net.Socket;

public class LoginController {
    @FXML private TextField    usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginBtn;
    @FXML private Button        backBtn;
    @FXML private Label         errorLabel;

    private static final String HOST = "localhost";
    private static final int    PORT = 12346;

    public void initialize() {
        backBtn.setOnAction(e -> sceneChange.changeScene("Welcome.fxml", backBtn));
        loginBtn.setOnAction(e -> doLogin());
        passwordField.setOnAction(e -> loginBtn.fire());
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if(username.isEmpty() || password.isEmpty()){
            errorLabel.setText("Please fill both fields.");
            return;
        }

        try{
            String resp = sendLogin(username, password);
            if("SUCCESS".equalsIgnoreCase(resp)){
                User u = Database.loadUser(username); // or new User(username) if not stored yet
                if(u==null) u = new User(username);
                Session.setUser(u);

                // open persistent connection for chat
                ClientConnection.getInstance().connect(HOST, PORT, username);

                sceneChange.changeScene("Dashboard.fxml", loginBtn, u);
            }else if(resp!=null && resp.startsWith("ERROR")){
                errorLabel.setText(resp.split("\\|",2)[1]);
            }else{
                errorLabel.setText("Unexpected server reply");
            }
        }catch(IOException ex){
            showAlert("Network Error", "Could not connect to server.");
            ex.printStackTrace();
        }
    }

    private String sendLogin(String user,String pass) throws IOException{
        try(Socket s=new Socket(HOST,PORT);
            BufferedReader br=new BufferedReader(new InputStreamReader(s.getInputStream()));
            PrintWriter out=new PrintWriter(s.getOutputStream(),true)){
            out.println("LOGIN|"+user+"|"+pass);
            return br.readLine();
        }
    }

    private void showAlert(String title,String msg){
        Alert a=new Alert(AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}


