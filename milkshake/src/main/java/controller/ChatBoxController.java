//
//package main.java.controller;
//
//import javafx.application.Platform;
//import javafx.collections.FXCollections;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.control.Button;
//import javafx.scene.control.ListView;
//import javafx.scene.control.ScrollPane;
//import javafx.scene.control.TextField;
//import javafx.scene.text.Text;
//import javafx.scene.text.TextAlignment;
//import javafx.scene.text.TextFlow;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//import javafx.beans.value.ChangeListener;
//import javafx.beans.value.ObservableValue;
//import javafx.event.ActionEvent;
//import javafx.event.EventHandler;
//import main.java.client.ClientConnection;
//import main.java.user.Session;
//import main.java.user.User;
//import main.java.util.sceneChange;
//
//import java.net.URL;
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
//
//    // Button to create a new group chat. This is wired up via FXML.
//    @FXML private Button createGroupButton;
//
//    private User currentUser;
//    private ClientConnection connection;
//    private final Map<String, List<Message>> chatHistory = new HashMap<>();
//    // Maintain a set of groups that the current user belongs to. These strings
//    // are treated like "friend" entries in the list view but represent group
//    // conversations. When present, they will be rendered in the same list as
//    // individual friends.
//    private final Set<String> groups = new HashSet<>();
//    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
//
//    @Override
//    public void initialize(URL location, ResourceBundle resources) {
//        ClientConnection.getInstance().setCurrentController(this);
//        currentUser = Session.getCurrentUser();
//        connection = ClientConnection.getInstance();
//        connection.registerListener(this);
//
//        // Request chat history from server
//        connection.requestChatHistory(currentUser.getUsername());
//        // Request the groups the current user is a member of. The server will
//        // respond with a GROUP_LIST message.
//        connection.requestGroupList();
//
//        populateFriendList();
//
//        friendListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
//            @Override
//            public void changed(ObservableValue<? extends String> obs, String oldValue, String newValue) {
//                loadChat(newValue);
//            }
//        });
//
//        sendButton.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                sendCurrentMessage();
//            }
//        });
//
//        messageField.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                sendCurrentMessage();
//            }
//        });
//
//        backButton.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                shutdown();
//                sceneChange.changeScene("Dashboard.fxml", backButton, currentUser);
//            }
//        });
//
//        // When the createGroupButton is clicked, open a simple dialog to
//        // compose a new group. The dialog allows entering a group name and
//        // selecting friends to include in the group. Once the group is
//        // created, the server will notify all participants via a GROUP_CREATED
//        // message which this controller will handle.
//        if (createGroupButton != null) {
//            createGroupButton.setOnAction(event -> openGroupCreationDialog());
//        }
//
//        // Auto-select first friend if available
//        if (!friendListView.getItems().isEmpty()) {
//            friendListView.getSelectionModel().selectFirst();
//            loadChat(friendListView.getSelectionModel().getSelectedItem());
//        }
//    }
//
//    /**
//     * Opens a modal dialog that allows the user to specify a name for the new
//     * group and select friends to be included. On confirmation, a CREATE_GROUP
//     * message will be sent to the server via the ClientConnection. No action
//     * is taken if the user cancels the dialog or leaves the name empty.
//     */
//    private void openGroupCreationDialog() {
//        // Ensure we have at least one friend to create a group with
//        if (currentUser.getFriends() == null || currentUser.getFriends().isEmpty()) {
//            return;
//        }
//        javafx.stage.Stage dialog = new javafx.stage.Stage();
//        dialog.setTitle("Create Group");
//        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
//        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10);
//        root.setPadding(new Insets(10));
//        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label("Group Name:");
//        javafx.scene.control.TextField nameField = new javafx.scene.control.TextField();
//        nameField.setPromptText("Enter group name");
//        javafx.scene.control.Label membersLabel = new javafx.scene.control.Label("Select members:");
//        javafx.scene.layout.VBox checkContainer = new javafx.scene.layout.VBox(5);
//        for (String f : currentUser.getFriends()) {
//            javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(f);
//            checkContainer.getChildren().add(cb);
//        }
//        javafx.scene.control.Button createBtn = new javafx.scene.control.Button("Create");
//        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("Cancel");
//        javafx.scene.layout.HBox buttonBar = new javafx.scene.layout.HBox(10, createBtn, cancelBtn);
//        buttonBar.setAlignment(Pos.CENTER_RIGHT);
//        root.getChildren().addAll(nameLabel, nameField, membersLabel, checkContainer, buttonBar);
//        javafx.scene.Scene scene = new javafx.scene.Scene(root);
//        dialog.setScene(scene);
//        createBtn.setOnAction(ev -> {
//            String groupName = nameField.getText().trim();
//            if (groupName.isEmpty()) {
//                return;
//            }
//            List<String> selected = new ArrayList<>();
//            for (javafx.scene.Node n : checkContainer.getChildren()) {
//                if (n instanceof javafx.scene.control.CheckBox cb && cb.isSelected()) {
//                    selected.add(cb.getText());
//                }
//            }
//            if (selected.isEmpty()) {
//                return;
//            }
//            connection.createGroup(groupName, selected);
//            dialog.close();
//        });
//        cancelBtn.setOnAction(ev -> dialog.close());
//        dialog.showAndWait();
//    }
//
//    private void populateFriendList() {
//        Set<String> friends = currentUser.getFriends();
//        if (friends == null || friends.isEmpty()) {
//            friendListView.setItems(FXCollections.observableArrayList());
//            return;
//        }
//
//        List<String> allEntries = new ArrayList<>();
//        // Add groups first so they appear at the top of the list
//        if (!groups.isEmpty()) {
//            allEntries.addAll(groups);
//        }
//        // Then add friends
//        allEntries.addAll(friends);
//        Collections.sort(allEntries, String::compareToIgnoreCase);
//        friendListView.setItems(FXCollections.observableArrayList(allEntries));
//    }
//
//    private void loadChat(String friend) {
//        chatMessagesBox.getChildren().clear();
//        if (friend == null) {
//            return;
//        }
//        List<Message> history = chatHistory.get(friend);
//        if (history != null) {
//            for (Message msg : history) {
//                chatMessagesBox.getChildren().add(makeBubble(msg));
//            }
//        }
//        scrollToBottom();
//    }
//
//    private void sendCurrentMessage() {
//        String recipient = friendListView.getSelectionModel().getSelectedItem();
//        String text = messageField.getText().trim();
//        if (recipient == null || text.isEmpty()) {
//            return;
//        }
//        LocalDateTime timestamp = LocalDateTime.now();
//        if (groups.contains(recipient)) {
//            // Send a group message
//            Message m = new Message(currentUser.getUsername(), recipient, text, timestamp);
//            appendAndRender(m);
//            connection.sendGroupMessage(recipient, text, timestamp);
//        } else {
//            // Send a private message
//            Message m = new Message(currentUser.getUsername(), recipient, text, timestamp);
//            appendAndRender(m);
//            connection.sendPrivateMessage(recipient, text, timestamp);
//        }
//        messageField.clear();
//    }
//
//    private void appendAndRender(Message m) {
//        String friend = m.getOtherParty(currentUser.getUsername());
//        if (!chatHistory.containsKey(friend)) {
//            chatHistory.put(friend, new ArrayList<>());
//        }
//        List<Message> history = chatHistory.get(friend);
//        history.add(m);
//
//        String selectedFriend = friendListView.getSelectionModel().getSelectedItem();
//        if (friend.equals(selectedFriend)) {
//            chatMessagesBox.getChildren().add(makeBubble(m));
//            scrollToBottom();
//        }
//    }
//
//    private HBox makeBubble(Message m) {
//        // Build the message bubble. For group messages we prepend the sender's
//        // name so recipients can identify who wrote the message.
//        boolean isSent = m.getSender().equals(currentUser.getUsername());
//        boolean isGroup = groups.contains(m.getReceiver());
//        String displayText;
//        if (isGroup && !isSent) {
//            displayText = m.getSender() + ": " + m.getText();
//        } else {
//            displayText = m.getText();
//        }
//        Text text = new Text(displayText);
//        Text timestampText = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "] ");
//        timestampText.setStyle("-fx-font-size: 10px; -fx-fill: gray;");
//        TextFlow flow = new TextFlow(timestampText, text);
//        flow.getStyleClass().add(isSent ? "sent" : "received");
//        flow.setTextAlignment(isSent ? TextAlignment.RIGHT : TextAlignment.LEFT);
//        flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") + "; " +
//                "-fx-padding: 8px; -fx-background-radius: 10px;");
//        HBox bubbleBox = new HBox(flow);
//        bubbleBox.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
//        bubbleBox.setPadding(new Insets(4, 8, 4, 8));
//        return bubbleBox;
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
//        System.out.println("UI received: from=" + from + ", body=" + body);
//        Platform.runLater(() -> handleIncomingMessage(from, body));
//    }
//
//    private void handleIncomingMessage(String from, String body) {
//        // Handle group and private chat messages as well as chat history
//        try {
//            if (body.startsWith("CHAT_HISTORY|")) {
//                // Private chat history payload
//                String[] parts = body.split("\\|", 4);
//                if (parts.length == 4) {
//                    String friend = parts[1];
//                    String user = parts[2];
//                    String payload = parts[3];
//                    if (!user.equals(currentUser.getUsername())) {
//                        return;
//                    }
//                    List<Message> history = parseChatHistory(payload, friend, user);
//                    chatHistory.put(friend, history);
//                    // Only refresh if this chat is currently visible
//                    String selectedFriend = friendListView.getSelectionModel().getSelectedItem();
//                    if (friend.equals(selectedFriend)) {
//                        loadChat(friend);
//                    }
//                }
//                return;
//            }
//            if (body.startsWith("CHAT_HISTORY_GROUP|")) {
//                // Group chat history payload: CHAT_HISTORY_GROUP|groupName|user|payload
//                String[] parts = body.split("\\|", 4);
//                if (parts.length == 4) {
//                    String groupName = parts[1];
//                    String user = parts[2];
//                    String payload = parts[3];
//                    if (!user.equals(currentUser.getUsername())) {
//                        return;
//                    }
//                    // parse payload lines as sender|timestamp|text
//                    List<Message> history = new ArrayList<>();
//                    if (!payload.isEmpty()) {
//                        String[] entries = payload.split(";;;", -1);
//                        for (String entry : entries) {
//                            String[] msgParts = entry.split("\\|", 3);
//                            if (msgParts.length == 3) {
//                                String sender = msgParts[0];
//                                String timestampStr = msgParts[1];
//                                String text = msgParts[2];
//                                LocalDateTime ts = LocalDateTime.parse(timestampStr);
//                                Message m = new Message(sender, groupName, text, ts);
//                                history.add(m);
//                            }
//                        }
//                    }
//                    groups.add(groupName);
//                    chatHistory.put(groupName, history);
//                    populateFriendList();
//                    String selected = friendListView.getSelectionModel().getSelectedItem();
//                    if (groupName.equals(selected)) {
//                        loadChat(groupName);
//                    }
//                }
//                return;
//            }
//            if (body.startsWith("GROUP_LIST|")) {
//                // Server response listing groups: GROUP_LIST|group1,group2,...
//                String[] parts = body.split("\\|", 2);
//                if (parts.length == 2) {
//                    String csv = parts[1];
//                    if (!csv.isEmpty()) {
//                        String[] gs = csv.split(",");
//                        for (String g : gs) {
//                            if (!g.isBlank()) groups.add(g.trim());
//                        }
//                    }
//                    populateFriendList();
//                }
//                return;
//            }
//            if (body.startsWith("GROUP_CREATED|")) {
//                // Notification that a group was created: GROUP_CREATED|groupName|creator|members
//                String[] parts = body.split("\\|", 4);
//                if (parts.length >= 3) {
//                    String groupName = parts[1];
//                    String creator = parts[2];
//                    String members = parts.length >= 4 ? parts[3] : "";
//                    // Only add if the current user is in the member list or the creator
//                    boolean include = creator.equals(currentUser.getUsername());
//                    if (!include && members != null && !members.isEmpty()) {
//                        String[] mems = members.split(",");
//                        for (String m : mems) {
//                            if (m.equals(currentUser.getUsername())) {
//                                include = true;
//                                break;
//                            }
//                        }
//                    }
//                    if (include) {
//                        groups.add(groupName);
//                        populateFriendList();
//                    }
//                }
//                return;
//            }
//            if (body.startsWith("GROUP_MSG|")) {
//                // Group message: body=GROUP_MSG|sender|timestamp|message; from parameter contains groupName
//                String[] parts = body.split("\\|", 4);
//                if (parts.length == 4) {
//                    String sender = parts[1];
//                    String timestampStr = parts[2];
//                    String text = parts[3];
//                    LocalDateTime ts = LocalDateTime.parse(timestampStr);
//                    String groupName = from;
//                    Message m = new Message(sender, groupName, text, ts);
//                    appendAndRender(m);
//                }
//                return;
//            }
//            // Fallback: treat as a private message (timestamp|text)
//            String[] bodyParts = body.split("\\|", 2);
//            // Body for private messages is timestamp|message; for offline messages
//            String timestampStr = bodyParts[0];
//            String messageText = bodyParts.length > 1 ? bodyParts[1] : "";
//            LocalDateTime timestamp = LocalDateTime.parse(timestampStr);
//            Message m = new Message(from, currentUser.getUsername(), messageText, timestamp);
//            appendAndRender(m);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private List<Message> parseChatHistory(String payload, String friend, String user) {
//        List<Message> messages = new ArrayList<>();
//        String[] entries = payload.split(";;;");
//        for (String entry : entries) {
//            String[] parts = entry.split("\\|", 3);
//            if (parts.length == 3) {
//                String sender = parts[0];
//                String timestampStr = parts[1];
//                String text = parts[2];
//                LocalDateTime timestamp = LocalDateTime.parse(timestampStr);
//                String receiver = sender.equals(user) ? friend : user;
//                messages.add(new Message(sender, receiver, text, timestamp));
//            }
//        }
//        return messages;
//    }
//
//    public static class Message {
//        private String sender;
//        private String receiver;
//        private String text;
//        private LocalDateTime timestamp;
//
//        public Message(String sender, String receiver, String text, LocalDateTime timestamp) {
//            this.sender = sender;
//            this.receiver = receiver;
//            this.text = text;
//            this.timestamp = timestamp;
//        }
//
//        public String getSender() {
//            return sender;
//        }
//
//        public String getReceiver() {
//            return receiver;
//        }
//
//        public String getText() {
//            return text;
//        }
//
//        public LocalDateTime getTimestamp() {
//            return timestamp;
//        }
//
//        public String getOtherParty(String u) {
//            return u.equals(sender) ? receiver : sender;
//        }
//    }
//
//    public void shutdown() {
//        connection.removeListener(this);
//    }
//}
//
package main.java.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import main.java.client.ClientConnection;
import main.java.user.Session;
import main.java.user.User;
import main.java.util.sceneChange;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for the chat box UI.  Handles sending/receiving both private
 * and group messages.  Group messages are distinguished by the "GROUP_MSG|"
 * prefix in the message body.
 */
public class ChatBoxController implements Initializable, ClientConnection.MessageListener {

    @FXML private ListView<String> friendListView;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesBox;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button backButton;
    @FXML private Button createGroupButton;

    private User currentUser;
    private ClientConnection connection;
    private final Map<String, List<Message>> chatHistory = new HashMap<>();
    private final Set<String> groups = new HashSet<>();
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
    }

    private void openGroupCreationDialog() {
        if (currentUser.getFriends() == null || currentUser.getFriends().isEmpty()) {
            return;
        }
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle("Create Group");
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10);
        root.setPadding(new Insets(10));
        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label("Group Name:");
        javafx.scene.control.TextField nameField = new javafx.scene.control.TextField();
        nameField.setPromptText("Enter group name");
        javafx.scene.control.Label membersLabel = new javafx.scene.control.Label("Select members:");
        javafx.scene.layout.VBox checkContainer = new javafx.scene.layout.VBox(5);
        for (String f : currentUser.getFriends()) {
            javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(f);
            checkContainer.getChildren().add(cb);
        }
        javafx.scene.control.Button createBtn = new javafx.scene.control.Button("Create");
        javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("Cancel");
        javafx.scene.layout.HBox buttonBar = new javafx.scene.layout.HBox(10, createBtn, cancelBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().addAll(nameLabel, nameField, membersLabel, checkContainer, buttonBar);
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        dialog.setScene(scene);
        createBtn.setOnAction(ev -> {
            String groupName = nameField.getText().trim();
            if (groupName.isEmpty()) return;
            List<String> selected = new ArrayList<>();
            for (javafx.scene.Node n : checkContainer.getChildren()) {
                if (n instanceof javafx.scene.control.CheckBox cb && cb.isSelected()) {
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

    private void appendAndRender(Message m) {
        String conversation = m.getReceiver();
        chatHistory.computeIfAbsent(conversation, k -> new ArrayList<>()).add(m);
        String selected = friendListView.getSelectionModel().getSelectedItem();
        if (conversation.equals(selected)) {
            chatMessagesBox.getChildren().add(makeBubble(m));
            scrollToBottom();
        }
    }

    private HBox makeBubble(Message m) {
        boolean isSent = m.getSender().equals(currentUser.getUsername());
        boolean isGroup = groups.contains(m.getReceiver());
        String displayText = isGroup && !isSent ? m.getSender() + ": " + m.getText() : m.getText();
        Text textNode = new Text(displayText);
        Text timestampNode = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "] ");
        timestampNode.setStyle("-fx-font-size: 10px; -fx-fill: gray;");
        TextFlow flow = new TextFlow(timestampNode, textNode);
        flow.getStyleClass().add(isSent ? "sent" : "received");
        flow.setTextAlignment(isSent ? TextAlignment.RIGHT : TextAlignment.LEFT);
        flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") +
                "; -fx-padding: 8px; -fx-background-radius: 10px;");
        HBox box = new HBox(flow);
        box.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        box.setPadding(new Insets(4, 8, 4, 8));
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
                // Private chat history
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
                // Group chat history
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
                    }
                }
                return;
            }
            if (body.startsWith("GROUP_MSG|")) {
                // Live or offline group message
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
            // Fallback: private or unrecognized message.  Only parse if first field looks like a timestamp.
            String[] bodyParts = body.split("\\|", 2);
            String tsStr = bodyParts[0];
            String msgText = bodyParts.length > 1 ? bodyParts[1] : "";
            if (tsStr.matches("\\d{4}-\\d{2}-\\d{2}T.*")) {
                LocalDateTime ts = LocalDateTime.parse(tsStr);
                Message m = new Message(from, currentUser.getUsername(), msgText, ts);
                appendAndRender(m);
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
                String receiver = sender.equals(user) ? friend : user;
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
