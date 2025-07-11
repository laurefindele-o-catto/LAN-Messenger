package main.java.util;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import main.java.user.User;


import java.io.IOException;
import java.net.URL;

public class sceneChange {
    public static void changeScene(String fxml, Node node) {
        changeScene(fxml, node, null);
    }

    public static void changeScene(String fxml, Node btn, User user){
        try{
            String path = "/view/" + fxml;

            URL resource = sceneChange.class.getResource(path);
            System.out.println("Resource url " + resource);

            if(resource == null){
                System.out.println(fxml + " file not found");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            Object controller = loader.getController();
            if(controller instanceof UserInterface){
                ((UserInterface)controller).setUser(user);
            }

            Stage stage = (Stage) btn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

            //fxmlloader reads the fxml file and returns a root root to the new layout tree
            //From the button, get its scene, and from that scene, get the window it's in — which is a Stage.
        } catch (IOException e) {
            //popup korbo
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("FXML file loading error");
            alert.setContentText("Failure to laod " + fxml);
            alert.showAndWait();
        }
    }
    
}
