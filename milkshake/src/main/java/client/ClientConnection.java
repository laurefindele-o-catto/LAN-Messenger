


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

    // Friend request listener interface
    public interface FriendListener {
        void onFriendRequestReceived(String from);
        void onFriendRequestAccepted(String from);
        void onFriendRequestDeclined(String from);
    }

    private final List<FriendListener> friendListeners = new CopyOnWriteArrayList<>();

    public void registerFriendListener(FriendListener l) {
        friendListeners.add(l);
    }

    public void removeFriendListener(FriendListener l) {
        friendListeners.remove(l);
    }


    private void dispatchRequests(String msg) {
        String[] parts = msg.split("\\|", 3);
        if (parts.length < 2) return;

        String header = parts[0];
        String from = parts[1];

        for (FriendListener l : friendListeners) {
            dispatchPool.submit(() -> {
                switch (header) {
                    case "ACCEPTED_REQUEST_FROM" -> l.onFriendRequestAccepted(from);
                    case "DECLINED_REQUEST_FROM" -> l.onFriendRequestDeclined(from);
                    case "GOT_FRIEND_REQUEST_FROM" -> l.onFriendRequestReceived(from);

                }
            });
        }
    }

    // Chat message listener interface
    public interface MessageListener {
        void onMessageReceived(String from, String body);
    }

    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();

    public void registerListener(MessageListener l) {
        listeners.add(l);
        // Inform server that client is online so it can send history/offline msgs
        if (socket != null && !socket.isClosed() && username != null) {
            out.println("ONLINE|" + username);
        }
        // Drain any pending messages that arrived before a listener was added
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
            System.out.println("Already connected, reusing socket");
            this.username = user;
            if (out != null) {
                out.println("ONLINE|" + username);
                out.flush();
            }
            return true;
        }
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            this.username = user;
            new Thread(this::listen, "ClientConnection-Reader").start();// ekhane listen thread shuru korchi
            out.println("ONLINE|" + username);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
            return false;
        }
    }

    // Send a private message with  timestamp
    public void sendPrivateMessage(String to, String body, LocalDateTime timestamp) {
        String timestampStr = timestamp.toString();
        out.println("PRIVATE|" + username + "|" + to + "|" + timestampStr + "|" + body);
    }

    // Send a private message using the current time
    public void sendPrivateMessage(String to, String body) {
        String timestampStr = LocalDateTime.now().toString();
        out.println("PRIVATE|" + username + "|" + to + "|" + timestampStr + "|" + body);
    }

    /*
     * Sends a group chat message to the server.
     * Format: GROUP_MSG|groupName|sender|timestamp|body
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
        if (out == null || username == null) return;
        String memberCsv = String.join(",", members);
        out.println("CREATE_GROUP|" + username + "|" + groupName + "|" + memberCsv);
    }

    // Asks the server to send chat history for all one-to-one conversations
    public void requestChatHistory(String username) {
        if (out != null) {
            out.println("ONLINE|" + username);
        }
    }

    // Asks the server to send chat history for a specific group
    public void requestGroupHistory(String username, String groupName) {
        if (out != null) {
            out.println("REQUEST_GROUP_HISTORY|" + username + "|" + groupName);
        }
    }

    /*
     * Requests the membership list for a specific group.  The server
     * responds with a line beginning with "GROUP_MEMBERS|" containing the
     * group name and a comma separated list of members.  The username of
     * the requester is included so the server can route the response to
     * the correct client.  If the group is unknown, an empty list will
     * be returned.
     *
     * @param groupName the name of the group whose members are desired
     */
    public void requestGroupMembers(String groupName) {
        if (out != null && username != null && groupName != null && !groupName.isEmpty()) {
            out.println("GROUP_MEMBERS_REQUEST|" + username + "|" + groupName);
        }
    }


    public void sendVideoCallRequest(String to) {
        if (out != null && username != null && to != null && !to.isEmpty()) {
            out.println("VIDEO_CALL_REQUEST|" + username + "|" + to);
            out.flush();
        }
    }


    public void sendVideoCallResponse(String to, boolean accept) {
        if (out != null && username != null && to != null && !to.isEmpty()) {
            String answer = accept ? "yes" : "no";
            out.println("VIDEO_CALL_RESPONSE|" + username + "|" + to + "|" + answer);
            out.flush();
        }
    }


    public void sendEndCall(String to) {
        if (out != null && username != null && to != null && !to.isEmpty()) {
            out.println("END_CALL|" + username + "|" + to);
            out.flush();
        }
    }


    public void sendVideoFrame(String to, String base64) {
        if (out != null && username != null && to != null && !to.isEmpty() && base64 != null) {
            out.println("VIDEO_FRAME|" + username + "|" + to + "|" + base64);
            out.flush();
        }
    }


    public void sendAudioFrame(String to, String base64) {
        if (out != null && username != null && to != null && !to.isEmpty() && base64 != null) {
            out.println("AUDIO_FRAME|" + username + "|" + to + "|" + base64);
            out.flush();
        }
    }

    /*
     * Listens on the socket for incoming lines and delegates them.
     * Runs in its own thread.
     */
    private void listen() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("Client received: " + line);

                // Always try to handle friend request notifications
                dispatchRequests(line);

                // If no chat listeners yet, queue messages for later
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

    /*
     * Dispatches a single line from the server.  Group and offline group messages
     * are prefaced with "GROUP_MSG|" so the UI can identify them.
     */
    // dispatchPool.execute use kora jeto but immediate exceptions throw korchilo.

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
        } else if ("GROUP_MEMBERS".equals(header) && p.length >= 3) {
            // GROUP_MEMBERS|groupName|m1,m2,m3
            String groupName  = p[1];
            String memberCsv = p[2];
            String body      = "GROUP_MEMBERS|" + groupName + "|" + memberCsv;
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
                return; // not for this user
            }
            String sender;
            String timestampStr;
            String msgText;
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
        } else {
            // Unknown header: check for video call commands
            if ("VIDEO_CALL_REQUEST".equals(header) && p.length >= 2) {
                String caller = p[1];
                for (MessageListener l : listeners) {
                    dispatchPool.submit(() -> l.onMessageReceived(caller, "VIDEO_CALL_REQUEST"));
                }
            } else if ("VIDEO_CALL_RESPONSE".equals(header) && p.length >= 3) {
                String responder = p[1];
                String answer = p[2];
                for (MessageListener l : listeners) {
                    dispatchPool.submit(() -> l.onMessageReceived(responder, "VIDEO_CALL_RESPONSE|" + answer));
                }
            } else if ("VIDEO_FRAME".equals(header) && p.length >= 3) {
                String sender = p[1];
                String data = p[2];
                for (MessageListener l : listeners) {
                    dispatchPool.submit(() -> l.onMessageReceived(sender, "VIDEO_FRAME|" + data));
                }
            } else if ("AUDIO_FRAME".equals(header) && p.length >= 3) {
                // Forward audio frames to listeners so they can be routed to the video call controller.
                String sender = p[1];
                String data = p[2];
                for (MessageListener l : listeners) {
                    dispatchPool.submit(() -> l.onMessageReceived(sender, "AUDIO_FRAME|" + data));
                }
            } else if ("END_CALL".equals(header) && p.length >= 2) {
                String sender = p[1];
                for (MessageListener l : listeners) {
                    dispatchPool.submit(() -> l.onMessageReceived(sender, "END_CALL"));
                }
            } else {
                // Unknown header: pass the whole message through
                String from = p.length > 1 ? p[1] : "";
                for (MessageListener l : listeners) {
                    dispatchPool.submit(() -> l.onMessageReceived(from, line));
                }
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

    /* Clears the current user and listeners on logout. */
    public void clearCurrentUser() {
        this.username = null;
        this.currentController = null;
        this.listeners.clear();
        this.friendListeners.clear();
    }

    public void setUsername(String u) {
        this.username = u;
    }


    public void uploadProfileImage(File file) {
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            // Write header (newline terminated to make it parseable)
            String header =
                    "UPLOAD_PROFILE_PHOTO|" + username + "|" + file.getName() + "|" + file.length() + "\n";
            dos.writeBytes(header);
            dos.flush();

            // Send file bytes
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptRequest(String requester) {
        out.println("ACCEPT_REQUEST|" + username + "|" + requester);
    }

    public void logout(String username) {
        if (out != null) {
            out.println("LOGOUT|" + username);
            out.flush();
        }
        // Close the current socket so that connect() will establish a new one next time.
        close();
        clearCurrentUser();
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
