
// 11/7/25
package main.java.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import main.java.client.ClientConnection;
import main.java.user.Database;
import main.java.user.Session;
import main.java.user.User;
import main.java.util.sceneChange;

import java.io.*;
import java.net.Socket;

/**
 * Signup flow that:
 *   • sends SIGNUP via a short‑lived socket (so no static call)
 *   • on success opens the persistent ClientConnection for chat
 */
public class SignupController {
    @FXML private TextField      usernameField;
    @FXML private PasswordField  passwordField;
    @FXML private Button         signupBtn;
    @FXML private Button         backBtn;
    @FXML private ImageView      logoImage;
    @FXML private Label          errorLabel;

    private static final String HOST = "localhost";
    private static final int    PORT = 12346;

    public void initialize() {
        styleButtons();
        backBtn.setOnAction(e -> sceneChange.changeScene("Welcome.fxml", backBtn));
        signupBtn.setOnAction(e -> registerUser());
        passwordField.setOnAction(e -> signupBtn.fire());
    }

    private void styleButtons(){
        String initial="-fx-background-color:#e6f7ff;-fx-text-fill:#595569;-fx-border-color:#3385ff;-fx-border-radius:8;-fx-background-radius:8;";
        String hover  ="-fx-background-color:grey;-fx-text-fill:white;-fx-border-color:black;-fx-border-radius:8;-fx-background-radius:8;";
        for(Button b:new Button[]{signupBtn,backBtn}){
            b.setStyle(initial);
            b.setOnMouseEntered(e->b.setStyle(hover));
            b.setOnMouseExited(e->b.setStyle(initial));
        }
    }

    private void registerUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if(username.isEmpty()||password.isEmpty()){
            showAlert("Unfilled boxes","Please enter both username and password.");
            return;
        }
        if(username.contains(" ")){
            errorLabel.setText("Username cannot contain spaces.");
            return;
        }

        try{
            String resp = sendSignup(username,password);
            if("SUCCESS".equalsIgnoreCase(resp)){
                User u=new User(username);

                u.setPhotoPath(String.valueOf(getClass().getResource("/images/default.jpeg")));
                Database.saveUser(u);
                Session.setUser(u);

                // open long‑lived socket for chat
                ClientConnection.getInstance().connect(HOST,PORT,username);
                ClientConnection.getInstance().setUsername(username);

                Database.saveUser(u);

                sceneChange.changeScene("Dashboard.fxml", signupBtn, u);
            }else if(resp!=null && resp.startsWith("ERROR")){
                errorLabel.setText(resp.split("\\|",2)[1]);
            }else{
                errorLabel.setText("Unexpected server response.");
            }
        }catch(IOException ex){
            showAlert("Network Error","Could not connect to server.");
            ex.printStackTrace();
        }
    }

    /** short‑lived socket just for SIGNUP */
    private String sendSignup(String user,String pass) throws IOException{
        try(Socket s=new Socket(HOST,PORT);
            BufferedReader br=new BufferedReader(new InputStreamReader(s.getInputStream()));
            PrintWriter out=new PrintWriter(s.getOutputStream(),true)){
            out.println("SIGNUP|"+user+"|"+pass);
            return br.readLine();
        }
    }

    private void showAlert(String title,String msg){
        Alert a=new Alert(AlertType.WARNING);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}
