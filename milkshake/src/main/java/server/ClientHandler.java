
package main.java.server;

import main.java.user.Database;
import main.java.user.User;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        out = new PrintWriter(osw, true);
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
                case "UPLOAD_PROFILE_PHOTO": handleUploadProfilePhoto(pieces); break;
                case "VIDEO_CALL_REQUEST": handleVideoCallRequest(pieces); break;
                case "VIDEO_CALL_RESPONSE": handleVideoCallResponse(pieces); break;
                case "END_CALL": handleEndCall(pieces); break;
                case "VIDEO_FRAME": handleVideoFrame(pieces); break;
                case "AUDIO_FRAME": handleAudioFrame(pieces); break;
                case "GROUP_MEMBERS_REQUEST": handleGroupMembersRequest(pieces); break;
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

    //    private void handleLogout(String[] p) {
//        /* Expected: LOGOUT|username */
//        if (p.length < 2) {
//            return;
//        }
//        String user = p[1];
//        ONLINE_WRITERS.remove(user);
//        System.out.println(user + " has logged out.");
//    }
    private void handleLogout(String[] p) {
        String user = p[1];
        ONLINE_WRITERS.remove(user);
        System.out.println(user + " has logged out.");
        if (user.equals(this.username)) {
            this.username = null;
        }
        if (out != null) {
            out.println("LOGOUT_OK");
        }
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

    /* ---------- group membership lookup ---------- */

    /**
     * Handles a request for the members of a specific group.  The client
     * sends a command of the form GROUP_MEMBERS_REQUEST|username|groupName.
     * The server responds with a single line: GROUP_MEMBERS|groupName|m1,m2,...
     * containing the names of all members in the specified group.  If the
     * group does not exist, an empty member list is sent.
     *
     * @param p the parsed command parts
     */
    private void handleGroupMembersRequest(String[] p) {
        // Expected: GROUP_MEMBERS_REQUEST|username|groupName
        if (p.length < 3) {
            return;
        }
        String requestingUser = p[1];
        String groupName      = p[2];

        Set<String> members = GROUP_MEMBERS.getOrDefault(groupName, Collections.emptySet());
        // Create a CSV of member names
        String memberCsv = String.join(",", members);
        // Respond to only the requesting client
        PrintWriter targetOut = ONLINE_WRITERS.get(requestingUser);
        if (targetOut != null) {
            targetOut.println("GROUP_MEMBERS|" + groupName + "|" + memberCsv);
        }
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

//    private void handleAcceptRequest(String[] p) {
//        /* ACCEPT_REQUEST|user|from */
//        if (p.length < 3) {
//            return;
//        }
//
//        String user = p[1];
//        String from = p[2];
//
//        User u1 = Database.loadUser(user);
//        User u2 = Database.loadUser(from);
//
//        if (u1 == null || u2 == null) {
//            return;
//        }
//
//        u1.acceptFriendRequest(u2);
//        Database.saveUser(u1);
//        Database.saveUser(u2);
//
//        PrintWriter pw = ONLINE_WRITERS.get(from);
//        if (pw != null) {
//            pw.println("ACCEPTED_REQUEST_FROM|" + user);
//        }
//    }

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

        // Notify the original requester that their friend request was accepted
        PrintWriter pwFrom = ONLINE_WRITERS.get(from);
        if (pwFrom != null) {
            pwFrom.println("ACCEPTED_REQUEST_FROM|" + user);
        }

        // Also notify the acceptor so their client can update immediately
        PrintWriter pwUser = ONLINE_WRITERS.get(user);
        if (pwUser != null) {
            pwUser.println("ACCEPTED_REQUEST_FROM|" + from);
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
    private void handleUploadProfilePhoto(String[] parts) {
        try {
            String username = parts[1];
            String filename = parts[2];
            int length = Integer.parseInt(parts[3]);

            byte[] imageBytes = new byte[length];
            InputStream is = socket.getInputStream();
            int bytesRead, total = 0;
            while (total < length && (bytesRead = is.read(imageBytes, total, length - total)) > 0) {
                total += bytesRead;
            }

            System.out.println("Read " + total + " of " + length + " bytes");

            // Save under users/username/profile.jpg
            File userDir = new File("users/" + username);
            userDir.mkdirs();
            File imageFile = new File(userDir, "profile.jpg");
            Files.write(imageFile.toPath(), imageBytes);

            System.out.println("Saved profile photo for user: " + username);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles an incoming video call request.  The expected format is
     * VIDEO_CALL_REQUEST|caller|callee.  If the callee is currently online,
     * the request is forwarded to them.  Otherwise the request is ignored.
     */
    private void handleVideoCallRequest(String[] p) {
        if (p.length < 3) {
            return;
        }
        String caller = p[1];
        String callee = p[2];
        // Forward the request to the callee if online
        PrintWriter targetOut = ONLINE_WRITERS.get(callee);
        if (targetOut != null) {
            targetOut.println("VIDEO_CALL_REQUEST|" + caller);
        }
    }

    /**
     * Handles a response to a video call request.  Expected format:
     * VIDEO_CALL_RESPONSE|responder|to|answer, where answer is "yes" or
     * "no".  The response is forwarded to the original caller if online.
     */
    private void handleVideoCallResponse(String[] p) {
        if (p.length < 4) {
            return;
        }
        String responder = p[1];
        String to = p[2];
        String answer = p[3];
        PrintWriter targetOut = ONLINE_WRITERS.get(to);
        if (targetOut != null) {
            targetOut.println("VIDEO_CALL_RESPONSE|" + responder + "|" + answer);
        }
    }

    /**
     * Handles a request to end an ongoing video call.  Expected format:
     * END_CALL|from|to.  The termination is forwarded to the other user
     * if they are online.
     */
    private void handleEndCall(String[] p) {
        if (p.length < 3) {
            return;
        }
        String from = p[1];
        String to = p[2];
        PrintWriter targetOut = ONLINE_WRITERS.get(to);
        if (targetOut != null) {
            targetOut.println("END_CALL|" + from);
        }
    }

    /**
     * Handles a video frame from a client.  The expected format is
     * VIDEO_FRAME|from|to|data.  The frame is forwarded to the intended
     * recipient if they are currently online.  Frames are encoded as
     * base64 JPEG strings.
     */
    private void handleVideoFrame(String[] p) {
        if (p.length < 4) {
            return;
        }
        String from = p[1];
        String to = p[2];
        String data = p[3];
        PrintWriter targetOut = ONLINE_WRITERS.get(to);
        if (targetOut != null) {
            targetOut.println("VIDEO_FRAME|" + from + "|" + data);
        }
    }

    /**
     * Handles a chunk of audio data from a client.  The expected format is
     * AUDIO_FRAME|from|to|data.  The audio is forwarded to the intended
     * recipient if they are currently online.  Audio frames are base64
     * encoded raw PCM bytes (e.g., 16‑bit little endian mono samples).
     *
     * @param p the split command array
     */
    private void handleAudioFrame(String[] p) {
        if (p.length < 4) {
            return;
        }
        String from = p[1];
        String to = p[2];
        String data = p[3];
        PrintWriter targetOut = ONLINE_WRITERS.get(to);
        if (targetOut != null) {
            targetOut.println("AUDIO_FRAME|" + from + "|" + data);
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
