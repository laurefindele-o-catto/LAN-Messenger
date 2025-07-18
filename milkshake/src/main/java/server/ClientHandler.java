////package main.java.server;
////
////import java.io.BufferedReader;
////import java.io.IOException;
////import java.io.InputStreamReader;
////import java.io.PrintWriter;
////import java.net.*;
////
////public class  ClientHandler implements Runnable{
////    private Socket socket;
////
////    public ClientHandler(Socket socket){
////        this.socket = socket;
////    }
////
////    public void run(){
////        try{
////            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
////            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
////
////            String request = in.readLine();
////            String [] words = request.split("\\|");
////
////            String task = words[0];
////            String username = words[1];
////            String password = words[2];
////
////            String response;
////
////            if(task.equalsIgnoreCase("SIGNUP")){
////                response = UserManager.register(username, password);
////            }
////            else if(task.equalsIgnoreCase("LOGIN")){
////                response = UserManager.login(username, password);
////            }
////            else{
////                response = "ERROR|Unknown prompt.";
////            }
////
////            out.println(response);
////
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
////    }
////}
//
////
////
////package main.java.server;
////
////import java.io.*;
////import java.net.Socket;
////import java.util.Map;
////import java.util.concurrent.ConcurrentHashMap;
////
/////**
//// * Handles a single client connection on the server side.
//// */
////public class ClientHandler implements Runnable {
////
////    private static final Map<String, PrintWriter> onlineWriters = new ConcurrentHashMap<>();
////
////    private final Socket socket;
////    private BufferedReader in;
////    private PrintWriter out;
////    private String username;
////
////    public ClientHandler(Socket s){ this.socket = s; }
////
////    @Override public void run() {
////        try {
////            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
////            out = new PrintWriter(socket.getOutputStream(), true);
////
////            String line;
////            while ((line = in.readLine()) != null) {
////                String[] parts = line.split("\\|", 4);
////                switch (parts[0]) {
////                    case "LOGIN":
////                        // LOGIN|user
////                        username = parts[1];
////                        onlineWriters.put(username, out);
////                        out.println("LOGIN_SUCCESS");
////                        break;
////
////                    case "PRIVATE":
////                        // PRIVATE|from|to|msg
////                        if (parts.length >= 4) {
////                            String from = parts[1];
////                            String to   = parts[2];
////                            String body = parts[3];
////                            PrintWriter target = onlineWriters.get(to);
////                            if (target != null) {
////                                target.println("PRIVATE|" + from + "|" + to + "|" + body);
////                            }
////                        }
////                        break;
////
////                    // add SIGNUP, etc.
////                }
////            }
////        } catch (IOException ignored) {
////        } finally {
////            if(username!=null) onlineWriters.remove(username);
////            try { socket.close(); } catch (IOException ignored) {}
////        }
////    }
////}
//
//
////11/7/25
////
////package main.java.server;
////
////import java.io.*;
////import java.net.Socket;
////import java.util.Map;
////import java.util.concurrent.ConcurrentHashMap;
////
/////**
//// * Handles a single client connection.
//// * Supports SIGNUP, LOGIN and PRIVATE chat routing.
//// */
////public class ClientHandler implements Runnable {
////
////    // All online users → their print writers
////    private static final Map<String, PrintWriter> onlineWriters = new ConcurrentHashMap<>();
////
////    private final Socket socket;
////    private BufferedReader in;
////    private PrintWriter out;
////    private String username;
////
////    public ClientHandler(Socket socket) { this.socket = socket; }
////
////    @Override
////    public void run() {
////        try {
////            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
////            out = new PrintWriter(socket.getOutputStream(), true);
////
////            String line;
////            while ((line = in.readLine()) != null) {
////                String[] parts = line.split("\\|", 4);
////                switch (parts[0]) {
////                    /* -------------------- SIGNUP -------------------- */
////                    case "SIGNUP":
////                        // SIGNUP|user|pass
////                        if (parts.length >= 3) {
////                            String newUser = parts[1];
////                            String pass    = parts[2];
////                            boolean taken  = userExists(newUser);
////                            if (taken) {
////                                out.println("ERROR|Username already taken");
////                            } else {
////                                saveCredentials(newUser, pass);
////                                out.println("SUCCESS");
////                            }
////                        }
////                        break;
////
////                    /* -------------------- LOGIN -------------------- */
////                    case "LOGIN":
////                        // LOGIN|user|pass
////                        if (parts.length >= 3) {
////                            String u = parts[1];
////                            String p = parts[2];
////                            if (verifyCredentials(u, p)) {
////                                username = u;
////                                onlineWriters.put(username, out);
////                                out.println("SUCCESS");
////                            } else {
////                                out.println("ERROR|Invalid credentials");
////                            }
////                        }
////                        break;
////
////                    /* -------------------- PRIVATE -------------------- */
////                    case "PRIVATE":
////                        // PRIVATE|from|to|msg
////                        if (parts.length >= 4) {
////                            String from = parts[1];
////                            String to   = parts[2];
////                            String body = parts[3];
////                            PrintWriter target = onlineWriters.get(to);
////                            if (target != null) {
////                                target.println("PRIVATE|" + from + "|" + to + "|" + body);
////                            }
////                        }
////                        break;
////                }
////            }
////        } catch (IOException ignored) {
////        } finally {
////            if (username != null) onlineWriters.remove(username);
////            try { socket.close(); } catch (IOException ignored) {}
////        }
////    }
////
////    /* ------------------------------------------------------------------ */
////    /*                       Simple credential store                      */
////    /* ------------------------------------------------------------------ */
////
////    private static final File CRED_FILE = new File("users.txt");
////
////    private boolean userExists(String user) throws IOException {
////        if (!CRED_FILE.exists()) return false;
////        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
////            String l;
////            while ((l = br.readLine()) != null) {
////                if (l.split("\\|", 2)[0].equals(user)) return true;
////            }
////            return false;
////        }
////    }
////
////    private void saveCredentials(String user, String pass) throws IOException {
////        if (!CRED_FILE.exists()) CRED_FILE.createNewFile();
////        try (PrintWriter pw = new PrintWriter(new FileWriter(CRED_FILE, true))) {
////            pw.println(user + "|" + pass);
////        }
////    }
////
////    private boolean verifyCredentials(String user, String pass) throws IOException {
////        if (!CRED_FILE.exists()) return false;
////        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
////            String l;
////            while ((l = br.readLine()) != null) {
////                String[] p = l.split("\\|", 2);
////                if (p.length == 2 && p[0].equals(user) && p[1].equals(pass)) return true;
////            }
////            return false;
////        }
////    }
////}
//
//
//// 11/7/25
//
//package main.java.server;
//
//import main.java.controller.ChatBoxController;
//
//import java.io.*;
//import java.net.Socket;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.stream.Collectors;
//
//import main.java.controller.ChatBoxController.Message;
//
//
//public class ClientHandler implements Runnable {
//
//    private static final Map<String, PrintWriter> onlineWriters = new ConcurrentHashMap<>();
//    private static final String CHAT_FOLDER = "chats";
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
//            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            out = new PrintWriter(socket.getOutputStream(), true);
//
//            String line;
//            while ((line = in.readLine()) != null) {
//                String[] parts = line.split("\\|", 4);
//                switch (parts[0]) {
//
//                    /* ------------------- ONLINE ------------------- */
//                    case "ONLINE":
//                        if (parts.length >= 2) {
//                            username = parts[1];
//                            onlineWriters.put(username, out);
//                            out.println("ONLINE_OK");
//                            sendOfflineMessages(username);
//                        }
//                        break;
//
//                    /* ------------------- SIGNUP ------------------- */
//                    case "SIGNUP":
//                        if (parts.length >= 3) {
//                            String newUser = parts[1];
//                            String pass = parts[2];
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
//                                sendOfflineMessages(username);
//                                sendChatHistory(username);
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
//                            String to = parts[2];
//                            String body = parts[3];
//
//                            if (from.equals(to)) break; // skip self-messages
//
//                            updateChatHistory(from, to, body);
//
//                            PrintWriter target = onlineWriters.get(to);
//                            String fullMessage = "PRIVATE|" + from + "|" + to + "|" + body;
//
//                            if (target != null) {
//                                target.println(fullMessage);
//                            } else {
//                                saveOfflineMessage(to, fullMessage);
//                            }
//                        }
//                        break;
//                }
//            }
//
//        } catch (IOException ignored) {
//        } finally {
//            if (username != null) onlineWriters.remove(username);
//            try { socket.close(); } catch (IOException ignored) {}
//        }
//    }
//
//    /* ---------------- Credential Helpers ---------------- */
//    private static final File CRED_FILE = new File("users.txt");
//
//    private boolean userExists(String user) throws IOException {
//        if (!CRED_FILE.exists()) return false;
//        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
//            String l;
//            while ((l = br.readLine()) != null)
//                if (l.split("\\|", 2)[0].equals(user)) return true;
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
//
//    /* ---------------- Chat & Offline Logic ---------------- */
//
//    private void updateChatHistory(String from, String to, String message) {
//        saveMessageToFile(from, to, message);
//        saveMessageToFile(to, from, message);
//    }
//
//    private synchronized void saveMessageToFile(String sender, String recipient, String message) {
//        File dir = new File(CHAT_FOLDER + "/" + recipient);
//        if (!dir.exists()) dir.mkdirs();
//
//        File chatFile = new File(dir, sender + ".txt");
//        try (PrintWriter writer = new PrintWriter(new FileWriter(chatFile, true))) {
//            writer.println(sender + ":" + message);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private synchronized void saveOfflineMessage(String recipient, String msg) {
//        String[] parts = msg.split("\\|", 4);
//        if (parts.length < 4) return;
//
//        String from = parts[1];
//        String body = parts[3];
//
//        File dir = new File(CHAT_FOLDER + "/" + recipient);
//        if (!dir.exists()) dir.mkdirs();
//
//        File offlineFile = new File(dir, from + ".offline.txt");
//
//        try (PrintWriter writer = new PrintWriter(new FileWriter(offlineFile, true))) {
//            writer.println(from + ":" + body);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void sendOfflineMessages(String username) {
//        File folder = new File(CHAT_FOLDER + "/" + username);
//        if (!folder.exists()) return;
//
//        for (File file : folder.listFiles()) {
//            String name = file.getName();
//            if (name.endsWith(".offline.txt")) {
//                String friend = name.replace(".offline.txt", "");
//                try {
//                    List<String> lines = Files.readAllLines(file.toPath());
//                    Set<String> seen = new HashSet<>();
//                    for (String line : lines) {
//                        if (seen.add(line)) {  // only if not duplicate
//                            PrintWriter writer = onlineWriters.get(username);
//                            if (writer != null) {
//                                writer.println("OFFLINE_MSG|" + friend + "|" + username + "|" + line);
//                            }
//                        }
//                    }
//
//                    // ✅ Merge into chat history
//                    Path permanentFile = Paths.get(CHAT_FOLDER, username, friend + ".txt");
//                    Set<String> existing = new HashSet<>();
//                    if (Files.exists(permanentFile)) {
//                        existing.addAll(Files.readAllLines(permanentFile));
//                    }
//
//                    List<String> newLines = lines.stream()
//                            .filter(line -> !existing.contains(line))
//                            .collect(Collectors.toList());
//
//                    if (!newLines.isEmpty()) {
//                        Files.write(permanentFile, newLines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
//                    }
//
//
//                    // ✅ Delete offline file
//                    Files.delete(file.toPath());
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//    private void sendChatHistory(String user) {
//        File dir = new File(CHAT_FOLDER + "/" + user);
//        if (!dir.exists()) return;
//
//        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") && !name.endsWith(".offline.txt"));
//        if (files == null) return;
//
//        for (File file : files) {
//            String friend = file.getName().replace(".txt", "");
//            if (friend.equals(user)) {
//                file.delete(); // remove self-msgs
//                continue;
//            }
//
//            List<String> history = new ArrayList<>();
//            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    history.add(line);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            if (!history.isEmpty()) {
//                String payload = String.join(";;;", history);
//                String lineToSend = "CHAT_HISTORY|" + friend + "|" + user + "|" + payload;
//                out.println(lineToSend);
//            }
//        }
//    }
//}

// xxxxxxxx-1111111111
//package main.java.server;
//
//import java.io.*;
//import java.net.Socket;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.stream.Collectors;
//
//public class ClientHandler implements Runnable {
//
//    private static final Map<String, PrintWriter> onlineWriters = new ConcurrentHashMap<>();
//    private static final String CHAT_FOLDER = "chats";
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
//            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            out = new PrintWriter(socket.getOutputStream(), true);
//
//            String line;
//            while ((line = in.readLine()) != null) {
//                String[] parts = line.split("\\|", 4);
//                switch (parts[0]) {
//                    case "ONLINE":
//                        if (parts.length >= 2) {
//                            username = parts[1];
//                            onlineWriters.put(username, out);
//                            out.println("ONLINE_OK");
//                            sendOfflineMessages(username);
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
//                                sendOfflineMessages(username);
//                                sendChatHistory(username);
//                            } else {
//                                out.println("ERROR|Invalid credentials");
//                            }
//                        }
//                        break;
//
//                    case "PRIVATE":
//                        if (parts.length >= 4) {
//                            String from = parts[1];
//                            String to = parts[2];
//                            String body = parts[3];
//                            if (from.equals(to)) break;
//
//                            updateChatHistory(from, to, body);
//                            PrintWriter target = onlineWriters.get(to);
//                            String fullMessage = "PRIVATE|" + from + "|" + to + "|" + body;
//                            if (target != null) {
//                                target.println(fullMessage);
//                            } else {
//                                saveOfflineMessage(to, fullMessage);
//                            }
//                        }
//                        break;
//                }
//            }
//        } catch (IOException ignored) {
//        } finally {
//            if (username != null) onlineWriters.remove(username);
//            try {
//                socket.close();
//            } catch (IOException ignored) {
//            }
//        }
//    }
//
//    private static final File CRED_FILE = new File("users.txt");
//
//    private boolean userExists(String user) throws IOException {
//        if (!CRED_FILE.exists()) return false;
//        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
//            String l;
//            while ((l = br.readLine()) != null)
//                if (l.split("\\|", 2)[0].equals(user)) return true;
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
//
//    private void updateChatHistory(String from, String to, String message) {
//        saveMessageToFile(from, to, message);
//        saveMessageToFile(to, from, message);
//    }
//
//    private synchronized void saveMessageToFile(String sender, String recipient, String message) {
//        File dir = new File(CHAT_FOLDER + "/" + recipient);
//        if (!dir.exists()) dir.mkdirs();
//
//        File chatFile = new File(dir, sender + ".txt");
//        try (PrintWriter writer = new PrintWriter(new FileWriter(chatFile, true))) {
//            writer.println(sender + ":" + message);
//        } catch (IOException e) {
//            System.err.println("Failed to save message for " + recipient + ": " + e.getMessage());
//        }
//    }
//
//    private synchronized void saveOfflineMessage(String recipient, String msg) {
//        String[] parts = msg.split("\\|", 4);
//        if (parts.length < 4) return;
//
//        String from = parts[1];
//        String body = parts[3];
//
//        File dir = new File(CHAT_FOLDER + "/" + recipient);
//        if (!dir.exists()) dir.mkdirs();
//
//        File offlineFile = new File(dir, from + ".offline.txt");
//        try (PrintWriter writer = new PrintWriter(new FileWriter(offlineFile, true))) {
//            writer.println(from + ":" + body);
//        } catch (IOException e) {
//            System.err.println("Failed to save offline message for " + recipient + ": " + e.getMessage());
//        }
//    }
//
//    private void sendOfflineMessages(String username) {
//        File folder = new File(CHAT_FOLDER + "/" + username);
//        if (!folder.exists()) return;
//
//        File[] files = folder.listFiles();
//        if (files == null) return;
//
//        for (File file : files) {
//            String name = file.getName();
//            if (name.endsWith(".offline.txt")) {
//                String friend = name.replace(".offline.txt", "");
//                try {
//                    List<String> lines = Files.readAllLines(file.toPath());
//                    Set<String> seen = new HashSet<>();
//                    for (String line : lines) {
//                        if (seen.add(line)) {
//                            PrintWriter writer = onlineWriters.get(username);
//                            if (writer != null) {
//                                String msg = "OFFLINE_MSG|" + friend + "|" + username + "|" + line;
//                                System.out.println("Sending to " + username + ": " + msg);
//                                writer.println(msg);
//                            }
//                        }
//                    }
//
//                    Path permanentFile = Paths.get(CHAT_FOLDER, username, friend + ".txt");
//                    Set<String> existing = new HashSet<>();
//                    if (Files.exists(permanentFile)) {
//                        existing.addAll(Files.readAllLines(permanentFile));
//                    }
//
//                    List<String> newLines = lines.stream()
//                            .filter(line -> !existing.contains(line))
//                            .collect(Collectors.toList());
//
//                    if (!newLines.isEmpty()) {
//                        Files.write(permanentFile, newLines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
//                    }
//
//                    Files.delete(file.toPath());
//                } catch (IOException e) {
//                    System.err.println("Failed to process offline messages for " + username + ": " + e.getMessage());
//                }
//            }
//        }
//    }
//
//    private void sendChatHistory(String user) {
//        File dir = new File(CHAT_FOLDER + "/" + user);
//        if (!dir.exists()) return;
//
//        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") && !name.endsWith(".offline.txt"));
//        if (files == null) return;
//
//        for (File file : files) {
//            String friend = file.getName().replace(".txt", "");
//            if (friend.equals(user)) {
//                file.delete();
//                continue;
//            }
//
//            List<String> history = new ArrayList<>();
//            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    history.add(line);
//                }
//            } catch (IOException e) {
//                System.err.println("Failed to read chat history for " + user + " and " + friend + ": " + e.getMessage());
//            }
//
//            if (!history.isEmpty()) {
//                String payload = String.join(";;;", history);
//                String lineToSend = "CHAT_HISTORY|" + friend + "|" + username + "|" + payload;
//                out.println(lineToSend);
//            }
//        }
//    }
//}
//
////xxxxxxxxx-2222222222222222222
//package main.java.server;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.nio.file.*;
//
//public class ClientHandler implements Runnable {
//
//    private static final Map<String, PrintWriter> onlineWriters = new ConcurrentHashMap<>();
//    private static final String CHAT_FOLDER = "chats";
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
//            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            out = new PrintWriter(socket.getOutputStream(), true);
//
//            String line;
//            while ((line = in.readLine()) != null) {
//                String[] parts = line.split("\\|", 4);
//                switch (parts[0]) {
//                    case "ONLINE":
//                        if (parts.length >= 2) {
//                            username = parts[1];
//                            onlineWriters.put(username, out);
//                            out.println("ONLINE_OK");
//                            sendOfflineMessages(username, out);
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
//                        if (parts.length >= 4) {
//                            String from = parts[1];
//                            String to = parts[2];
//                            String body = parts[3];
//                            if (from.equals(to)) break;
//
//                            updateChatHistory(from, to, body);
//                            PrintWriter target = onlineWriters.get(to);
//                            String fullMessage = "PRIVATE|" + from + "|" + to + "|" + body;
//                            if (target != null) {
//                                target.println(fullMessage);
//                            } else {
//                                saveOfflineMessage(to, from, body);
//                            }
//                        }
//                        break;
//                }
//            }
//        } catch (IOException ignored) {
//        } finally {
//            if (username != null) onlineWriters.remove(username);
//            try {
//                socket.close();
//            } catch (IOException ignored) {
//            }
//        }
//    }
//
//    private static final File CRED_FILE = new File("users.txt");
//
//    private boolean userExists(String user) throws IOException {
//        if (!CRED_FILE.exists()) return false;
//        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
//            String l;
//            while ((l = br.readLine()) != null)
//                if (l.split("\\|", 2)[0].equals(user)) return true;
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
//
//    private void updateChatHistory(String from, String to, String message) {
//        saveMessageToFile(from, to, message);
//        saveMessageToFile(to, from, message);
//    }
//
//    private void saveMessageToFile(String sender, String recipient, String message) {
//        File dir = new File(CHAT_FOLDER + "/" + recipient);
//        if (!dir.exists()) dir.mkdirs();
//
//        File chatFile = new File(dir, sender + ".txt");
//        try (PrintWriter writer = new PrintWriter(new FileWriter(chatFile, true))) {
//            writer.println(sender + ":" + message);
//        } catch (IOException e) {
//            System.err.println("Failed to save message for " + recipient + ": " + e.getMessage());
//        }
//    }
//
//    private void saveOfflineMessage(String to, String from, String message) {
//        File dir = new File(CHAT_FOLDER + "/" + to);
//        if (!dir.exists()) dir.mkdirs();
//
//        File offlineFile = new File(dir, from + ".offline.txt");
//        try (PrintWriter writer = new PrintWriter(new FileWriter(offlineFile, true))) {
//            writer.println(from + ":" + message);
//            System.out.println("Saved offline message to " + offlineFile.getPath());
//        } catch (IOException e) {
//            System.err.println("Failed to save offline message for " + to + ": " + e.getMessage());
//        }
//    }
//
//    private void sendOfflineMessages(String username, PrintWriter writer) {
//        File folder = new File(CHAT_FOLDER + "/" + username);
//        if (!folder.exists()) return;
//
//        File[] files = folder.listFiles((d, name) -> name.endsWith(".offline.txt"));
//        if (files == null) return;
//
//        for (File file : files) {
//            String friend = file.getName().replace(".offline.txt", "");
//            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
//                String line;
//                Set<String> seen = new HashSet<>();
//                while ((line = br.readLine()) != null) {
//                    if (seen.add(line)) {
//                        String msg = "OFFLINE_MSG|" + friend + "|" + username + "|" + line;
//                        System.out.println("Sending: " + msg);
//                        writer.println(msg);
//                    }
//                }
//                // Delete the offline file after sending
//                file.delete();
//            } catch (IOException e) {
//                System.err.println("Error sending offline messages: " + e.getMessage());
//            }
//        }
//    }
//
//    private void sendChatHistory(String username, PrintWriter writer) {
//        File dir = new File(CHAT_FOLDER + "/" + username);
//        if (!dir.exists()) return;
//
//        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") && !name.endsWith(".offline.txt"));
//        if (files == null) return;
//
//        for (File file : files) {
//            String friend = file.getName().replace(".txt", "");
//            if (friend.equals(username)) {
//                file.delete();
//                continue;
//            }
//
//            List<String> history = new ArrayList<>();
//            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    history.add(line);
//                }
//            } catch (IOException e) {
//                System.err.println("Failed to read chat history for " + username + " and " + friend + ": " + e.getMessage());
//            }
//
//            if (!history.isEmpty()) {
//                String payload = String.join(";;;", history);
//                String lineToSend = "CHAT_HISTORY|" + friend + "|" + username + "|" + payload;
//                writer.println(lineToSend);
//            }
//        }
//    }
//}

/////xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx-3333333333333333333333333333
package main.java.server;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split("\\|", 4);
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
                        if (parts.length >= 4) {
                            String from = parts[1];
                            String to = parts[2];
                            String body = parts[3];
                            if (from.equals(to)) break;

                            updateChatHistory(from, to, body);
                            PrintWriter target = onlineWriters.get(to);
                            String fullMessage = "PRIVATE|" + from + "|" + to + "|" + body;
                            if (target != null) {
                                target.println(fullMessage);
                            } else {
                                saveOfflineMessage(to, from, body);
                            }
                        }
                        break;
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (username != null) onlineWriters.remove(username);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

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

    private void updateChatHistory(String from, String to, String message) {
        String line = from + ":" + message;
        appendToChatFile(to, from, line);   // For recipient: chats/to/from.txt
        appendToChatFile(from, to, line);   // For sender: chats/from/to.txt
    }

    private void appendToChatFile(String user, String friend, String line) {
        File dir = new File(CHAT_FOLDER + "/" + user);
        if (!dir.exists()) dir.mkdirs();
        File chatFile = new File(dir, friend + ".txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(chatFile, true))) {
            writer.println(line);
        } catch (IOException e) {
            System.err.println("Failed to append to chat file for " + user + ": " + e.getMessage());
        }
    }

    private void saveOfflineMessage(String to, String from, String message) {
        File dir = new File(CHAT_FOLDER + "/" + to);
        if (!dir.exists()) dir.mkdirs();
        File offlineFile = new File(dir, from + ".offline.txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(offlineFile, true))) {
            writer.println(from + ":" + message);
        } catch (IOException e) {
            System.err.println("Failed to save offline message for " + to + ": " + e.getMessage());
        }
    }

    private void sendOfflineMessages(String username, PrintWriter writer) {
        File folder = new File(CHAT_FOLDER + "/" + username);
        if (!folder.exists()) return;
        File[] files = folder.listFiles((d, name) -> name.endsWith(".offline.txt"));
        if (files == null) return;
        for (File file : files) {
            String friend = file.getName().replace(".offline.txt", "");
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String msg = "OFFLINE_MSG|" + friend + "|" + username + "|" + line;
                    writer.println(msg);
                }
                file.delete();
            } catch (IOException e) {
                System.err.println("Error sending offline messages: " + e.getMessage());
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
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    history.add(line);
                }
            } catch (IOException e) {
                System.err.println("Failed to read chat history for " + username + " and " + friend + ": " + e.getMessage());
            }
            if (!history.isEmpty()) {
                String payload = String.join(";;;", history);
                String lineToSend = "CHAT_HISTORY|" + friend + "|" + username + "|" + payload;
                writer.println(lineToSend);
            }
        }
    }
}