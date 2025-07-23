//
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
//    @FXML private Button backButton;
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
//        // Request chat history from server
//        connection.requestChatHistory(currentUser.getUsername());
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
//        // Auto-select first friend if available
//        if (!friendListView.getItems().isEmpty()) {
//            friendListView.getSelectionModel().selectFirst();
//            loadChat(friendListView.getSelectionModel().getSelectedItem());
//        }
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
//                    chatHistory.put(sender, new ArrayList<>());
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
//                chatHistory.put(from, new ArrayList<>());
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
//}

package main.java.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
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
        LocalDateTime timestamp = LocalDateTime.now();
        Message m = new Message(currentUser.getUsername(), friend, text, timestamp);
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

//    private HBox makeBubble(Message m) {
//        Text text = new Text(m.getText());
//        Text timestampText = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "] ");
//        timestampText.setStyle("-fx-font-size: 10px; -fx-fill: gray;");
//        TextFlow flow = new TextFlow(timestampText, text);
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
private HBox makeBubble(Message m) {
    boolean isSent = m.getSender().equals(currentUser.getUsername());

    // Create timestamp text
    Text timestampText = new Text("[" + m.getTimestamp().format(TIMESTAMP_FORMATTER) + "]");
    timestampText.setStyle("-fx-font-size: 10px; -fx-fill: gray;");

    // Create message label for text wrapping
    Label messageLabel = new Label(m.getText());
    messageLabel.setWrapText(true);

    // Use VBox to stack timestamp and message vertically
    VBox messageBox = new VBox(2, timestampText, messageLabel); // 2 pixels spacing
    messageBox.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") + "; " +
            "-fx-padding: 8px; -fx-background-radius: 10px;");
    messageBox.setMaxWidth(300); // Set a max width to ensure text wrapping

    // Wrap in HBox for alignment
    HBox bubbleBox = new HBox(messageBox);
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
            String[] parts = body.split("\\|", 5);
            if (parts.length == 5) {
                String sender = parts[1];
                String user = parts[2];
                String timestampStr = parts[3];
                String msgText = parts[4];

                if (!user.equals(currentUser.getUsername())) {
                    System.out.println("Ignored offline message not meant for me: " + user);
                    return;
                }

                LocalDateTime timestamp = LocalDateTime.parse(timestampStr);
                if (!chatHistory.containsKey(sender)) {
                    chatHistory.put(sender, new ArrayList<>());
                }
                List<Message> history = chatHistory.get(sender);
                if (!messageExists(history, sender, msgText, timestamp)) {
                    Message m = new Message(sender, currentUser.getUsername(), msgText, timestamp);
                    history.add(m);
                    String selectedFriend = friendListView.getSelectionModel().getSelectedItem();
                    if (sender.equals(selectedFriend)) {
                        chatMessagesBox.getChildren().add(makeBubble(m));
                        scrollToBottom();
                    }
                }
            }
        } else if (body.startsWith("PRIVATE|")) {
            String[] parts = body.split("\\|", 5);
            if (parts.length == 5) {
                String sender = parts[1];
                String user = parts[2];
                String timestampStr = parts[3];
                String msgText = parts[4];

                if (!user.equals(currentUser.getUsername())) {
                    System.out.println("Ignored private message not meant for me: " + user);
                    return;
                }

                LocalDateTime timestamp = LocalDateTime.parse(timestampStr);
                if (!chatHistory.containsKey(sender)) {
                    chatHistory.put(sender, new ArrayList<>());
                }
                List<Message> history = chatHistory.get(sender);
                if (!messageExists(history, sender, msgText, timestamp)) {
                    Message m = new Message(sender, currentUser.getUsername(), msgText, timestamp);
                    history.add(m);
                    String selectedFriend = friendListView.getSelectionModel().getSelectedItem();
                    if (sender.equals(selectedFriend)) {
                        chatMessagesBox.getChildren().add(makeBubble(m));
                        scrollToBottom();
                    }
                }
            }
        } else {
            String[] bodyParts = body.split("\\|", 2);
            String timestampStr = bodyParts.length == 2 ? bodyParts[0] : null;
            String msgText = bodyParts.length == 2 ? bodyParts[1] : bodyParts[0];
            LocalDateTime timestamp = timestampStr != null ? LocalDateTime.parse(timestampStr) : LocalDateTime.now();
            if (!chatHistory.containsKey(from)) {
                chatHistory.put(from, new ArrayList<>());
            }
            List<Message> history = chatHistory.get(from);
            if (!messageExists(history, from, msgText, timestamp)) {
                Message m = new Message(from, currentUser.getUsername(), msgText, timestamp);
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
            String[] parts = entry.split("\\|", 3);
            if (parts.length == 3) {
                String sender = parts[0];
                String timestampStr = parts[1];
                String text = parts[2];
                LocalDateTime timestamp = LocalDateTime.parse(timestampStr);
                String receiver = sender.equals(user) ? friend : user;
                messages.add(new Message(sender, receiver, text, timestamp));
            } else if (parts.length == 2) { // Handle legacy format without timestamp
                String sender = parts[0];
                String text = parts[1];
                String receiver = sender.equals(user) ? friend : user;
                messages.add(new Message(sender, receiver, text, LocalDateTime.now()));
            }
        }
        return messages;
    }

    private boolean messageExists(List<Message> history, String sender, String text, LocalDateTime timestamp) {
        for (Message m : history) {
            if (m.getSender().equals(sender) && m.getText().equals(text) && m.getTimestamp().equals(timestamp)) {
                return true;
            }
        }
        return false;
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

        public Message(String sender, String receiver, String text) {
            this(sender, receiver, text, LocalDateTime.now());
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
