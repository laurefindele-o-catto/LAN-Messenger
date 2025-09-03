//import javafx.animation.KeyFrame;
//import javafx.animation.ScaleTransition;
//import javafx.animation.Timeline;
//import javafx.application.Application;
//import javafx.fxml.FXMLLoader;
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.scene.control.Label;
//import javafx.scene.effect.DropShadow;
//import javafx.scene.image.Image;
//import javafx.scene.image.ImageView;
//import javafx.scene.layout.*;
//import javafx.scene.paint.Color;
//import javafx.scene.shape.Circle;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//
//public class Main extends Application {
//
//    @Override
//    public void start(Stage primaryStage) throws Exception {
//        // ===== SPLASH SCREEN SETUP =====
//        StackPane root = new StackPane();
//        // Dark grey background for the splash
//        root.setBackground(new Background(
//                new BackgroundFill(Color.web("#1F2937"), CornerRadii.EMPTY, Insets.EMPTY)
//        ));
//
//        VBox cardBox = new VBox();
//        cardBox.setAlignment(Pos.CENTER);
//        cardBox.setPadding(new Insets(32));
//        // Slightly lighter card background
//        cardBox.setBackground(new Background(
//                new BackgroundFill(Color.web("#1E293B"), new CornerRadii(24), Insets.EMPTY)
//        ));
//        cardBox.setEffect(new DropShadow(16, Color.rgb(0, 0, 0, 0.3)));
//        cardBox.setSpacing(0);
//
//        Label titleLabel = new Label("Meow Messenger");
//        titleLabel.setFont(javafx.scene.text.Font.font("Comic Sans MS", 32));
//        titleLabel.setTextFill(Color.web("#C7D2FE"));
//        cardBox.getChildren().add(titleLabel);
//        VBox.setMargin(titleLabel, new Insets(0, 0, 32, 0));
//
//        // ===== Cat Loader Graphic =====
//        Pane catContainer = new Pane();
//        catContainer.setPrefSize(250, 200); // size to prevent clipping
//
//        // Load an animated GIF for smooth animation
//        // Ensure that "cat_running.gif" is located in your project folder or adjust the path
////        Image catGif = new Image("file:cat_running.gif");
////        Image catGif = new Image(getClass().getResource("/image/Cup_Of_Coffee_Cat.gif").toExternalForm());
//        //Image catGif = new Image(getClass().getResource("src/main/resources/images/Cup_Of_Coffee_Cat.gif").toExternalForm());
//        Image catGif = new Image("/images/Cat-Coffee-GIF-by-Meowingtons-unscreen.gif");
//        ImageView catGifView = new ImageView(catGif);
//        catGifView.setPreserveRatio(true);
//        catGifView.setFitWidth(180);
//        catGifView.setLayoutX(100);
//        catGifView.setLayoutY(10);
//        catContainer.getChildren().add(catGifView);
//
//        // Loading text and pulsing dots
//        Label loadingLabel = new Label("Loading your purrfect chats");
//        loadingLabel.setTextFill(Color.web("#E2E8F0"));
//
//        HBox dotsBox = new HBox(5);
//        dotsBox.setAlignment(Pos.CENTER);
//        Circle dot1 = new Circle(4, Color.web("#FBBF24"));
//        Circle dot2 = new Circle(4, Color.web("#FBBF24"));
//        Circle dot3 = new Circle(4, Color.web("#FBBF24"));
//        dotsBox.getChildren().addAll(dot1, dot2, dot3);
//
//        // Add elements into card
//        cardBox.getChildren().add(catContainer);
//        VBox.setMargin(catContainer, new Insets(0, 0, 24, 0));
//        cardBox.getChildren().add(loadingLabel);
//        VBox.setMargin(loadingLabel, new Insets(0, 0, 12, 0));
//        cardBox.getChildren().add(dotsBox);
//
//        // Add card to root
//        root.getChildren().add(cardBox);
//
//        // ===== Animations =====
//        // Pulsing dots
//        ScaleTransition pulse1 = new ScaleTransition(Duration.seconds(1.5), dot1);
//        pulse1.setFromX(1); pulse1.setFromY(1);
//        pulse1.setToX(1.1); pulse1.setToY(1.1);
//        pulse1.setAutoReverse(true);
//        pulse1.setCycleCount(javafx.animation.Animation.INDEFINITE);
//
//        ScaleTransition pulse2 = new ScaleTransition(Duration.seconds(1.5), dot2);
//        pulse2.setFromX(1); pulse2.setFromY(1);
//        pulse2.setToX(1.1); pulse2.setToY(1.1);
//        pulse2.setAutoReverse(true);
//        pulse2.setCycleCount(javafx.animation.Animation.INDEFINITE);
//        pulse2.setDelay(Duration.seconds(0.2));
//
//        ScaleTransition pulse3 = new ScaleTransition(Duration.seconds(1.5), dot3);
//        pulse3.setFromX(1); pulse3.setFromY(1);
//        pulse3.setToX(1.1); pulse3.setToY(1.1);
//        pulse3.setAutoReverse(true);
//        pulse3.setCycleCount(javafx.animation.Animation.INDEFINITE);
//        pulse3.setDelay(Duration.seconds(0.4));
//
//        // Timeline to change text and move to the next scene
//        Timeline slideIn = new Timeline(
//                new KeyFrame(Duration.seconds(3), e -> {
//                    dotsBox.getChildren().clear();
//                    Label ready = new Label("Ready to chat!");
//                    ready.setTextFill(Color.web("#C7D2FE"));
//                    dotsBox.getChildren().add(ready);
//                }),
//                // Update the loading label instead of using a speech bubble
//                new KeyFrame(Duration.seconds(4), e -> loadingLabel.setText("Let's chat!")),
//                new KeyFrame(Duration.seconds(5), e -> {
//                    try {
//                        Parent welcomeRoot = FXMLLoader.load(getClass().getResource("/view/Welcome.fxml"));
//                        primaryStage.setScene(new Scene(welcomeRoot));
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                })
//        );
//
//        // Start animations
//        pulse1.play();
//        pulse2.play();
//        pulse3.play();
//        slideIn.play();
//
//        // Display the splash screen
//        primaryStage.setScene(new Scene(root, 500, 450));
//        primaryStage.setTitle("Meow Messenger");
//        primaryStage.show();
//    }
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//}
//
//

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // ===== SPLASH SCREEN SETUP =====
        StackPane root = new StackPane();
        // Dark grey background for the splash
        root.setBackground(new Background(
                new BackgroundFill(Color.web("#1F2937"), CornerRadii.EMPTY, Insets.EMPTY)
        ));

        VBox cardBox = new VBox();
        cardBox.setAlignment(Pos.CENTER);
        cardBox.setPadding(new Insets(32));
        // Slightly lighter card background with increased corner radius
        cardBox.setBackground(new Background(
                new BackgroundFill(Color.web("#1E293B"), new CornerRadii(32), Insets.EMPTY)
        ));
        // Increased drop shadow for better effect
        cardBox.setEffect(new DropShadow(24, Color.rgb(0, 0, 0, 0.3)));
        cardBox.setSpacing(0);

        Label titleLabel = new Label("Chatty Whiskers");
        titleLabel.setFont(javafx.scene.text.Font.font("Comic Sans MS", 48)); // Larger font size
        titleLabel.setTextFill(Color.web("#C7D2FE"));
        cardBox.getChildren().add(titleLabel);
        VBox.setMargin(titleLabel, new Insets(0, 0, 32, 0));

        // ===== Cat Loader Graphic =====
        StackPane catContainer = new StackPane(); // Changed to StackPane for centering
        catContainer.setPrefSize(350, 250); // Adjusted size to accommodate larger image

        // Load an animated GIF for smooth animation
        Image catGif = new Image("/images/1uHg.gif");
        ImageView catGifView = new ImageView(catGif);
        catGifView.setPreserveRatio(true);
        catGifView.setFitWidth(300); // Increased image size
        catContainer.getChildren().add(catGifView);

        // Loading text and pulsing dots
        Label loadingLabel = new Label("Loading your purrfect chats");
        loadingLabel.setFont(javafx.scene.text.Font.font(24)); // Added font size
        loadingLabel.setTextFill(Color.web("#E2E8F0"));

        HBox dotsBox = new HBox(10); // Increased spacing
        dotsBox.setAlignment(Pos.CENTER);
        Circle dot1 = new Circle(6, Color.web("#FBBF24")); // Larger dots
        Circle dot2 = new Circle(6, Color.web("#FBBF24"));
        Circle dot3 = new Circle(6, Color.web("#FBBF24"));
        dotsBox.getChildren().addAll(dot1, dot2, dot3);

        // Add elements into card
        cardBox.getChildren().add(catContainer);
        VBox.setMargin(catContainer, new Insets(0, 0, 24, 0));
        cardBox.getChildren().add(loadingLabel);
        VBox.setMargin(loadingLabel, new Insets(0, 0, 12, 0));
        cardBox.getChildren().add(dotsBox);

        // Add card to root
        root.getChildren().add(cardBox);

        // ===== Animations =====
        // Pulsing dots
        ScaleTransition pulse1 = new ScaleTransition(Duration.seconds(1.5), dot1);
        pulse1.setFromX(1); pulse1.setFromY(1);
        pulse1.setToX(1.1); pulse1.setToY(1.1);
        pulse1.setAutoReverse(true);
        pulse1.setCycleCount(javafx.animation.Animation.INDEFINITE);

        ScaleTransition pulse2 = new ScaleTransition(Duration.seconds(1.5), dot2);
        pulse2.setFromX(1); pulse2.setFromY(1);
        pulse2.setToX(1.1); pulse2.setToY(1.1);
        pulse2.setAutoReverse(true);
        pulse2.setCycleCount(javafx.animation.Animation.INDEFINITE);
        pulse2.setDelay(Duration.seconds(0.2));

        ScaleTransition pulse3 = new ScaleTransition(Duration.seconds(1.5), dot3);
        pulse3.setFromX(1); pulse3.setFromY(1);
        pulse3.setToX(1.1); pulse3.setToY(1.1);
        pulse3.setAutoReverse(true);
        pulse3.setCycleCount(javafx.animation.Animation.INDEFINITE);
        pulse3.setDelay(Duration.seconds(0.4));

        // Timeline to change text and move to the next scene
        Timeline slideIn = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> {
                    dotsBox.getChildren().clear();
                    Label ready = new Label("Ready to chat!");
                    ready.setFont(javafx.scene.text.Font.font(24)); // Added font size
                    ready.setTextFill(Color.web("#C7D2FE"));
                    dotsBox.getChildren().add(ready);
                }),
                // Update the loading label instead of using a speech bubble
                new KeyFrame(Duration.seconds(4), e -> loadingLabel.setText("Let's chat!")),
                new KeyFrame(Duration.seconds(5), e -> {
                    try {
                        Parent welcomeRoot = FXMLLoader.load(getClass().getResource("/view/Welcome.fxml"));
                        primaryStage.setScene(new Scene(welcomeRoot));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                })
        );

        // Start animations
        pulse1.play();
        pulse2.play();
        pulse3.play();
        slideIn.play();

        // Display the splash screen with a larger window
        primaryStage.setScene(new Scene(root, 800, 600)); // Increased window size
        primaryStage.setTitle("Meow Messenger");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}