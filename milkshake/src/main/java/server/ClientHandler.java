//package main.java.server;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.net.*;
//
//public class  ClientHandler implements Runnable{
//    private Socket socket;
//
//    public ClientHandler(Socket socket){
//        this.socket = socket;
//    }
//
//    public void run(){
//        try{
//            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//
//            String request = in.readLine();
//            String [] words = request.split("\\|");
//
//            String task = words[0];
//            String username = words[1];
//            String password = words[2];
//
//            String response;
//
//            if(task.equalsIgnoreCase("SIGNUP")){
//                response = UserManager.register(username, password);
//            }
//            else if(task.equalsIgnoreCase("LOGIN")){
//                response = UserManager.login(username, password);
//            }
//            else{
//                response = "ERROR|Unknown prompt.";
//            }
//
//            out.println(response);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}

//
//
//package main.java.server;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * Handles a single client connection on the server side.
// */
//public class ClientHandler implements Runnable {
//
//    private static final Map<String, PrintWriter> onlineWriters = new ConcurrentHashMap<>();
//
//    private final Socket socket;
//    private BufferedReader in;
//    private PrintWriter out;
//    private String username;
//
//    public ClientHandler(Socket s){ this.socket = s; }
//
//    @Override public void run() {
//        try {
//            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            out = new PrintWriter(socket.getOutputStream(), true);
//
//            String line;
//            while ((line = in.readLine()) != null) {
//                String[] parts = line.split("\\|", 4);
//                switch (parts[0]) {
//                    case "LOGIN":
//                        // LOGIN|user
//                        username = parts[1];
//                        onlineWriters.put(username, out);
//                        out.println("LOGIN_SUCCESS");
//                        break;
//
//                    case "PRIVATE":
//                        // PRIVATE|from|to|msg
//                        if (parts.length >= 4) {
//                            String from = parts[1];
//                            String to   = parts[2];
//                            String body = parts[3];
//                            PrintWriter target = onlineWriters.get(to);
//                            if (target != null) {
//                                target.println("PRIVATE|" + from + "|" + to + "|" + body);
//                            }
//                        }
//                        break;
//
//                    // add SIGNUP, etc.
//                }
//            }
//        } catch (IOException ignored) {
//        } finally {
//            if(username!=null) onlineWriters.remove(username);
//            try { socket.close(); } catch (IOException ignored) {}
//        }
//    }
//}


//11/7/25
//
//package main.java.server;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * Handles a single client connection.
// * Supports SIGNUP, LOGIN and PRIVATE chat routing.
// */
//public class ClientHandler implements Runnable {
//
//    // All online users → their print writers
//    private static final Map<String, PrintWriter> onlineWriters = new ConcurrentHashMap<>();
//
//    private final Socket socket;
//    private BufferedReader in;
//    private PrintWriter out;
//    private String username;
//
//    public ClientHandler(Socket socket) { this.socket = socket; }
//
//    @Override
//    public void run() {
//        try {
//            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            out = new PrintWriter(socket.getOutputStream(), true);
//
//            String line;
//            while ((line = in.readLine()) != null) {
//                String[] parts = line.split("\\|", 4);
//                switch (parts[0]) {
//                    /* -------------------- SIGNUP -------------------- */
//                    case "SIGNUP":
//                        // SIGNUP|user|pass
//                        if (parts.length >= 3) {
//                            String newUser = parts[1];
//                            String pass    = parts[2];
//                            boolean taken  = userExists(newUser);
//                            if (taken) {
//                                out.println("ERROR|Username already taken");
//                            } else {
//                                saveCredentials(newUser, pass);
//                                out.println("SUCCESS");
//                            }
//                        }
//                        break;
//
//                    /* -------------------- LOGIN -------------------- */
//                    case "LOGIN":
//                        // LOGIN|user|pass
//                        if (parts.length >= 3) {
//                            String u = parts[1];
//                            String p = parts[2];
//                            if (verifyCredentials(u, p)) {
//                                username = u;
//                                onlineWriters.put(username, out);
//                                out.println("SUCCESS");
//                            } else {
//                                out.println("ERROR|Invalid credentials");
//                            }
//                        }
//                        break;
//
//                    /* -------------------- PRIVATE -------------------- */
//                    case "PRIVATE":
//                        // PRIVATE|from|to|msg
//                        if (parts.length >= 4) {
//                            String from = parts[1];
//                            String to   = parts[2];
//                            String body = parts[3];
//                            PrintWriter target = onlineWriters.get(to);
//                            if (target != null) {
//                                target.println("PRIVATE|" + from + "|" + to + "|" + body);
//                            }
//                        }
//                        break;
//                }
//            }
//        } catch (IOException ignored) {
//        } finally {
//            if (username != null) onlineWriters.remove(username);
//            try { socket.close(); } catch (IOException ignored) {}
//        }
//    }
//
//    /* ------------------------------------------------------------------ */
//    /*                       Simple credential store                      */
//    /* ------------------------------------------------------------------ */
//
//    private static final File CRED_FILE = new File("users.txt");
//
//    private boolean userExists(String user) throws IOException {
//        if (!CRED_FILE.exists()) return false;
//        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
//            String l;
//            while ((l = br.readLine()) != null) {
//                if (l.split("\\|", 2)[0].equals(user)) return true;
//            }
//            return false;
//        }
//    }
//
//    private void saveCredentials(String user, String pass) throws IOException {
//        if (!CRED_FILE.exists()) CRED_FILE.createNewFile();
//        try (PrintWriter pw = new PrintWriter(new FileWriter(CRED_FILE, true))) {
//            pw.println(user + "|" + pass);
//        }
//    }
//
//    private boolean verifyCredentials(String user, String pass) throws IOException {
//        if (!CRED_FILE.exists()) return false;
//        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
//            String l;
//            while ((l = br.readLine()) != null) {
//                String[] p = l.split("\\|", 2);
//                if (p.length == 2 && p[0].equals(user) && p[1].equals(pass)) return true;
//            }
//            return false;
//        }
//    }
//}


// 11/7/25

package main.java.server;

import main.java.controller.ChatBoxController;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import main.java.controller.ChatBoxController.Message;

/**
 * Handles a single client connection.
 * Supports ONLINE (presence), SIGNUP, LOGIN and PRIVATE chat routing.
 */
//public class ClientHandler implements Runnable {
//
//    private static final Map<String, PrintWriter> onlineWriters = new ConcurrentHashMap<>();
//    private static final String CHAT_FOLDER = "chats";
//    private static final String OFFLINE_FOLDER = "offline";
//
//    private final Socket socket;
//    private BufferedReader in;
//    private PrintWriter out;
//    private String username;
//
//    public ClientHandler(Socket socket) { this.socket = socket; }
//
//    @Override
//    public void run() {
//        try {
//            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            out = new PrintWriter(socket.getOutputStream(), true);
//
//            String line;
//            while ((line = in.readLine()) != null) {
//                String[] parts = line.split("\\|", 4);
//                switch (parts[0]) {
//                    /* ------------------- ONLINE ------------------- */
//                    case "ONLINE":
//                        // ONLINE|username
//                        if (parts.length >= 2) {
//                            username = parts[1];
//                            onlineWriters.put(username, out);
//                            out.println("ONLINE_OK");
//                            sendPendingMessages(username);
//                            sendOfflineMessages(username);
//                        }
//                        break;
//
//                    /* ------------------- SIGNUP ------------------- */
//                    case "SIGNUP":
//                        if (parts.length >= 3) {
//                            String newUser = parts[1];
//                            String pass    = parts[2];
//                            if (userExists(newUser)) {
//                                out.println("ERROR|Username already taken");
//                            } else {
//                                saveCredentials(newUser, pass);
//                                out.println("SUCCESS");
//                            }
//                        }
//                        break;
//
//                    /* -------------------- LOGIN -------------------- */
//                    case "LOGIN":
//                        if (parts.length >= 3) {
//                            String u = parts[1];
//                            String p = parts[2];
//                            if (verifyCredentials(u, p)) {
//                                username = u;
//                                onlineWriters.put(username, out);
//                                out.println("SUCCESS");
//
//                                sendPendingMessages(username);
//                                sendOfflineMessages(username);
//                                sendChatHistory(username);
//                                System.out.println("Calling sendPendingMessages for: " + username);
//                            } else {
//                                out.println("ERROR|Invalid credentials");
//                            }
//                        }
//                        break;
//
//                    /* ------------------- PRIVATE ------------------- */
//                    case "PRIVATE":
//                        if (parts.length >= 4) {
//                            String from = parts[1];
//                            String to   = parts[2];
//                            String body = parts[3];
//
//                            if(from.equals(to)) {
//                                System.out.println("Ignoring message from user to self: " + from);
//                                break; // skip processing self-messages
//                            }
//
//                            updateChatHistory(from, to, body);
//
//                            PrintWriter target = onlineWriters.get(to);
//                            String fullMessage = "PRIVATE|" + from + "|" + to + "|" + body;
//
//                            if (target != null) {
//                                target.println(fullMessage);
//                            }
//                            else{
//                                //save as offline message
//                                System.out.println("Sending offline message: " + fullMessage);
//                                saveOfflineMessage(to, fullMessage);
//                            }
//                        }
//                        break;
//                }
//            }
//        } catch (IOException ignored) {
//        } finally {
//            if (username != null) onlineWriters.remove(username);
//            try { socket.close(); } catch (IOException ignored) {}
//        }
//    }
//
//    /* ---------------- credential helpers ---------------- */
//    private static final File CRED_FILE = new File("users.txt");
//
//    private boolean userExists(String user) throws IOException {
//        if (!CRED_FILE.exists()) return false;
//        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
//            String l; while ((l = br.readLine()) != null) if (l.split("\\|",2)[0].equals(user)) return true;
//            return false;
//        }
//    }
//
//    private void saveCredentials(String user, String pass) throws IOException {
//        if (!CRED_FILE.exists()) CRED_FILE.createNewFile();
//        try (PrintWriter pw = new PrintWriter(new FileWriter(CRED_FILE, true))) {
//            pw.println(user + "|" + pass);
//        }
//    }
//
//    private boolean verifyCredentials(String user, String pass) throws IOException {
//        if (!CRED_FILE.exists()) return false;
//        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
//            String l; while ((l = br.readLine()) != null) {
//                String[] p = l.split("\\|",2);
//                if (p.length==2 && p[0].equals(user) && p[1].equals(pass)) return true;
//            }
//            return false;
//        }
//    }
//
//    private void updateChatHistory(String from, String to, String message){
//        if (from.equals(to)) {
//            System.out.println("Skipping self-message save for: " + from);
//            return;
//        }
//        saveMessageToFile(from, to, message);
//        saveMessageToFile(to, from, message);
//        System.out.println("Updating chat history: " + from + " <-> " + to);
//    }
//
//    private void saveMessageToFile(String sender, String recipient, String message) {
//        if(sender.equals(recipient)) {
//            System.out.println("Not saving self-message for: " + sender);
//            return; // skip saving self messages
//        }
//
//        File dir = new File(CHAT_FOLDER + "/" + recipient);
//
//        if (!dir.exists()){
//            dir.mkdirs();
//            System.out.println("Created chat folder: " + dir.getAbsolutePath());
//        }
//
//        File chatFile = new File(dir, recipient + ".txt");
//        try (PrintWriter writer = new PrintWriter(new FileWriter(chatFile, true))) {
//            writer.println(sender + ":" + message);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void saveOfflineMessage(String recipient, String msg) {
//        // msg format: "PRIVATE|from|to|body"
//        String[] parts = msg.split("\\|", 4);
//        if (parts.length < 4) return;
//
//        String from = parts[1];
//        String body = parts[3];
//
//        File dir = new File(CHAT_FOLDER + "/" + recipient);
//        if (!dir.exists()) dir.mkdirs();
//
//        // Save offline messages separately
//        File offlineFile = new File(dir, from + ".offline.txt");
//
//        try (PrintWriter writer = new PrintWriter(new FileWriter(offlineFile, true))) {
//            writer.println(from + ":" + body);
//            System.out.println("Saved offline message from " + from + " to " + recipient);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void sendOfflineMessages(String user) {
//        File dir = new File(CHAT_FOLDER + "/" + user);
//        if (!dir.exists()) return;
//
//        File[] offlineFiles = dir.listFiles((d, name) -> name.endsWith(".offline.txt"));
//        if (offlineFiles == null) return;
//
//        for (File offlineFile : offlineFiles) {
//            String sender = offlineFile.getName().replace(".offline.txt", "");
//            if(sender.equals(user)) {
//                System.out.println("Skipping offline self messages for " + user);
//                continue;
//            }
//
//            try (BufferedReader reader = new BufferedReader(new FileReader(offlineFile))) {
//                String msg;
//                while ((msg = reader.readLine()) != null) {
//                    String lineToSend = "OFFLINE_MSG|" + sender + "|" + user + "|" + msg;
//                    System.out.println("Sending offline message to " + user + ": " + lineToSend);
//                    out.println(lineToSend);
//                    // Also save to real chat history
//                    updateChatHistory(sender, user, msg);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            offlineFile.delete();
//        }
//    }
//
//
//    private void sendPendingMessages(String user) {
//        File dir = new File("chats/" + user);
//        if (!dir.exists()) return;
//
//        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") && !name.endsWith(".offline.txt"));
//        if (files == null) return;
//
//        for (File file : files) {
//            String friend = file.getName().replace(".txt", "");
//            System.out.println("Checking file: " + file.getName() + " for user: " + user);
//            if(friend.equals(user)) {
//                // skip self-message file
//                continue;
//            }
//            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    // format: sender:message
//                    int sep = line.indexOf(":");
//                    if (sep > 0) {
//                        String sender = line.substring(0, sep);
//                        String msg = line.substring(sep + 1);
//                        String fullMsg = "PRIVATE|" + sender + "|" + user + "|" + msg;
//
//                        // 1. Send it over the wire
//                        System.out.println("Delivering pending msg to " + user + ": " + "PRIVATE|" + sender + "|" + user + "|" + msg);
//                        out.println(fullMsg);
//
//                        // 2. Save it in the real chat history
//                        updateChatHistory(friend, user, msg);
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            file.delete(); // remove after delivering
//        }
//    }
//
//    private void sendChatHistory(String user) {
//        File dir = new File("chats/" + user);
//        if (!dir.exists()) return;
//
//        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") && !name.endsWith(".offline.txt"));
//        if (files == null) return;
//
//        for (File file : files) {
//            String friend = file.getName().replace(".txt", "");
//            if (friend.equals(user)) continue;
//
//            List<String> history = new ArrayList<>();
//            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    history.add(line); // format: sender:msg
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            if (!history.isEmpty()) {
//                // Format: CHAT_HISTORY|friend|user|sender1:msg1;;;sender2:msg2;;;
//                String payload = String.join(";;;", history);
//                String lineToSend = "CHAT_HISTORY|" + friend + "|" + user + "|" + payload;
//                out.println(lineToSend);
//            }
//        }
//    }
//}


//public class ClientHandler implements Runnable {
//
//    private static final Map<String, PrintWriter> onlineWriters = new ConcurrentHashMap<>();
//    private static final String CHAT_FOLDER = "chats";
//    private static final String OFFLINE_FOLDER = "offline";
//
//    private final Socket socket;
//    private BufferedReader in;
//    private PrintWriter out;
//    private String username;
//
//    public ClientHandler(Socket socket) { this.socket = socket; }
//
//    @Override
//    public void run() {
//        try {
//            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            out = new PrintWriter(socket.getOutputStream(), true);
//
//            String line;
//            while ((line = in.readLine()) != null) {
//                String[] parts = line.split("\\|", 4);
//                switch (parts[0]) {
//                    /* ------------------- ONLINE ------------------- */
//                    case "ONLINE":
//                        // ONLINE|username
//                        if (parts.length >= 2) {
//                            username = parts[1];
//                            onlineWriters.put(username, out);
//                            out.println("ONLINE_OK");
//                            sendPendingMessages(username);
//                            sendOfflineMessages(username);
//                        }
//                        break;
//
//                    /* ------------------- SIGNUP ------------------- */
//                    case "SIGNUP":
//                        if (parts.length >= 3) {
//                            String newUser = parts[1];
//                            String pass    = parts[2];
//                            if (userExists(newUser)) {
//                                out.println("ERROR|Username already taken");
//                            } else {
//                                saveCredentials(newUser, pass);
//                                out.println("SUCCESS");
//                            }
//                        }
//                        break;
//
//                    /* -------------------- LOGIN -------------------- */
//                    case "LOGIN":
//                        if (parts.length >= 3) {
//                            String u = parts[1];
//                            String p = parts[2];
//                            if (verifyCredentials(u, p)) {
//                                username = u;
//                                onlineWriters.put(username, out);
//                                out.println("SUCCESS");
//
//                                //sendPendingMessages(username);
//                                sendOfflineMessages(username);
//                                sendChatHistory(username);
//                                System.out.println("Calling sendPendingMessages for: " + username);
//                            } else {
//                                out.println("ERROR|Invalid credentials");
//                            }
//                        }
//                        break;
//
//                    /* ------------------- PRIVATE ------------------- */
//                    case "PRIVATE":
//                        if (parts.length >= 4) {
//                            String from = parts[1];
//                            String to   = parts[2];
//                            String body = parts[3];
//
//                            if(from.equals(to)) {
//                                System.out.println("Ignoring message from user to self: " + from);
//                                break; // skip processing self-messages
//                            }
//
//                            updateChatHistory(from, to, body);
//
//                            PrintWriter target = onlineWriters.get(to);
//                            String fullMessage = "PRIVATE|" + from + "|" + to + "|" + body;
//
//                            if (target != null) {
//                                target.println(fullMessage);
//                            }
//                            else{
//                                //save as offline message
//                                System.out.println("Sending offline message: " + fullMessage);
//                                saveOfflineMessage(to, fullMessage);
//                            }
//                        }
//                        break;
//                }
//            }
//        } catch (IOException ignored) {
//        } finally {
//            if (username != null) onlineWriters.remove(username);
//            try { socket.close(); } catch (IOException ignored) {}
//        }
//    }
//
//    /* ---------------- credential helpers ---------------- */
//    private static final File CRED_FILE = new File("users.txt");
//
//    private boolean userExists(String user) throws IOException {
//        if (!CRED_FILE.exists()) return false;
//        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
//            String l; while ((l = br.readLine()) != null) if (l.split("\\|",2)[0].equals(user)) return true;
//            return false;
//        }
//    }
//
//    private void saveCredentials(String user, String pass) throws IOException {
//        if (!CRED_FILE.exists()) CRED_FILE.createNewFile();
//        try (PrintWriter pw = new PrintWriter(new FileWriter(CRED_FILE, true))) {
//            pw.println(user + "|" + pass);
//        }
//    }
//
//    private boolean verifyCredentials(String user, String pass) throws IOException {
//        if (!CRED_FILE.exists()) return false;
//        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
//            String l; while ((l = br.readLine()) != null) {
//                String[] p = l.split("\\|",2);
//                if (p.length==2 && p[0].equals(user) && p[1].equals(pass)) return true;
//            }
//            return false;
//        }
//    }
//
//    private void updateChatHistory(String from, String to, String message){
//        if (from.equals(to)) {
//            System.out.println("Skipping self-message save for: " + from);
//            return;
//        }
//        saveMessageToFile(from, to, message);
//        saveMessageToFile(to, from, message);
//        System.out.println("Updating chat history: " + from + " <-> " + to);
//    }
//
//    private void saveMessageToFile(String sender, String recipient, String message) {
//        if(sender.equals(recipient)) {
//            System.out.println("Not saving self-message for: " + sender);
//            return; // skip saving self messages
//        }
//
//        File dir = new File(CHAT_FOLDER + "/" + recipient);
//
//        if (!dir.exists()){
//            dir.mkdirs();
//            System.out.println("Created chat folder: " + dir.getAbsolutePath());
//        }
//
//        // ** FIXED HERE: chatFile must be named after the other user (sender), not recipient **
//        File chatFile = new File(dir, sender + ".txt");
//
//        try (PrintWriter writer = new PrintWriter(new FileWriter(chatFile, true))) {
//            writer.println(sender + ":" + message);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void saveOfflineMessage(String recipient, String msg) {
//        // msg format: "PRIVATE|from|to|body"
//        String[] parts = msg.split("\\|", 4);
//        if (parts.length < 4) return;
//
//        String from = parts[1];
//        String body = parts[3];
//
//        File dir = new File(CHAT_FOLDER + "/" + recipient);
//        if (!dir.exists()) dir.mkdirs();
//
//        // Save offline messages separately
//        File offlineFile = new File(dir, from + ".offline.txt");
//
//        try (PrintWriter writer = new PrintWriter(new FileWriter(offlineFile, true))) {
//            writer.println(from + ":" + body);
//            System.out.println("Saved offline message from " + from + " to " + recipient);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
////    private void sendOfflineMessages(String user) {
////        File dir = new File(CHAT_FOLDER + "/" + user);
////        if (!dir.exists()) return;
////
////        File[] offlineFiles = dir.listFiles((d, name) -> name.endsWith(".offline.txt"));
////        if (offlineFiles == null) return;
////
////        for (File offlineFile : offlineFiles) {
////            String sender = offlineFile.getName().replace(".offline.txt", "");
////            if(sender.equals(user)) {
////                System.out.println("Skipping offline self messages for " + user);
////                continue;
////            }
////
////            try (BufferedReader reader = new BufferedReader(new FileReader(offlineFile))) {
////                String msg;
////                while ((msg = reader.readLine()) != null) {
////                    String lineToSend = "OFFLINE_MSG|" + sender + "|" + user + "|" + msg;
////                    System.out.println("Sending offline message to " + user + ": " + lineToSend);
////                    out.println(lineToSend);
////                    // Also save to real chat history
////                    //updateChatHistory(sender, user, msg);
////                }
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
////            offlineFile.delete();
////        }
////    }
//
//    private void sendOfflineMessages(String username) {
//        File folder = new File("chats/" + username);
//        if (!folder.exists()) return;
//
//        for (File file : folder.listFiles()) {
//            String name = file.getName();
//            if (name.endsWith(".offline.txt")) {
//                String friend = name.replace(".offline.txt", "");
//                try {
//                    List<String> lines = Files.readAllLines(file.toPath());
//                    for (String line : lines) {
//                        PrintWriter writer = onlineWriters.get(username);
//                        if (writer != null) {
//                            writer.println("OFFLINE_MSG|" + friend + "|" + username + "|" + line);
//                            writer.flush();
//                        }
//                    }
//
//                    // ✅ Move lines to permanent chat history file
//                    Path permanentFile = Paths.get("chats", username, friend + ".txt");
//                    Files.write(permanentFile, lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
//
//                    // ✅ Delete the offline file
//                    Files.delete(file.toPath());
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//
//    private void sendPendingMessages(String user) {
//        File dir = new File("chats/" + user);
//        if (!dir.exists()) return;
//
//        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") && !name.endsWith(".offline.txt"));
//        if (files == null) return;
//
//        for (File file : files) {
//            String friend = file.getName().replace(".txt", "");
//            System.out.println("Checking file: " + file.getName() + " for user: " + user);
//
//            // Skip and delete self-message files to avoid loops
//            if(friend.equals(user)) {
//                System.out.println("Deleting invalid self-message file: " + file.getAbsolutePath());
//                file.delete();
//                continue;
//            }
//
//            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    // format: sender:message
//                    int sep = line.indexOf(":");
//                    if (sep > 0) {
//                        String sender = line.substring(0, sep);
//                        String msg = line.substring(sep + 1);
//
//                        if(sender.equals(user)) {
//                            System.out.println("Skipping self-message line in pending messages: " + line);
//                            continue;
//                        }
//
//                        String fullMsg = "PRIVATE|" + sender + "|" + user + "|" + msg;
//
//                        // 1. Send it over the wire
//                        System.out.println("Delivering pending msg to " + user + ": " + fullMsg);
//                        out.println(fullMsg);
//
//                        // 2. Save it in the real chat history
//                        //updateChatHistory(friend, user, msg);
//                    }
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            file.delete(); // remove after delivering
//        }
//    }
//
//    private void sendChatHistory(String user) {
//        File dir = new File("chats/" + user);
//        if (!dir.exists()) return;
//
//        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") && !name.endsWith(".offline.txt"));
//        if (files == null) return;
//
//        for (File file : files) {
//            String friend = file.getName().replace(".txt", "");
//            if (friend.equals(user)) {
//                System.out.println("Deleting invalid self-message file in chat history: " + file.getAbsolutePath());
//                file.delete();
//                continue;
//            }
//
//            List<String> history = new ArrayList<>();
//            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    history.add(line); // format: sender:msg
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            if (!history.isEmpty()) {
//                // Format: CHAT_HISTORY|friend|user|sender1:msg1;;;sender2:msg2;;;
//                String payload = String.join(";;;", history);
//                String lineToSend = "CHAT_HISTORY|" + friend + "|" + user + "|" + payload;
//                out.println(lineToSend);
//            }
//        }
//    }
//}

public class ClientHandler implements Runnable {

    private static final Map<String, PrintWriter> onlineWriters = new ConcurrentHashMap<>();
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
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split("\\|", 4);
                switch (parts[0]) {

                    /* ------------------- ONLINE ------------------- */
                    case "ONLINE":
                        if (parts.length >= 2) {
                            username = parts[1];
                            onlineWriters.put(username, out);
                            out.println("ONLINE_OK");
                            sendOfflineMessages(username);
                        }
                        break;

                    /* ------------------- SIGNUP ------------------- */
                    case "SIGNUP":
                        if (parts.length >= 3) {
                            String newUser = parts[1];
                            String pass = parts[2];
                            if (userExists(newUser)) {
                                out.println("ERROR|Username already taken");
                            } else {
                                saveCredentials(newUser, pass);
                                out.println("SUCCESS");
                            }
                        }
                        break;

                    /* -------------------- LOGIN -------------------- */
                    case "LOGIN":
                        if (parts.length >= 3) {
                            String u = parts[1];
                            String p = parts[2];
                            if (verifyCredentials(u, p)) {
                                username = u;
                                onlineWriters.put(username, out);
                                out.println("SUCCESS");
                                sendOfflineMessages(username);
                                sendChatHistory(username);
                            } else {
                                out.println("ERROR|Invalid credentials");
                            }
                        }
                        break;

                    /* ------------------- PRIVATE ------------------- */
                    case "PRIVATE":
                        if (parts.length >= 4) {
                            String from = parts[1];
                            String to = parts[2];
                            String body = parts[3];

                            if (from.equals(to)) break; // skip self-messages

                            updateChatHistory(from, to, body);

                            PrintWriter target = onlineWriters.get(to);
                            String fullMessage = "PRIVATE|" + from + "|" + to + "|" + body;

                            if (target != null) {
                                target.println(fullMessage);
                            } else {
                                saveOfflineMessage(to, fullMessage);
                            }
                        }
                        break;
                }
            }

        } catch (IOException ignored) {
        } finally {
            if (username != null) onlineWriters.remove(username);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /* ---------------- Credential Helpers ---------------- */
    private static final File CRED_FILE = new File("users.txt");

    private boolean userExists(String user) throws IOException {
        if (!CRED_FILE.exists()) return false;
        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
            String l;
            while ((l = br.readLine()) != null)
                if (l.split("\\|", 2)[0].equals(user)) return true;
            return false;
        }
    }

    private void saveCredentials(String user, String pass) throws IOException {
        if (!CRED_FILE.exists()) CRED_FILE.createNewFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(CRED_FILE, true))) {
            pw.println(user + "|" + pass);
        }
    }

    private boolean verifyCredentials(String user, String pass) throws IOException {
        if (!CRED_FILE.exists()) return false;
        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
            String l;
            while ((l = br.readLine()) != null) {
                String[] p = l.split("\\|", 2);
                if (p.length == 2 && p[0].equals(user) && p[1].equals(pass)) return true;
            }
            return false;
        }
    }

    /* ---------------- Chat & Offline Logic ---------------- */

    private void updateChatHistory(String from, String to, String message) {
        saveMessageToFile(from, to, message);
        saveMessageToFile(to, from, message);
    }

    private synchronized void saveMessageToFile(String sender, String recipient, String message) {
        File dir = new File(CHAT_FOLDER + "/" + recipient);
        if (!dir.exists()) dir.mkdirs();

        File chatFile = new File(dir, sender + ".txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(chatFile, true))) {
            writer.println(sender + ":" + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void saveOfflineMessage(String recipient, String msg) {
        String[] parts = msg.split("\\|", 4);
        if (parts.length < 4) return;

        String from = parts[1];
        String body = parts[3];

        File dir = new File(CHAT_FOLDER + "/" + recipient);
        if (!dir.exists()) dir.mkdirs();

        File offlineFile = new File(dir, from + ".offline.txt");

        try (PrintWriter writer = new PrintWriter(new FileWriter(offlineFile, true))) {
            writer.println(from + ":" + body);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendOfflineMessages(String username) {
        File folder = new File(CHAT_FOLDER + "/" + username);
        if (!folder.exists()) return;

        for (File file : folder.listFiles()) {
            String name = file.getName();
            if (name.endsWith(".offline.txt")) {
                String friend = name.replace(".offline.txt", "");
                try {
                    List<String> lines = Files.readAllLines(file.toPath());
                    Set<String> seen = new HashSet<>();
                    for (String line : lines) {
                        if (seen.add(line)) {  // only if not duplicate
                            PrintWriter writer = onlineWriters.get(username);
                            if (writer != null) {
                                writer.println("OFFLINE_MSG|" + friend + "|" + username + "|" + line);
                            }
                        }
                    }

                    // ✅ Merge into chat history
                    Path permanentFile = Paths.get(CHAT_FOLDER, username, friend + ".txt");
                    Set<String> existing = new HashSet<>();
                    if (Files.exists(permanentFile)) {
                        existing.addAll(Files.readAllLines(permanentFile));
                    }

                    List<String> newLines = lines.stream()
                            .filter(line -> !existing.contains(line))
                            .collect(Collectors.toList());

                    if (!newLines.isEmpty()) {
                        Files.write(permanentFile, newLines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    }


                    // ✅ Delete offline file
                    Files.delete(file.toPath());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendChatHistory(String user) {
        File dir = new File(CHAT_FOLDER + "/" + user);
        if (!dir.exists()) return;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") && !name.endsWith(".offline.txt"));
        if (files == null) return;

        for (File file : files) {
            String friend = file.getName().replace(".txt", "");
            if (friend.equals(user)) {
                file.delete(); // remove self-msgs
                continue;
            }

            List<String> history = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    history.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!history.isEmpty()) {
                String payload = String.join(";;;", history);
                String lineToSend = "CHAT_HISTORY|" + friend + "|" + user + "|" + payload;
                out.println(lineToSend);
            }
        }
    }
}
