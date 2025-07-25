//
//package main.java.client;
//
//import javafx.application.Platform;
//import main.java.controller.FriendRequestsController;
//import main.java.controller.ViewProfileController;
//import main.java.user.Session;
//import main.java.user.User;
//
//import java.io.*;
//import java.net.Socket;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
////decrypting multiple versions of the code because the file contains an older commented-out version and the current implementation. The relevant, active portion of the code starts with the `package` declaration shown here. It's been patched so that group messages are sent in the format `GROUP_MSG|groupName|sender|timestamp|message`, ensuring that both the server and client are using the same message order:
//
//public class ClientConnection {
//
//    private static ClientConnection INSTANCE;
//    private final Queue<String> pendingMessages = new LinkedList<>();
//    private final ExecutorService dispatchPool = Executors.newSingleThreadExecutor();
//
//    public static ClientConnection getInstance() {
//        if (INSTANCE == null) {
//            INSTANCE = new ClientConnection();
//        }
//        return INSTANCE;
//    }
//
//    public interface MessageListener {
//        void onMessageReceived(String from, String body);
//    }
//
//    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
//
//    public void registerListener(MessageListener l) {
//        listeners.add(l);
//        // Notify server user is online and flush any queued messages
//        if (socket != null && !socket.isClosed() && username != null) {
//            out.println("ONLINE|" + username);
//        }
//        synchronized (pendingMessages) {
//            while (!pendingMessages.isEmpty()) {
//                dispatch(pendingMessages.poll());
//            }
//        }
//    }
//
//    public void removeListener(MessageListener l) {
//        listeners.remove(l);
//    }
//
//    private Socket socket;
//    private PrintWriter out;
//    private String username;
//    private Object currentController;
//
//    private ClientConnection() {}
//
//    /**
//     * Connects to the server and marks the user as online.
//     */
//    public boolean connect(String host, int port, String user) {
//        if (socket != null && !socket.isClosed()) {
//            System.out.println("Already connected, skipping new connection");
//            return true;
//        }
//        try {
//            socket = new Socket(host, port);
//            out = new PrintWriter(socket.getOutputStream(), true);
//            this.username = user;
//
//            // Start listener thread for incoming server messages
//            new Thread(this::listen, "ClientConnection-Reader").start();
//
//            out.println("ONLINE|" + username);
//            return true;
//        } catch (IOException e) {
//            System.err.println("Failed to connect: " + e.getMessage());
//            return false;
//        }
//    }
//
//    /**
//     * Sends a private one-to-one chat message to another user.
//     */
//    public void sendPrivateMessage(String to, String body, LocalDateTime timestamp) {
//        String timestampStr = timestamp.toString();
//        out.println("PRIVATE|" + username + "|" + to + "|" + timestampStr + "|" + body);
//    }
//
//    public void sendPrivateMessage(String to, String body) {
//        String timestampStr = LocalDateTime.now().toString();
//        out.println("PRIVATE|" + username + "|" + to + "|" + timestampStr + "|" + body);
//    }
//
//    /**
//     * Sends a group chat message to the server in the order groupName|sender|timestamp|body.
//     *
//     * @param group the group name (conversation identifier)
//     * @param body  the message text
//     * @param timestamp the timestamp of the message
//     */
//    public void sendGroupMessage(String group, String body, LocalDateTime timestamp) {
//        String timestampStr = timestamp.toString();
//        // Compose group message in the order expected by both the client and server:
//        // header, group name, sender, timestamp, body. Placing the group name first
//        // makes it easy for the receiver to key off the conversation identifier.
//        out.println("GROUP_MSG|" + group + "|" + username + "|" + timestampStr + "|" + body);
//    }
//
//    /**
//     * Overload for sending a group chat message using the current time.
//     *
//     * @param group the group name (conversation identifier)
//     * @param body  the message text
//     */
//    public void sendGroupMessage(String group, String body) {
//        String timestampStr = LocalDateTime.now().toString();
//        // Use the same group message ordering (groupName|sender|timestamp|body) for the
//        // overload that defaults the timestamp.
//        out.println("GROUP_MSG|" + group + "|" + username + "|" + timestampStr + "|" + body);
//    }
//
//    /**
//     * Requests the list of groups that the current user is a member of. The server
//     * will respond with a GROUP_LIST message containing comma-separated group names.
//     */
//    public void requestGroupList() {
//        if (out != null && username != null) {
//            out.println("GROUPS_REQUEST|" + username);
//        }
//    }
//
//    /**
//     * Requests the server to create a new group. The creator (the current
//     * username) will always be included automatically on the server side.
//     *
//     * @param groupName the desired group name
//     * @param members   additional usernames to include in the group (excluding the creator)
//     */
//    public void createGroup(String groupName, List<String> members) {
//        if (out == null || username == null) {
//            return;
//        }
//        // Build a comma-separated list of member usernames. The creator will be
//        // added automatically on the server, so we do not include ourselves here.
//        String memberCsv = String.join(",", members);
//        out.println("CREATE_GROUP|" + username + "|" + groupName + "|" + memberCsv);
//    }
//
//    /**
//     * Requests full chat history (all conversations) from the server for the current user.
//     */
//    public void requestChatHistory(String username) {
//        if (out != null) {
//            out.println("ONLINE|" + username);
//        }
//    }
//
//    /**
//     * Requests full history of a specific group chat from the server.
//     */
//    public void requestGroupHistory(String username, String groupName) {
//        if (out != null) {
//            out.println("REQUEST_GROUP_HISTORY|" + username + "|" + groupName);
//        }
//    }
//
//    /**
//     * Listens for messages from the server and dispatches them to registered listeners.
//     */
//    private void listen() {
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                System.out.println("Client received: " + line);
//                if (listeners.isEmpty()) {
//                    // Queue messages until a listener is registered
//                    synchronized (pendingMessages) {
//                        pendingMessages.offer(line);
//                    }
//                } else {
//                    dispatch(line);
//                }
//            }
//        } catch (IOException ignored) {
//            System.out.println("Connection closed or error in listen loop");
//        }
//    }
//
//    /**
//     * Dispatches a single line message from the server to all listeners.
//     */
//    private void dispatch(String line) {
//        String[] p = line.split("\\|", 6);
//        String header = p[0];
//
//        if ("PRIVATE".equals(header) && p.length >= 5) {
//            // Private message format: PRIVATE|from|to|timestamp|body
//            String from = p[1];
//            String timestampStr = p[3];
//            String msg = p[4];
//            String body = timestampStr + "|" + msg;
//            for (MessageListener l : listeners) {
//                dispatchPool.submit(() -> l.onMessageReceived(from, body));
//            }
//        } else if ("GROUP_MSG".equals(header) && p.length >= 5) {
//            // Group message sent from the server. The format is:
//            // GROUP_MSG|groupName|sender|timestamp|body
//            String groupName = p[1];
//            String sender = p[2];
//            String timestampStr = p[3];
//            String msg = p[4];
//            // Compose a body that includes the sender so the UI can render the name
//            String body = sender + "|" + timestampStr + "|" + msg;
//            for (MessageListener l : listeners) {
//                dispatchPool.submit(() -> l.onMessageReceived(groupName, body));
//            }
//        } else if ("GROUP_LIST".equals(header) && p.length >= 2) {
//            // Response listing all groups for this user: GROUP_LIST|group1,group2,...
//            String groupsCsv = p.length >= 2 ? p[1] : "";
//            String body = "GROUP_LIST|" + groupsCsv;
//            for (MessageListener l : listeners) {
//                String from = "";
//                dispatchPool.submit(() -> l.onMessageReceived(from, body));
//            }
//        } else if ("GROUP_CREATED".equals(header) && p.length >= 3) {
//            // Notification that a new group has been created. Format:
//            // GROUP_CREATED|groupName|creator|members
//            String groupName = p[1];
//            String creator = p[2];
//            String members = p.length >= 4 ? p[3] : "";
//            String body = "GROUP_CREATED|" + groupName + "|" + creator + "|" + members;
//            for (MessageListener l : listeners) {
//                String from = "";
//                dispatchPool.submit(() -> l.onMessageReceived(from, body));
//            }
//        } else if ("CHAT_HISTORY_GROUP".equals(header) && p.length >= 4) {
//            // Group chat history payload: CHAT_HISTORY_GROUP|groupName|username|payload
//            String groupName = p[1];
//            String user = p[2];
//            String payload = p[3];
//            String body = "CHAT_HISTORY_GROUP|" + groupName + "|" + user + "|" + payload;
//            for (MessageListener l : listeners) {
//                String from = "";
//                dispatchPool.submit(() -> l.onMessageReceived(from, body));
//            }
//        } else if ("OFFLINE_MSG".equals(header) && p.length >= 4) {
//            // Offline message delivered upon login. Format (from server): OFFLINE_MSG|conv|user|sender|timestamp|msg
//            String conv = p[1];
//            String user = (p.length >= 3 ? p[2] : "");
//            if (!user.equals(username)) {
//                // Not intended for this user
//                return;
//            }
//            String sender, timestampStr, msgText;
//            if (p.length >= 6) {
//                // Expected format with sender and timestamp
//                sender = p[3];
//                timestampStr = p[4];
//                msgText = p[5];
//            } else if (p.length == 5) {
//                // Missing timestamp (legacy offline format)
//                sender = p[3];
//                timestampStr = LocalDateTime.now().toString();
//                msgText = p[4];
//            } else {
//                // Invalid format, skip
//                System.err.println("Invalid OFFLINE_MSG format: " + line);
//                return;
//            }
//            // Determine conversation type: friend vs group
//            if (Session.getCurrentUser() != null && Session.getCurrentUser().getFriends().contains(conv)) {
//                // Treat as friend message
//                String body = timestampStr + "|" + msgText;
//                for (MessageListener l : listeners) {
//                    dispatchPool.submit(() -> l.onMessageReceived(conv, body));
//                }
//            } else {
//                // Treat as group message
//                String body = sender + "|" + timestampStr + "|" + msgText;
//                for (MessageListener l : listeners) {
//                    dispatchPool.submit(() -> l.onMessageReceived(conv, body));
//                }
//            }
//        } else if ("GOT_FRIEND_REQUEST_FROM".equals(header) && p.length >= 2) {
//            // Friend request notifications
//            String from = p[1];
//            System.out.println("Processing GOT_FRIEND_REQUEST_FROM from " + from + " for user " + username);
//            Platform.runLater(() -> {
//                Session.refreshCurrentUser();
//                User currentUser = Session.getCurrentUser();
//                System.out.println("Updated session for " + currentUser.getUsername() + " with new friend request from " + from);
//                System.out.println("Current friend requests received: " + currentUser.getFriendRequestsReceived());
//                if (currentController instanceof FriendRequestsController f) {
//                    f.refresh();
//                } else if (currentController instanceof ViewProfileController v) {
//                    v.refreshProfile();
//                } else {
//                    System.out.println("User is on a different screen: " +
//                            (currentController != null ? currentController.getClass().getSimpleName() : "null"));
//                }
//            });
//        } else if ("DECLINED_REQUEST_FROM".equals(header) && p.length >= 2) {
//            String friend = p[1];
//            Session.getCurrentUser().declineFriendRequest(User.loadUser(friend));
//            Platform.runLater(() -> {
//                if (currentController instanceof ViewProfileController v) {
//                    v.refreshProfile();
//                }
//            });
//        } else if ("ACCEPTED_REQUEST_FROM".equals(header) && p.length >= 2) {
//            String friend = p[1];
//            Session.getCurrentUser().acceptFriendRequest(User.loadUser(friend));
//            Platform.runLater(() -> {
//                if (currentController instanceof ViewProfileController v) {
//                    v.refreshProfile();
//                }
//            });
//        } else {
//            // Default: pass entire line to listeners
//            String from = p.length > 1 ? p[1] : "";
//            for (MessageListener l : listeners) {
//                dispatchPool.submit(() -> l.onMessageReceived(from, line));
//            }
//        }
//    }
//
//    /**
//     * Closes the connection to the server.
//     */
//    public void close() {
//        try {
//            if (socket != null && !socket.isClosed()) {
//                socket.close();
//            }
//        } catch (IOException e) {
//            System.err.println("Error closing socket: " + e.getMessage());
//        }
//    }
//
//    public void clearCurrentUser() {
//        this.username = null;
//        this.currentController = null;
//        this.listeners.clear();
//    }
//
//    public void setUsername(String u) {
//        this.username = u;
//    }
//
//    /**
//     * Uploads a profile image file to the server.
//     */
//    public void uploadProfileImage(File file) {
//        try {
//            // Use a separate PrintWriter for this one-off transfer
//            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
//            writer.println("UPLOAD_PROFILE_PHOTO|" + username + "|" + file.getName() + "|" + file.length());
//
//            FileInputStream fis = new FileInputStream(file);
//            OutputStream socketOut = socket.getOutputStream();
//
//            byte[] buffer = new byte[4096];
//            int bytesRead;
//            while ((bytesRead = fis.read(buffer)) != -1) {
//                socketOut.write(buffer, 0, bytesRead);
//            }
//            socketOut.flush();
//            fis.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void acceptRequest(String requester) {
//        out.println("ACCEPT_REQUEST|" + username + "|" + requester);
//    }
//
//    public void logout(String username) {
//        out.println("LOGOUT|" + username);
//    }
//
//    public void declineRequest(String requester) {
//        out.println("DECLINE_REQUEST|" + username + "|" + requester);
//    }
//
//    public void sendRequest(String to) {
//        System.out.println("Sending: SEND_REQUEST|" + username + "|" + to);
//        out.println("SEND_REQUEST|" + username + "|" + to);
//        out.flush();
//        System.out.println("Error check: out.checkError() = " + out.checkError());
//    }
//
//    public void setCurrentController(Object controller) {
//        this.currentController = controller;
//    }
//}
package main.java.client;

import javafx.application.Platform;
import main.java.controller.FriendRequestsController;
import main.java.controller.ViewProfileController;
import main.java.user.Session;
import main.java.user.User;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages the client's connection to the server and dispatches incoming messages.
 * Group messages are now delivered with a "GROUP_MSG|" prefix so the UI can
 * distinguish them from one-to-one messages.
 */
public class ClientConnection {

    private static ClientConnection INSTANCE;
    private final Queue<String> pendingMessages = new LinkedList<>();
    private final ExecutorService dispatchPool = Executors.newSingleThreadExecutor();

    public static ClientConnection getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClientConnection();
        }
        return INSTANCE;
    }

    public interface MessageListener {
        void onMessageReceived(String from, String body);
    }

    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();

    public void registerListener(MessageListener l) {
        listeners.add(l);
        if (socket != null && !socket.isClosed() && username != null) {
            out.println("ONLINE|" + username);
        }
        synchronized (pendingMessages) {
            while (!pendingMessages.isEmpty()) {
                dispatch(pendingMessages.poll());
            }
        }
    }

    public void removeListener(MessageListener l) {
        listeners.remove(l);
    }

    private Socket socket;
    private PrintWriter out;
    private String username;
    private Object currentController;

    private ClientConnection() {}

    public boolean connect(String host, int port, String user) {
        if (socket != null && !socket.isClosed()) {
            System.out.println("Already connected, skipping new connection");
            return true;
        }
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            this.username = user;
            new Thread(this::listen, "ClientConnection-Reader").start();
            out.println("ONLINE|" + username);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
            return false;
        }
    }

    public void sendPrivateMessage(String to, String body, LocalDateTime timestamp) {
        String timestampStr = timestamp.toString();
        out.println("PRIVATE|" + username + "|" + to + "|" + timestampStr + "|" + body);
    }

    public void sendPrivateMessage(String to, String body) {
        String timestampStr = LocalDateTime.now().toString();
        out.println("PRIVATE|" + username + "|" + to + "|" + timestampStr + "|" + body);
    }

    /**
     * Sends a group chat message to the server.  The order is always:
     * GROUP_MSG|groupName|sender|timestamp|body.  The server forwards it
     * in exactly the same order.
     */
    public void sendGroupMessage(String group, String body, LocalDateTime timestamp) {
        String timestampStr = timestamp.toString();
        out.println("GROUP_MSG|" + group + "|" + username + "|" + timestampStr + "|" + body);
    }

    public void sendGroupMessage(String group, String body) {
        String timestampStr = LocalDateTime.now().toString();
        out.println("GROUP_MSG|" + group + "|" + username + "|" + timestampStr + "|" + body);
    }

    public void requestGroupList() {
        if (out != null && username != null) {
            out.println("GROUPS_REQUEST|" + username);
        }
    }

    public void createGroup(String groupName, List<String> members) {
        if (out == null || username == null) {
            return;
        }
        String memberCsv = String.join(",", members);
        out.println("CREATE_GROUP|" + username + "|" + groupName + "|" + memberCsv);
    }

    public void requestChatHistory(String username) {
        if (out != null) {
            out.println("ONLINE|" + username);
        }
    }

    public void requestGroupHistory(String username, String groupName) {
        if (out != null) {
            out.println("REQUEST_GROUP_HISTORY|" + username + "|" + groupName);
        }
    }

    private void listen() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("Client received: " + line);
                if (listeners.isEmpty()) {
                    synchronized (pendingMessages) {
                        pendingMessages.offer(line);
                    }
                } else {
                    dispatch(line);
                }
            }
        } catch (IOException ignored) {
            System.out.println("Connection closed or error in listen loop");
        }
    }

    /**
     * Dispatch a single line from the server.  Group and offline group messages
     * are prefaced with "GROUP_MSG|" so the UI can identify them.
     */
    private void dispatch(String line) {
        String[] p = line.split("\\|", 6);
        String header = p[0];

        if ("PRIVATE".equals(header) && p.length >= 5) {
            String from = p[1];
            String timestampStr = p[3];
            String msg = p[4];
            String body = timestampStr + "|" + msg;
            for (MessageListener l : listeners) {
                dispatchPool.submit(() -> l.onMessageReceived(from, body));
            }
        } else if ("GROUP_MSG".equals(header) && p.length >= 5) {
            String groupName = p[1];
            String sender = p[2];
            String timestampStr = p[3];
            String msg = p[4];
            // Prefix with GROUP_MSG so the controller knows this is a group chat
            String body = "GROUP_MSG|" + sender + "|" + timestampStr + "|" + msg;
            for (MessageListener l : listeners) {
                dispatchPool.submit(() -> l.onMessageReceived(groupName, body));
            }
        } else if ("GROUP_LIST".equals(header) && p.length >= 2) {
            String groupsCsv = p.length >= 2 ? p[1] : "";
            String body = "GROUP_LIST|" + groupsCsv;
            for (MessageListener l : listeners) {
                dispatchPool.submit(() -> l.onMessageReceived("", body));
            }
        } else if ("GROUP_CREATED".equals(header) && p.length >= 3) {
            String groupName = p[1];
            String creator = p[2];
            String members = p.length >= 4 ? p[3] : "";
            String body = "GROUP_CREATED|" + groupName + "|" + creator + "|" + members;
            for (MessageListener l : listeners) {
                dispatchPool.submit(() -> l.onMessageReceived("", body));
            }
        } else if ("CHAT_HISTORY_GROUP".equals(header) && p.length >= 4) {
            String groupName = p[1];
            String user = p[2];
            String payload = p[3];
            String body = "CHAT_HISTORY_GROUP|" + groupName + "|" + user + "|" + payload;
            for (MessageListener l : listeners) {
                dispatchPool.submit(() -> l.onMessageReceived("", body));
            }
        } else if ("OFFLINE_MSG".equals(header) && p.length >= 4) {
            // OFFLINE_MSG|conv|user|sender|timestamp|msg
            String conv = p[1];
            String user = (p.length >= 3 ? p[2] : "");
            if (!user.equals(username)) {
                return;
            }
            String sender, timestampStr, msgText;
            if (p.length >= 6) {
                sender = p[3];
                timestampStr = p[4];
                msgText = p[5];
            } else if (p.length == 5) {
                sender = p[3];
                timestampStr = LocalDateTime.now().toString();
                msgText = p[4];
            } else {
                System.err.println("Invalid OFFLINE_MSG format: " + line);
                return;
            }
            if (Session.getCurrentUser() != null &&
                    Session.getCurrentUser().getFriends().contains(conv)) {
                // friend offline message: timestamp|message
                String body = timestampStr + "|" + msgText;
                for (MessageListener l : listeners) {
                    dispatchPool.submit(() -> l.onMessageReceived(conv, body));
                }
            } else {
                // group offline message: prefix with GROUP_MSG
                String body = "GROUP_MSG|" + sender + "|" + timestampStr + "|" + msgText;
                for (MessageListener l : listeners) {
                    dispatchPool.submit(() -> l.onMessageReceived(conv, body));
                }
            }
        } else if ("GOT_FRIEND_REQUEST_FROM".equals(header) && p.length >= 2) {
            String from = p[1];
            Platform.runLater(() -> {
                Session.refreshCurrentUser();
                User currentUser = Session.getCurrentUser();
                if (currentController instanceof FriendRequestsController f) {
                    f.refresh();
                } else if (currentController instanceof ViewProfileController v) {
                    v.refreshProfile();
                }
            });
        } else if ("DECLINED_REQUEST_FROM".equals(header) && p.length >= 2) {
            String friend = p[1];
            Session.getCurrentUser().declineFriendRequest(User.loadUser(friend));
            Platform.runLater(() -> {
                if (currentController instanceof ViewProfileController v) {
                    v.refreshProfile();
                }
            });
        } else if ("ACCEPTED_REQUEST_FROM".equals(header) && p.length >= 2) {
            String friend = p[1];
            Session.getCurrentUser().acceptFriendRequest(User.loadUser(friend));
            Platform.runLater(() -> {
                if (currentController instanceof ViewProfileController v) {
                    v.refreshProfile();
                }
            });
        } else {
            // Unknown header: pass the whole message through
            String from = p.length > 1 ? p[1] : "";
            for (MessageListener l : listeners) {
                dispatchPool.submit(() -> l.onMessageReceived(from, line));
            }
        }
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }

    public void clearCurrentUser() {
        this.username = null;
        this.currentController = null;
        this.listeners.clear();
    }

    public void setUsername(String u) {
        this.username = u;
    }

    public void uploadProfileImage(File file) {
        try {
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println("UPLOAD_PROFILE_PHOTO|" + username + "|" +
                    file.getName() + "|" + file.length());
            FileInputStream fis = new FileInputStream(file);
            OutputStream socketOut = socket.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                socketOut.write(buffer, 0, bytesRead);
            }
            socketOut.flush();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptRequest(String requester) {
        out.println("ACCEPT_REQUEST|" + username + "|" + requester);
    }

    public void logout(String username) {
        out.println("LOGOUT|" + username);
    }

    public void declineRequest(String requester) {
        out.println("DECLINE_REQUEST|" + username + "|" + requester);
    }

    public void sendRequest(String to) {
        System.out.println("Sending: SEND_REQUEST|" + username + "|" + to);
        out.println("SEND_REQUEST|" + username + "|" + to);
        out.flush();
    }

    public void setCurrentController(Object controller) {
        this.currentController = controller;
    }
}
