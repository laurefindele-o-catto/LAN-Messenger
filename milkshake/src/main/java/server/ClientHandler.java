//
//package main.java.server;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
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
//                            sendChatHistory(username, out);
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
//                            if (from.equals(to)) {
//                                break;
//                            }
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
//    private static final File CRED_FILE = new File("users.txt");
//
//    private boolean userExists(String user) throws IOException {
//        if (!CRED_FILE.exists()) {
//            return false;
//        }
//        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
//            String l;
//            while ((l = br.readLine()) != null) {
//                String[] parts = l.split("\\|", 2);
//                if (parts[0].equals(user)) {
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
//        try (PrintWriter pw = new PrintWriter(new FileWriter(CRED_FILE, true))) {
//            pw.println(user + "|" + pass);
//        }
//    }
//
//    private boolean verifyCredentials(String user, String pass) throws IOException {
//        if (!CRED_FILE.exists()) {
//            return false;
//        }
//        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
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
//    private void updateChatHistory(String from, String to, String message) {
//        String line = from + ":" + message;
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
//        try (PrintWriter writer = new PrintWriter(new FileWriter(chatFile, true))) {
//            writer.println(line);
//        } catch (IOException e) {
//            System.err.println("Failed to append to chat file for " + user + ": " + e.getMessage());
//        }
//    }
//
//    private void saveOfflineMessage(String to, String from, String message) {
//        File dir = new File(CHAT_FOLDER + "/" + to);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        File offlineFile = new File(dir, from + ".offline.txt");
//        try (PrintWriter writer = new PrintWriter(new FileWriter(offlineFile, true))) {
//            writer.println(from + ":" + message);
//        } catch (IOException e) {
//            System.err.println("Failed to save offline message for " + to + ": " + e.getMessage());
//        }
//    }
//
//    private void sendOfflineMessages(String username, PrintWriter writer) {
//        File folder = new File(CHAT_FOLDER + "/" + username);
//        if (!folder.exists()) {
//            return;
//        }
//        File[] files = folder.listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String name) {
//                return name.endsWith(".offline.txt");
//            }
//        });
//        if (files == null) {
//            return;
//        }
//        for  (File file : files) {
//            String friend = file.getName().replace(".offline.txt", "");
//            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
//                String line;
//                while ((line = br.readLine()) != null) {
//                    String msg = "OFFLINE_MSG|" + friend + "|" + username + "|" + line;
//                    writer.println(msg);
//                }
//                file.delete();
//            } catch (IOException e) {
//                System.err.println("Error sending offline messages: " + e.getMessage());
//            }
//        }
//    }
//
//    private void sendChatHistory(String username, PrintWriter writer) {
//        File dir = new File(CHAT_FOLDER + "/" + username);
//        if (!dir.exists()) {
//            return;
//        }
//        File[] files = dir.listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File d, String name) {
//                return name.endsWith(".txt") && !name.endsWith(".offline.txt");
//            }
//        });
//        if (files == null) {
//            return;
//        }
//        for (File file : files) {
//            String friend = file.getName().replace(".txt", "");
//            if (friend.equals(username)) {
//                file.delete();
//                continue;
//            }
//            List<String> history = new ArrayList<>();
//            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    history.add(line);
//                }
//            } catch (IOException e) {
//                System.err.println("Failed to read chat history for " + username + " and " + friend + ": " + e.getMessage());
//            }
//            if (!history.isEmpty()) {
//                String payload = String.join(";;;", history);
//                String lineToSend = "CHAT_HISTORY|" + friend + "|" + username + "|" + payload;
//                writer.println(lineToSend);
//            }
//        }
//    }
//}
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
                        if (parts.length >= 5) {
                            String from = parts[1];
                            String to = parts[2];
                            String timestamp = parts[3];
                            String body = parts[4];
                            if (from.equals(to)) {
                                break;
                            }

                            updateChatHistory(from, to, timestamp, body);
                            PrintWriter target = onlineWriters.get(to);
                            String fullMessage = "PRIVATE|" + from + "|" + to + "|" + timestamp + "|" + body;
                            if (target != null) {
                                target.println(fullMessage);
                            } else {
                                saveOfflineMessage(to, from, timestamp, body);
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

    private static final File CRED_FILE = new File("users.txt");

    private boolean userExists(String user) throws IOException {
        if (!CRED_FILE.exists()) {
            return false;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
            String l;
            while ((l = br.readLine()) != null) {
                String[] parts = l.split("\\|", 2);
                if (parts[0].equals(user)) {
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
        try (PrintWriter pw = new PrintWriter(new FileWriter(CRED_FILE, true))) {
            pw.println(user + "|" + pass);
        }
    }

    private boolean verifyCredentials(String user, String pass) throws IOException {
        if (!CRED_FILE.exists()) {
            return false;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
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

    private void updateChatHistory(String from, String to, String timestamp, String message) {
        String line = from + "|" + timestamp + "|" + message;
        appendToChatFile(to, from, line);
        appendToChatFile(from, to, line);
    }

    private void appendToChatFile(String user, String friend, String line) {
        File dir = new File(CHAT_FOLDER + "/" + user);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File chatFile = new File(dir, friend + ".txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(chatFile, true))) {
            writer.println(line);
        } catch (IOException e) {
            System.err.println("Failed to append to chat file for " + user + ": " + e.getMessage());
        }
    }

    private void saveOfflineMessage(String to, String from, String timestamp, String message) {
        File dir = new File(CHAT_FOLDER + "/" + to);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File offlineFile = new File(dir, from + ".offline.txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(offlineFile, true))) {
            writer.println(from + "|" + timestamp + "|" + message);
        } catch (IOException e) {
            System.err.println("Failed to save offline message for " + to + ": " + e.getMessage());
        }
    }

    private void sendOfflineMessages(String username, PrintWriter writer) {
        File folder = new File(CHAT_FOLDER + "/" + username);
        if (!folder.exists()) {
            return;
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".offline.txt"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String friend = file.getName().replace(".offline.txt", "");
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|", 3);
                    if (parts.length == 3) {
                        String from = parts[0];
                        String timestamp = parts[1];
                        String message = parts[2];
                        writer.println("OFFLINE_MSG|" + from + "|" + username + "|" + timestamp + "|" + message);
                    }
                }
                file.delete();
            } catch (IOException e) {
                System.err.println("Error sending offline messages: " + e.getMessage());
            }
        }
    }

    private void sendChatHistory(String username, PrintWriter writer) {
        File dir = new File(CHAT_FOLDER + "/" + username);
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") && !name.endsWith(".offline.txt"));
        if (files == null) {
            return;
        }
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