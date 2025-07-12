////package main.java.controller;
////
////import java.net.URL;
////import java.util.*;
////import javafx.fxml.FXML;
////import javafx.fxml.Initializable;
////import javafx.scene.control.*;
////import javafx.scene.layout.VBox;
////import javafx.scene.text.Text;
////import javafx.scene.text.TextFlow;
////import main.java.user.User;
////import main.java.user.Session;
////import main.java.client.ClientConnection;
////
////public class ChatBoxController implements Initializable {
////    @FXML private ListView<String> friendListView;
////    @FXML private VBox chatVBox;
////    @FXML private TextField messageField;
////    @FXML private Button sendButton;
////    @FXML private ScrollPane chatScrollPane;
////
////    private User currentUser;
////    private ClientConnection connection;
////
////    // Maintain chat history locally; in practice you'd load from server or file
////    private Map<String, List<Message>> chatHistory = new HashMap<>();
////
////    @Override
////    public void initialize(URL location, ResourceBundle resources) {
////        // Obtain currentUser and connection (e.g., via Session)
////        currentUser = Session.getCurrentUser();
////        connection = ClientConnection.getInstance();
////
////        // Populate friends list
////        List<String> friends = new ArrayList<>(currentUser.getFriends());
////        friendListView.getItems().setAll(friends);
////
////        // When user selects a friend, load their chat
////        friendListView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
////            loadChat(selected);
////        });
////    }
////
////    private void loadChat(String friendUsername) {
////        chatVBox.getChildren().clear();
////        List<Message> history = chatHistory.getOrDefault(friendUsername, Collections.emptyList());
////        for (Message msg : history) {
////            TextFlow bubble = createBubble(msg);
////            chatVBox.getChildren().add(bubble);
////        }
////        // Scroll to bottom
////        chatScrollPane.layout();
////        chatScrollPane.setVvalue(1.0);
////    }
////
////    @FXML
////    private void onSendClicked() {
////        String text = messageField.getText().trim();
////        String friend = friendListView.getSelectionModel().getSelectedItem();
////        if (text.isEmpty() || friend == null) return;
////
////        Message msg = new Message(currentUser.getUsername(), text, new Date());
////        chatHistory.computeIfAbsent(friend, k -> new ArrayList<>()).add(msg);
////        chatVBox.getChildren().add(createBubble(msg));
////        chatScrollPane.layout();
////        chatScrollPane.setVvalue(1.0);
////
////        // send through network
////        connection.sendPrivateMessage( friend, text);
////        messageField.clear();
////    }
////
////    private TextFlow createBubble(Message msg) {
////        Text content = new Text(msg.getContent());
////        TextFlow tf = new TextFlow(content);
////        tf.getStyleClass().add(msg.getSender().equals(currentUser.getUsername()) ? "sent" : "received");
////        return tf;
////    }
////
////    // Receive messages from network (e.g., via listener)
////    public void onMessageReceived(String fromUser, String text) {
////        Message msg = new Message(fromUser, text, new Date());
////        chatHistory.computeIfAbsent(fromUser, k -> new ArrayList<>()).add(msg);
////        String selected = friendListView.getSelectionModel().getSelectedItem();
////        if (fromUser.equals(selected)) {
////            chatVBox.getChildren().add(createBubble(msg));
////            chatScrollPane.layout();
////            chatScrollPane.setVvalue(1.0);
////        }
////    }
////
////    // Simple message model
////    private static class Message {
////        private String sender;
////        private String content;
////        private Date timestamp;
////
////        public Message(String sender, String content, Date timestamp) {
////            this.sender = sender;
////            this.content = content;
////            this.timestamp = timestamp;
////        }
////        public String getSender() { return sender; }
////        public String getContent() { return content; }
////        public Date getTimestamp() { return timestamp; }
////    }
////}
//
//
//package main.java.controller;
//
//import javafx.application.Platform;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.scene.control.*;
//import javafx.scene.layout.VBox;
//import javafx.scene.text.Text;
//import javafx.scene.text.TextFlow;
//import main.java.client.ClientConnection;
//import main.java.user.Session;
//import main.java.user.User;
//
//import java.net.URL;
//import java.util.*;
//
///**
// * Chat window: shows friend list on the left and message thread on the right.
// */
//public class ChatBoxController implements Initializable, ClientConnection.MessageListener {
//
//    // FXML hooks
//    @FXML private ListView<String> friendListView;
//    @FXML private ScrollPane chatScrollPane;
//    @FXML private VBox chatVBox;
//    @FXML private TextField messageField;
//    @FXML private Button sendButton;
//
//    // Runtime
//    private User currentUser;
//    private ClientConnection connection;
//    private final Map<String, List<Message>> chatHistory = new HashMap<>();
//
//    @Override public void initialize(URL url, ResourceBundle rb) {
//        currentUser = Session.getCurrentUser();
//        connection   = ClientConnection.getInstance();
//        connection.registerListener(this);
//
//        // populate friend list
//        friendListView.getItems().setAll(new ArrayList<>(currentUser.getFriends()));
//        friendListView.getSelectionModel().selectedItemProperty()
//                .addListener((o, oldV, newV) -> loadChat(newV));
//
//        sendButton.setOnAction(e -> sendCurrentMessage());
//        messageField.setOnAction(e -> sendCurrentMessage());
//    }
//
//    /* ---------- UI helpers ---------- */
//    private void loadChat(String friend) {
//        chatVBox.getChildren().clear();
//        if (friend == null) return;
//        chatHistory.computeIfAbsent(friend, f -> new ArrayList<>())
//                .forEach(m -> chatVBox.getChildren().add(makeBubble(m)));
//        scrollToBottom();
//    }
//
//    private void sendCurrentMessage() {
//        String friend = friendListView.getSelectionModel().getSelectedItem();
//        String text   = messageField.getText().trim();
//        if (friend == null || text.isEmpty()) return;
//
//        Message m = new Message(currentUser.getUsername(), friend, text);
//        appendAndRender(m);
//        connection.sendPrivateMessage(friend, text);
//        messageField.clear();
//    }
//
//    private void appendAndRender(Message m) {
//        String friend = m.getOtherParty(currentUser.getUsername());
//        chatHistory.computeIfAbsent(friend, f -> new ArrayList<>()).add(m);
//        if (friend.equals(friendListView.getSelectionModel().getSelectedItem())) {
//            chatVBox.getChildren().add(makeBubble(m));
//            scrollToBottom();
//        }
//    }
//
//    private TextFlow makeBubble(Message m) {
//        TextFlow flow = new TextFlow(new Text(m.text));
//        flow.getStyleClass().add(m.isSentBy(currentUser.getUsername()) ? "sent" : "received");
//        return flow;
//    }
//
//    private void scrollToBottom() {
//        Platform.runLater(() -> {
//            chatScrollPane.layout();
//            chatScrollPane.setVvalue(1.0);
//        });
//    }
//
//    /* ---------- ClientConnection.MessageListener ---------- */
//    @Override public void onMessageReceived(String from, String body) {
//        Platform.runLater(() -> {
//            Message m = new Message(from, currentUser.getUsername(), body);
//            appendAndRender(m);
//        });
//    }
//
//    /* ---------- tiny message model ---------- */
//    private static class Message {
//        final String sender, receiver, text;
//        Message(String s, String r, String t) { sender=s; receiver=r; text=t; }
//        boolean isSentBy(String u){ return sender.equals(u); }
//        String getOtherParty(String u){ return u.equals(sender)?receiver:sender; }
//    }
//
//    /* ---------- cleanup ---------- */
//    public void shutdown(){ connection.removeListener(this); }
//}



//
//
////CAN CHAT ONLY WITH FRIENDS IN THIS VERSION
//package main.java.controller;
//
//import javafx.application.Platform;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.scene.control.*;
//import javafx.scene.layout.VBox;
//import javafx.scene.text.Text;
//import javafx.scene.text.TextFlow;
//import main.java.client.ClientConnection;
//import main.java.user.Session;
//import main.java.user.User;
//
//import java.net.URL;
//import java.util.*;
//
///**
// * Chat window: shows friend list on the left and message thread on the right.
// */
//public class ChatBoxController implements Initializable, ClientConnection.MessageListener {
//
//    // ---------- FXML ----------
//    @FXML private ListView<String> friendListView;
//    @FXML private ScrollPane       chatScrollPane;
//    @FXML private VBox             chatVBox;
//    @FXML private TextField        messageField;
//    @FXML private Button           sendButton;
//
//    // ---------- Runtime ----------
//    private User currentUser;
//    private ClientConnection connection;
//    private final Map<String, List<Message>> chatHistory = new HashMap<>();
//
//    @Override
//    public void initialize(URL url, ResourceBundle rb) {
//        currentUser = Session.getCurrentUser();
//        connection  = ClientConnection.getInstance();
//        connection.registerListener(this);
//
//        // populate friend list
//        friendListView.getItems().setAll(new ArrayList<>(currentUser.getFriends()));
//        friendListView.getSelectionModel().selectedItemProperty()
//                .addListener((o, oldV, newV) -> loadChat(newV));
//
//        sendButton.setOnAction(e -> sendCurrentMessage());
//        messageField.setOnAction(e -> sendCurrentMessage());
//    }
//
//    /* ---------- UI helpers ---------- */
//    private void loadChat(String friend) {
//        chatVBox.getChildren().clear();
//        if (friend == null) return;
//        chatHistory.computeIfAbsent(friend, f -> new ArrayList<>())
//                .forEach(m -> chatVBox.getChildren().add(makeBubble(m)));
//        scrollToBottom();
//    }
//
//    private void sendCurrentMessage() {
//        String friend = friendListView.getSelectionModel().getSelectedItem();
//        String text   = messageField.getText().trim();
//        if (friend == null || text.isEmpty()) return;
//
//        Message m = new Message(currentUser.getUsername(), friend, text);
//        appendAndRender(m);
//        connection.sendPrivateMessage(friend, text);
//        messageField.clear();
//    }
//
//    private void appendAndRender(Message m) {
//        String friend = m.getOtherParty(currentUser.getUsername());
//        chatHistory.computeIfAbsent(friend, f -> new ArrayList<>()).add(m);
//        if (friend.equals(friendListView.getSelectionModel().getSelectedItem())) {
//            chatVBox.getChildren().add(makeBubble(m));
//            scrollToBottom();
//        }
//    }
//
//    private TextFlow makeBubble(Message m) {
//        TextFlow flow = new TextFlow(new Text(m.text));
//        flow.getStyleClass().add(m.isSentBy(currentUser.getUsername()) ? "sent" : "received");
//        return flow;
//    }
//
//    private void scrollToBottom() {
//        Platform.runLater(() -> {
//            chatScrollPane.layout();
//            chatScrollPane.setVvalue(1.0);
//        });
//    }
//
//    /* ---------- ClientConnection.MessageListener ---------- */
//    @Override
//    public void onMessageReceived(String from, String body) {
//        Platform.runLater(() -> {
//            Message m = new Message(from, currentUser.getUsername(), body);
//            appendAndRender(m);
//        });
//    }
//
//    /* ---------- tiny message model ---------- */
//    private static class Message {
//        final String sender, receiver, text;
//        Message(String s, String r, String t) { sender=s; receiver=r; text=t; }
//        boolean isSentBy(String u){ return sender.equals(u); }
//        String getOtherParty(String u){ return u.equals(sender)?receiver:sender; }
//    }
//
//    /* ---------- cleanup ---------- */
//    public void shutdown(){ connection.removeListener(this); }
//}


// CAN CHAT WITH EVERYONE IN THIS VERSION

package main.java.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import main.java.client.ClientConnection;
import main.java.user.Session;
import main.java.user.User;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Chat window where **every registered user is treated as a friend** – so the
 * list shows *all* usernames except the current user. No need to add friends
 * manually; new sign‑ups appear automatically the next time the window opens.
 */
public class ChatBoxController implements Initializable, ClientConnection.MessageListener {

    /* ------------------------------------------------------------- FXML */
    @FXML private ListView<String> friendListView;
    @FXML private ScrollPane       chatScrollPane;
    @FXML private VBox             chatVBox;
    @FXML private TextField        messageField;
    @FXML private Button           sendButton;

    /* --------------------------------------------------------- Runtime */
    private User currentUser;
    private ClientConnection connection;
    private final Map<String, List<Message>> chatHistory = new HashMap<>();

    @Override public void initialize(URL url, ResourceBundle rb) {
        currentUser = Session.getCurrentUser();
        connection  = ClientConnection.getInstance();
        connection.registerListener(this);

        populateFriendList();
        // auto‑select first friend for immediate chat
        if (!friendListView.getItems().isEmpty()) {
            friendListView.getSelectionModel().selectFirst();
            loadChat(friendListView.getSelectionModel().getSelectedItem());
        }

        // list selection listener
        friendListView.getSelectionModel().selectedItemProperty()
                .addListener((o, oldV, newV) -> loadChat(newV));

        // send handlers
        sendButton.setOnAction(e -> sendCurrentMessage());
        messageField.setOnAction(e -> sendCurrentMessage());

        // Disable send until a user is selected
        sendButton.disableProperty().bind(friendListView.getSelectionModel().selectedItemProperty().isNull());
    }

    /* ---------------------------------------------------- Friend Loader */
    private void populateFriendList() {
        friendListView.setItems(FXCollections.observableArrayList(loadAllUsernamesExceptSelf()));
    }

//    private List<String> loadAllUsernamesExceptSelf() {
//        File usersDir = new File("users");
//        if (!usersDir.exists()) return Collections.emptyList();
//        return Arrays.stream(Objects.requireNonNull(usersDir.listFiles((d, n) -> n.endsWith(".ser"))))
//                .map(f -> f.getName().replace(".ser", ""))
//                .filter(name -> !name.equals(currentUser.getUsername()))
//                .sorted(String::compareToIgnoreCase)
//                .collect(Collectors.toList());
//    }
private List<String> loadAllUsernamesExceptSelf() {
    List<String> names = new ArrayList<>();                 // final list we’ll return

    File usersDir = new File("users");                      // 1. locate folder
    if (!usersDir.exists()) {                               // 2. guard-clause if it’s missing
        return names;                                       //    (empty list)
    }

    File[] files = usersDir.listFiles();                    // 3. grab *all* files
    if (files == null) {                                    //    rare null if I/O error
        return names;
    }

    for (File f : files) {                                  // 4. loop through each entry
        String fileName = f.getName();                      //    e.g.  alice.ser
        if (!fileName.endsWith(".ser")) {                   // 4a. skip anything not .ser
            continue;
        }

        String username = fileName.substring(               // 4b. strip the extension
                0, fileName.length() - ".ser".length());

        if (username.equals(currentUser.getUsername())) {   // 4c. skip yourself
            continue;
        }

        names.add(username);                                // 4d. keep it
    }

    // 5. sort alphabetically, case-insensitive
    names.sort(String::compareToIgnoreCase);

    return names;                                           // 6. done!
}


    /* ----------------------------------------------------- UI helpers */
//    private void loadChat(String friend) {
//        chatVBox.getChildren().clear(); // clear na korle ager box er chats theke jabe when i switch to another friend
//        if (friend == null) return;
//        chatHistory.computeIfAbsent(friend, f -> new ArrayList<>())
//                .forEach(m -> chatVBox.getChildren().add(makeBubble(m)));
//        scrollToBottom();
//    }
    private void loadChat(String friend) {
        // 1. Clear any bubbles from the previous conversation
        chatVBox.getChildren().clear();

        // 2. If no friend is selected, just stop
        if (friend == null) {
            return;
        }

        // 3. Look up this friend’s history list (or create it the first time)
        List<Message> history = chatHistory.get(friend);
        if (history == null) {
            history = new ArrayList<>();
            chatHistory.put(friend, history);
        }

        // 4. Add each saved Message to the VBox as a bubble
        for (Message m : history) {
            TextFlow bubble = makeBubble(m);   // convert Message → UI node
            chatVBox.getChildren().add(bubble);
        }

        // 5. Scroll to the latest message
        scrollToBottom();
    }


    private void sendCurrentMessage() {
        String friend = friendListView.getSelectionModel().getSelectedItem();
        String text   = messageField.getText().trim();
        if (friend == null || text.isEmpty()) return;

        Message m = new Message(currentUser.getUsername(), friend, text);
        appendAndRender(m);
        connection.sendPrivateMessage(friend, text);
        messageField.clear();
    }

    private void appendAndRender(Message m) {
        String friend = m.getOtherParty(currentUser.getUsername());
        chatHistory.computeIfAbsent(friend, f -> new ArrayList<>()).add(m);
        if (friend.equals(friendListView.getSelectionModel().getSelectedItem())) {
            chatVBox.getChildren().add(makeBubble(m));
            scrollToBottom();
        }
    }

    private TextFlow makeBubble(Message m) {
        TextFlow flow = new TextFlow(new Text(m.text));
        flow.getStyleClass().add(m.isSentBy(currentUser.getUsername()) ? "sent" : "received");
        return flow;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatScrollPane.layout();
            chatScrollPane.setVvalue(1.0);
        });
    }

    /* ------------------------- ClientConnection.MessageListener */
    @Override public void onMessageReceived(String from, String body) {
        Platform.runLater(() -> {
            Message m = new Message(from, currentUser.getUsername(), body);
            appendAndRender(m);
        });
    }

    /* ------------------------------------------------ Message model */
    private static class Message {
        final String sender, receiver, text;
        Message(String s, String r, String t) { sender=s; receiver=r; text=t; }
        boolean isSentBy(String u){ return sender.equals(u); }
        String getOtherParty(String u){ return u.equals(sender)?receiver:sender; }
    }

    /* ------------------------------------------------ cleanup */
    public void shutdown(){ connection.removeListener(this); }
}
