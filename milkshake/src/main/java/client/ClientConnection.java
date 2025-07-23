//
//package main.java.client;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.*;
//import java.util.concurrent.*;
//
//public class ClientConnection {
//
//    private static ClientConnection INSTANCE; // SingleTon ensure korte
//    private final Queue<String> pendingMessages = new LinkedList<>();
//    private final ExecutorService dispatchPool = Executors.newSingleThreadExecutor();
//
//    public static ClientConnection getInstance() {
//        if (INSTANCE == null) {
//            INSTANCE = new ClientConnection(); // GETTER FUNCTION FOR THE SINGLETON
//        }
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
//        // Re-send ONLINE message to ensure server sends chat history and offline messages
//        if (socket != null && !socket.isClosed() && username != null) {
//            out.println("ONLINE|" + username);
//        }
//        synchronized (pendingMessages) {
//            while (!pendingMessages.isEmpty()) {
//                dispatch(pendingMessages.poll());
//            }
//        }
//    }
//
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
//    public void requestChatHistory(String username) {
//        if (out != null) {
//            out.println("ONLINE|" + username);
//        }
//    }
//
//    private void listen() {
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) { //lISTENS FROM THE SERVER
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
//        } catch (IOException ignored) {
//            System.out.println("Connection closed or error in listen loop");
//        }
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
//
//    public void close() {
//        try {
//            if (socket != null && !socket.isClosed()) {
//                socket.close();
//            }
//        } catch (IOException e) {
//            System.err.println("Error closing socket: " + e.getMessage());
//        }
//    }
//}
//
//
package main.java.client;

import java.io.*;
import java.net.Socket;
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
        String timestamp = LocalDateTime.now().toString();
        out.println("PRIVATE|" + username + "|" + to + "|" + timestamp + "|" + body);
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
        String[] p = line.split("\\|", 5);
        String header = p[0];

        if ("PRIVATE".equals(header) && p.length == 5) {
            String from = p[1];
            String timestamp = p[3];
            String msg = p[4];
            String body = timestamp + "|" + msg;
            for (MessageListener l : listeners) {
                dispatchPool.submit(() -> l.onMessageReceived(from, body));
            }
        } else if ("OFFLINE_MSG".equals(header) && p.length == 5) {
            String from = p[1];
            String timestamp = p[3];
            String msg = p[4];
            String body = timestamp + "|" + msg;
            for (MessageListener l : listeners) {
                dispatchPool.submit(() -> l.onMessageReceived(from, body));
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
////// CLIENT CONNECTION is a singleton  class , that is used to manage the client's connection to the server(Client Handler ) using sockets.
//////Used for sending, receiving and notifying listeners
////
//////  private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();  here, this is a threadSafe Version of the arraylist.
////// PrintWriter use korchi for strings and characters
////
////
//////ONLINE | IS BEING USED FOR BOTH LOGIN AND CHAThISTORY, SO THERE'S SCOPE FOR REDUNDANCY
////
////
////// METHODS:
//////1) public static ClientConnection getInstance() : GETTER FUNCTION FOR SINGLETON
//////2)
//
//
