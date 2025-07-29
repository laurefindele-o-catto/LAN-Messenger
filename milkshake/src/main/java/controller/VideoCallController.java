package main.java.controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import main.java.client.ClientConnection;

import com.github.sarxos.webcam.Webcam;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles all video call related functionality separately from the main chat controller.
 *
 * This class is responsible for sending call requests, handling responses, opening
 * and closing the video call window, capturing local webcam frames, sending them to
 * the remote user and rendering incoming frames. By isolating the logic here
 * the {@link ChatBoxController} remains focussed on chat behaviour and keeps
 * the application modular.
 */
public class VideoCallController {

    /** The active client connection used to send commands to the server. */
    private final ClientConnection connection;
    /** The username of the current user. */
    private final String currentUsername;

    /** Flag indicating whether a call is currently active. */
    private volatile boolean inCall = false;
    /** The remote party we are currently calling or in call with. */
    private String activePartner;

    /** Stage that displays the video call window. */
    private Stage callStage;
    /** Executor used for periodic webcam capture. */
    private ScheduledExecutorService captureExecutor;
    /** The webcam used for capturing local video frames. */
    private Webcam webcam;
    /** Image view showing the local video feed. */
    private ImageView localVideoView;
    /** Image view showing the remote video feed. */
    private ImageView remoteVideoView;

    public VideoCallController(ClientConnection connection, String currentUsername) {
        this.connection = connection;
        this.currentUsername = currentUsername;
    }

    /**
     * Initiates a video call to the specified user.  If a call is already in
     * progress an informational alert is shown.
     *
     * @param user the username of the person to call
     */
    public void initiateCall(String user) {
        if (inCall) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Video Call");
            alert.setHeaderText(null);
            alert.setContentText("You are already in a call.");
            alert.showAndWait();
            return;
        }
        if (user == null || user.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Video Call");
            alert.setHeaderText(null);
            alert.setContentText("No user selected for call.");
            alert.showAndWait();
            return;
        }
        // Send call request via connection
        connection.sendVideoCallRequest(user);
        activePartner = user;
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Video Call");
        info.setHeaderText(null);
        info.setContentText("Calling " + user + "… Please wait for them to answer.");
        info.show();
    }

    /**
     * Receives an incoming call request.  Presents the user with an accept/decline dialog and
     * sends the corresponding response back through the connection.
     *
     * @param from the username of the caller
     */
    public void receiveCallRequest(String from) {
        if (inCall) {
            // Already in a call, automatically decline
            connection.sendVideoCallResponse(from, false);
            return;
        }
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Incoming Video Call");
            alert.setHeaderText(null);
            alert.setContentText(from + " is calling you. Accept?");
            ButtonType accept = new ButtonType("Accept", ButtonBar.ButtonData.YES);
            ButtonType decline = new ButtonType("Decline", ButtonBar.ButtonData.NO);
            alert.getButtonTypes().setAll(accept, decline);
            Optional<ButtonType> result = alert.showAndWait();
            boolean accepted = result.isPresent() && result.get() == accept;
            connection.sendVideoCallResponse(from, accepted);
            if (accepted) {
                // Start the call as incoming
                startCall(from);
            }
        });
    }

    /**
     * Handles the response to a call request.  Opens the video call window if accepted or
     * shows a decline message otherwise.
     *
     * @param from     the username of the other party responding
     * @param accepted true if the call was accepted, false if declined
     */
    public void handleCallResponse(String from, boolean accepted) {
        if (!accepted) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Video Call");
                alert.setHeaderText(null);
                alert.setContentText(from + " declined your call.");
                alert.showAndWait();
            });
            // Reset state
            activePartner = null;
            return;
        }
        // They accepted, start call
        startCall(from);
    }

    /**
     * Starts a video call with the specified partner.  Opens the UI and begins capturing and
     * transmitting frames.
     *
     * @param partner the username of the call partner
     */
    private void startCall(String partner) {
        if (inCall) {
            return;
        }
        inCall = true;
        activePartner = partner;
        Platform.runLater(() -> {
            callStage = new Stage();
            callStage.setTitle("Video Call with " + partner);
            VBox root = new VBox(10);
            root.setPadding(new Insets(10));
            root.setAlignment(Pos.CENTER);

            Label infoLabel = new Label("In call with " + partner);
            infoLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14;");

            localVideoView = new ImageView();
            localVideoView.setFitWidth(200);
            localVideoView.setFitHeight(150);
            localVideoView.setPreserveRatio(true);
            Label localLabel = new Label("Your video");
            localLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
            VBox localBox = new VBox(5, localVideoView, localLabel);
            localBox.setAlignment(Pos.CENTER);

            remoteVideoView = new ImageView();
            remoteVideoView.setFitWidth(200);
            remoteVideoView.setFitHeight(150);
            remoteVideoView.setPreserveRatio(true);
            Label remoteLabel = new Label(partner + "’s video");
            remoteLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12;");
            VBox remoteBox = new VBox(5, remoteVideoView, remoteLabel);
            remoteBox.setAlignment(Pos.CENTER);

            HBox feeds = new HBox(10, localBox, remoteBox);
            feeds.setAlignment(Pos.CENTER);

            Button endCallButton = new Button("End Call");
            endCallButton.setStyle("-fx-background-color: #e53935; -fx-text-fill: white;");
            endCallButton.setOnAction(ev -> endCall());

            root.getChildren().addAll(infoLabel, feeds, endCallButton);
            root.setStyle("-fx-background-color:#2b2b2b;");
            callStage.setScene(new Scene(root, 480, 360));
            callStage.setOnCloseRequest(ev -> endCall());
            callStage.show();
            startCapture();
        });
    }

    /**
     * Captures video frames from the default webcam and transmits them to the partner at
     * regular intervals.  Also updates the local video view.
     */
    private void startCapture() {
        stopCapture();
        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Video Capture Error");
                    alert.setHeaderText(null);
                    alert.setContentText("No webcam detected on this system.");
                    alert.showAndWait();
                });
                return;
            }
            webcam.setViewSize(new Dimension(320, 240));
            webcam.open();
            captureExecutor = Executors.newSingleThreadScheduledExecutor();
            captureExecutor.scheduleAtFixedRate(() -> {
                if (!inCall || webcam == null) {
                    return;
                }
                try {
                    BufferedImage frame = webcam.getImage();
                    if (frame != null) {
                        Image fxImg = SwingFXUtils.toFXImage(frame, null);
                        // Update local view on UI thread
                        Platform.runLater(() -> {
                            if (localVideoView != null) {
                                localVideoView.setImage(fxImg);
                            }
                        });
                        // Encode frame and send
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(frame, "jpg", baos);
                        baos.flush();
                        String encoded = Base64.getEncoder().encodeToString(baos.toByteArray());
                        // Send frame to partner
                        if (activePartner != null) {
                            connection.sendVideoFrame(activePartner, encoded);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Stops capturing video frames and releases resources.
     */
    private void stopCapture() {
        if (captureExecutor != null) {
            captureExecutor.shutdownNow();
            captureExecutor = null;
        }
        try {
            if (webcam != null) {
                if (webcam.isOpen()) {
                    webcam.close();
                }
                webcam = null;
            }
        } catch (Exception ignored) { }
    }

    /**
     * Receives a video frame from the remote user and updates the remote video view.
     *
     * @param from  the sender (should match activePartner)
     * @param data  base64 encoded JPEG image data
     */
    public void receiveVideoFrame(String from, String data) {
        if (!inCall || activePartner == null || !activePartner.equals(from)) {
            // Ignore frames not from the current call partner
            return;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            Image img = new Image(bais);
            Platform.runLater(() -> {
                if (remoteVideoView != null) {
                    remoteVideoView.setImage(img);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Ends the current call and notifies the partner.
     */
    public void endCall() {
        if (!inCall) {
            return;
        }
        String partner = activePartner;
        activePartner = null;
        inCall = false;
        stopCapture();
        if (callStage != null) {
            Stage s = callStage;
            callStage = null;
            Platform.runLater(s::close);
        }
        if (partner != null) {
            connection.sendEndCall(partner);
        }
    }

    /**
     * Handles an END_CALL message from the partner.
     *
     * @param from the partner who ended the call
     */
    public void handleCallEnd(String from) {
        if (!inCall || activePartner == null || !activePartner.equals(from)) {
            return;
        }
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Video Call Ended");
            alert.setHeaderText(null);
            alert.setContentText(from + " has ended the call.");
            alert.showAndWait();
        });
        activePartner = null;
        inCall = false;
        stopCapture();
        if (callStage != null) {
            Stage s = callStage;
            callStage = null;
            Platform.runLater(s::close);
        }
    }
}