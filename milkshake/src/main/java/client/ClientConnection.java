package main.java.client;

import javafx.application.Platform;
import main.java.controller.FriendRequestsController;
import main.java.controller.ViewProfileController;
import main.java.user.Session;
import main.java.user.User;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

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

    public void requestChatHistory(String username) {
        if (out != null) {
            out.println("ONLINE|" + username);
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
        } else if ("OFFLINE_MSG".equals(header) && p.length >= 4) {
            String from = p[1];
            String[] messageParts = p[3].split("\\|", 3);
            String msg = messageParts.length >= 2 ? messageParts[1] + "|" + messageParts[2] : p[3];
            for (MessageListener l : listeners) {
                dispatchPool.submit(() -> l.onMessageReceived(from, msg));
            }
        } else if ("GOT_FRIEND_REQUEST_FROM".equals(header) && p.length >= 2) {
            String from = p[1];
            System.out.println("Processing GOT_FRIEND_REQUEST_FROM from " + from + " for user " + username);
            Platform.runLater(() -> {
                Session.refreshCurrentUser();
                User currentUser = Session.getCurrentUser();
                System.out.println("Updated session for " + currentUser.getUsername() + " with new friend request from " + from);
                System.out.println("Current friend requests received: " + currentUser.getFriendRequestsReceived());
                if (currentController instanceof FriendRequestsController f) {
                    System.out.println("User is on FriendRequests screen, refreshing");
                    f.refresh();
                } else if (currentController instanceof ViewProfileController v) {
                    System.out.println("User is on ViewProfile screen, refreshing");
                    v.refreshProfile();
                } else {
                    System.out.println("User is on a different screen: " + (currentController != null ? currentController.getClass().getSimpleName() : "null"));
                }
            });
        } else if ("DECLINED_REQUEST_FROM".equals(header) && p.length >= 2) {
            String friend = p[1];
            Session.getCurrentUser().declineFriendRequest(User.loadUser(friend));
            Platform.runLater(() -> {
                if (currentController instanceof ViewProfileController v) {
                    System.out.println("Processing DECLINED_REQUEST_FROM from " + friend + ", refreshing ViewProfile");
                    v.refreshProfile();
                }
            });
        } else if ("ACCEPTED_REQUEST_FROM".equals(header) && p.length >= 2) {
            String friend = p[1];
            Session.getCurrentUser().acceptFriendRequest(User.loadUser(friend));
            Platform.runLater(() -> {
                if (currentController instanceof ViewProfileController v) {
                    System.out.println("Processing ACCEPTED_REQUEST_FROM from " + friend + ", refreshing ViewProfile");
                    v.refreshProfile();
                }
            });
        } else {
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
            writer.println("UPLOAD_PROFILE_PHOTO|" + username + "|" + file.getName() + "|" + file.length());

            FileInputStream fis = new FileInputStream(file);
            OutputStream out = socket.getOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
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
        System.out.println("Error check: out.checkError() = " + out.checkError());
    }

    public void setCurrentController(Object controller) {
        this.currentController = controller;
    }
}