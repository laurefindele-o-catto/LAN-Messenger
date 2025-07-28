//package main.java.server;
//
//import main.java.user.Database;
//import main.java.user.User;
//
//import java.io.*;
//import java.net.Socket;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * Handles communication with a single client.  Uses a unified protocol for
// * private and group messages.  Group messages are expected in the format:
// * GROUP_MSG|groupName|sender|timestamp|message and are broadcast to all
// * members (except the sender) in that same order.
// */
//public class ClientHandler implements Runnable {
//
//    private static final Map<String, PrintWriter> onlineWriters = new ConcurrentHashMap<>();
//    private static final Map<String, Set<String>> groupMembers = new ConcurrentHashMap<>();
//
//    private static final String CHAT_FOLDER = "chats";
//    /**
//     * Root folder where group membership files are stored.  Each group
//     * corresponds to a file named <group>.members containing one username
//     * per line.  Persisting memberships allows groups to survive server
//     * restarts so they remain available the next time the server starts.
//     */
//    private static final String GROUP_FOLDER = "groups";
//
//    // Static initializer to load any previously saved group memberships.  This
//    // block executes when the class is first loaded, ensuring the
//    // groupMembers map is populated before any handler instances process
//    // requests.
//    static {
//        loadGroupMembersFromDisk();
//    }
//
//    private final Socket socket;
//    private BufferedReader in;
//    private PrintWriter out;
//    private String username;
//
//    public ClientHandler(Socket socket) {
//        this.socket = socket;
//    }
//
//    @Override
//    public void run() {
//        try {
//            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
//            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
//
//            String line;
//            while ((line = in.readLine()) != null) {
//                System.out.println("Server received: " + line);
//                String[] parts = line.split("\\|", 5);
//                switch (parts[0]) {
//                    case "ONLINE":
//                        if (parts.length >= 2) {
//                            username = parts[1];
//                            onlineWriters.put(username, out);
//                            out.println("ONLINE_OK");
//                            sendOfflineMessages(username, out);
//                            sendChatHistory(username, out);
//                        }
//                        break;
//
//                    case "LOGOUT":
//                        if (parts.length >= 2) {
//                            String logoutUser = parts[1];
//                            onlineWriters.remove(logoutUser);
//                            System.out.println(logoutUser + " has logged out.");
//                        }
//                        break;
//
//                    case "SIGNUP":
//                        if (parts.length >= 3) {
//                            String newUser = parts[1];
//                            String pass = parts[2];
//                            if (userExists(newUser)) {
//                                out.println("ERROR|Username already taken");
//                            } else {
//                                saveCredentials(newUser, pass);
//                                onlineWriters.put(newUser, out);
//                                out.println("SUCCESS");
//                            }
//                        }
//                        break;
//
//                    case "LOGIN":
//                        if (parts.length >= 3) {
//                            String u = parts[1];
//                            String p = parts[2];
//                            if (verifyCredentials(u, p)) {
//                                username = u;
//                                onlineWriters.put(username, out);
//                                out.println("SUCCESS");
//                                sendOfflineMessages(username, out);
//                                sendChatHistory(username, out);
//                            } else {
//                                out.println("ERROR|Invalid credentials");
//                            }
//                        }
//                        break;
//
//                    case "PRIVATE":
//                        if (parts.length >= 5) {
//                            String from = parts[1];
//                            String to = parts[2];
//                            String timestampStr = parts[3];
//                            String body = parts[4];
//                            if (from.equals(to)) break;
//                            updateChatHistory(from, to, timestampStr, body);
//                            PrintWriter target = onlineWriters.get(to);
//                            String fullMessage = "PRIVATE|" + from + "|" + to + "|" + timestampStr + "|" + body;
//                            if (target != null) {
//                                target.println(fullMessage);
//                            } else {
//                                saveOfflineMessage(to, from, timestampStr, body);
//                            }
//                        } else if (parts.length == 4) {
//                            String from = parts[1];
//                            String to = parts[2];
//                            String body = parts[3];
//                            if (from.equals(to)) break;
//                            String timestampStr = LocalDateTime.now().toString();
//                            updateChatHistory(from, to, timestampStr, body);
//                            PrintWriter target = onlineWriters.get(to);
//                            String fullMessage = "PRIVATE|" + from + "|" + to + "|" + timestampStr + "|" + body;
//                            if (target != null) {
//                                target.println(fullMessage);
//                            } else {
//                                saveOfflineMessage(to, from, timestampStr, body);
//                            }
//                        }
//                        break;
//
//                    case "CREATE_GROUP":
//                        // Expected format: CREATE_GROUP|creator|groupName|member1,member2,...
//                        if (parts.length >= 3) {
//                            String creator = parts[1];
//                            String groupName = parts[2];
//                            String memberCsv = parts.length >= 4 ? parts[3] : "";
//                            // Split comma-separated members and add them to the set
//                            Set<String> members = Collections.newSetFromMap(new ConcurrentHashMap<>());
//                            if (memberCsv != null && !memberCsv.isEmpty()) {
//                                String[] memArr = memberCsv.split(",");
//                                for (String m : memArr) {
//                                    String trimmed = m.trim();
//                                    if (!trimmed.isEmpty()) {
//                                        members.add(trimmed);
//                                    }
//                                }
//                            }
//                            // Always include the creator
//                            members.add(creator);
//                            if (groupMembers.containsKey(groupName)) {
//                                out.println("ERROR|Group already exists");
//                            } else {
//                                groupMembers.put(groupName, members);
//                                System.out.println("Group created: " + groupName + " by " + creator + " with members " + members);
//                                // Persist the new group's membership to disk so it survives server restarts
//                                saveGroupMembersToDisk(groupName, members);
//                                // Notify all online members of the new group
//                                String memberListCsv = String.join(",", members);
//                                String notifyMsg = "GROUP_CREATED|" + groupName + "|" + creator + "|" + memberListCsv;
//                                for (String m : members) {
//                                    PrintWriter target = onlineWriters.get(m);
//                                    if (target != null) {
//                                        target.println(notifyMsg);
//                                    }
//                                }
//                            }
//                        }
//                        break;
//
//                    case "GROUP_MSG":
//                        // Format: GROUP_MSG|groupName|sender|timestamp|message
//                        if (parts.length >= 5) {
//                            String groupName = parts[1];
//                            String senderUser = parts[2];
//                            String timestampStr = parts[3];
//                            String body = parts[4];
//                            Set<String> members = groupMembers.get(groupName);
//                            if (members == null) {
//                                out.println("ERROR|No such group");
//                                break;
//                            }
//                            if (!members.contains(senderUser)) {
//                                out.println("ERROR|Not a member of " + groupName);
//                                break;
//                            }
//                            // Persist and broadcast
//                            String record = senderUser + "|" + timestampStr + "|" + body;
//                            for (String member : members) {
//                                appendToChatFile(member, groupName, record);
//                            }
//                            String full = "GROUP_MSG|" + groupName + "|" + senderUser + "|" + timestampStr + "|" + body;
//                            for (String member : members) {
//                                if (!member.equals(senderUser)) {
//                                    PrintWriter pw = onlineWriters.get(member);
//                                    if (pw != null) pw.println(full);
//                                    else saveOfflineMessage(member, groupName, senderUser, timestampStr, body);
//                                }
//                            }
//                        }
//                        break;
//
//                    case "GROUPS_REQUEST":
//                        // Respond with a comma-separated list of groups that this user belongs to
//                        if (parts.length >= 2) {
//                            String userReq = parts[1];
//                            List<String> groupsOfUser = new ArrayList<>();
//                            for (Map.Entry<String, Set<String>> entry : groupMembers.entrySet()) {
//                                if (entry.getValue().contains(userReq)) {
//                                    groupsOfUser.add(entry.getKey());
//                                }
//                            }
//                            String csv = String.join(",", groupsOfUser);
//                            out.println("GROUP_LIST|" + csv);
//                        }
//                        break;
//                    case "SEND_REQUEST":
//                        if (parts.length >= 3) {
//                            String from = parts[1];
//                            String to = parts[2];
//                            System.out.println("Processing SEND_REQUEST from " + from + " to " + to);
//                            User sender = Database.loadUser(from);
//                            User receiver = Database.loadUser(to);
//                            if (sender != null && receiver != null) {
//                                sender.sendFriendRequest(receiver);
//                                Database.saveUser(sender);
//                                Database.saveUser(receiver);
//                                System.out.println("Friend request saved: " + from + " -> " + to);
//                                if (onlineWriters.containsKey(to)) {
//                                    onlineWriters.get(to).println("GOT_FRIEND_REQUEST_FROM|" + from);
//                                    System.out.println("Notified " + to + " of friend request from " + from);
//                                } else {
//                                    System.out.println("Receiver " + to + " is offline, not notified");
//                                }
//                            } else {
//                                System.out.println("Sender or receiver not found: sender=" + from + ", receiver=" + to);
//                            }
//                        }
//                        break;
//
//                    case "ACCEPT_REQUEST":
//                        if (parts.length >= 3) {
//                            String user = parts[1];
//                            String from = parts[2];
//                            User u1 = Database.loadUser(user);
//                            User u2 = Database.loadUser(from);
//                            if (u1 != null && u2 != null) {
//                                u1.acceptFriendRequest(u2);
//                                Database.saveUser(u1);
//                                Database.saveUser(u2);
//                                System.out.println("Friend request accepted: " + user + " accepted " + from);
//                                if (onlineWriters.containsKey(u2.getUsername())) {
//                                    onlineWriters.get(u2.getUsername()).println("ACCEPTED_REQUEST_FROM|" + user);
//                                    System.out.println("Notified " + u2.getUsername() + " of accepted request from " + user);
//                                }
//                            } else {
//                                System.out.println("User not found for ACCEPT_REQUEST: user=" + user + ", from=" + from);
//                            }
//                        }
//                        break;
//
//                    case "DECLINE_REQUEST":
//                        if (parts.length >= 3) {
//                            String user = parts[1];
//                            String from = parts[2];
//                            User u1 = Database.loadUser(user);
//                            User u2 = Database.loadUser(from);
//                            if (u1 != null && u2 != null) {
//                                u1.declineFriendRequest(u2);
//                                Database.saveUser(u1);
//                                Database.saveUser(u2);
//                                System.out.println("Friend request declined: " + user + " declined " + from);
//                                if (onlineWriters.containsKey(u2.getUsername())) {
//                                    onlineWriters.get(u2.getUsername()).println("DECLINED_REQUEST_FROM|" + user);
//                                    System.out.println("Notified " + u2.getUsername() + " of declined request from " + user);
//                                }
//                            } else {
//                                System.out.println("User not found for DECLINE_REQUEST: user=" + user + ", from=" + from);
//                            }
//                        }
//                        break;
//                    // ... (other cases remain unchanged, including friend requests) ...
//                }
//            }
//        } catch (IOException ignored) {
//        } finally {
//            if (username != null) {
//                onlineWriters.remove(username);
//            }
//            try {
//                socket.close();
//            } catch (IOException ignored) {
//            }
//        }
//    }
//
//    // User credential management (userExists, saveCredentials, verifyCredentials)
//    // and chat history methods (updateChatHistory, appendToChatFile, saveOfflineMessage,
//    // sendOfflineMessages, sendChatHistory) remain unchanged.
//    private static final File CRED_FILE = new File("users.txt");
//
//    private boolean userExists(String user) throws IOException {
//        if (!CRED_FILE.exists()) return false;
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(CRED_FILE), StandardCharsets.UTF_8))) {
//            String l;
//            while ((l = br.readLine()) != null) {
//                if (l.split("\\|", 2)[0].equals(user)) {
//                    return true;
//                }
//            }
//            return false;
//        }
//    }
//
//    private void saveCredentials(String user, String pass) throws IOException {
//        if (!CRED_FILE.exists()) {
//            CRED_FILE.createNewFile();
//        }
//        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(CRED_FILE, true), StandardCharsets.UTF_8))) {
//            pw.println(user + "|" + pass);
//        }
//    }
//
//    private boolean verifyCredentials(String user, String pass) throws IOException {
//        if (!CRED_FILE.exists()) return false;
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(CRED_FILE), StandardCharsets.UTF_8))) {
//            String l;
//            while ((l = br.readLine()) != null) {
//                String[] p = l.split("\\|", 2);
//                if (p.length == 2 && p[0].equals(user) && p[1].equals(pass)) {
//                    return true;
//                }
//            }
//            return false;
//        }
//    }
//
//    private void updateChatHistory(String from, String to, String timestampStr, String message) {
//        String line = from + "|" + timestampStr + "|" + message;
//        appendToChatFile(to, from, line);
//        appendToChatFile(from, to, line);
//    }
//
//    private void appendToChatFile(String user, String friend, String line) {
//        File dir = new File(CHAT_FOLDER + "/" + user);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        File chatFile = new File(dir, friend + ".txt");
//        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(chatFile, true), StandardCharsets.UTF_8))) {
//            writer.println(line);
//        } catch (IOException e) {
//            System.err.println("Failed to append to chat file for " + user + ": " + e.getMessage());
//        }
//    }
//
//    private void saveOfflineMessage(String to, String from, String timestampStr, String message) {
//        File dir = new File(CHAT_FOLDER + "/" + to);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        File offlineFile = new File(dir, from + ".offline.txt");
//        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(offlineFile, true), StandardCharsets.UTF_8))) {
//            writer.println(from + "|" + timestampStr + "|" + message);
//        } catch (IOException e) {
//            System.err.println("Failed to save offline message for " + to + ": " + e.getMessage());
//        }
//    }
//
//    private void saveOfflineMessage(String to, String group, String from, String timestampStr, String message) {
//        File dir = new File(CHAT_FOLDER + "/" + to);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        File offlineFile = new File(dir, group + ".offline.txt");
//        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(offlineFile, true), StandardCharsets.UTF_8))) {
//            writer.println(from + "|" + timestampStr + "|" + message);
//        } catch (IOException e) {
//            System.err.println("Failed to save offline message for " + to + ": " + e.getMessage());
//        }
//    }
//
//    private void sendOfflineMessages(String username, PrintWriter writer) {
//        File folder = new File(CHAT_FOLDER + "/" + username);
//        if (!folder.exists()) return;
//        File[] files = folder.listFiles((dir, name) -> name.endsWith(".offline.txt"));
//        if (files == null) return;
//        for (File file : files) {
//            String friend = file.getName().replace(".offline.txt", "");
//            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
//                String line;
//                while ((line = br.readLine()) != null) {
//                    String[] parts = line.split("\\|", 3);
//                    String sender, timestampStr, msgText;
//                    if (parts.length == 3) {
//                        sender = parts[0];
//                        timestampStr = parts[1];
//                        msgText = parts[2];
//                    } else if (parts.length >= 2) {
//                        sender = parts[0];
//                        timestampStr = LocalDateTime.now().toString();
//                        msgText = parts[1];
//                    } else {
//                        System.err.println("Invalid offline message format in " + file.getName() + ": " + line);
//                        continue;
//                    }
//                    String msg = "OFFLINE_MSG|" + friend + "|" + username + "|" + sender + "|" + timestampStr + "|" + msgText;
//                    writer.println(msg);
//                    System.out.println("Sent offline message to " + username + ": " + msg);
//                }
//                file.delete();
//            } catch (IOException e) {
//                System.err.println("Error sending offline messages for " + username + ": " + e.getMessage());
//            }
//        }
//    }
//
//    private void sendChatHistory(String username, PrintWriter writer) {
//        File dir = new File(CHAT_FOLDER + "/" + username);
//        if (!dir.exists()) return;
//        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") && !name.endsWith(".offline.txt"));
//        if (files == null) return;
//        for (File file : files) {
//            String friend = file.getName().replace(".txt", "");
//            if (friend.equals(username)) {
//                file.delete();
//                continue;
//            }
//            List<String> history = new ArrayList<>();
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    String[] parts = line.split("\\|", 3);
//                    if (parts.length == 3) {
//                        history.add(line);
//                    } else if (parts.length >= 2) {
//                        history.add(parts[0] + "|" + LocalDateTime.now().toString() + "|" + parts[1]);
//                    }
//                }
//            } catch (IOException e) {
//                System.err.println("Failed to read chat history for " + username + " and " + friend + ": " + e.getMessage());
//            }
//            if (!history.isEmpty()) {
//                String payload = String.join(";;;", history);
//                String lineToSend = "CHAT_HISTORY|" + friend + "|" + username + "|" + payload;
//                writer.println(lineToSend);
//                System.out.println("Sent chat history to " + username + " for " + friend);
//            }
//        }
//    }
//    /**
//     * Load all persisted group membership files from disk into the in-memory
//     * groupMembers map.  Each file in the GROUP_FOLDER directory with a
//     * ".members" extension is interpreted as a group name.  Lines within the
//     * file are treated as usernames belonging to that group.  If the folder
//     * does not exist or no valid files are found, nothing happens.
//     */
//    private static void loadGroupMembersFromDisk() {
//        File dir = new File(GROUP_FOLDER);
//        if (!dir.exists()) {
//            return;
//        }
//        File[] files = dir.listFiles((d, name) -> name.endsWith(".members"));
//        if (files == null) {
//            return;
//        }
//        for (File f : files) {
//            String fname = f.getName();
//            String groupName = fname.substring(0, fname.length() - ".members".length());
//            Set<String> members = Collections.newSetFromMap(new ConcurrentHashMap<>());
//            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
//                String line;
//                while ((line = br.readLine()) != null) {
//                    String trimmed = line.trim();
//                    if (!trimmed.isEmpty()) {
//                        members.add(trimmed);
//                    }
//                }
//            } catch (IOException e) {
//                System.err.println("Failed to load group membership for " + groupName + ": " + e.getMessage());
//                continue;
//            }
//            if (!members.isEmpty()) {
//                groupMembers.put(groupName, members);
//            }
//        }
//    }
//
//    /**
//     * Persist a single group's membership to disk.  Creates the GROUP_FOLDER
//     * directory if necessary and writes a file named <groupName>.members
//     * containing one username per line.  Overwrites any existing file for
//     * that group.  Called whenever a new group is created.
//     *
//     * @param groupName the name of the group
//     * @param members   set of usernames belonging to the group
//     */
//    private static void saveGroupMembersToDisk(String groupName, Set<String> members) {
//        File dir = new File(GROUP_FOLDER);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        File f = new File(dir, groupName + ".members");
//        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
//            for (String m : members) {
//                pw.println(m);
//            }
//        } catch (IOException e) {
//            System.err.println("Failed to save group membership for " + groupName + ": " + e.getMessage());
//        }
//    }
//}
//
//

package main.java.server;

import main.java.user.Database;
import main.java.user.User;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One thread per connected socket.
 *
 * <p>Understands the following      (pipe-delimited) commands—every field is UTF-8:</p>
 * <ul>
 *   <li><b>ONLINE</b> | username</li>
 *   <li><b>LOGOUT</b> | username</li>
 *   <li><b>SIGNUP</b> | username | password</li>
 *   <li><b>LOGIN</b>  | username | password</li>
 *   <li><b>PRIVATE</b> | from | to | timestamp | text</li>
 *   <li><b>CREATE_GROUP</b> | creator | groupName | m1,m2,…</li>
 *   <li><b>GROUP_MSG</b> | groupName | sender | timestamp | text</li>
 *   <li><b>GROUPS_REQUEST</b> | username</li>
 *   <li><b>SEND_REQUEST</b> | from | to</li>
 *   <li><b>ACCEPT_REQUEST</b> | user | from</li>
 *   <li><b>DECLINE_REQUEST</b> | user | from</li>
 * </ul>
 *
 * <p>Files on disk:</p>
 * <pre>
 *   chats/<user>/<friend>.txt            – chat history
 *   chats/<user>/<friend>.offline.txt    – queued messages
 *   groups/<group>.members               – one username per line
 *   users.txt                            – "user|password" lines
 * </pre>
 */
public class ClientHandler implements Runnable {

    /* ───────────────────────────── Shared state ───────────────────────────── */

    /** username → PrintWriter for every online user */
    private static final Map<String, PrintWriter> ONLINE_WRITERS = new ConcurrentHashMap<>();

    /** groupName → member usernames */
    private static final Map<String, Set<String>> GROUP_MEMBERS = new ConcurrentHashMap<>();

    /** base folders */
    private static final String CHAT_FOLDER  = "chats";
    private static final String GROUP_FOLDER = "groups";

    /** credentials file */
    private static final File CRED_FILE = new File("users.txt");

    /* ───────────────────────────── Static bootstrap ───────────────────────── */

    /** Load group membership files once per JVM start-up. */
    static {
        loadGroupMembersFromDisk();
    }

    /* ───────────────────────────── Instance state ─────────────────────────── */

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter    out;
    private String         username;          // set after ONLINE / LOGIN / SIGNUP

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    /* ────────────────────────────────── Main loop ─────────────────────────── */

    @Override
    public void run() {
        try {
            prepareIOStreams();
            listenForCommands();
        } catch (IOException ioEx) {
            System.err.println("I/O error in handler: " + ioEx.getMessage());
        } finally {
            tidyUp();
        }
    }

    /** Create UTF-8 reader / writer wrappers around the socket streams. */
    private void prepareIOStreams() throws IOException {
        InputStream  socketIn  = socket.getInputStream(); // gets the byte stream
        OutputStream socketOut = socket.getOutputStream();

        InputStreamReader   isr = new InputStreamReader(socketIn, StandardCharsets.UTF_8); // convert the byte stream to character stream,utf-8 standard;
        OutputStreamWriter  osw = new OutputStreamWriter(socketOut, StandardCharsets.UTF_8);// useful for various symblols .

        in  = new BufferedReader(isr);
        out = new PrintWriter(osw, /*autoFlush=*/true);
    }

    /** Blocking loop – parses each command line and dispatches. */
    private void listenForCommands() throws IOException {
        String line;

        while ((line = in.readLine()) != null) {
            System.out.println("Server received: " + line);

            /* Split only up to 5 parts; the message body may contain pipes. */
            String[] pieces = line.split("\\|", 5); // splitting the string into 5 parts using '|' as delimiter;
            if (pieces.length == 0) {
                continue;   // defensive: ignore blank lines
            }

            String command = pieces[0];

            switch (command) {
                case "ONLINE":        handleOnline(pieces);        break;
                case "LOGOUT":        handleLogout(pieces);        break;
                case "SIGNUP":        handleSignup(pieces);        break;
                case "LOGIN":         handleLogin(pieces);         break;
                case "PRIVATE":       handlePrivate(pieces);       break;
                case "CREATE_GROUP":  handleCreateGroup(pieces);   break;
                case "GROUP_MSG":     handleGroupMessage(pieces);  break;
                case "GROUPS_REQUEST":handleGroupsRequest(pieces); break;
                case "SEND_REQUEST":  handleSendRequest(pieces);   break;
                case "ACCEPT_REQUEST":handleAcceptRequest(pieces); break;
                case "DECLINE_REQUEST":handleDeclineRequest(pieces);break;
                /* Unknown commands are ignored for now */
            }
        }
    }

    /* ─────────────────────────── Command handlers ─────────────────────────── */

    private void handleOnline(String[] p) {
        /* Expected: ONLINE|username */
        if (p.length < 2) {
            return;
        }
        username = p[1];
        ONLINE_WRITERS.put(username, out);
        out.println("ONLINE_OK");

        sendOfflineMessages(username, out);
        sendChatHistory(username, out);
    }

    private void handleLogout(String[] p) {
        /* Expected: LOGOUT|username */
        if (p.length < 2) {
            return;
        }
        String user = p[1];
        ONLINE_WRITERS.remove(user);
        System.out.println(user + " has logged out.");
    }

    private void handleSignup(String[] p) {
        /* Expected: SIGNUP|user|pass */
        if (p.length < 3) {
            return;
        }
        String user = p[1];
        String pass = p[2];

        try {
            if (userExists(user)) {
                out.println("ERROR|Username already taken");
                return;
            }

            saveCredentials(user, pass);

            username = user;                     // auto-login after sign-up
            ONLINE_WRITERS.put(user, out);
            out.println("SUCCESS");
        } catch (IOException ex) {
            out.println("ERROR|Could not store credentials");
        }
    }

    private void handleLogin(String[] p) {
        /* Expected: LOGIN|user|pass */
        if (p.length < 3) {
            return;
        }
        String user = p[1];
        String pass = p[2];

        try {
            boolean ok = verifyCredentials(user, pass);
            if (!ok) {
                out.println("ERROR|Invalid credentials");
                return;
            }

            username = user;
            ONLINE_WRITERS.put(user, out);
            out.println("SUCCESS");

            sendOfflineMessages(user, out);
            sendChatHistory(user, out);
        } catch (IOException ex) {
            out.println("ERROR|Credential check failed");
        }
    }

    /* ---------- private, 1-to-1 chat ---------- */

    private void handlePrivate(String[] p) {
        /*
         * Two variants:
         *   PRIVATE|from|to|timestamp|body
         *   PRIVATE|from|to|body                 (timestamp filled on server)
         */
        if (p.length < 4) {
            return;
        }

        String from       = p[1];
        String to         = p[2];
        String timestamp  = (p.length >= 5) ? p[3] : LocalDateTime.now().toString(); // checking in case na pathaye thaki
        String body       = (p.length >= 5) ? p[4] : p[3];

        if (from.equals(to)) {
            return;                 // ignore self-messages
        }

        /* Persist on both sides */
        updateChatHistory(from, to, timestamp, body);

        /* Build protocol line to forward */
        String forward = "PRIVATE|" + from + "|" + to + "|" + timestamp + "|" + body;

        PrintWriter targetWriter = ONLINE_WRITERS.get(to); // getting the writing socket for sending our message
        if (targetWriter != null) {
            targetWriter.println(forward);
        } else {
            saveOfflineMessage(to, from, timestamp, body);
        }
    }

    /* ---------- group creation ---------- */

    private void handleCreateGroup(String[] p) {
        /* CREATE_GROUP|creator|groupName|m1,m2,... */
        if (p.length < 3) {
            return;
        }

        String creator   = p[1];
        String groupName = p[2];
        String rawCsv    = p.length >= 4 ? p[3] : "";

        /* Build a thread-safe set for members */
        Set<String> members = ConcurrentHashMap.newKeySet();

        /* Split comma-separated members */
        if (!rawCsv.isEmpty()) {
            String[] tokens = rawCsv.split(",");
            for (String t : tokens) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) {
                    members.add(trimmed);
                }
            }
        }
        members.add(creator);      // always include creator

        if (GROUP_MEMBERS.containsKey(groupName)) {
            out.println("ERROR|Group already exists");
            return;
        }

        GROUP_MEMBERS.put(groupName, members);
        saveGroupMembersToDisk(groupName, members);

        /* Notify every online member */
        String memberCsv = String.join(",", members);
        String notice    = "GROUP_CREATED|" + groupName + "|" + creator + "|" + memberCsv;

        for (String m : members) {
            PrintWriter pw = ONLINE_WRITERS.get(m);
            if (pw != null) {
                pw.println(notice);
            }
        }

        System.out.println("Group created: " + groupName + " members=" + memberCsv);
    }

    /* ---------- group chat ---------- */

    private void handleGroupMessage(String[] p) {
        /* GROUP_MSG|group|sender|timestamp|body */
        if (p.length < 5) {
            return;
        }

        String groupName = p[1];
        String sender    = p[2];
        String ts        = p[3];
        String body      = p[4];

        Set<String> members = GROUP_MEMBERS.get(groupName);
        if (members == null) {
            out.println("ERROR|No such group");
            return;
        }
        if (!members.contains(sender)) {
            out.println("ERROR|Not a member of " + groupName);
            return;
        }

        /* Persist to each member's history */
        String record = sender + "|" + ts + "|" + body;
        for (String m : members) {
            appendToChatFile(m, groupName, record);
        }

        /* Build packet */
        String packet = "GROUP_MSG|" + groupName + "|" + sender + "|" + ts + "|" + body;

        /* Fan-out to online members or queue offline */
        for (String m : members) {
            if (m.equals(sender)) {
                continue;   // do not echo to the sender
            }
            PrintWriter pw = ONLINE_WRITERS.get(m);
            if (pw != null) {
                pw.println(packet);
            } else {
                saveOfflineMessage(m, groupName, sender, ts, body);
            }
        }
    }

    /* ---------- list groups for user ---------- */

    private void handleGroupsRequest(String[] p) {
        /* GROUPS_REQUEST|username */
        if (p.length < 2) {
            return;
        }

        String user = p[1];
        List<String> groups = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : GROUP_MEMBERS.entrySet()) {
            String gName = entry.getKey();
            Set<String> mem = entry.getValue();
            if (mem.contains(user)) {
                groups.add(gName);
            }
        }

        String csv = String.join(",", groups);
        out.println("GROUP_LIST|" + csv);
    }

    /* ---------- friend requests ---------- */

    private void handleSendRequest(String[] p) {
        /* SEND_REQUEST|from|to */
        if (p.length < 3) {
            return;
        }

        String from = p[1];
        String to   = p[2];

        System.out.println("SEND_REQUEST " + from + " → " + to);

        User sender   = Database.loadUser(from);
        User receiver = Database.loadUser(to);

        if (sender == null || receiver == null) {
            System.out.println("Either sender or receiver missing in DB.");
            return;
        }

        sender.sendFriendRequest(receiver);
        Database.saveUser(sender);
        Database.saveUser(receiver);

        PrintWriter pw = ONLINE_WRITERS.get(to);
        if (pw != null) {
            pw.println("GOT_FRIEND_REQUEST_FROM|" + from);
        }
    }

    private void handleAcceptRequest(String[] p) {
        /* ACCEPT_REQUEST|user|from */
        if (p.length < 3) {
            return;
        }

        String user = p[1];
        String from = p[2];

        User u1 = Database.loadUser(user);
        User u2 = Database.loadUser(from);

        if (u1 == null || u2 == null) {
            return;
        }

        u1.acceptFriendRequest(u2);
        Database.saveUser(u1);
        Database.saveUser(u2);

        PrintWriter pw = ONLINE_WRITERS.get(from);
        if (pw != null) {
            pw.println("ACCEPTED_REQUEST_FROM|" + user);
        }
    }

    private void handleDeclineRequest(String[] p) {
        /* DECLINE_REQUEST|user|from */
        if (p.length < 3) {
            return;
        }

        String user = p[1];
        String from = p[2];

        User u1 = Database.loadUser(user);
        User u2 = Database.loadUser(from);

        if (u1 == null || u2 == null) {
            return;
        }

        u1.declineFriendRequest(u2);
        Database.saveUser(u1);
        Database.saveUser(u2);

        PrintWriter pw = ONLINE_WRITERS.get(from);
        if (pw != null) {
            pw.println("DECLINED_REQUEST_FROM|" + user);
        }
    }

    /* ─────────────────── Credential helper routines ──────────────────────── */

    private boolean userExists(String user) throws IOException {
        if (!CRED_FILE.exists()) {
            return false;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(CRED_FILE), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", 2);
                if (parts.length >= 1 && parts[0].equals(user)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void saveCredentials(String user, String pass) throws IOException {
        if (!CRED_FILE.exists()) {
            CRED_FILE.createNewFile();
        }

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(CRED_FILE, true), StandardCharsets.UTF_8))) {

            String record = user + "|" + pass;
            pw.println(record);
        }
    }

    private boolean verifyCredentials(String user, String pass) throws IOException {
        if (!CRED_FILE.exists()) {
            return false;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(CRED_FILE), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|", 2);
                if (parts.length == 2 && parts[0].equals(user) && parts[1].equals(pass)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* ─────────────────── Chat history / offline helpers ──────────────────── */

    private void updateChatHistory(String from, String to,
                                   String timestamp, String body) {

        String record = from + "|" + timestamp + "|" + body;
        appendToChatFile(from, to, record);
        appendToChatFile(to, from, record);
    }

    private void appendToChatFile(String user, String friend, String line) {
        File userDir = new File(CHAT_FOLDER, user);
        if (!userDir.exists()) {
            userDir.mkdirs();
        }

        File chatFile = new File(userDir, friend + ".txt");

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(chatFile, true), StandardCharsets.UTF_8))) {

            pw.println(line);
        } catch (IOException ex) {
            System.err.println("Cannot write chat file: " + ex.getMessage());
        }
    }

    private void saveOfflineMessage(String to, String from,
                                    String ts, String body) {

        File userDir = new File(CHAT_FOLDER, to);
        if (!userDir.exists()) {
            userDir.mkdirs();
        }

        File offFile = new File(userDir, from + ".offline.txt");

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(offFile, true), StandardCharsets.UTF_8))) {

            String rec = from + "|" + ts + "|" + body;
            pw.println(rec);
        } catch (IOException ex) {
            System.err.println("Cannot queue offline msg: " + ex.getMessage());
        }
    }

    private void saveOfflineMessage(String to, String group,
                                    String from, String ts, String body) {

        File userDir = new File(CHAT_FOLDER, to);
        if (!userDir.exists()) {
            userDir.mkdirs();
        }

        File offFile = new File(userDir, group + ".offline.txt");

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(offFile, true), StandardCharsets.UTF_8))) {

            String rec = from + "|" + ts + "|" + body;
            pw.println(rec);
        } catch (IOException ex) {
            System.err.println("Cannot queue offline group msg: " + ex.getMessage());
        }
    }

    private void sendOfflineMessages(String user, PrintWriter writer) {
        File userDir = new File(CHAT_FOLDER, user);
        if (!userDir.exists()) {
            return;
        }

        File[] offFiles = userDir.listFiles(
                (dir, name) -> name.endsWith(".offline.txt"));

        if (offFiles == null) {
            return;
        }

        for (File f : offFiles) {
            playAndDeleteOfflineFile(f, user, writer);
        }
    }

    private void playAndDeleteOfflineFile(File f, String user, PrintWriter writer) {
        String counterpart = f.getName().replace(".offline.txt", "");

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {

            String rec;
            while ((rec = br.readLine()) != null) {
                String payload = "OFFLINE_MSG|" + counterpart + "|" + user + "|" + rec;
                writer.println(payload);
            }

        } catch (IOException ex) {
            System.err.println("Failed to replay offline file: " + ex.getMessage());
        }

        // delete regardless of success; failed lines will queue again next time
        f.delete();
    }

    private void sendChatHistory(String user, PrintWriter writer) {
        File userDir = new File(CHAT_FOLDER, user);
        if (!userDir.exists()) {
            return;
        }

        File[] chatFiles = userDir.listFiles(
                (dir, name) -> name.endsWith(".txt") && !name.endsWith(".offline.txt"));

        if (chatFiles == null) {
            return;
        }

        for (File chatFile : chatFiles) {
            sendOneChatHistory(chatFile, user, writer);
        }
    }

    private void sendOneChatHistory(File chatFile,
                                    String user, PrintWriter writer) {

        String friend = chatFile.getName().replace(".txt", "");
        if (friend.equals(user)) {
            return;     // ignore self-log
        }

        List<String> entries = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(chatFile), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                entries.add(line);
            }

        } catch (IOException ex) {
            System.err.println("Could not read history: " + ex.getMessage());
            return;
        }

        if (entries.isEmpty()) {
            return;
        }

        String joined = String.join(";;;", entries);
        String packet = "CHAT_HISTORY|" + friend + "|" + user + "|" + joined;

        writer.println(packet);
    }

    /* ────────────────────── Group membership persistence ─────────────────── */

    private static void loadGroupMembersFromDisk() {
        File dir = new File(GROUP_FOLDER);
        if (!dir.exists()) {
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".members"));
        if (files == null) {
            return;
        }

        for (File f : files) {
            String fileName  = f.getName();
            String groupName = fileName.substring(0, fileName.length() - ".members".length());

            Set<String> members = ConcurrentHashMap.newKeySet();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {

                String line;
                while ((line = br.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        members.add(trimmed);
                    }
                }
            } catch (IOException ex) {
                System.err.println("Cannot load group: " + ex.getMessage());
                continue;
            }

            if (!members.isEmpty()) {
                GROUP_MEMBERS.put(groupName, members);
            }
        }
    }

    private static void saveGroupMembersToDisk(String groupName, Set<String> members) {
        File dir = new File(GROUP_FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File outFile = new File(dir, groupName + ".members");
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {

            for (String m : members) {
                pw.println(m);
            }
        } catch (IOException ex) {
            System.err.println("Cannot persist group members: " + ex.getMessage());
        }
    }

    /* ───────────────────────────── Clean up ──────────────────────────────── */

    private void tidyUp() {
        if (username != null) {
            ONLINE_WRITERS.remove(username);
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
