

package main.java.server;

import main.java.user.Database;
import main.java.user.User;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;

/**
 * Handles communication with a single client.  Uses a unified protocol for
 * private and group messages.  Group messages are expected in the format:
 * GROUP_MSG|groupName|sender|timestamp|message and are broadcast to all
 * members (except the sender) in that same order.
 */
public class ClientHandler implements Runnable {

    private static final Map<String, PrintWriter> onlineWriters = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> groupMembers = new ConcurrentHashMap<>();
    private static final String CHAT_FOLDER = "chats";

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Server received: " + line);
                String[] parts = line.split("\\|", 5);
                switch (parts[0]) {
                    case "ONLINE":
                        if (parts.length >= 2) {
                            username = parts[1];
                            onlineWriters.put(username, out);
                            out.println("ONLINE_OK");
                            sendOfflineMessages(username, out);
                            sendChatHistory(username, out);
                        }
                        break;

                    case "LOGOUT":
                        if (parts.length >= 2) {
                            String logoutUser = parts[1];
                            onlineWriters.remove(logoutUser);
                            System.out.println(logoutUser + " has logged out.");
                        }
                        break;

                    case "SIGNUP":
                        if (parts.length >= 3) {
                            String newUser = parts[1];
                            String pass = parts[2];
                            if (userExists(newUser)) {
                                out.println("ERROR|Username already taken");
                            } else {
                                saveCredentials(newUser, pass);
                                onlineWriters.put(newUser, out);
                                out.println("SUCCESS");
                            }
                        }
                        break;

                    case "LOGIN":
                        if (parts.length >= 3) {
                            String u = parts[1];
                            String p = parts[2];
                            if (verifyCredentials(u, p)) {
                                username = u;
                                onlineWriters.put(username, out);
                                out.println("SUCCESS");
                                sendOfflineMessages(username, out);
                                sendChatHistory(username, out);
                            } else {
                                out.println("ERROR|Invalid credentials");
                            }
                        }
                        break;

                    case "PRIVATE":
                        if (parts.length >= 5) {
                            String from = parts[1];
                            String to = parts[2];
                            String timestampStr = parts[3];
                            String body = parts[4];
                            if (from.equals(to)) break;
                            updateChatHistory(from, to, timestampStr, body);
                            PrintWriter target = onlineWriters.get(to);
                            String fullMessage = "PRIVATE|" + from + "|" + to + "|" + timestampStr + "|" + body;
                            if (target != null) {
                                target.println(fullMessage);
                            } else {
                                saveOfflineMessage(to, from, timestampStr, body);
                            }
                        } else if (parts.length == 4) { // Handle legacy format
                            String from = parts[1];
                            String to = parts[2];
                            String body = parts[3];
                            if (from.equals(to)) break;
                            String timestampStr = LocalDateTime.now().toString();
                            updateChatHistory(from, to, timestampStr, body);
                            PrintWriter target = onlineWriters.get(to);
                            String fullMessage = "PRIVATE|" + from + "|" + to + "|" + timestampStr + "|" + body;
                            if (target != null) {
                                target.println(fullMessage);
                            } else {
                                saveOfflineMessage(to, from, timestampStr, body);
                            }
                        }
                        break;

                    case "CREATE_GROUP":
                        // Expected format: CREATE_GROUP|creator|groupName|member1,member2,...
                        // The creator will always be included automatically. Member list can be empty.
                        if (parts.length >= 3) {
                            String creator = parts[1];
                            String groupName = parts[2];
                            String memberCsv = parts.length >= 4 ? parts[3] : "";
                            // Split comma-separated members and add them to the set
                            Set<String> members = Collections.newSetFromMap(new ConcurrentHashMap<>());
                            if (memberCsv != null && !memberCsv.isEmpty()) {
                                String[] memArr = memberCsv.split(",");
                                for (String m : memArr) {
                                    String trimmed = m.trim();
                                    if (!trimmed.isEmpty()) {
                                        members.add(trimmed);
                                    }
                                }
                            }
                            // Always include the creator
                            members.add(creator);
                            if (groupMembers.containsKey(groupName)) {
                                out.println("ERROR|Group already exists");
                            } else {
                                groupMembers.put(groupName, members);
                                System.out.println("Group created: " + groupName + " by " + creator + " with members " + members);
                                // Notify all online members of the new group. Offline members will learn through group list on next login.
                                String memberListCsv = String.join(",", members);
                                String notifyMsg = "GROUP_CREATED|" + groupName + "|" + creator + "|" + memberListCsv;
                                for (String m : members) {
                                    PrintWriter target = onlineWriters.get(m);
                                    if (target != null) {
                                        target.println(notifyMsg);
                                    }
                                }
                            }
                        }
                        break;

                    case "GROUP_MSG":
                        // Format: GROUP_MSG|groupName|sender|timestamp|message
                        if (parts.length >= 5) {
                            String groupName = parts[1];
                            String senderUser = parts[2];
                            String timestampStr = parts[3];
                            String body = parts[4];
                            Set<String> members = groupMembers.get(groupName);
                            if (members == null) {
                                out.println("ERROR|No such group");
                                break;
                            }
                            if (!members.contains(senderUser)) {
                                out.println("ERROR|Not a member of " + groupName);
                                break;
                            }
                            // Persist and broadcast
                            String record = senderUser + "|" + timestampStr + "|" + body;
                            for (String member : members) {
                                appendToChatFile(member, groupName, record);
                            }
                            String full = "GROUP_MSG|" + groupName + "|" + senderUser + "|" + timestampStr + "|" + body;
                            for (String member : members) {
                                if (!member.equals(senderUser)) {
                                    PrintWriter pw = onlineWriters.get(member);
                                    if (pw != null) pw.println(full);
                                    else saveOfflineMessage(member, groupName, senderUser, timestampStr, body);
                                }
                            }
                        }
                        break;


                    case "GROUPS_REQUEST":
                        // Respond with a comma-separated list of groups that this user belongs to
                        if (parts.length >= 2) {
                            String userReq = parts[1];
                            List<String> groupsOfUser = new ArrayList<>();
                            for (Map.Entry<String, Set<String>> entry : groupMembers.entrySet()) {
                                if (entry.getValue().contains(userReq)) {
                                    groupsOfUser.add(entry.getKey());
                                }
                            }
                            String csv = String.join(",", groupsOfUser);
                            // Use GROUP_LIST prefix to be compatible with client expectations
                            out.println("GROUP_LIST|" + csv);
                        }
                        break;

                    case "SEND_REQUEST":
                        if (parts.length >= 3) {
                            String from = parts[1];
                            String to = parts[2];
                            System.out.println("Processing SEND_REQUEST from " + from + " to " + to);
                            User sender = Database.loadUser(from);
                            User receiver = Database.loadUser(to);
                            if (sender != null && receiver != null) {
                                sender.sendFriendRequest(receiver);
                                Database.saveUser(sender);
                                Database.saveUser(receiver);
                                System.out.println("Friend request saved: " + from + " -> " + to);
                                if (onlineWriters.containsKey(to)) {
                                    onlineWriters.get(to).println("GOT_FRIEND_REQUEST_FROM|" + from);
                                    System.out.println("Notified " + to + " of friend request from " + from);
                                } else {
                                    System.out.println("Receiver " + to + " is offline, not notified");
                                }
                            } else {
                                System.out.println("Sender or receiver not found: sender=" + from + ", receiver=" + to);
                            }
                        }
                        break;

                    case "ACCEPT_REQUEST":
                        if (parts.length >= 3) {
                            String user = parts[1];
                            String from = parts[2];
                            User u1 = Database.loadUser(user);
                            User u2 = Database.loadUser(from);
                            if (u1 != null && u2 != null) {
                                u1.acceptFriendRequest(u2);
                                Database.saveUser(u1);
                                Database.saveUser(u2);
                                System.out.println("Friend request accepted: " + user + " accepted " + from);
                                if (onlineWriters.containsKey(u2.getUsername())) {
                                    onlineWriters.get(u2.getUsername()).println("ACCEPTED_REQUEST_FROM|" + user);
                                    System.out.println("Notified " + u2.getUsername() + " of accepted request from " + user);
                                }
                            } else {
                                System.out.println("User not found for ACCEPT_REQUEST: user=" + user + ", from=" + from);
                            }
                        }
                        break;

                    case "DECLINE_REQUEST":
                        if (parts.length >= 3) {
                            String user = parts[1];
                            String from = parts[2];
                            User u1 = Database.loadUser(user);
                            User u2 = Database.loadUser(from);
                            if (u1 != null && u2 != null) {
                                u1.declineFriendRequest(u2);
                                Database.saveUser(u1);
                                Database.saveUser(u2);
                                System.out.println("Friend request declined: " + user + " declined " + from);
                                if (onlineWriters.containsKey(u2.getUsername())) {
                                    onlineWriters.get(u2.getUsername()).println("DECLINED_REQUEST_FROM|" + user);
                                    System.out.println("Notified " + u2.getUsername() + " of declined request from " + user);
                                }
                            } else {
                                System.out.println("User not found for DECLINE_REQUEST: user=" + user + ", from=" + from);
                            }
                        }
                        break;
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (username != null) {
                onlineWriters.remove(username);
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    // Helper methods for user management, chat history, and offline message handling follow…
    // These methods include: userExists, saveCredentials, verifyCredentials,
    // updateChatHistory, appendToChatFile, saveOfflineMessage (for both private and group),
    // sendOfflineMessages, and sendChatHistory.
    private static final File CRED_FILE = new File("users.txt");

    private boolean userExists(String user) throws IOException {
        if (!CRED_FILE.exists()) return false;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(CRED_FILE), StandardCharsets.UTF_8))) {
            String l;
            while ((l = br.readLine()) != null) {
                if (l.split("\\|", 2)[0].equals(user)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void saveCredentials(String user, String pass) throws IOException {
        if (!CRED_FILE.exists()) {
            CRED_FILE.createNewFile();
        }
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(CRED_FILE, true), StandardCharsets.UTF_8))) {
            pw.println(user + "|" + pass);
        }
    }

    private boolean verifyCredentials(String user, String pass) throws IOException {
        if (!CRED_FILE.exists()) return false;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(CRED_FILE), StandardCharsets.UTF_8))) {
            String l;
            while ((l = br.readLine()) != null) {
                String[] p = l.split("\\|", 2);
                if (p.length == 2 && p[0].equals(user) && p[1].equals(pass)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void updateChatHistory(String from, String to, String timestampStr, String message) {
        String line = from + "|" + timestampStr + "|" + message;
        appendToChatFile(to, from, line);
        appendToChatFile(from, to, line);
    }

    private void appendToChatFile(String user, String friend, String line) {
        File dir = new File(CHAT_FOLDER + "/" + user);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File chatFile = new File(dir, friend + ".txt");
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(chatFile, true), StandardCharsets.UTF_8))) {
            writer.println(line);
        } catch (IOException e) {
            System.err.println("Failed to append to chat file for " + user + ": " + e.getMessage());
        }
    }

    private void saveOfflineMessage(String to, String from, String timestampStr, String message) {
        File dir = new File(CHAT_FOLDER + "/" + to);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File offlineFile = new File(dir, from + ".offline.txt");
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(offlineFile, true), StandardCharsets.UTF_8))) {
            writer.println(from + "|" + timestampStr + "|" + message);
        } catch (IOException e) {
            System.err.println("Failed to save offline message for " + to + ": " + e.getMessage());
        }
    }

    private void saveOfflineMessage(String to, String group, String from, String timestampStr, String message) {
        File dir = new File(CHAT_FOLDER + "/" + to);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File offlineFile = new File(dir, group + ".offline.txt");
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(offlineFile, true), StandardCharsets.UTF_8))) {
            writer.println(from + "|" + timestampStr + "|" + message);
        } catch (IOException e) {
            System.err.println("Failed to save offline message for " + to + ": " + e.getMessage());
        }
    }

    private void sendOfflineMessages(String username, PrintWriter writer) {
        File folder = new File(CHAT_FOLDER + "/" + username);
        if (!folder.exists()) return;
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".offline.txt"));
        if (files == null) return;
        for (File file : files) {
            String friend = file.getName().replace(".offline.txt", "");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|", 3);
                    String sender, timestampStr, msgText;
                    if (parts.length == 3) {
                        sender = parts[0];
                        timestampStr = parts[1];
                        msgText = parts[2];
                    } else if (parts.length >= 2) {
                        sender = parts[0];
                        timestampStr = LocalDateTime.now().toString();
                        msgText = parts[1];
                    } else {
                        System.err.println("Invalid offline message format in " + file.getName() + ": " + line);
                        continue;
                    }
                    String msg = "OFFLINE_MSG|" + friend + "|" + username + "|" + sender + "|" + timestampStr + "|" + msgText;
                    writer.println(msg);
                    System.out.println("Sent offline message to " + username + ": " + msg);
                }
                file.delete();
            } catch (IOException e) {
                System.err.println("Error sending offline messages for " + username + ": " + e.getMessage());
            }
        }
    }

    private void sendChatHistory(String username, PrintWriter writer) {
        File dir = new File(CHAT_FOLDER + "/" + username);
        if (!dir.exists()) return;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") && !name.endsWith(".offline.txt"));
        if (files == null) return;
        for (File file : files) {
            String friend = file.getName().replace(".txt", "");
            if (friend.equals(username)) {
                file.delete();
                continue;
            }
            List<String> history = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|", 3);
                    if (parts.length == 3) {
                        history.add(line);
                    } else if (parts.length >= 2) {
                        history.add(parts[0] + "|" + LocalDateTime.now().toString() + "|" + parts[1]);
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to read chat history for " + username + " and " + friend + ": " + e.getMessage());
            }
            if (!history.isEmpty()) {
                String payload = String.join(";;;", history);
                String lineToSend = "CHAT_HISTORY|" + friend + "|" + username + "|" + payload;
                writer.println(lineToSend);
                System.out.println("Sent chat history to " + username + " for " + friend);
            }
        }
    }
}


