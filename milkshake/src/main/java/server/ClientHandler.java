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

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles a single client connection.
 * Supports ONLINE (presence), SIGNUP, LOGIN and PRIVATE chat routing.
 */
public class ClientHandler implements Runnable {

    private static final Map<String, PrintWriter> onlineWriters = new ConcurrentHashMap<>();

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket) { this.socket = socket; }

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
                        // ONLINE|username
                        if (parts.length >= 2) {
                            username = parts[1];
                            onlineWriters.put(username, out);
                            out.println("ONLINE_OK");
                        }
                        break;

                    /* ------------------- SIGNUP ------------------- */
                    case "SIGNUP":
                        if (parts.length >= 3) {
                            String newUser = parts[1];
                            String pass    = parts[2];
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
                            } else {
                                out.println("ERROR|Invalid credentials");
                            }
                        }
                        break;

                    /* ------------------- PRIVATE ------------------- */
                    case "PRIVATE":
                        if (parts.length >= 4) {
                            String from = parts[1];
                            String to   = parts[2];
                            String body = parts[3];
                            PrintWriter target = onlineWriters.get(to);
                            if (target != null) {
                                target.println("PRIVATE|" + from + "|" + to + "|" + body);
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

    /* ---------------- credential helpers ---------------- */
    private static final File CRED_FILE = new File("users.txt");

    private boolean userExists(String user) throws IOException {
        if (!CRED_FILE.exists()) return false;
        try (BufferedReader br = new BufferedReader(new FileReader(CRED_FILE))) {
            String l; while ((l = br.readLine()) != null) if (l.split("\\|",2)[0].equals(user)) return true;
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
            String l; while ((l = br.readLine()) != null) {
                String[] p = l.split("\\|",2);
                if (p.length==2 && p[0].equals(user) && p[1].equals(pass)) return true;
            }
            return false;
        }
    }
}