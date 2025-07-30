

package main.java.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import main.java.client.ClientConnection;
import main.java.user.Session;
import main.java.user.User;
import main.java.util.sceneChange;
import main.java.user.Database;
import javafx.scene.shape.Circle;

import java.util.Base64;

import main.java.controller.VideoCallController;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for the chat box view.  Handles sending/receiving private and
 * group messages, rendering chat bubbles, and updating the friend list in
 * real time when friend requests are accepted or declined.  Implements both
 * {@link ClientConnection.MessageListener} to receive messages and
 * {@link ClientConnection.FriendListener} to react to friend request events.
 */
public class ChatBoxController implements Initializable,
        ClientConnection.MessageListener,
        ClientConnection.FriendListener {

    // FXML injected controls
    @FXML private ListView<String> friendListView;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesBox;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button backButton;
    @FXML private Button createGroupButton;
    @FXML private Button imageButton;
    @FXML private Button membersButton;
    @FXML private Button videoCallButton;
    @FXML private Label chatTitleLabel;
    @FXML private ImageView profileImageView;

    private User currentUser;
    private ClientConnection connection;
    /** Map of conversation identifiers (friend username or group name) to their
     * message history.  Used to render existing messages when switching
     * between conversations. */
    private final Map<String, List<Message>> chatHistory = new HashMap<>();
    /** Set of group names that the current user belongs to. */
    private final Set<String> groups = new HashSet<>();
    /** Map of group name to the list of members in that group.  Populated
     * whenever group history or creation events are received. */
    private final Map<String, List<String>> groupMembersMap = new HashMap<>();
    /** Timestamp formatter for chat bubbles. */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Video call controller that manages call lifecycle and frame handling.
     * This keeps the chat controller focused on messaging and friend logic.
     */
    private VideoCallController videoCallController;

    /**
     * Initialize the chat box.  Registers the controller as a message and
     * friend listener, requests chat and group history, populates the friend
     * list, and wires up button handlers.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        connection = ClientConnection.getInstance();
        connection.setCurrentController(this);
        currentUser = Session.getCurrentUser();
        connection.registerListener(this);
        connection.registerFriendListener(this);

        // Instantiate the video call controller after obtaining the current user.  The
        // controller will manage all call related state and communication.
        videoCallController = new VideoCallController(connection, currentUser.getUsername());
        // Request initial histories
        connection.requestChatHistory(currentUser.getUsername());
        connection.requestGroupList();
        // Build the friend/group list in the UI
        populateFriendList();
        // When the user selects a conversation, load its history
        friendListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> loadChat(newVal));
        // Send message on button click or enter key
        sendButton.setOnAction(e -> sendCurrentMessage());
        messageField.setOnAction(e -> sendCurrentMessage());
        // Back to dashboard; remove listeners
        backButton.setOnAction(e -> {
            shutdown();
            sceneChange.changeScene("Dashboard.fxml", backButton, currentUser);
        });
        // Create a new group
        if (createGroupButton != null) {
            createGroupButton.setOnAction(e -> openGroupCreationDialog());
        }
        // Attach image to current conversation
        if (imageButton != null) {
            imageButton.setOnAction(e -> sendImageAttachment());
        }
        // Show group members for the currently selected group
        if (membersButton != null) {
            membersButton.setOnAction(e -> showGroupMembers());
        }

        // Initiate a video call on click (only valid for private chats)
        if (videoCallButton != null) {
            videoCallButton.setOnAction(e -> initiateVideoCall());
        }
    }

    /**
     * Opens a modal dialog allowing the user to create a new group.  The
     * current user's friends are listed with checkboxes to select members.
     */

    private void openGroupCreationDialog() {
        if (currentUser.getFriends() == null || currentUser.getFriends().isEmpty()) {
            return;
        }
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle("Create Group");
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10);
        root.setPadding(new Insets(10));
        root.getStyleClass().add("dialog-root");

        Label nameLabel = new Label("Group Name:");
        TextField nameField = new TextField();
        nameField.setPromptText("Enter group name");
        Label membersLabel = new Label("Select members:");
        VBox checkContainer = new VBox(5);
        for (String f : currentUser.getFriends()) {
            CheckBox cb = new CheckBox(f);
            checkContainer.getChildren().add(cb);
        }
        Button createBtn = new Button("Create");
        Button cancelBtn = new Button("Cancel");
        HBox buttonBar = new HBox(10, createBtn, cancelBtn);

        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().addAll(nameLabel, nameField, membersLabel, checkContainer, buttonBar);
        javafx.scene.Scene scene = new javafx.scene.Scene(root);

        scene.getStylesheets().add(getClass().getResource("/view/dialog-box.css").toExternalForm());
        dialog.setScene(scene);
        createBtn.setOnAction(ev -> {
            String groupName = nameField.getText().trim();
            if (groupName.isEmpty()) return;
            List<String> selected = new ArrayList<>();
            for (javafx.scene.Node n : checkContainer.getChildren()) {
                if (n instanceof CheckBox cb && cb.isSelected()) {
                    selected.add(cb.getText());
                }
            }
            if (selected.isEmpty()) return;
            connection.createGroup(groupName, selected);
            dialog.close();
        });
        cancelBtn.setOnAction(ev -> dialog.close());
        dialog.showAndWait();
    }

    /**
     * Populates the friend list UI with the current user's friends and any
     * groups the user belongs to.  The list is sorted alphabetically.
     */
    private void populateFriendList() {
        Set<String> friends = currentUser.getFriends();
        List<String> all = new ArrayList<>();
        all.addAll(groups);
        if (friends != null) {
            all.addAll(friends);
        }
        all.sort(String::compareToIgnoreCase);
        friendListView.setItems(FXCollections.observableArrayList(all));
    }

    /**
     * Loads and displays the message history for a conversation (friend or
     * group).  Also updates the chat title and members button visibility.
     *
     * @param conversation the name of the friend or group
     */
    private void loadChat(String conversation) {
        // Update the title label
        if (chatTitleLabel != null) {
            chatTitleLabel.setText(conversation == null ? "" : conversation);
        }
        // Show or hide the members button depending on whether this is a group
        if (membersButton != null) {
            boolean isGroup = conversation != null && groups.contains(conversation);
            membersButton.setVisible(isGroup);
        }

        // Show or hide the video call button for one‑to‑one chats
        if (videoCallButton != null) {
            boolean isGroup = conversation != null && groups.contains(conversation);
            // Only show when a conversation is selected and it is not a group
            videoCallButton.setVisible(conversation != null && !isGroup);
        }
        // Clear existing messages and render the conversation history
        chatMessagesBox.getChildren().clear();
        if (conversation == null) return;
        List<Message> history = chatHistory.get(conversation);
        if (history != null) {
            for (Message m : history) {
                chatMessagesBox.getChildren().add(makeBubble(m));
            }
        }
        scrollToBottom();

        // If this is a group conversation and we don't yet know its members,
        // proactively request the membership list from the server.  The
        // asynchronous response will update groupMembersMap so that
        // showGroupMembers() will have accurate data.  We only request
        // membership once per group to avoid unnecessary round trips.
        if (conversation != null && groups.contains(conversation)) {
            if (!groupMembersMap.containsKey(conversation) || groupMembersMap.get(conversation).isEmpty()) {
                connection.requestGroupMembers(conversation);
            }
        }
        //show the image


        File imgFile = new File("users/" + conversation + "/profile.jpg");
        Image image;

        if (imgFile.exists()) {
            image = new Image(imgFile.toURI().toString(), false);
            if (image.isError()) {
                System.out.println("Error loading image: " + image.getException());
            }
        } else {
            image = new Image(getClass().getResource("/images/default.jpeg").toString());
        }

        profileImageView.setImage(image);
        double radius = Math.min(profileImageView.getFitWidth(), profileImageView.getFitHeight()) / 2;
        Circle clip = new Circle(profileImageView.getFitWidth() / 2, profileImageView.getFitHeight() / 2, radius);
        profileImageView.setClip(clip);
    }

    /**
     * Sends the text currently in the message field to the selected
     * conversation.  Handles both private and group chats.
     */
    private void sendCurrentMessage() {
        String conversation = friendListView.getSelectionModel().getSelectedItem();
        String text = messageField.getText().trim();
        if (conversation == null || text.isEmpty()) {
            return;
        }
        LocalDateTime ts = LocalDateTime.now();
        if (groups.contains(conversation)) {
            Message m = new Message(currentUser.getUsername(), conversation, text, ts);
            appendAndRender(m);
            connection.sendGroupMessage(conversation, text, ts);
        } else {
            Message m = new Message(currentUser.getUsername(), conversation, text, ts);
            appendAndRender(m);
            connection.sendPrivateMessage(conversation, text, ts);
        }
        messageField.clear();
    }

    /**
     * Initiates a video call to the currently selected friend.  Only allowed
     * when not in an ongoing call and the selected conversation is a private
     * chat.  Sends a request to the remote user via the server and opens the
     * caller’s video call window after the remote party accepts.
     */
    private void initiateVideoCall() {
        String conversation = friendListView.getSelectionModel().getSelectedItem();
        // Only allow calls to friends (not groups)
        if (conversation == null || groups.contains(conversation)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Video Call");
            alert.setHeaderText(null);
            alert.setContentText("Video calls are only available in one‑to‑one chats.");
            alert.showAndWait();
            return;
        }
        // Delegate call initiation to the video call controller.  It will handle
        // sending the request and notifying the user of call status.
        videoCallController.initiateCall(conversation);
    }

    private void sendImageAttachment() {
        String conversation = friendListView.getSelectionModel().getSelectedItem();
        if (conversation == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File file = chooser.showOpenDialog(imageButton.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String encoded = Base64.getEncoder().encodeToString(bytes);
            String body = "IMG:" + file.getName() + ":" + encoded;
            LocalDateTime ts = LocalDateTime.now();
            Message m = new Message(currentUser.getUsername(), conversation, body, ts);
            appendAndRender(m);
            if (groups.contains(conversation)) {
                connection.sendGroupMessage(conversation, body, ts);
            } else {
                connection.sendPrivateMessage(conversation, body, ts);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Image Attachment Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to send image: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Adds a message to the history for a conversation and renders it if
     * currently viewing that conversation.
     */
    private void appendAndRender(Message m) {
        String conversation = m.getReceiver();
        chatHistory.computeIfAbsent(conversation, k -> new ArrayList<>()).add(m);
        String selected = friendListView.getSelectionModel().getSelectedItem();
        if (conversation.equals(selected)) {
            chatMessagesBox.getChildren().add(makeBubble(m));
            scrollToBottom();
        }
    }

    /**
     * Displays the members of the currently selected group chat in an alert.
     */
    private void showGroupMembers() {
        String conversation = friendListView.getSelectionModel().getSelectedItem();
        if (conversation == null || !groups.contains(conversation)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Group Members");
            alert.setHeaderText(null);
            alert.setContentText("This conversation is not a group chat.");
            alert.showAndWait();
            return;
        }
        List<String> members = groupMembersMap.get(conversation);
        // If membership information is missing, attempt to fetch it from the server.
        if (members == null || members.isEmpty()) {
            // Request the member list.  The response will update groupMembersMap.
            connection.requestGroupMembers(conversation);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Group Members");
            alert.setHeaderText(null);
            alert.setContentText("Fetching group member information. Please try again shortly.");
            alert.showAndWait();
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String m : members) {
            sb.append(m).append("\n");
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Group Members");
        alert.setHeaderText("Members of " + conversation);
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

    /**
     * Creates a chat bubble for a given message.  Supports both text and
     * image messages.  Group messages include the sender’s name for
     * messages not sent by the current user.
     */
//    private HBox makeBubble(Message m) {
//        boolean isSent = m.getSender().equals(currentUser.getUsername());
//        boolean isGroup = groups.contains(m.getReceiver());
//        HBox box = new HBox();
//        box.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
//        box.setPadding(new Insets(4, 8, 4, 8));
//        String text = m.getText();
//        if (text != null && text.startsWith("IMG:")) {
//            // Parse out the base64 encoded image
//            String payload = text.substring(4);
//            int idx = payload.indexOf(":");
//            if (idx > 0) {
//                String encoded = payload.substring(idx + 1);
//                try {
//                    byte[] data = Base64.getDecoder().decode(encoded);
//                    Image img = new Image(new ByteArrayInputStream(data));
//                    ImageView iv = new ImageView(img);
//                    iv.setFitWidth(200);
//                    iv.setPreserveRatio(true);
//                    Text timestampNode = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "] ");
//                    timestampNode.setStyle("-fx-font-size: 10px; -fx-fill: gray;");
//                    VBox v = new VBox();
//                    v.setSpacing(2);
//                    v.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
//                    v.getChildren().add(iv);
//                    v.getChildren().add(timestampNode);
//                    v.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") +
//                            "; -fx-padding: 8px; -fx-background-radius: 10px;");
//                    if (isGroup && !isSent) {
//                        Text senderNode = new Text(m.getSender() + ":\n");
//                        senderNode.setStyle("-fx-font-weight: bold; -fx-fill: black;");
//                        VBox wrapper = new VBox();
//                        wrapper.getChildren().addAll(senderNode, v);
//                        box.getChildren().add(wrapper);
//                    } else {
//                        box.getChildren().add(v);
//                    }
//                } catch (Exception e) {
//                    // Fallback for invalid image data
//                    String fallback = isGroup && !isSent ? m.getSender() + ": [Invalid image]" : "[Invalid image]";
//                    Text textNode = new Text(fallback);
//                    Text timestampNode = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "] ");
//                    timestampNode.setStyle("-fx-font-size: 10px; -fx-fill: gray;");
//                    TextFlow flow = new TextFlow(timestampNode, textNode);
//                    flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") +
//                            "; -fx-padding: 8px; -fx-background-radius: 10px;");
//                    box.getChildren().add(flow);
//                }
//            } else {
//                // Malformed image message: treat remainder as text
//                String remainder = payload;
//                String displayText = isGroup && !isSent ? m.getSender() + ": " + remainder : remainder;
//                Text textNode = new Text(displayText);
//                Text timestampNode = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "] ");
//                timestampNode.setStyle("-fx-font-size: 10px; -fx-fill: gray;");
//                TextFlow flow = new TextFlow(timestampNode, textNode);
//                flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") +
//                        "; -fx-padding: 8px; -fx-background-radius: 10px;");
//                box.getChildren().add(flow);
//            }
//        } else {
//            // Standard text message
//            String displayText = text == null ? "" : (isGroup && !isSent ? m.getSender() + ": " + text : text);
//            Text textNode = new Text(displayText);
//            Text timestampNode = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "] ");
//            timestampNode.setStyle("-fx-font-size: 10px; -fx-fill: gray;");
//            TextFlow flow = new TextFlow(timestampNode, textNode);
//            flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") +
//                    "; -fx-padding: 8px; -fx-background-radius: 10px;");
//            box.getChildren().add(flow);
//        }
//        return box;
//    }
    private HBox makeBubble(Message m) {
        boolean isSent = m.getSender().equals(currentUser.getUsername());
        boolean isGroup = groups.contains(m.getReceiver());
        HBox box = new HBox();

        box.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        box.setPadding(new Insets(4, 8, 4, 8));

        String text = m.getText();
        if (text != null && text.startsWith("IMG:")) {
            // Image message parsing
            String payload = text.substring(4);
            int idx = payload.indexOf(":");
            if (idx > 0) {
                String encoded = payload.substring(idx + 1);
                try {
                    byte[] data = Base64.getDecoder().decode(encoded);
                    Image img = new Image(new ByteArrayInputStream(data));
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(200);
                    iv.setPreserveRatio(true);
                    Text timestampNode = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "] ");
                    timestampNode.setStyle("-fx-font-size: 10px; -fx-fill: gray;");
                    VBox v = new VBox(iv, timestampNode);
                    v.setSpacing(2);
                    v.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                    v.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") +
                            "; -fx-padding: 8px; -fx-background-radius: 10px;");

                    // ✅ NEW: Create profile pic (for all incoming messages)
                    if (!isSent) {
                        File profileFile = new File("users/" + m.getSender() + "/profile.jpg");
                        Image profileImage = profileFile.exists()
                                ? new Image(profileFile.toURI().toString())
                                : new Image(getClass().getResource("/images/default.jpeg").toString());

                        ImageView profilePic = new ImageView(profileImage);
                        profilePic.setFitWidth(30);
                        profilePic.setFitHeight(30);
                        Circle clip = new Circle(15, 15, 15);
                        profilePic.setClip(clip);

                        VBox wrapper = new VBox();
                        wrapper.setSpacing(4);

                        // ✅ NEW: Sender label only in group
                        if (isGroup) {
                            Label senderLabel = new Label(m.getSender());
                            senderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 12;");
                            wrapper.getChildren().add(senderLabel);
                        }

                        wrapper.getChildren().add(v);
                        HBox messageWithPic = new HBox(8, profilePic, wrapper);
                        messageWithPic.setAlignment(Pos.CENTER_LEFT);
                        box.getChildren().add(messageWithPic);
                    } else {
                        box.getChildren().add(v);
                    }
                } catch (Exception e) {
                    // Fallback for invalid image data
                    String fallback = isGroup && !isSent ? m.getSender() + ": [Invalid image]" : "[Invalid image]";
                    Text textNode = new Text(fallback);
                    Text timestampNode = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "] ");
                    timestampNode.setStyle("-fx-font-size: 10px; -fx-fill: gray;");
                    TextFlow flow = new TextFlow(timestampNode, textNode);
                    flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") +
                            "; -fx-padding: 8px; -fx-background-radius: 10px;");
                    box.getChildren().add(flow);
                }
            } else {
                // Malformed image
                String remainder = payload;
                String displayText = isGroup && !isSent ? m.getSender() + ": " + remainder : remainder;
                Text textNode = new Text(displayText);
                Text timestampNode = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "] ");
                timestampNode.setStyle("-fx-font-size: 10px; -fx-fill: gray;");
                TextFlow flow = new TextFlow(timestampNode, textNode);
                flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") +
                        "; -fx-padding: 8px; -fx-background-radius: 10px;");
                box.getChildren().add(flow);
            }
        } else {
            // Standard text message
            Text textNode = new Text(text == null ? "" : text);
            Text timestampNode = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "] ");
            timestampNode.setStyle("-fx-font-size: 10px; -fx-fill: gray;");
            TextFlow flow = new TextFlow(timestampNode, textNode);
            flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") +
                    "; -fx-padding: 8px; -fx-background-radius: 10px;");

            // ✅ NEW: if incoming message
            if (!isSent) {
                File profileFile = new File("users/" + m.getSender() + "/profile.jpg");
                Image profileImage = profileFile.exists()
                        ? new Image(profileFile.toURI().toString())
                        : new Image(getClass().getResource("/images/default.jpeg").toString());

                ImageView profilePic = new ImageView(profileImage);
                profilePic.setFitWidth(30);
                profilePic.setFitHeight(30);
                Circle clip = new Circle(15, 15, 15);
                profilePic.setClip(clip);

                VBox wrapper = new VBox();
                wrapper.setSpacing(4);

                // ✅ NEW: add sender label only if group
                if (isGroup) {
                    Label senderLabel = new Label(m.getSender());
                    senderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 12;");
                    wrapper.getChildren().add(senderLabel);
                }

                wrapper.getChildren().add(flow);
                HBox messageWithPic = new HBox(8, profilePic, wrapper);
                messageWithPic.setAlignment(Pos.CENTER_LEFT);
                box.getChildren().add(messageWithPic);
            } else {
                box.getChildren().add(flow);
            }
        }
        return box;
    }

    /**
     * Scrolls the chat view to the bottom.  Called after messages are added.
     */
    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatScrollPane.layout();
            chatScrollPane.setVvalue(1.0);
        });
    }

    /**
     * Rebuilds the map of group members based on the current chat history.
     * This method iterates over all known groups and extracts unique
     * senders from each group's message history.  The current user is
     * always included in the membership.  Invoking this after new group
     * messages or histories ensures the group member list is accurate
     * across sessions.
     */
    private void rebuildGroupMembers() {
        groupMembersMap.clear();
        for (String groupName : groups) {
            List<Message> msgs = chatHistory.get(groupName);
            if (msgs == null) continue;
            LinkedHashSet<String> senders = new LinkedHashSet<>();
            for (Message m : msgs) {
                if (m.getSender() != null && !m.getSender().isEmpty()) {
                    senders.add(m.getSender());
                }
            }
            // Ensure the current user is included
            senders.add(currentUser.getUsername());
            groupMembersMap.put(groupName, new java.util.ArrayList<>(senders));
        }
    }

    /**
     * Invoked when a message is received from the server.  Delegates to
     * {@link #handleIncomingMessage(String, String)} on the JavaFX thread.
     */
    @Override
    public void onMessageReceived(String from, String body) {
        Platform.runLater(() -> handleIncomingMessage(from, body));
    }

    /* ------------------------------------------------------------------- */
    /*                   FriendListener implementation                     */
    /* These callbacks fire when a friend request is received, accepted,   */
    /* or declined.  Refresh the current user and repopulate the friend   */
    /* list so that newly accepted friends appear immediately.            */

    @Override
    public void onFriendRequestReceived(String from) {
        Platform.runLater(this::refreshFriendList);
    }

    @Override
    public void onFriendRequestAccepted(String from) {
        Platform.runLater(this::refreshFriendList);
    }

    @Override
    public void onFriendRequestDeclined(String from) {
        Platform.runLater(this::refreshFriendList);
    }

    /**
     * Refresh the current user from disk and update the friend list.  The
     * currently selected conversation is preserved if possible.
     */
    private void refreshFriendList() {
        Session.refreshCurrentUser();
        currentUser = Session.getCurrentUser();
        String selected = friendListView.getSelectionModel().getSelectedItem();
        populateFriendList();
        if (selected != null) {
            friendListView.getSelectionModel().select(selected);
        }
    }

    /**
     * Parses and handles incoming messages from the server.  Supports chat
     * histories, group histories, group creation, group messages, and
     * fallback handling for simple private messages.
     */
    private void handleIncomingMessage(String from, String body) {
        try {
            // Delegate all video call related messages to the video call controller
            // Incoming video frame
            if (body != null && body.startsWith("VIDEO_FRAME|")) {
                String data = body.substring("VIDEO_FRAME|".length());
                videoCallController.receiveVideoFrame(from, data);
                return;
            }
            // Incoming audio frame
            if (body != null && body.startsWith("AUDIO_FRAME|")) {
                String data = body.substring("AUDIO_FRAME|".length());
                videoCallController.receiveAudioFrame(from, data);
                return;
            }
            // Video call request from remote user
            if ("VIDEO_CALL_REQUEST".equals(body)) {
                videoCallController.receiveCallRequest(from);
                return;
            }
            // Response to a previously sent video call request
            if (body != null && body.startsWith("VIDEO_CALL_RESPONSE|")) {
                String[] parts = body.split("\\|", 2);
                String accepted = parts.length >= 2 ? parts[1] : "no";
                boolean accept = "yes".equalsIgnoreCase(accepted);
                videoCallController.handleCallResponse(from, accept);
                return;
            }
            // Remote party ended the call
            if ("END_CALL".equals(body)) {
                videoCallController.handleCallEnd(from);
                return;
            }
            if (body.startsWith("CHAT_HISTORY|")) {
                String[] parts = body.split("\\|", 4);
                if (parts.length == 4) {
                    String friend = parts[1];
                    String user = parts[2];
                    String payload = parts[3];
                    if (!user.equals(currentUser.getUsername())) return;
                    List<Message> history = parseChatHistory(payload, friend, user);
                    chatHistory.put(friend, history);
                    if (friend.equals(friendListView.getSelectionModel().getSelectedItem())) {
                        loadChat(friend);
                    }
                }
                return;
            }
            if (body.startsWith("CHAT_HISTORY_GROUP|")) {
                String[] parts = body.split("\\|", 4);
                if (parts.length == 4) {
                    String groupName = parts[1];
                    String user = parts[2];
                    String payload = parts[3];
                    if (!user.equals(currentUser.getUsername())) return;
                    List<Message> history = new ArrayList<>();
                    if (!payload.isEmpty()) {
                        String[] entries = payload.split(";;;", -1);
                        for (String entry : entries) {
                            String[] mp = entry.split("\\|", 3);
                            if (mp.length == 3) {
                                String sender = mp[0];
                                String tsStr = mp[1];
                                String txt = mp[2];
                                LocalDateTime ts = LocalDateTime.parse(tsStr);
                                history.add(new Message(sender, groupName, txt, ts));
                            }
                        }
                    }
                    groups.add(groupName);
                    chatHistory.put(groupName, history);
                    populateFriendList();
                    if (groupName.equals(friendListView.getSelectionModel().getSelectedItem())) {
                        loadChat(groupName);
                    }
                    // Infer members from history if none provided
                    if (!groupMembersMap.containsKey(groupName)) {
                        Set<String> senders = new LinkedHashSet<>();
                        for (Message msg : history) {
                            String sender = msg.getSender();
                            if (sender != null && !sender.isEmpty()) {
                                senders.add(sender);
                            }
                        }
                        senders.add(currentUser.getUsername());
                        groupMembersMap.put(groupName, new ArrayList<>(senders));
                    }
                    // After updating group histories, rebuild member lists to ensure they persist
                    rebuildGroupMembers();
                }
                return;
            }
            if (body.startsWith("GROUP_MEMBERS|")) {
                // Format: GROUP_MEMBERS|groupName|m1,m2,m3
                String[] parts = body.split("\\|", 3);
                if (parts.length >= 3) {
                    String groupName  = parts[1];
                    String memberCsv = parts[2];
                    List<String> memberList = new ArrayList<>();
                    if (!memberCsv.isEmpty()) {
                        for (String m : memberCsv.split(",")) {
                            String trimmed = m.trim();
                            if (!trimmed.isEmpty() && !memberList.contains(trimmed)) {
                                memberList.add(trimmed);
                            }
                        }
                    }
                    // Ensure current user is included in the list
                    if (!memberList.contains(currentUser.getUsername())) {
                        memberList.add(currentUser.getUsername());
                    }
                    groupMembersMap.put(groupName, memberList);
                }
                return;
            }
            if (body.startsWith("GROUP_LIST|")) {
                String[] parts = body.split("\\|", 2);
                if (parts.length == 2) {
                    String csv = parts[1];
                    if (!csv.isEmpty()) {
                        for (String g : csv.split(",")) {
                            if (!g.isBlank()) groups.add(g.trim());
                        }
                    }
                    populateFriendList();
                }
                return;
            }
            if (body.startsWith("GROUP_CREATED|")) {
                String[] parts = body.split("\\|", 4);
                if (parts.length >= 3) {
                    String groupName = parts[1];
                    String creator = parts[2];
                    String members = parts.length >= 4 ? parts[3] : "";
                    boolean include = creator.equals(currentUser.getUsername());
                    if (!include && members != null && !members.isEmpty()) {
                        for (String m : members.split(",")) {
                            if (m.equals(currentUser.getUsername())) {
                                include = true;
                                break;
                            }
                        }
                    }
                    if (include) {
                        groups.add(groupName);
                        populateFriendList();
                        // Parse and store group members
                        List<String> memberList = new ArrayList<>();
                        if (creator != null && !creator.isEmpty()) {
                            memberList.add(creator);
                        }
                        if (members != null && !members.isEmpty()) {
                            for (String m : members.split(",")) {
                                String trimmed = m.trim();
                                if (!trimmed.isEmpty() && !memberList.contains(trimmed)) {
                                    memberList.add(trimmed);
                                }
                            }
                        }
                        if (!memberList.contains(currentUser.getUsername())) {
                            memberList.add(currentUser.getUsername());
                        }
                        groupMembersMap.put(groupName, memberList);
                        // Ensure our cache of group members is updated for all groups
                        rebuildGroupMembers();
                    }
                }
                return;
            }
            if (body.startsWith("GROUP_MSG|")) {
                String[] parts = body.split("\\|", 4);
                if (parts.length == 4) {
                    String sender = parts[1];
                    String tsStr = parts[2];
                    String text = parts[3];
                    LocalDateTime ts = LocalDateTime.parse(tsStr);
                    String groupName = from;
                    Message m = new Message(sender, groupName, text, ts);
                    appendAndRender(m);
                    // Rebuild group members after receiving a new group message, just in case
                    rebuildGroupMembers();
                }
                return;
            }
            // Fallback for private messages: treat first token as timestamp
            String[] bodyParts = body.split("\\|", 2);
            String tsStr = bodyParts[0];
            String msgText = bodyParts.length > 1 ? bodyParts[1] : "";
            if (tsStr.matches("\\d{4}-\\d{2}-\\d{2}T.*")) {
                try {
                    LocalDateTime ts = LocalDateTime.parse(tsStr);
                    String conversation = from;
                    String sender = from;
                    Message m = new Message(sender, conversation, msgText, ts);
                    appendAndRender(m);
                } catch (Exception parseEx) {
                    System.out.println("Failed to parse message timestamp: " + body);
                }
            } else {
                System.out.println("Ignoring unrecognized message: " + body);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses a chat history payload into a list of Message objects.  Each
     * entry is expected to be of the form sender|timestamp|text.  The
     * receiver is always stored as the friend’s username for private chats.
     */
    private List<Message> parseChatHistory(String payload, String friend, String user) {
        List<Message> messages = new ArrayList<>();
        String[] entries = payload.split(";;;", -1);
        for (String entry : entries) {
            String[] parts = entry.split("\\|", 3);
            if (parts.length == 3) {
                String sender = parts[0];
                String tsStr = parts[1];
                String text = parts[2];
                LocalDateTime ts = LocalDateTime.parse(tsStr);
                String receiver = friend; // always store under the friend’s name
                messages.add(new Message(sender, receiver, text, ts));
            }
        }
        return messages;
    }

    /**
     * Simple data class representing a chat message.  Contains the sender,
     * receiver (conversation identifier), message text, and timestamp.
     */
    public static class Message {
        private final String sender;
        private final String receiver;
        private final String text;
        private final LocalDateTime timestamp;
        public Message(String sender, String receiver, String text, LocalDateTime timestamp) {
            this.sender = sender;
            this.receiver = receiver;
            this.text = text;
            this.timestamp = timestamp;
        }
        public String getSender() { return sender; }
        public String getReceiver() { return receiver; }
        public String getText() { return text; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getOtherParty(String currentUser) {
            return currentUser.equals(sender) ? receiver : sender;
        }
    }

    /**
     * Called when the chat UI is closed.  Unregisters this controller from
     * receiving further message and friend events.
     */
    public void shutdown() {
        connection.removeListener(this);
        connection.removeFriendListener(this);
        // Ensure any ongoing call is terminated when the controller shuts down.  Delegate to the
        // video call controller so it can clean up resources and notify the remote party.
        if (videoCallController != null) {
            videoCallController.endCall();
        }
    }

//
//// VIDEO CONTROLLER DOCUMENTATION
//
//    /**
//     * Displays a simple video call window.  For this prototype, the window
//     * contains placeholder content indicating the call state.  When the user
//     * closes the window or clicks the end call button, an END_CALL message
//     * is sent to the remote peer and the call is terminated.
//     *
//     * @param withUser the username of the other participant
//     * @param incoming true if this call was initiated by the other user
//     */
//    private void openVideoCallWindow(String withUser, boolean incoming) {
//        // Deprecated: this implementation has been moved to VideoCallController
//    }
//
//    /**
//     * Closes the current video call window and resets the call state.
//     */
//    private void closeVideoCallWindow() {
//        // Deprecated: functionality moved to VideoCallController
//    }
//
//    /**
//     * Starts capturing webcam frames and sending them to the remote user.
//     * This method initialises the webcam (if available), sets up a scheduled
//     * executor to grab frames at ~10 FPS, updates the local feed view, and
//     * transmits the frames encoded as base64 strings over the network.
//     *
//     * @param withUser the username of the remote user in this call
//     */
//    private void startVideoCapture(String withUser) {
//        // Deprecated: functionality moved to VideoCallController
//    }
//
//    /**
//     * Stops the webcam capture service and releases resources.  Called when
//     * the call ends or the window is closed.
//     */
//    private void stopVideoCapture() {
//        // Deprecated: functionality moved to VideoCallController
//    }
//
//    /**
//     * Prompts the user to select an image file, encodes it in base64, and
//     * sends it to the currently selected conversation.  Prepends "IMG:" to
//     * the body so the receiving controller knows to render an image bubble.
//     */
}








