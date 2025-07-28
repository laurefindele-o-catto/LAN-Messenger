//package main.java.controller;
//
//import javafx.application.Platform;
//import javafx.collections.FXCollections;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.control.*;
//import javafx.scene.image.Image;
//import javafx.scene.image.ImageView;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//import javafx.scene.text.Text;
//import javafx.scene.text.TextFlow;
//import javafx.stage.FileChooser;
//import main.java.client.ClientConnection;
//import main.java.user.Session;
//import main.java.user.User;
//import main.java.util.sceneChange;
//
//import java.io.ByteArrayInputStream;
//import java.io.File;
//import java.io.IOException;
//import java.net.URL;
//import java.nio.file.Files;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//
//public class ChatBoxController implements Initializable, ClientConnection.MessageListener {
//
//    @FXML private ListView<String> friendListView;
//    @FXML private ScrollPane chatScrollPane;
//    @FXML private VBox chatMessagesBox;
//    @FXML private TextField messageField;
//    @FXML private Button sendButton;
//    @FXML private Button backButton;
//    @FXML private Button createGroupButton;
//    // Button for attaching images
//    @FXML private Button imageButton;
//
//    // NEW: button to view group members
//    @FXML private Button membersButton;
//
//    // Label that shows the current conversation name (friend or group)
//    @FXML private Label chatTitleLabel;
//
//    private User currentUser;
//    private ClientConnection connection;
//    private final Map<String, List<Message>> chatHistory = new HashMap<>();
//    private final Set<String> groups = new HashSet<>();
//    // NEW: map of group name to its members (including the creator and yourself)
//    private final Map<String, List<String>> groupMembersMap = new HashMap<>();
//    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
//            DateTimeFormatter.ofPattern("HH:mm");
//
//    @Override
//    public void initialize(URL location, ResourceBundle resources) {
//        ClientConnection.getInstance().setCurrentController(this);
//        currentUser = Session.getCurrentUser();
//        connection = ClientConnection.getInstance();
//        connection.registerListener(this);
//        connection.requestChatHistory(currentUser.getUsername());
//        connection.requestGroupList();
//
//        populateFriendList();
//
//        friendListView.getSelectionModel().selectedItemProperty()
//                .addListener((obs, oldVal, newVal) -> loadChat(newVal));
//
//        sendButton.setOnAction(e -> sendCurrentMessage());
//        messageField.setOnAction(e -> sendCurrentMessage());
//        backButton.setOnAction(e -> {
//            shutdown();
//            sceneChange.changeScene("Dashboard.fxml", backButton, currentUser);
//        });
//        if (createGroupButton != null) {
//            createGroupButton.setOnAction(e -> openGroupCreationDialog());
//        }
//        if (imageButton != null) {
//            imageButton.setOnAction(e -> sendImageAttachment());
//        }
//        // NEW: wire up the members button to show group members
//        if (membersButton != null) {
//            membersButton.setOnAction(e -> showGroupMembers());
//        }
//    }
//
//    private void openGroupCreationDialog() {
//        if (currentUser.getFriends() == null || currentUser.getFriends().isEmpty()) {
//            return;
//        }
//        javafx.stage.Stage dialog = new javafx.stage.Stage();
//        dialog.setTitle("Create Group");
//        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
//        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10);
//        root.setPadding(new Insets(10));
//        Label nameLabel = new Label("Group Name:");
//        TextField nameField = new TextField();
//        nameField.setPromptText("Enter group name");
//        Label membersLabel = new Label("Select members:");
//        VBox checkContainer = new VBox(5);
//        for (String f : currentUser.getFriends()) {
//            CheckBox cb = new CheckBox(f);
//            checkContainer.getChildren().add(cb);
//        }
//        Button createBtn = new Button("Create");
//        Button cancelBtn = new Button("Cancel");
//        HBox buttonBar = new HBox(10, createBtn, cancelBtn);
//        buttonBar.setAlignment(Pos.CENTER_RIGHT);
//        root.getChildren().addAll(nameLabel, nameField, membersLabel, checkContainer, buttonBar);
//        javafx.scene.Scene scene = new javafx.scene.Scene(root);
//        dialog.setScene(scene);
//        createBtn.setOnAction(ev -> {
//            String groupName = nameField.getText().trim();
//            if (groupName.isEmpty()) return;
//            List<String> selected = new ArrayList<>();
//            for (javafx.scene.Node n : checkContainer.getChildren()) {
//                if (n instanceof CheckBox cb && cb.isSelected()) {
//                    selected.add(cb.getText());
//                }
//            }
//            if (selected.isEmpty()) return;
//            connection.createGroup(groupName, selected);
//            dialog.close();
//        });
//        cancelBtn.setOnAction(ev -> dialog.close());
//        dialog.showAndWait();
//    }
//
//    private void populateFriendList() {
//        Set<String> friends = currentUser.getFriends();
//        List<String> all = new ArrayList<>();
//        all.addAll(groups);
//        if (friends != null) {
//            all.addAll(friends);
//        }
//        all.sort(String::compareToIgnoreCase);
//        friendListView.setItems(FXCollections.observableArrayList(all));
//    }
//
//    private void loadChat(String conversation) {
//        // Update the title label to show the current conversation name
//        if (chatTitleLabel != null) {
//            chatTitleLabel.setText(conversation == null ? "" : conversation);
//        }
//        // Show or hide the "Members" button based on whether this is a group chat
//        if (membersButton != null) {
//            boolean isGroup = conversation != null && groups.contains(conversation);
//            membersButton.setVisible(isGroup);
//        }
//        chatMessagesBox.getChildren().clear();
//        if (conversation == null) return;
//        List<Message> history = chatHistory.get(conversation);
//        if (history != null) {
//            for (Message m : history) {
//                chatMessagesBox.getChildren().add(makeBubble(m));
//            }
//        }
//        scrollToBottom();
//    }
//
//    private void sendCurrentMessage() {
//        String conversation = friendListView.getSelectionModel().getSelectedItem();
//        String text = messageField.getText().trim();
//        if (conversation == null || text.isEmpty()) {
//            return;
//        }
//        LocalDateTime ts = LocalDateTime.now();
//        if (groups.contains(conversation)) {
//            Message m = new Message(currentUser.getUsername(), conversation, text, ts);
//            appendAndRender(m);
//            connection.sendGroupMessage(conversation, text, ts);
//        } else {
//            Message m = new Message(currentUser.getUsername(), conversation, text, ts);
//            appendAndRender(m);
//            connection.sendPrivateMessage(conversation, text, ts);
//        }
//        messageField.clear();
//    }
//
//    /** Selects an image file, encodes it as base64, and sends it. */
//    private void sendImageAttachment() {
//        String conversation = friendListView.getSelectionModel().getSelectedItem();
//        if (conversation == null) {
//            return;
//        }
//        FileChooser chooser = new FileChooser();
//        chooser.setTitle("Select Image");
//        chooser.getExtensionFilters().addAll(
//                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
//        );
//        File file = chooser.showOpenDialog(imageButton.getScene().getWindow());
//        if (file == null) {
//            return;
//        }
//        try {
//            byte[] bytes = Files.readAllBytes(file.toPath());
//            String encoded = java.util.Base64.getEncoder().encodeToString(bytes);
//            String body = "IMG:" + file.getName() + ":" + encoded;
//            LocalDateTime ts = LocalDateTime.now();
//            Message m = new Message(currentUser.getUsername(), conversation, body, ts);
//            appendAndRender(m);
//            if (groups.contains(conversation)) {
//                connection.sendGroupMessage(conversation, body, ts);
//            } else {
//                connection.sendPrivateMessage(conversation, body, ts);
//            }
//        } catch (IOException ex) {
//            ex.printStackTrace();
//            Alert alert = new Alert(Alert.AlertType.ERROR);
//            alert.setTitle("Image Attachment Error");
//            alert.setHeaderText(null);
//            alert.setContentText("Failed to send image: " + ex.getMessage());
//            alert.showAndWait();
//        }
//    }
//
//    private void appendAndRender(Message m) {
//        String conversation = m.getReceiver();
//        chatHistory.computeIfAbsent(conversation, k -> new ArrayList<>()).add(m);
//        String selected = friendListView.getSelectionModel().getSelectedItem();
//        if (conversation.equals(selected)) {
//            chatMessagesBox.getChildren().add(makeBubble(m));
//            scrollToBottom();
//        }
//    }
//
//    /** Shows the members of the currently selected group chat. */
//    private void showGroupMembers() {
//        String conversation = friendListView.getSelectionModel().getSelectedItem();
//        if (conversation == null || !groups.contains(conversation)) {
//            Alert alert = new Alert(Alert.AlertType.INFORMATION);
//            alert.setTitle("Group Members");
//            alert.setHeaderText(null);
//            alert.setContentText("This conversation is not a group chat.");
//            alert.showAndWait();
//            return;
//        }
//        List<String> members = groupMembersMap.get(conversation);
//        if (members == null || members.isEmpty()) {
//            Alert alert = new Alert(Alert.AlertType.INFORMATION);
//            alert.setTitle("Group Members");
//            alert.setHeaderText(null);
//            alert.setContentText("No member information available for this group.");
//            alert.showAndWait();
//            return;
//        }
//        StringBuilder sb = new StringBuilder();
//        for (String m : members) {
//            sb.append(m).append("\n");
//        }
//        Alert alert = new Alert(Alert.AlertType.INFORMATION);
//        alert.setTitle("Group Members");
//        alert.setHeaderText("Members of " + conversation);
//        alert.setContentText(sb.toString());
//        alert.showAndWait();
//    }
//
//    private HBox makeBubble(Message m) {
//        boolean isSent = m.getSender().equals(currentUser.getUsername());
//        boolean isGroup = groups.contains(m.getReceiver());
//        HBox box = new HBox();
//        box.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
//        box.setPadding(new Insets(4, 8, 4, 8));
//
//        String text = m.getText();
//        // If the message starts with IMG:, parse and display the image
//        if (text != null && text.startsWith("IMG:")) {
//            String payload = text.substring(4);
//            int idx = payload.indexOf(":");
//            if (idx > 0) {
//                String encoded = payload.substring(idx + 1);
//                try {
//                    byte[] data = java.util.Base64.getDecoder().decode(encoded);
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
//                // Malformed image message, treat the remainder as text
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
//            // Normal text message
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
//
//    private void scrollToBottom() {
//        Platform.runLater(() -> {
//            chatScrollPane.layout();
//            chatScrollPane.setVvalue(1.0);
//        });
//    }
//
//    @Override
//    public void onMessageReceived(String from, String body) {
//        Platform.runLater(() -> handleIncomingMessage(from, body));
//    }
//
//    /** Processes various incoming messages.  Also collects group members. */
//    private void handleIncomingMessage(String from, String body) {
//        try {
//            if (body.startsWith("CHAT_HISTORY|")) {
//                String[] parts = body.split("\\|", 4);
//                if (parts.length == 4) {
//                    String friend = parts[1];
//                    String user = parts[2];
//                    String payload = parts[3];
//                    if (!user.equals(currentUser.getUsername())) return;
//                    List<Message> history = parseChatHistory(payload, friend, user);
//                    chatHistory.put(friend, history);
//                    if (friend.equals(friendListView.getSelectionModel().getSelectedItem())) {
//                        loadChat(friend);
//                    }
//                }
//                return;
//            }
//            if (body.startsWith("CHAT_HISTORY_GROUP|")) {
//                String[] parts = body.split("\\|", 4);
//                if (parts.length == 4) {
//                    String groupName = parts[1];
//                    String user = parts[2];
//                    String payload = parts[3];
//                    if (!user.equals(currentUser.getUsername())) return;
//                    List<Message> history = new ArrayList<>();
//                    if (!payload.isEmpty()) {
//                        String[] entries = payload.split(";;;", -1);
//                        for (String entry : entries) {
//                            String[] mp = entry.split("\\|", 3);
//                            if (mp.length == 3) {
//                                String sender = mp[0];
//                                String tsStr = mp[1];
//                                String txt = mp[2];
//                                LocalDateTime ts = LocalDateTime.parse(tsStr);
//                                history.add(new Message(sender, groupName, txt, ts));
//                            }
//                        }
//                    }
//                    groups.add(groupName);
//                    chatHistory.put(groupName, history);
//                    populateFriendList();
//                    if (groupName.equals(friendListView.getSelectionModel().getSelectedItem())) {
//                        loadChat(groupName);
//                    }
//                    // NEW: infer members from chat history if none have been stored yet
//                    if (!groupMembersMap.containsKey(groupName)) {
//                        Set<String> senders = new LinkedHashSet<>();
//                        for (Message msg : history) {
//                            String sender = msg.getSender();
//                            if (sender != null && !sender.isEmpty()) {
//                                senders.add(sender);
//                            }
//                        }
//                        // Always include yourself
//                        senders.add(currentUser.getUsername());
//                        groupMembersMap.put(groupName, new ArrayList<>(senders));
//                    }
//                }
//                return;
//            }
//            if (body.startsWith("GROUP_LIST|")) {
//                String[] parts = body.split("\\|", 2);
//                if (parts.length == 2) {
//                    String csv = parts[1];
//                    if (!csv.isEmpty()) {
//                        for (String g : csv.split(",")) {
//                            if (!g.isBlank()) groups.add(g.trim());
//                        }
//                    }
//                    populateFriendList();
//                }
//                return;
//            }
//            if (body.startsWith("GROUP_CREATED|")) {
//                String[] parts = body.split("\\|", 4);
//                if (parts.length >= 3) {
//                    String groupName = parts[1];
//                    String creator = parts[2];
//                    String members = parts.length >= 4 ? parts[3] : "";
//                    boolean include = creator.equals(currentUser.getUsername());
//                    if (!include && members != null && !members.isEmpty()) {
//                        for (String m : members.split(",")) {
//                            if (m.equals(currentUser.getUsername())) {
//                                include = true;
//                                break;
//                            }
//                        }
//                    }
//                    if (include) {
//                        groups.add(groupName);
//                        populateFriendList();
//                        // NEW: parse and store the group members from this creation event
//                        List<String> memberList = new ArrayList<>();
//                        if (creator != null && !creator.isEmpty()) {
//                            memberList.add(creator);
//                        }
//                        if (members != null && !members.isEmpty()) {
//                            for (String m : members.split(",")) {
//                                String trimmed = m.trim();
//                                if (!trimmed.isEmpty() && !memberList.contains(trimmed)) {
//                                    memberList.add(trimmed);
//                                }
//                            }
//                        }
//                        // Always include the current user if not already in list
//                        if (!memberList.contains(currentUser.getUsername())) {
//                            memberList.add(currentUser.getUsername());
//                        }
//                        groupMembersMap.put(groupName, memberList);
//                    }
//                }
//                return;
//            }
//            if (body.startsWith("GROUP_MSG|")) {
//                String[] parts = body.split("\\|", 4);
//                if (parts.length == 4) {
//                    String sender = parts[1];
//                    String tsStr = parts[2];
//                    String text = parts[3];
//                    LocalDateTime ts = LocalDateTime.parse(tsStr);
//                    String groupName = from;
//                    Message m = new Message(sender, groupName, text, ts);
//                    appendAndRender(m);
//                }
//                return;
//            }
//            // Fallback for private or other messages
//            String[] bodyParts = body.split("\\|", 2);
//            String tsStr = bodyParts[0];
//            String msgText = bodyParts.length > 1 ? bodyParts[1] : "";
//            if (tsStr.matches("\\d{4}-\\d{2}-\\d{2}T.*")) {
//                LocalDateTime ts = LocalDateTime.parse(tsStr);
//                Message m = new Message(from, currentUser.getUsername(), msgText, ts);
//                appendAndRender(m);
//            } else {
//                System.out.println("Ignoring unrecognized message: " + body);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private List<Message> parseChatHistory(String payload, String friend, String user) {
//        List<Message> messages = new ArrayList<>();
//        String[] entries = payload.split(";;;", -1);
//        for (String entry : entries) {
//            String[] parts = entry.split("\\|", 3);
//            if (parts.length == 3) {
//                String sender = parts[0];
//                String tsStr = parts[1];
//                String text = parts[2];
//                LocalDateTime ts = LocalDateTime.parse(tsStr);
//                String receiver = sender.equals(user) ? friend : user;
//                messages.add(new Message(sender, receiver, text, ts));
//            }
//        }
//        return messages;
//    }
//
//    /** Simple POJO for chat messages. */
//    public static class Message {
//        private final String sender;
//        private final String receiver;
//        private final String text;
//        private final LocalDateTime timestamp;
//
//        public Message(String sender, String receiver, String text, LocalDateTime timestamp) {
//            this.sender = sender;
//            this.receiver = receiver;
//            this.text = text;
//            this.timestamp = timestamp;
//        }
//
//        public String getSender() { return sender; }
//        public String getReceiver() { return receiver; }
//        public String getText() { return text; }
//        public LocalDateTime getTimestamp() { return timestamp; }
//        public String getOtherParty(String currentUser) {
//            return currentUser.equals(sender) ? receiver : sender;
//        }
//    }
//
//    public void shutdown() {
//        connection.removeListener(this);
//    }
//}






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
import main.java.client.ClientConnection;
import main.java.user.Session;
import main.java.user.User;
import main.java.util.sceneChange;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatBoxController implements Initializable, ClientConnection.MessageListener {

    @FXML private ListView<String> friendListView;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesBox;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button backButton;
    @FXML private Button createGroupButton;
    @FXML private Button imageButton;
    @FXML private Button membersButton;
    @FXML private Label chatTitleLabel;

    private User currentUser;
    private ClientConnection connection;
    private final Map<String, List<Message>> chatHistory = new HashMap<>();
    private final Set<String> groups = new HashSet<>();
    private final Map<String, List<String>> groupMembersMap = new HashMap<>();

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ClientConnection.getInstance().setCurrentController(this);
        currentUser = Session.getCurrentUser();
        connection = ClientConnection.getInstance();
        connection.registerListener(this);
        connection.requestChatHistory(currentUser.getUsername());
        connection.requestGroupList();

        populateFriendList();

        friendListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> loadChat(newVal));

        sendButton.setOnAction(e -> sendCurrentMessage());
        messageField.setOnAction(e -> sendCurrentMessage());
        backButton.setOnAction(e -> {
            shutdown();
            sceneChange.changeScene("Dashboard.fxml", backButton, currentUser);
        });
        if (createGroupButton != null) {
            createGroupButton.setOnAction(e -> openGroupCreationDialog());
        }
        if (imageButton != null) {
            imageButton.setOnAction(e -> sendImageAttachment());
        }
        if (membersButton != null) {
            membersButton.setOnAction(e -> showGroupMembers());
        }
    }

    private void openGroupCreationDialog() {
        if (currentUser.getFriends() == null || currentUser.getFriends().isEmpty()) return;
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle("Create Group");
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10);
        root.setPadding(new Insets(10));
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

    private void loadChat(String conversation) {
        if (chatTitleLabel != null) {
            chatTitleLabel.setText(conversation == null ? "" : conversation);
        }
        if (membersButton != null) {
            boolean isGroup = conversation != null && groups.contains(conversation);
            membersButton.setVisible(isGroup);
        }
        chatMessagesBox.getChildren().clear();
        if (conversation == null) return;
        List<Message> history = chatHistory.get(conversation);
        if (history != null) {
            for (Message m : history) {
                chatMessagesBox.getChildren().add(makeBubble(m));
            }
        }
        scrollToBottom();
    }

    private void sendCurrentMessage() {
        String conversation = friendListView.getSelectionModel().getSelectedItem();
        String text = messageField.getText().trim();
        if (conversation == null || text.isEmpty()) return;
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

    private void sendImageAttachment() {
        String conversation = friendListView.getSelectionModel().getSelectedItem();
        if (conversation == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File file = chooser.showOpenDialog(imageButton.getScene().getWindow());
        if (file == null) return;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String encoded = java.util.Base64.getEncoder().encodeToString(bytes);
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

    private void appendAndRender(Message m) {
        String conversation = m.getReceiver();
        chatHistory.computeIfAbsent(conversation, k -> new ArrayList<>()).add(m);
        String selected = friendListView.getSelectionModel().getSelectedItem();
        if (conversation.equals(selected)) {
            chatMessagesBox.getChildren().add(makeBubble(m));
            scrollToBottom();
        }
    }

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
        if (members == null || members.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Group Members");
            alert.setHeaderText(null);
            alert.setContentText("No member information available for this group.");
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

    private HBox makeBubble(Message m) {
        boolean isSent = m.getSender().equals(currentUser.getUsername());
        boolean isGroup = groups.contains(m.getReceiver());
        HBox box = new HBox();
        box.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        box.setPadding(new Insets(4, 8, 4, 8));

        String text = m.getText();
        if (text != null && text.startsWith("IMG:")) {
            String payload = text.substring(4);
            int idx = payload.indexOf(":");
            if (idx > 0) {
                String encoded = payload.substring(idx + 1);
                try {
                    byte[] data = java.util.Base64.getDecoder().decode(encoded);
                    Image img = new Image(new ByteArrayInputStream(data));
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(200);
                    iv.setPreserveRatio(true);
                    Text timestampNode = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "] ");
                    timestampNode.setStyle("-fx-font-size: 10px; -fx-fill: gray;");
                    VBox v = new VBox();
                    v.setSpacing(2);
                    v.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                    v.getChildren().add(iv);
                    v.getChildren().add(timestampNode);
                    v.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") +
                            "; -fx-padding: 8px; -fx-background-radius: 10px;");
                    if (isGroup && !isSent) {
                        Text senderNode = new Text(m.getSender() + ":\n");
                        senderNode.setStyle("-fx-font-weight: bold; -fx-fill: black;");
                        VBox wrapper = new VBox();
                        wrapper.getChildren().addAll(senderNode, v);
                        box.getChildren().add(wrapper);
                    } else {
                        box.getChildren().add(v);
                    }
                } catch (Exception e) {
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
            String displayText = text == null ? "" : (isGroup && !isSent ? m.getSender() + ": " + text : text);
            Text textNode = new Text(displayText);
            Text timestampNode = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "] ");
            timestampNode.setStyle("-fx-font-size: 10px; -fx-fill: gray;");
            TextFlow flow = new TextFlow(timestampNode, textNode);
            flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") +
                    "; -fx-padding: 8px; -fx-background-radius: 10px;");
            box.getChildren().add(flow);
        }
        return box;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatScrollPane.layout();
            chatScrollPane.setVvalue(1.0);
        });
    }

    @Override
    public void onMessageReceived(String from, String body) {
        Platform.runLater(() -> handleIncomingMessage(from, body));
    }

    private void handleIncomingMessage(String from, String body) {
        try {
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
                }
                return;
            }
            // Fallback for one-to-one messages
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
                String receiver = friend; // always store under the friend's name
                messages.add(new Message(sender, receiver, text, ts));
            }
        }
        return messages;
    }

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

    public void shutdown() {
        connection.removeListener(this);
    }
}
