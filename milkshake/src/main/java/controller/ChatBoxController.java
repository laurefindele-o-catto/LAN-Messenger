//
//////original
//package main.java.controller;
//
//import javafx.application.Platform;
//import javafx.collections.FXCollections;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.control.*;
//import javafx.scene.image.ImageView;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//import javafx.scene.text.Text;
//import javafx.scene.text.TextAlignment;
//import javafx.scene.text.TextFlow;
//import main.java.client.ClientConnection;
//import main.java.user.Session;
//import main.java.user.User;
//import main.java.util.sceneChange;
//
//import java.io.*;
//import java.net.URL;
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Chat window where **every registered user is treated as a friend** – so the
// * list shows *all* usernames except the current user. No need to add friends
// * manually; new sign‑ups appear automatically the next time the window opens.
// */
//public class ChatBoxController implements Initializable, ClientConnection.MessageListener {
//
//    /* ------------------------------------------------------------- FXML */
//    @FXML private ListView<String> friendListView;
//    @FXML private VBox chatMessagesBox;
//    @FXML private ScrollPane chatScrollPane;
//    @FXML private TextField messageField;
//    @FXML private Button sendButton;
//    @FXML private ImageView chatUserPhoto;
//    @FXML private Label chatUserLabel;
//    @FXML private VBox activeUsersBox;
//    @FXML private Button backButton;
//
//    /* --------------------------------------------------------- Runtime */
//    private User currentUser;
//    private ClientConnection connection;
//    private final Map<String, List<Message>> chatHistory = new HashMap<>();
//
//    @Override public void initialize(URL url, ResourceBundle rb) {
//        currentUser = Session.getCurrentUser();
//        connection  = ClientConnection.getInstance();
//        connection.registerListener(this);
//
//        populateFriendList();
//        loadStoredChats();
//        updateActiveUsers();
//
//        // auto‑select first friend for immediate chat
//        if (!friendListView.getItems().isEmpty()) {
//            friendListView.getSelectionModel().selectFirst();
//            loadChat(friendListView.getSelectionModel().getSelectedItem());
//        }
//
//        // list selection listener
//        friendListView.getSelectionModel().selectedItemProperty()
//                .addListener((o, oldV, newV) -> loadChat(newV));
//
//        // send handlers
//        sendButton.setOnAction(e -> sendCurrentMessage());
//        messageField.setOnAction(e -> sendCurrentMessage());
//
//        // Disable send until a user is selected
//        sendButton.disableProperty().bind(friendListView.getSelectionModel().selectedItemProperty().isNull());
//
//        backButton.setOnAction(e -> {
//            shutdown(); // remove listener before scene changes
//            sceneChange.changeScene("Dashboard.fxml", backButton, currentUser);
//        });
//    }
//
//    /* ---------------------------------------------------- Friend Loader */
//    private void populateFriendList() {
//        File dir = new File("users");
//        if (!dir.exists()) return;
//
//        List<String> names = Arrays.stream(Objects.requireNonNull(dir.listFiles((d, n) -> n.endsWith(".ser"))))
//                .map(f -> f.getName().replace(".ser", ""))
//                .filter(name -> !name.equals(currentUser.getUsername()))
//                .sorted(String::compareToIgnoreCase)
//                .collect(Collectors.toList());
//
//        friendListView.setItems(FXCollections.observableArrayList(names));
//    }
//
//    private void updateActiveUsers(){
//        List<String> active = new ArrayList<>();
//        activeUsersBox.getChildren().clear();
//        for (String name : active) {
//            Label userLabel = new Label(name);
//            userLabel.setStyle("-fx-text-fill: white; -fx-background-color: #3a3a3a; -fx-padding: 5px; -fx-background-radius: 5px;");
//            userLabel.setMaxWidth(Double.MAX_VALUE);
//            activeUsersBox.getChildren().add(userLabel);
//        }
//    }
//
//    private List<String> loadAllUsernamesExceptSelf() {
//    List<String> names = new ArrayList<>();                 // final list we’ll return
//
//    File usersDir = new File("users");                      // 1. locate folder
//    if (!usersDir.exists()) {                               // 2. guard-clause if it’s missing
//        return names;                                       //    (empty list)
//    }
//
//    File[] files = usersDir.listFiles();                    // 3. grab *all* files
//    if (files == null) {                                    //    rare null if I/O error
//        return names;
//    }
//
//    for (File f : files) {                                  // 4. loop through each entry
//        String fileName = f.getName();                      //    e.g.  alice.ser
//        if (!fileName.endsWith(".ser")) {                   // 4a. skip anything not .ser
//            continue;
//        }
//
//        String username = fileName.substring(               // 4b. strip the extension
//                0, fileName.length() - ".ser".length());
//
//        if (username.equals(currentUser.getUsername())) {   // 4c. skip yourself
//            continue;
//        }
//
//        names.add(username);                                // 4d. keep it
//    }
//
//    // 5. sort alphabetically, case-insensitive
//    names.sort(String::compareToIgnoreCase);
//
//    return names;                                           // 6. done!
//}
//
//
//    /* ----------------------------------------------------- UI helpers */
//
//    private void loadStoredChats(){
//        File file = new File("chats/" + currentUser.getUsername() + "_chats.ser");
//        if(!file.exists())
//            return;
//        try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))){
//            Object obj = in.readObject();
//            if(obj instanceof Map<?, ?>m){
//                for(Object k: m.keySet()){
//                    if(k instanceof String user && m.get(k) instanceof List<?> l){
//                        List<Message> msgList = new ArrayList<>();
//                        for(Object o:l){
//                            if(o instanceof Message msg){
//                                msgList.add(msg);
//                            }
//                        }
//                        chatHistory.put(user, msgList);
//                    }
//                }
//            }
//        } catch (IOException | ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void saveChatsToDisk() {
//        File dir = new File("chats");
//        if (!dir.exists()) dir.mkdirs();
//        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("chats/" + currentUser.getUsername() + "_chats.ser"))) {
//            out.writeObject(chatHistory);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void loadChat(String friend) {
//        // 1. Clear any bubbles from the previous conversation
//        chatMessagesBox.getChildren().clear();
//
//        // 2. If no friend is selected, just stop
//        if (friend == null) {
//            return;
//        }
//
//        chatUserLabel.setText(friend);
//
//        // 3. Look up this friend’s history list (or create it the first time)
//        List<Message> history = chatHistory.get(friend);
//
//        if (history == null) {
//            history = new ArrayList<>();
//            chatHistory.put(friend, history);
//        }
//
//        //List<Message> messages = chatHistory.getOrDefault(friend, new ArrayList<>())
//
//        // 4. Add each saved Message to the VBox as a bubble
//        for (Message m : history) {
//            HBox bubble = makeBubble(m);   // convert Message → UI node
//            chatMessagesBox.getChildren().add(bubble);
//        }
//
//        // 5. Scroll to the latest message
//        scrollToBottom();
//    }
//
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
//        saveChatsToDisk();
//    }
//
//    private void appendAndRender(Message m) {
//        String friend = m.getOtherParty(currentUser.getUsername());
//        chatHistory.computeIfAbsent(friend, f -> new ArrayList<>()).add(m);
//        if (friend.equals(friendListView.getSelectionModel().getSelectedItem())) {
//            chatMessagesBox.getChildren().add(makeBubble(m));
//            scrollToBottom();
//        }
//    }
//
//    private HBox makeBubble(Message m) {
//        Text text = new Text(m.text);
//        TextFlow flow = new TextFlow(text);
//        flow.getStyleClass().add(m.isSentBy(currentUser.getUsername()) ? "sent" : "received");
//        flow.setTextAlignment(m.isSentBy(currentUser.getUsername()) ? TextAlignment.RIGHT : TextAlignment.LEFT);
//        flow.setStyle("-fx-background-color: " + (m.isSentBy(currentUser.getUsername()) ? "#DCF8C6" : "#FFFFFF") + "; " +
//                "-fx-padding: 8px; -fx-background-radius: 10px;");
//
//        HBox bubbleBox = new HBox(flow);
//        bubbleBox.setAlignment(m.isSentBy(currentUser.getUsername()) ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
//        bubbleBox.setPadding(new Insets(4, 8, 4, 8));
//
//        return bubbleBox;
//    }
//
//
//
//    private void scrollToBottom() {
//        Platform.runLater(() -> {
//            chatScrollPane.layout();
//            chatScrollPane.setVvalue(1.0);
//        });
//    }
//
//    /* ------------------------- ClientConnection.MessageListener */
//    @Override
//    public void onMessageReceived(String from, String body) {
//        System.out.println("UI received message from: " + from + " | " + body);
//
//        Platform.runLater(() -> {
//            if (body.startsWith("CHAT_HISTORY|")) {
//                String[] parts = body.split("\\|", 4);
//                if (parts.length == 4) {
//                    String friend = parts[1];
//                    String user = parts[2];
//                    String allMsgs = parts[3];
//
////                    if (!user.equals(currentUser.getUsername())) {
////                        System.out.println("⚠ Ignored history not meant for me: " + user + " vs " + currentUser.getUsername());
////                        return;
////                    }
//
//                    List<Message> history = chatHistory.computeIfAbsent(friend, f -> new ArrayList<>());
//
//                    String[] messages = allMsgs.split(";;;");
//                    for (String entry : messages) {
//                        int sep = entry.indexOf(":");
//                        if (sep > 0) {
//                            String sender = entry.substring(0, sep);
//                            String msg = entry.substring(sep + 1);
//
//                            if (!messageExists(history, sender, msg)) {
//                                history.add(new Message(sender, friend, msg));
//                            }
//                        }
//                    }
//
//                    if (friend.equals(friendListView.getSelectionModel().getSelectedItem())) {
//                        loadChat(friend);
//                    }
//
//                    saveChatsToDisk();
//                }
//            }
//
//            else if (body.startsWith("OFFLINE_MSG|")) {
//                String[] parts = body.split("\\|", 4);
//                if (parts.length == 4) {
//                    String sender = parts[1];
//                    String user = parts[2];
//                    String msgLine = parts[3];
//                    //Offline_msg|1|3|hi three
//
////                    if (!user.equals(currentUser.getUsername())) {
////                        System.out.println("⚠ Ignored history not meant for me: " + user + " vs " + currentUser.getUsername());
////                        return;
////                    }
//
//                    int sep = msgLine.indexOf(":");
//                    if (sep > 0) {
//                        String msgText = msgLine.substring(sep + 1);
//                        List<Message> history = chatHistory.computeIfAbsent(sender, f -> new ArrayList<>());
//
//                        if (!messageExists(history, sender, msgText)) {
//                            Message m = new Message(sender, user, msgText);
//                            history.add(m);
//
//                            if (sender.equals(friendListView.getSelectionModel().getSelectedItem())) {
//                                chatMessagesBox.getChildren().add(makeBubble(m));
//                                scrollToBottom();
//                            }
//
//                            saveChatsToDisk();
//                        }
//                    }
//                }
//            }
//
//            else {
//                Message m = new Message(from, currentUser.getUsername(), body);
//                List<Message> history = chatHistory.computeIfAbsent(from, f -> new ArrayList<>());
//
//                if (!messageExists(history, from, body)) {
//                    history.add(m);
//
//                    if (from.equals(friendListView.getSelectionModel().getSelectedItem())) {
//                        chatMessagesBox.getChildren().add(makeBubble(m));
//                        scrollToBottom();
//                    }
//
//                    saveChatsToDisk();
//                }
//            }
//        });
//    }
//
//
//
//
//
//    public void addIncomingMessage(String from, String message) {
//        Label msgLabel = new Label(from + ": " + message);
//        msgLabel.setStyle("-fx-text-fill: white; -fx-background-color: #2b2b2b; -fx-padding: 5;");
//        chatMessagesBox.getChildren().add(msgLabel);
//        chatScrollPane.setVvalue(1.0); // scroll to bottom
//    }
//
//
//    /* ------------------------------------------------ Message model */
//    public static class Message implements Serializable {
//        final String sender, receiver, text;
//        public Message(String s, String r, String t) { sender=s; receiver=r; text=t; }
//        boolean isSentBy(String u){ return sender.equals(u); }
//        String getOtherParty(String u){ return u.equals(sender)?receiver:sender; }
//    }
//
//    private boolean messageExists(List<Message> list, String sender, String text) {
//        return list.stream().anyMatch(m -> m.sender.equals(sender) && m.text.equals(text));
//    }
//
//    /* ------------------------------------------------ cleanup */
//    public void shutdown(){ connection.removeListener(this); }
//}
//-------XXXXXXXXXXXXXXXXXX---------111111111111111111111111111111
//package main.java.controller;
//
//import javafx.application.Platform;
//import javafx.collections.FXCollections;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.control.*;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//import javafx.scene.text.Text;
//import javafx.scene.text.TextAlignment;
//import javafx.scene.text.TextFlow;
//import main.java.client.ClientConnection;
//import main.java.user.Session;
//import main.java.user.User;
//
//import java.io.File;
//import java.net.URL;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class ChatBoxController implements Initializable, ClientConnection.MessageListener {
//
//    @FXML private ListView<String> friendListView;
//    @FXML private ScrollPane chatScrollPane;
//    @FXML private VBox chatMessagesBox;
//    @FXML private TextField messageField;
//    @FXML private Button sendButton;
//
//    private User currentUser;
//    private ClientConnection connection;
//    private final Map<String, List<Message>> chatHistory = new HashMap<>();
//
//    @Override
//    public void initialize(URL location, ResourceBundle resources) {
//        currentUser = Session.getCurrentUser();
//        connection = ClientConnection.getInstance();
//        connection.registerListener(this);
//
//        populateFriendList();
//
//        friendListView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
//            loadChat(selected);
//        });
//
//        sendButton.setOnAction(e -> sendCurrentMessage());
//        messageField.setOnAction(e -> sendCurrentMessage());
//    }
//
//    private void populateFriendList() {
//        File dir = new File("users");
//        if (!dir.exists()) return;
//        List<String> names = Arrays.stream(dir.listFiles((d, n) -> n.endsWith(".ser")))
//                .map(f -> f.getName().replace(".ser", ""))
//                .filter(name -> !name.equals(currentUser.getUsername()))
//                .sorted(String::compareToIgnoreCase)
//                .collect(Collectors.toList());
//        friendListView.setItems(FXCollections.observableArrayList(names));
//    }
//
//    private void loadChat(String friend) {
//        chatMessagesBox.getChildren().clear();
//        if (friend == null) return;
//        List<Message> history = chatHistory.getOrDefault(friend, Collections.emptyList());
//        for (Message msg : history) {
//            chatMessagesBox.getChildren().add(makeBubble(msg));
//        }
//        scrollToBottom();
//    }
//
//    private void sendCurrentMessage() {
//        String friend = friendListView.getSelectionModel().getSelectedItem();
//        String text = messageField.getText().trim();
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
//            chatMessagesBox.getChildren().add(makeBubble(m));
//            scrollToBottom();
//        }
//    }
//
//    private HBox makeBubble(Message m) {
//        Text text = new Text(m.getText());
//        TextFlow flow = new TextFlow(text);
//        boolean isSent = m.getSender().equals(currentUser.getUsername());
//        flow.getStyleClass().add(isSent ? "sent" : "received");
//        flow.setTextAlignment(isSent ? TextAlignment.RIGHT : TextAlignment.LEFT);
//        flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") + "; " +
//                "-fx-padding: 8px; -fx-background-radius: 10px;");
//
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
//        Platform.runLater(() -> {
//            if (body.startsWith("CHAT_HISTORY|")) {
//                String[] parts = body.split("\\|", 4);
//                if (parts.length == 4) {
//                    String friend = parts[1];
//                    String user = parts[2];
//                    String payload = parts[3];
//                    List<Message> history = parseChatHistory(payload, friend, user);
//                    chatHistory.put(friend, history);
//                    if (friend.equals(friendListView.getSelectionModel().getSelectedItem())) {
//                        loadChat(friend);
//                    }
//                }
//            } else {
//                Message m = new Message(from, currentUser.getUsername(), body);
//                appendAndRender(m);
//            }
//        });
//    }
//
//    private List<Message> parseChatHistory(String payload, String friend, String user) {
//        List<Message> messages = new ArrayList<>();
//        String[] entries = payload.split(";;;");
//        for (String entry : entries) {
//            String[] parts = entry.split(":", 2);
//            if (parts.length == 2) {
//                String sender = parts[0];
//                String text = parts[1];
//                String receiver = sender.equals(friend) ? user : friend;
//                messages.add(new Message(sender, receiver, text));
//            }
//        }
//        return messages;
//    }
//
//    public static class Message {
//        private String sender;
//        private String receiver;
//        private String text;
//
//        public Message(String sender, String receiver, String text) {
//            this.sender = sender;
//            this.receiver = receiver;
//            this.text = text;
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
//        public String getOtherParty(String u) {
//            return u.equals(sender) ? receiver : sender;
//        }
//    }
//
//    public void shutdown() {
//        connection.removeListener(this);
//    }
//}

// xxxxxxxxxxxxxxxxxx-222222222222222222
//
//package main.java.controller;
//
//import javafx.application.Platform;
//import javafx.collections.FXCollections;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.control.*;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//import javafx.scene.text.Text;
//import javafx.scene.text.TextAlignment;
//import javafx.scene.text.TextFlow;
//import main.java.client.ClientConnection;
//import main.java.user.Session;
//import main.java.user.User;
//
//import java.io.File;
//import java.net.URL;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class ChatBoxController implements Initializable, ClientConnection.MessageListener {
//
//    @FXML private ListView<String> friendListView;
//    @FXML private ScrollPane chatScrollPane;
//    @FXML private VBox chatMessagesBox;
//    @FXML private TextField messageField;
//    @FXML private Button sendButton;
//
//    private User currentUser;
//    private ClientConnection connection;
//    private final Map<String, List<Message>> chatHistory = new HashMap<>();
//
//    @Override
//    public void initialize(URL location, ResourceBundle resources) {
//        currentUser = Session.getCurrentUser();
//        connection = ClientConnection.getInstance();
//        connection.registerListener(this);
//
//        populateFriendList();
//
//        friendListView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
//            loadChat(selected);
//        });
//
//        sendButton.setOnAction(e -> sendCurrentMessage());
//        messageField.setOnAction(e -> sendCurrentMessage());
//    }
//
//    private void populateFriendList() {
//        File dir = new File("users");
//        if (!dir.exists()) return;
//        List<String> names = Arrays.stream(dir.listFiles((d, n) -> n.endsWith(".ser")))
//                .map(f -> f.getName().replace(".ser", ""))
//                .filter(name -> !name.equals(currentUser.getUsername()))
//                .sorted(String::compareToIgnoreCase)
//                .collect(Collectors.toList());
//        friendListView.setItems(FXCollections.observableArrayList(names));
//    }
//
//    private void loadChat(String friend) {
//        chatMessagesBox.getChildren().clear();
//        if (friend == null) return;
//        List<Message> history = chatHistory.getOrDefault(friend, Collections.emptyList());
//        for (Message msg : history) {
//            chatMessagesBox.getChildren().add(makeBubble(msg));
//        }
//        scrollToBottom();
//    }
//
//    private void sendCurrentMessage() {
//        String friend = friendListView.getSelectionModel().getSelectedItem();
//        String text = messageField.getText().trim();
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
//            chatMessagesBox.getChildren().add(makeBubble(m));
//            scrollToBottom();
//        }
//    }
//
//    private HBox makeBubble(Message m) {
//        Text text = new Text(m.getText());
//        TextFlow flow = new TextFlow(text);
//        boolean isSent = m.getSender().equals(currentUser.getUsername());
//        flow.getStyleClass().add(isSent ? "sent" : "received");
//        flow.setTextAlignment(isSent ? TextAlignment.RIGHT : TextAlignment.LEFT);
//        flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") + "; " +
//                "-fx-padding: 8px; -fx-background-radius: 10px;");
//
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
//        Platform.runLater(() -> {
//            Message m = new Message(from, currentUser.getUsername(), body);
//            appendAndRender(m);
//        });
//    }
//
//    public static class Message {
//        private String sender;
//        private String receiver;
//        private String text;
//
//        public Message(String sender, String receiver, String text) {
//            this.sender = sender;
//            this.receiver = receiver;
//            this.text = text;
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
//        public String getOtherParty(String u) {
//            return u.equals(sender) ? receiver : sender;
//        }
//    }
//
//    public void shutdown() {
//        connection.removeListener(this);
//    }
//}


//Xxxxxxxxxxxxxxxxxx-3333333333333333333
//
//package main.java.controller;
//
//import javafx.application.Platform;
//import javafx.collections.FXCollections;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.control.*;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//import javafx.scene.text.Text;
//import javafx.scene.text.TextAlignment;
//import javafx.scene.text.TextFlow;
//import main.java.client.ClientConnection;
//import main.java.user.Session;
//import main.java.user.User;
//
//import java.io.File;
//import java.net.URL;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class ChatBoxController implements Initializable, ClientConnection.MessageListener {
//
//    @FXML private ListView<String> friendListView;
//    @FXML private ScrollPane chatScrollPane;
//    @FXML private VBox chatMessagesBox;
//    @FXML private TextField messageField;
//    @FXML private Button sendButton;
//
//    private User currentUser;
//    private ClientConnection connection;
//    private final Map<String, List<Message>> chatHistory = new HashMap<>();
//
//    @Override
//    public void initialize(URL location, ResourceBundle resources) {
//        currentUser = Session.getCurrentUser();
//        connection = ClientConnection.getInstance();
//        connection.registerListener(this);
//
//        populateFriendList();
//
//        friendListView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
//            loadChat(selected);
//        });
//
//        sendButton.setOnAction(e -> sendCurrentMessage());
//        messageField.setOnAction(e -> sendCurrentMessage());
//    }
//
//    private void populateFriendList() {
//        File dir = new File("users");
//        if (!dir.exists()) return;
//        List<String> names = Arrays.stream(dir.listFiles((d, n) -> n.endsWith(".ser")))
//                .map(f -> f.getName().replace(".ser", ""))
//                .filter(name -> !name.equals(currentUser.getUsername()))
//                .sorted(String::compareToIgnoreCase)
//                .collect(Collectors.toList());
//        friendListView.setItems(FXCollections.observableArrayList(names));
//    }
//
//    private void loadChat(String friend) {
//        chatMessagesBox.getChildren().clear();
//        if (friend == null) return;
//        List<Message> history = chatHistory.getOrDefault(friend, Collections.emptyList());
//        for (Message msg : history) {
//            chatMessagesBox.getChildren().add(makeBubble(msg));
//        }
//        scrollToBottom();
//    }
//
//    private void sendCurrentMessage() {
//        String friend = friendListView.getSelectionModel().getSelectedItem();
//        String text = messageField.getText().trim();
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
//            chatMessagesBox.getChildren().add(makeBubble(m));
//            scrollToBottom();
//        }
//    }
//
//    private HBox makeBubble(Message m) {
//        Text text = new Text(m.getText());
//        TextFlow flow = new TextFlow(text);
//        boolean isSent = m.getSender().equals(currentUser.getUsername());
//        flow.getStyleClass().add(isSent ? "sent" : "received");
//        flow.setTextAlignment(isSent ? TextAlignment.RIGHT : TextAlignment.LEFT);
//        flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") + "; " +
//                "-fx-padding: 8px; -fx-background-radius: 10px;");
//
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
//        Platform.runLater(() -> {
//            if (body.startsWith("CHAT_HISTORY|")) {
//                String[] parts = body.split("\\|", 4);
//                if (parts.length == 4) {
//                    String friend = parts[1];
//                    String user = parts[2];
//                    String payload = parts[3];
//
//                    if (!user.equals(currentUser.getUsername())) {
//                        System.out.println("Ignored history not meant for me: " + user);
//                        return;
//                    }
//
//                    List<Message> history = parseChatHistory(payload, friend, user);
//                    chatHistory.put(friend, history);
//                    if (friend.equals(friendListView.getSelectionModel().getSelectedItem())) {
//                        loadChat(friend);
//                    }
//                }
//            } else if (body.startsWith("OFFLINE_MSG|")) {
//                String[] parts = body.split("\\|", 4);
//                if (parts.length == 4) {
//                    String sender = parts[1];
//                    String user = parts[2];
//                    String msgLine = parts[3];
//
//                    if (!user.equals(currentUser.getUsername())) {
//                        System.out.println("Ignored offline message not meant for me: " + user);
//                        return;
//                    }
//
//                    String[] messageParts = msgLine.split(":", 2);
//                    String senderFromLine = messageParts[0];
//                    String msgText = messageParts.length == 2 ? messageParts[1] : "";
//
//                    if (!senderFromLine.equals(sender)) {
//                        System.out.println("Sender mismatch in offline message: expected " + sender + ", got " + senderFromLine);
//                        return;
//                    }
//
//                    List<Message> history = chatHistory.computeIfAbsent(sender, f -> new ArrayList<>());
//                    if (!messageExists(history, sender, msgText)) {
//                        Message m = new Message(sender, user, msgText);
//                        history.add(m);
//                        if (sender.equals(friendListView.getSelectionModel().getSelectedItem())) {
//                            chatMessagesBox.getChildren().add(makeBubble(m));
//                            scrollToBottom();
//                        }
//                    }
//                }
//            } else if (body.startsWith("PRIVATE|")) {
//                String[] parts = body.split("\\|", 4);
//                if (parts.length == 4) {
//                    String sender = parts[1];
//                    String receiver = parts[2];
//                    String msgText = parts[3];
//
//                    if (!receiver.equals(currentUser.getUsername())) {
//                        System.out.println("Ignored private message not meant for me: " + receiver);
//                        return;
//                    }
//
//                    List<Message> history = chatHistory.computeIfAbsent(sender, f -> new ArrayList<>());
//                    if (!messageExists(history, sender, msgText)) {
//                        Message m = new Message(sender, receiver, msgText);
//                        history.add(m);
//                        if (sender.equals(friendListView.getSelectionModel().getSelectedItem())) {
//                            chatMessagesBox.getChildren().add(makeBubble(m));
//                            scrollToBottom();
//                        }
//                    }
//                }
//            }
//        });
//    }
//
//    private List<Message> parseChatHistory(String payload, String friend, String user) {
//        List<Message> messages = new ArrayList<>();
//        String[] entries = payload.split(";;;");
//        for (String entry : entries) {
//            String[] parts = entry.split(":", 2);
//            if (parts.length == 2) {
//                String sender = parts[0];
//                String text = parts[1];
//                String receiver = sender.equals(user) ? friend : user;
//                messages.add(new Message(sender, receiver, text));
//            }
//        }
//        return messages;
//    }
//
//    private boolean messageExists(List<Message> history, String sender, String text) {
//        return history.stream().anyMatch(m -> m.getSender().equals(sender) && m.getText().equals(text));
//    }
//
//    public static class Message {
//        private String sender;
//        private String receiver;
//        private String text;
//
//        public Message(String sender, String receiver, String text) {
//            this.sender = sender;
//            this.receiver = receiver;
//            this.text = text;
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
//        public String getOtherParty(String u) {
//            return u.equals(sender) ? receiver : sender;
//        }
//    }
//
//    public void shutdown() {
//        connection.removeListener(this);
//    }
//}

///XXXXXXXXXXXXX------33333.22222
//
//package main.java.controller;
//
//import javafx.application.Platform;
//import javafx.collections.FXCollections;
//import javafx.fxml.FXML;
//import javafx.fxml.Initializable;
//import javafx.geometry.Insets;
//import javafx.geometry.Pos;
//import javafx.scene.control.*;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.VBox;
//import javafx.scene.text.Text;
//import javafx.scene.text.TextAlignment;
//import javafx.scene.text.TextFlow;
//import main.java.client.ClientConnection;
//import main.java.user.Session;
//import main.java.user.User;
//
//import java.io.File;
//import java.net.URL;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class ChatBoxController implements Initializable, ClientConnection.MessageListener {
//
//    @FXML private ListView<String> friendListView;
//    @FXML private ScrollPane chatScrollPane;
//    @FXML private VBox chatMessagesBox;
//    @FXML private TextField messageField;
//    @FXML private Button sendButton;
//
//    private User currentUser;
//    private ClientConnection connection;
//    private final Map<String, List<Message>> chatHistory = new HashMap<>();
//
//    @Override
//    public void initialize(URL location, ResourceBundle resources) {
//        currentUser = Session.getCurrentUser();
//        connection = ClientConnection.getInstance();
//        connection.registerListener(this);
//
//        populateFriendList();
//
//        friendListView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
//            loadChat(selected);
//        });
//
//        sendButton.setOnAction(e -> sendCurrentMessage());
//        messageField.setOnAction(e -> sendCurrentMessage());
//    }
//
//    private void populateFriendList() {
//        File dir = new File("users");
//        if (!dir.exists()) return;
//        List<String> names = Arrays.stream(dir.listFiles((d, n) -> n.endsWith(".ser")))
//                .map(f -> f.getName().replace(".ser", ""))
//                .filter(name -> !name.equals(currentUser.getUsername()))
//                .sorted(String::compareToIgnoreCase)
//                .collect(Collectors.toList());
//        friendListView.setItems(FXCollections.observableArrayList(names));
//    }
//
//    private void loadChat(String friend) {
//        chatMessagesBox.getChildren().clear();
//        if (friend == null) return;
//        List<Message> history = chatHistory.getOrDefault(friend, Collections.emptyList());
//        for (Message msg : history) {
//            chatMessagesBox.getChildren().add(makeBubble(msg));
//        }
//        scrollToBottom();
//    }
//
//    private void sendCurrentMessage() {
//        String friend = friendListView.getSelectionModel().getSelectedItem();
//        String text = messageField.getText().trim();
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
//            chatMessagesBox.getChildren().add(makeBubble(m));
//            scrollToBottom();
//        }
//    }
//
//    private HBox makeBubble(Message m) {
//        Text text = new Text(m.getText());
//        TextFlow flow = new TextFlow(text);
//        boolean isSent = m.getSender().equals(currentUser.getUsername());
//        flow.getStyleClass().add(isSent ? "sent" : "received");
//        flow.setTextAlignment(isSent ? TextAlignment.RIGHT : TextAlignment.LEFT);
//        flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") + "; " +
//                "-fx-padding: 8px; -fx-background-radius: 10px;");
//
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
//        Platform.runLater(() -> {
//            if (body.startsWith("CHAT_HISTORY|")) {
//                String[] parts = body.split("\\|", 4);
//                if (parts.length == 4) {
//                    String friend = parts[1];
//                    String user = parts[2];
//                    String payload = parts[3];
//
//                    if (!user.equals(currentUser.getUsername())) {
//                        System.out.println("Ignored history not meant for me: " + user);
//                        return;
//                    }
//
//                    List<Message> history = parseChatHistory(payload, friend, user);
//                    chatHistory.put(friend, history);
//                    if (friend.equals(friendListView.getSelectionModel().getSelectedItem())) {
//                        loadChat(friend);
//                    }
//                }
//            } else if (body.startsWith("OFFLINE_MSG|")) {
//                String[] parts = body.split("\\|", 4);
//                if (parts.length == 4) {
//                    String sender = parts[1];
//                    String user = parts[2];
//                    String msgLine = parts[3];
//
//                    if (!user.equals(currentUser.getUsername())) {
//                        System.out.println("Ignored offline message not meant for me: " + user);
//                        return;
//                    }
//
//                    String[] messageParts = msgLine.split(":", 2);
//                    String senderFromLine = messageParts[0];
//                    String msgText = messageParts.length == 2 ? messageParts[1] : "";
//
//                    if (!senderFromLine.equals(sender)) {
//                        System.out.println("Sender mismatch in offline message: expected " + sender + ", got " + senderFromLine);
//                        return;
//                    }
//
//                    List<Message> history = chatHistory.computeIfAbsent(sender, f -> new ArrayList<>());
//                    if (!messageExists(history, sender, msgText)) {
//                        Message m = new Message(sender, user, msgText);
//                        history.add(m);
//                        if (sender.equals(friendListView.getSelectionModel().getSelectedItem())) {
//                            chatMessagesBox.getChildren().add(makeBubble(m));
//                            scrollToBottom();
//                        }
//                    }
//                }
//            } else {
//                // Handle regular private messages
//                List<Message> history = chatHistory.computeIfAbsent(from, f -> new ArrayList<>());
//                if (!messageExists(history, from, body)) {
//                    Message m = new Message(from, currentUser.getUsername(), body);
//                    history.add(m);
//                    if (from.equals(friendListView.getSelectionModel().getSelectedItem())) {
//                        chatMessagesBox.getChildren().add(makeBubble(m));
//                        scrollToBottom();
//                    }
//                }
//            }
//        });
//    }
//
//    private List<Message> parseChatHistory(String payload, String friend, String user) {
//        List<Message> messages = new ArrayList<>();
//        String[] entries = payload.split(";;;");
//        for (String entry : entries) {
//            String[] parts = entry.split(":", 2);
//            if (parts.length == 2) {
//                String sender = parts[0];
//                String text = parts[1];
//                String receiver = sender.equals(user) ? friend : user;
//                messages.add(new Message(sender, receiver, text));
//            }
//        }
//        return messages;
//    }
//
//    private boolean messageExists(List<Message> history, String sender, String text) {
//        return history.stream().anyMatch(m -> m.getSender().equals(sender) && m.getText().equals(text));
//    }
//
//    public static class Message {
//        private String sender;
//        private String receiver;
//        private String text;
//
//        public Message(String sender, String receiver, String text) {
//            this.sender = sender;
//            this.receiver = receiver;
//            this.text = text;
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
//
//import java.io.File;
//import java.io.FilenameFilter;
//import java.net.URL;
//import java.util.*;
//
//public class ChatBoxController implements Initializable, ClientConnection.MessageListener {
//
//    @FXML private ListView<String> friendListView;
//    @FXML private ScrollPane chatScrollPane;
//    @FXML private VBox chatMessagesBox;
//    @FXML private TextField messageField;
//    @FXML private Button sendButton;
//
//    private User currentUser;
//    private ClientConnection connection;
//    private final Map<String, List<Message>> chatHistory = new HashMap<>();
//
//    @Override
//    public void initialize(URL location, ResourceBundle resources) {
//        currentUser = Session.getCurrentUser();
//        connection = ClientConnection.getInstance();
//        connection.registerListener(this);
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
//    }
//
//    private void populateFriendList() {
//        File dir = new File("users");
//        if (!dir.exists()) {
//            return;
//        }
//        File[] files = dir.listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String name) {
//                return name.endsWith(".ser");
//            }
//        });
//        List<String> names = new ArrayList<>();
//        if (files != null) {
//            for (File f : files) {
//                String name = f.getName().replace(".ser", "");
//                if (!name.equals(currentUser.getUsername())) {
//                    names.add(name);
//                }
//            }
//            Collections.sort(names, new Comparator<String>() {
//                @Override
//                public int compare(String s1, String s2) {
//                    return s1.compareToIgnoreCase(s2);
//                }
//            });
//        }
//        friendListView.setItems(FXCollections.observableArrayList(names));
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
//        String friend = friendListView.getSelectionModel().getSelectedItem();
//        String text = messageField.getText().trim();
//        if (friend == null || text.isEmpty()) {
//            return;
//        }
//        Message m = new Message(currentUser.getUsername(), friend, text);
//        appendAndRender(m);
//        connection.sendPrivateMessage(friend, text);
//        messageField.clear();
//    }
//
//    private void appendAndRender(Message m) {
//        String friend = m.getOtherParty(currentUser.getUsername());
//        if (!chatHistory.containsKey(friend)) {
//            chatHistory.put(friend, new ArrayList<Message>());
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
//        Text text = new Text(m.getText());
//        TextFlow flow = new TextFlow(text);
//        boolean isSent = m.getSender().equals(currentUser.getUsername());
//        flow.getStyleClass().add(isSent ? "sent" : "received");
//        flow.setTextAlignment(isSent ? TextAlignment.RIGHT : TextAlignment.LEFT);
//        flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") + "; " +
//                "-fx-padding: 8px; -fx-background-radius: 10px;");
//
//        HBox bubbleBox = new HBox(flow);
//        bubbleBox.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
//        bubbleBox.setPadding(new Insets(4, 8, 4, 8));
//        return bubbleBox;
//    }
//
//    private void scrollToBottom() {
//        Platform.runLater(new Runnable() {
//            @Override
//            public void run() {
//                chatScrollPane.layout();
//                chatScrollPane.setVvalue(1.0);
//            }
//        });
//    }
//
//    @Override
//    public void onMessageReceived(String from, String body) {
//        System.out.println("UI received: from=" + from + ", body=" + body);
//        Platform.runLater(new Runnable() {
//            @Override
//            public void run() {
//                handleIncomingMessage(from, body);
//            }
//        });
//    }
//
//    private void handleIncomingMessage(String from, String body) {
//        if (body.startsWith("CHAT_HISTORY|")) {
//            String[] parts = body.split("\\|", 4);
//            if (parts.length == 4) {
//                String friend = parts[1];
//                String user = parts[2];
//                String payload = parts[3];
//
//                if (!user.equals(currentUser.getUsername())) {
//                    System.out.println("Ignored history not meant for me: " + user);
//                    return;
//                }
//
//                List<Message> history = parseChatHistory(payload, friend, user);
//                chatHistory.put(friend, history);
//                String selectedFriend = friendListView.getSelectionModel().getSelectedItem();
//                if (friend.equals(selectedFriend)) {
//                    loadChat(friend);
//                }
//            }
//        } else if (body.startsWith("OFFLINE_MSG|")) {
//            String[] parts = body.split("\\|", 4);
//            if (parts.length == 4) {
//                String sender = parts[1];
//                String user = parts[2];
//                String msgLine = parts[3];
//
//                if (!user.equals(currentUser.getUsername())) {
//                    System.out.println("Ignored offline message not meant for me: " + user);
//                    return;
//                }
//
//                String[] messageParts = msgLine.split(":", 2);
//                String senderFromLine = messageParts[0];
//                String msgText = messageParts.length == 2 ? messageParts[1] : "";
//
//                if (!senderFromLine.equals(sender)) {
//                    System.out.println("Sender mismatch in offline message: expected " + sender + ", got " + senderFromLine);
//                    return;
//                }
//                if (!chatHistory.containsKey(sender)) {
//                    chatHistory.put(sender, new ArrayList<Message>());
//                }
//                List<Message> history = chatHistory.get(sender);
//                if (!messageExists(history, sender, msgText)) {
//                    Message m = new Message(sender, currentUser.getUsername(), msgText);
//                    history.add(m);
//                    String selectedFriend = friendListView.getSelectionModel().getSelectedItem();
//                    if (sender.equals(selectedFriend)) {
//                        chatMessagesBox.getChildren().add(makeBubble(m));
//                        scrollToBottom();
//                    }
//                }
//            }
//        } else {
//            if (!chatHistory.containsKey(from)) {
//                chatHistory.put(from, new ArrayList<Message>());
//            }
//            List<Message> history = chatHistory.get(from);
//            if (!messageExists(history, from, body)) {
//                Message m = new Message(from, currentUser.getUsername(), body);
//                history.add(m);
//                String selectedFriend = friendListView.getSelectionModel().getSelectedItem();
//                if (from.equals(selectedFriend)) {
//                    chatMessagesBox.getChildren().add(makeBubble(m));
//                    scrollToBottom();
//                }
//            }
//        }
//    }
//
//    private List<Message> parseChatHistory(String payload, String friend, String user) {
//        List<Message> messages = new ArrayList<Message>();
//        String[] entries = payload.split(";;;");
//        for (String entry : entries) {
//            String[] parts = entry.split(":", 2);
//            if (parts.length == 2) {
//                String sender = parts[0];
//                String text = parts[1];
//                String receiver = sender.equals(user) ? friend : user;
//                messages.add(new Message(sender, receiver, text));
//            }
//        }
//        return messages;
//    }
//
//    private boolean messageExists(List<Message> history, String sender, String text) {
//        for (Message m : history) {
//            if (m.getSender().equals(sender) && m.getText().equals(text)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public static class Message {
//        private String sender;
//        private String receiver;
//        private String text;
//
//        public Message(String sender, String receiver, String text) {
//            this.sender = sender;
//            this.receiver = receiver;
//            this.text = text;
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
//        public String getOtherParty(String u) {
//            if (u.equals(sender)) {
//                return receiver;
//            } else {
//                return sender;
//            }
//        }
//    }
//
//    public void shutdown() {
//        connection.removeListener(this);
//    }
//
//}

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
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import main.java.client.ClientConnection;
import main.java.user.Session;
import main.java.user.User;
import main.java.util.sceneChange;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.*;

public class ChatBoxController implements Initializable, ClientConnection.MessageListener {

    @FXML private ListView<String> friendListView;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesBox;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button backButton;

    private User currentUser;
    private ClientConnection connection;
    private final Map<String, List<Message>> chatHistory = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = Session.getCurrentUser();
        connection = ClientConnection.getInstance();
        connection.registerListener(this);

        // Request chat history from server
        connection.requestChatHistory(currentUser.getUsername());

        populateFriendList();

        friendListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldValue, String newValue) {
                loadChat(newValue);
            }
        });

        sendButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                sendCurrentMessage();
            }
        });

        messageField.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                sendCurrentMessage();
            }
        });

        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                shutdown();
                sceneChange.changeScene("Dashboard.fxml", backButton, currentUser);
            }
        });

        // Auto-select first friend if available
        if (!friendListView.getItems().isEmpty()) {
            friendListView.getSelectionModel().selectFirst();
            loadChat(friendListView.getSelectionModel().getSelectedItem());
        }
    }

    private void populateFriendList() {
        File dir = new File("users");
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".ser");
            }
        });
        List<String> names = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                String name = f.getName().replace(".ser", "");
                if (!name.equals(currentUser.getUsername())) {
                    names.add(name);
                }
            }
            Collections.sort(names, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    return s1.compareToIgnoreCase(s2);
                }
            });
        }
        friendListView.setItems(FXCollections.observableArrayList(names));
    }

    private void loadChat(String friend) {
        chatMessagesBox.getChildren().clear();
        if (friend == null) {
            return;
        }
        List<Message> history = chatHistory.get(friend);
        if (history != null) {
            for (Message msg : history) {
                chatMessagesBox.getChildren().add(makeBubble(msg));
            }
        }
        scrollToBottom();
    }

    private void sendCurrentMessage() {
        String friend = friendListView.getSelectionModel().getSelectedItem();
        String text = messageField.getText().trim();
        if (friend == null || text.isEmpty()) {
            return;
        }
        Message m = new Message(currentUser.getUsername(), friend, text);
        appendAndRender(m);
        connection.sendPrivateMessage(friend, text);
        messageField.clear();
    }

    private void appendAndRender(Message m) {
        String friend = m.getOtherParty(currentUser.getUsername());
        if (!chatHistory.containsKey(friend)) {
            chatHistory.put(friend, new ArrayList<>());
        }
        List<Message> history = chatHistory.get(friend);
        history.add(m);

        String selectedFriend = friendListView.getSelectionModel().getSelectedItem();
        if (friend.equals(selectedFriend)) {
            chatMessagesBox.getChildren().add(makeBubble(m));
            scrollToBottom();
        }
    }

    private HBox makeBubble(Message m) {
        Text text = new Text(m.getText());
        TextFlow flow = new TextFlow(text);
        boolean isSent = m.getSender().equals(currentUser.getUsername());
        flow.getStyleClass().add(isSent ? "sent" : "received");
        flow.setTextAlignment(isSent ? TextAlignment.RIGHT : TextAlignment.LEFT);
        flow.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") + "; " +
                "-fx-padding: 8px; -fx-background-radius: 10px;");

        HBox bubbleBox = new HBox(flow);
        bubbleBox.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubbleBox.setPadding(new Insets(4, 8, 4, 8));
        return bubbleBox;
    }

    private void scrollToBottom() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                chatScrollPane.layout();
                chatScrollPane.setVvalue(1.0);
            }
        });
    }

    @Override
    public void onMessageReceived(String from, String body) {
        System.out.println("UI received: from=" + from + ", body=" + body);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                handleIncomingMessage(from, body);
            }
        });
    }

    private void handleIncomingMessage(String from, String body) {
        if (body.startsWith("CHAT_HISTORY|")) {
            String[] parts = body.split("\\|", 4);
            if (parts.length == 4) {
                String friend = parts[1];
                String user = parts[2];
                String payload = parts[3];

                if (!user.equals(currentUser.getUsername())) {
                    System.out.println("Ignored history not meant for me: " + user);
                    return;
                }

                List<Message> history = parseChatHistory(payload, friend, user);
                chatHistory.put(friend, history);
                String selectedFriend = friendListView.getSelectionModel().getSelectedItem();
                if (friend.equals(selectedFriend)) {
                    loadChat(friend);
                }
            }
        } else if (body.startsWith("OFFLINE_MSG|")) {
            String[] parts = body.split("\\|", 4);
            if (parts.length == 4) {
                String sender = parts[1];
                String user = parts[2];
                String msgLine = parts[3];

                if (!user.equals(currentUser.getUsername())) {
                    System.out.println("Ignored offline message not meant for me: " + user);
                    return;
                }

                String[] messageParts = msgLine.split(":", 2);
                String senderFromLine = messageParts[0];
                String msgText = messageParts.length == 2 ? messageParts[1] : "";

                if (!senderFromLine.equals(sender)) {
                    System.out.println("Sender mismatch in offline message: expected " + sender + ", got " + senderFromLine);
                    return;
                }
                if (!chatHistory.containsKey(sender)) {
                    chatHistory.put(sender, new ArrayList<>());
                }
                List<Message> history = chatHistory.get(sender);
                if (!messageExists(history, sender, msgText)) {
                    Message m = new Message(sender, currentUser.getUsername(), msgText);
                    history.add(m);
                    String selectedFriend = friendListView.getSelectionModel().getSelectedItem();
                    if (sender.equals(selectedFriend)) {
                        chatMessagesBox.getChildren().add(makeBubble(m));
                        scrollToBottom();
                    }
                }
            }
        } else {
            if (!chatHistory.containsKey(from)) {
                chatHistory.put(from, new ArrayList<>());
            }
            List<Message> history = chatHistory.get(from);
            if (!messageExists(history, from, body)) {
                Message m = new Message(from, currentUser.getUsername(), body);
                history.add(m);
                String selectedFriend = friendListView.getSelectionModel().getSelectedItem();
                if (from.equals(selectedFriend)) {
                    chatMessagesBox.getChildren().add(makeBubble(m));
                    scrollToBottom();
                }
            }
        }
    }

    private List<Message> parseChatHistory(String payload, String friend, String user) {
        List<Message> messages = new ArrayList<>();
        String[] entries = payload.split(";;;");
        for (String entry : entries) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
                String sender = parts[0];
                String text = parts[1];
                String receiver = sender.equals(user) ? friend : user;
                messages.add(new Message(sender, receiver, text));
            }
        }
        return messages;
    }

    private boolean messageExists(List<Message> history, String sender, String text) {
        for (Message m : history) {
            if (m.getSender().equals(sender) && m.getText().equals(text)) {
                return true;
            }
        }
        return false;
    }

    public static class Message {
        private String sender;
        private String receiver;
        private String text;

        public Message(String sender, String receiver, String text) {
            this.sender = sender;
            this.receiver = receiver;
            this.text = text;
        }

        public String getSender() {
            return sender;
        }

        public String getReceiver() {
            return receiver;
        }

        public String getText() {
            return text;
        }

        public String getOtherParty(String u) {
            if (u.equals(sender)) {
                return receiver;
            } else {
                return sender;
            }
        }
    }

    public void shutdown() {
        connection.removeListener(this);
    }
}