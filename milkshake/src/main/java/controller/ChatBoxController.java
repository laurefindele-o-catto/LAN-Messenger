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

import java.net.URL;
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

    private User currentUser;
    private ClientConnection connection;
    private final Map<String, List<Message>> chatHistory = new HashMap<>();
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ClientConnection.getInstance().setCurrentController(this);
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
        Set<String> friends = currentUser.getFriends();
        if (friends == null || friends.isEmpty()) {
            friendListView.setItems(FXCollections.observableArrayList());
            return;
        }

        List<String> sortedFriends = new ArrayList<>(friends);
        Collections.sort(sortedFriends, String::compareToIgnoreCase);
        friendListView.setItems(FXCollections.observableArrayList(sortedFriends));
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
        LocalDateTime timestamp = LocalDateTime.now();
        Message m = new Message(currentUser.getUsername(), friend, text, timestamp);
        appendAndRender(m);
        connection.sendPrivateMessage(friend, text, timestamp);
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
        Text timestampText = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "] ");
        timestampText.setStyle("-fx-font-size: 10px; -fx-fill: gray;");
        TextFlow flow = new TextFlow(timestampText, text);
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
        Platform.runLater(() -> {
            chatScrollPane.layout();
            chatScrollPane.setVvalue(1.0);
        });
    }

    @Override
    public void onMessageReceived(String from, String body) {
        System.out.println("UI received: from=" + from + ", body=" + body);
        Platform.runLater(() -> handleIncomingMessage(from, body));
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
        } else {
            String[] bodyParts = body.split("\\|", 2);
            String timestampStr = bodyParts[0];
            String messageText = bodyParts.length > 1 ? bodyParts[1] : "";
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr);

            Message m = new Message(from, currentUser.getUsername(), messageText, timestamp);
            appendAndRender(m);
        }
    }

    private List<Message> parseChatHistory(String payload, String friend, String user) {
        List<Message> messages = new ArrayList<>();
        String[] entries = payload.split(";;;");
        for (String entry : entries) {
            String[] parts = entry.split("\\|", 3);
            if (parts.length == 3) {
                String sender = parts[0];
                String timestampStr = parts[1];
                String text = parts[2];
                LocalDateTime timestamp = LocalDateTime.parse(timestampStr);
                String receiver = sender.equals(user) ? friend : user;
                messages.add(new Message(sender, receiver, text, timestamp));
            }
        }
        return messages;
    }

    public static class Message {
        private String sender;
        private String receiver;
        private String text;
        private LocalDateTime timestamp;

        public Message(String sender, String receiver, String text, LocalDateTime timestamp) {
            this.sender = sender;
            this.receiver = receiver;
            this.text = text;
            this.timestamp = timestamp;
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

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getOtherParty(String u) {
            return u.equals(sender) ? receiver : sender;
        }
    }

    public void shutdown() {
        connection.removeListener(this);
    }
}