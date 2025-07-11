package main.java.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.java.user.Database;
import main.java.user.Session;
import main.java.user.User;
import main.java.util.UserInterface;
import main.java.util.sceneChange;


import java.io.File;
import java.time.LocalDate;

public class ProfilePageController implements UserInterface {
    @FXML private ImageView profilePhoto;
    @FXML private Label usernameLabel;
    @FXML private ImageView newPhoto;
    @FXML private Button uploadPhotoBtn;
    @FXML private TextField firstName;
    @FXML private TextField lastName;
    @FXML private DatePicker birthday;
    @FXML private TextField email;
    @FXML private TextArea bio;
    @FXML private Button saveBtn;
    @FXML private Button backBtn;

    private User user;
    private File photoFile = null;

    @Override
    public void setUser(User user){
        this.user = user;
        Session.setUser(user);
        loadInfo();
    }

    private void loadInfo(){
        usernameLabel.setText(user.getUsername());
        firstName.setText(user.getFirstName());
        lastName.setText(user.getLastName());
        email.setText(user.getEmail());
        bio.setText(user.getBio());
        birthday.setValue(user.getBirthday());

        if(user.getPhotoPath()!=null){
            File file = new File(user.getPhotoPath());
            if(file.exists()){
                profilePhoto.setImage(new Image(file.toURI().toString()));
            }
        }
    }

    @FXML
    public void initialize(){

        uploadPhotoBtn.setOnAction(e->handleUploadPhoto());

        saveBtn.setOnAction(e->{
            saveProfile();
            loadInfo();
        });

        backBtn.setOnAction(e-> sceneChange.changeScene("Dashboard.fxml", backBtn, user));
    }

    private void handleUploadPhoto(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Picture");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(new Stage());
        if(file != null){
            photoFile = file;
            newPhoto.setImage(new Image(file.toURI().toString()));
            profilePhoto.setImage(new Image(file.toURI().toString()));
        }
    }

    private void saveProfile(){
        user.setFirstName(firstName.getText());
        user.setLastName(lastName.getText());
        user.setEmail(email.getText());
        user.setBio(bio.getText());

        if (photoFile != null) {
            user.setPhotoPath(photoFile.getAbsolutePath());
        }

        LocalDate date = birthday.getValue();
        if(date != null){
            user.setBirthday(date);
        }

        Database.saveUser(user);
    }
}
