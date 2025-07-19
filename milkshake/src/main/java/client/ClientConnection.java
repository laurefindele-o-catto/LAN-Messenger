//
// //original
//package main.java.client;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Queue;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
///**
// * One persistent socket per running client. Announces ONLINE as soon as it
// * connects so the server can route incoming PRIVATE messages.
// */
//public class ClientConnection {
//
//    /* ---------------- singleton ---------------- */
//    private static ClientConnection INSTANCE;
//    private final Queue<String> pendingMessages = new LinkedList<>();
//    private final ExecutorService dispatchPool = Executors.newSingleThreadExecutor();
//
//    public static ClientConnection getInstance() {
//        if (INSTANCE == null) INSTANCE = new ClientConnection();
//        return INSTANCE;
//    }
//
//    /* ---------------- listeners ---------------- */
//    public interface MessageListener { void onMessageReceived(String from, String body); }
//    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
//    public void registerListener(MessageListener l) {
//        System.out.println("registering listener");
//        listeners.add(l);
//        synchronized (pendingMessages) {
//            while (!pendingMessages.isEmpty()) {
//                String line = pendingMessages.poll();
//                System.out.println("Dispatching buffered: " + line);
//                dispatch(line);
//            }
//        }
//    }
//    public void removeListener(MessageListener l)   { listeners.remove(l); }
//
//    /* ---------------- socket ---------------- */
//    private Socket socket;
//    private PrintWriter out;
//    private String username;
//
//    private ClientConnection() {}
//
//    /**
//     * Opens the socket **and immediately sends ONLINE|username** so the server
//     * stores this writer in its online map.
//     */
//    public boolean connect(String host, int port, String user) {
//        try {
//            socket   = new Socket(host, port);
//            out      = new PrintWriter(socket.getOutputStream(), true);
//            username = user;
//
//            // start inbound reader
//            new Thread(this::listen, "ClientConnection-Reader").start();
//
//            // announce presence
//            out.println("ONLINE|" + username);
//            return true;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /* high-level helper */
//    public void sendPrivateMessage(String to, String body) {
//        out.println("PRIVATE|" + username + "|" + to + "|" + body);
//    }
//
//    /* listen loop – dispatches PRIVATE packets to registered listeners */
//    private void listen() {
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                if (listeners.isEmpty()) {
//                    // Buffer messages until listeners are registered
//                    synchronized (pendingMessages) {
//                        //debugging
//                        System.out.println("Buffering message: " + line);
//                        pendingMessages.offer(line); // 🧠 buffer until listener is ready
//                    }
//
//                    //for debugging
//                    System.out.println("[Received] " + line);
//                }else {
//                    dispatch(line);
//                }
//            }
//        } catch (IOException ignored) {}
//    }
//
//    private void dispatch(String line) {
//        String[] p = line.split("\\|", 4);
//        if ((p[0].equals("PRIVATE") || p[0].equals("OFFLINE_MSG")) && p.length == 4) {
//            String from = p[1];
//            String msg  = p[3];
//            for (MessageListener l : listeners) {
//                System.out.println("Dispatching to listeners: " + from + " -> " + msg);
//                dispatchPool.submit(() -> l.onMessageReceived(from, msg));
//            }
//        }
//    }
//}
//
///XXXXXXXXXXXXXXXXXXXXXXXXXX-111111111
//

//
//package main.java.client;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Queue;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class ClientConnection {
//
//    private static ClientConnection INSTANCE;
//    private final Queue<String> pendingMessages = new LinkedList<>();
//    private final ExecutorService dispatchPool = Executors.newSingleThreadExecutor();
//
//    public static ClientConnection getInstance() {
//        if (INSTANCE == null) INSTANCE = new ClientConnection();
//        return INSTANCE;
//    }
//
//    public interface MessageListener {
//        void onMessageReceived(String from, String body);
//    }
//    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
//
//    public void registerListener(MessageListener l) {
//        listeners.add(l);
//        synchronized (pendingMessages) {
//            while (!pendingMessages.isEmpty()) {
//                dispatch(pendingMessages.poll());
//            }
//        }
//    }
//    public void removeListener(MessageListener l) {
//        listeners.remove(l);
//    }
//
//    private Socket socket;
//    private PrintWriter out;
//    private String username;
//
//    private ClientConnection() {}
//
//    public boolean connect(String host, int port, String user) {
//        try {
//            socket = new Socket(host, port);
//            out = new PrintWriter(socket.getOutputStream(), true);
//            username = user;
//
//            new Thread(this::listen, "ClientConnection-Reader").start();
//
//            out.println("ONLINE|" + username);
//            return true;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    public void sendPrivateMessage(String to, String body) {
//        out.println("PRIVATE|" + username + "|" + to + "|" + body);
//    }
//
//    private void listen() {
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                if (listeners.isEmpty()) {
//                    synchronized (pendingMessages) {
//                        pendingMessages.offer(line);
//                    }
//                } else {
//                    dispatch(line);
//                }
//            }
//        } catch (IOException ignored) {}
//    }
//
//    private void dispatch(String line) {
//        String[] p = line.split("\\|", 4);
//        String header = p[0];
//
//        if ("PRIVATE".equals(header) && p.length == 4) {
//            String from = p[1];
//            String msg = p[3];
//            for (MessageListener l : listeners) {
//                dispatchPool.submit(() -> l.onMessageReceived(from, msg));
//            }
//        } else if ("OFFLINE_MSG".equals(header) && p.length == 4) {
//            String from = p[1];
//            String[] messageParts = p[3].split(":", 2);
//            String msg = messageParts.length == 2 ? messageParts[1] : p[3];
//            for (MessageListener l : listeners) {
//                dispatchPool.submit(() -> l.onMessageReceived(from, msg));
//            }
//        } else {
//            String from = p.length > 1 ? p[1] : "";
//            for (MessageListener l : listeners) {
//                dispatchPool.submit(() -> l.onMessageReceived(from, line));
//            }
//        }
//    }
//}

//
////xxxxxxxxxxxxxxxxx-22222222222222222
//package main.java.client;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.*;
//import java.util.concurrent.*;
//
//public class ClientConnection {
//
//    private static ClientConnection INSTANCE;
//    private final Queue<String> pendingMessages = new LinkedList<>();
//    private final ExecutorService dispatchPool = Executors.newSingleThreadExecutor();
//
//    public static ClientConnection getInstance() {
//        if (INSTANCE == null) INSTANCE = new ClientConnection();
//        return INSTANCE;
//    }
//
//    public interface MessageListener {
//        void onMessageReceived(String from, String body);
//    }
//    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
//
//    public void registerListener(MessageListener l) {
//        listeners.add(l);
//        synchronized (pendingMessages) {
//            while (!pendingMessages.isEmpty()) {
//                dispatch(pendingMessages.poll());
//            }
//        }
//    }
//    public void removeListener(MessageListener l) {
//        listeners.remove(l);
//    }
//
//    private Socket socket;
//    private PrintWriter out;
//    private String username;
//
//    private ClientConnection() {}
//
//    public boolean connect(String host, int port, String user) {
//        try {
//            socket = new Socket(host, port);
//            out = new PrintWriter(socket.getOutputStream(), true);
//            username = user;
//
//            new Thread(this::listen, "ClientConnection-Reader").start();
//
//            out.println("ONLINE|" + username);
//            return true;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    public void sendPrivateMessage(String to, String body) {
//        out.println("PRIVATE|" + username + "|" + to + "|" + body);
//    }
//
//    private void listen() {
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                System.out.println("Client received: " + line);
//                if (listeners.isEmpty()) {
//                    synchronized (pendingMessages) {
//                        pendingMessages.offer(line);
//                    }
//                } else {
//                    dispatch(line);
//                }
//            }
//        } catch (IOException ignored) {}
//    }
//
//    private void dispatch(String line) {
//        String[] p = line.split("\\|", 4);
//        String header = p[0];
//
//        if ("PRIVATE".equals(header) && p.length == 4) {
//            String from = p[1];
//            String msg = p[3];
//            for (MessageListener l : listeners) {
//                dispatchPool.submit(() -> l.onMessageReceived(from, msg));
//            }
//        } else if ("OFFLINE_MSG".equals(header) && p.length == 4) {
//            String from = p[1];
//            String[] messageParts = p[3].split(":", 2);
//            String msg = messageParts.length == 2 ? messageParts[1] : p[3];
//            for (MessageListener l : listeners) {
//                dispatchPool.submit(() -> l.onMessageReceived(from, msg));
//            }
//        } else {
//            String from = p.length > 1 ? p[1] : "";
//            for (MessageListener l : listeners) {
//                dispatchPool.submit(() -> l.onMessageReceived(from, line));
//            }
//        }
//    }
//}

// xxxxxxxxxxxxxxxxxxxxx-33333333333333333
//package main.java.client;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.*;
//import java.util.concurrent.*;
//
//public class ClientConnection {
//
//    private static ClientConnection INSTANCE;
//    private final Queue<String> pendingMessages = new LinkedList<>();
//    private final ExecutorService dispatchPool = Executors.newSingleThreadExecutor();
//
//    public static ClientConnection getInstance() {
//        if (INSTANCE == null) INSTANCE = new ClientConnection();
//        return INSTANCE;
//    }
//
//    public interface MessageListener {
//        void onMessageReceived(String from, String body);
//    }
//    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
//
//    public void registerListener(MessageListener l) {
//        listeners.add(l);
//        synchronized (pendingMessages) {
//            while (!pendingMessages.isEmpty()) {
//                dispatch(pendingMessages.poll());
//            }
//        }
//    }
//    public void removeListener(MessageListener l) {
//        listeners.remove(l);
//    }
//
//    private Socket socket;
//    private PrintWriter out;
//    private String username;
//
//    private ClientConnection() {}
//
//    public boolean connect(String host, int port, String user) {
//        if (socket != null && !socket.isClosed()) {
//            System.out.println("Already connected, skipping new connection");
//            return true;
//        }
//        try {
//            socket = new Socket(host, port);
//            out = new PrintWriter(socket.getOutputStream(), true);
//            username = user;
//
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
//    public void sendPrivateMessage(String to, String body) {
//        out.println("PRIVATE|" + username + "|" + to + "|" + body);
//    }
//
//    private void listen() {
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                System.out.println("Client received: " + line);
//                if (listeners.isEmpty()) {
//                    synchronized (pendingMessages) {
//                        pendingMessages.offer(line);
//                    }
//                } else {
//                    dispatch(line);
//                }
//            }
//        } catch (IOException ignored) {}
//    }
//
//    private void dispatch(String line) {
//        String[] p = line.split("\\|", 4);
//        String header = p[0];
//
//        if ("PRIVATE".equals(header) && p.length == 4) {
//            String from = p[1];
//            String msg = p[3];
//            for (MessageListener l : listeners) {
//                dispatchPool.submit(() -> l.onMessageReceived(from, msg));
//            }
//        } else if ("OFFLINE_MSG".equals(header) && p.length == 4) {
//            String from = p[1];
//            String[] messageParts = p[3].split(":", 2);
//            String msg = messageParts.length == 2 ? messageParts[1] : p[3];
//            for (MessageListener l : listeners) {
//                dispatchPool.submit(() -> l.onMessageReceived(from, msg));
//            }
//        } else {
//            String from = p.length > 1 ? p[1] : "";
//            for (MessageListener l : listeners) {
//                dispatchPool.submit(() -> l.onMessageReceived(from, line));
//            }
//        }
//    }
//}

package main.java.client;

import java.io.*;
import java.net.Socket;
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
        // Re-send ONLINE message to ensure server sends chat history and offline messages
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

    private ClientConnection() {}

    public boolean connect(String host, int port, String user) {
        if (socket != null && !socket.isClosed()) {
            System.out.println("Already connected, skipping new connection");
            return true;
        }
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            username = user;

            new Thread(this::listen, "ClientConnection-Reader").start();

            out.println("ONLINE|" + username);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
            return false;
        }
    }

    public void sendPrivateMessage(String to, String body) {
        out.println("PRIVATE|" + username + "|" + to + "|" + body);
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
        String[] p = line.split("\\|", 4);
        String header = p[0];

        if ("PRIVATE".equals(header) && p.length == 4) {
            String from = p[1];
            String msg = p[3];
            for (MessageListener l : listeners) {
                dispatchPool.submit(() -> l.onMessageReceived(from, msg));
            }
        } else if ("OFFLINE_MSG".equals(header) && p.length == 4) {
            String from = p[1];
            String[] messageParts = p[3].split(":", 2);
            String msg = messageParts.length == 2 ? messageParts[1] : p[3];
            for (MessageListener l : listeners) {
                dispatchPool.submit(() -> l.onMessageReceived(from, msg));
            }
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
}