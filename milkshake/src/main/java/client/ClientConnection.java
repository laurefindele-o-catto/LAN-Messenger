//package main.java.client;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.net.*;
//import java.util.List;
//import java.util.concurrent.CopyOnWriteArrayList;
//
//public class ClientConnection {
//
//    public interface MessageListener{ void onMessageReceived(String fromUser,String body);}
//    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
//    public void registerListener(MessageListener l){ listeners.add(l);}
//    public void removeListener(MessageListener l){ listeners.remove(l);}
//
//    public void sendPrivateMessage(String to,String body){
//        out.println("PRIVATE|"+username+"|"+to+"|"+body);
//    }
//    public static String send(String message) throws IOException {
//        try(Socket socket = new Socket("localhost", 12345);
//            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())))
//        {
//
//            out.println(message);
//            return in.readLine();
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//            return "ERROR|Cannot connect to server";
//        }
//    }
//
//    public static synchronized ClientConnection getInstance() {
//        if (instance == null) {
//            try {
//                instance = new ClientConnection();
//            } catch (IOException e) {
//                throw new RuntimeException("Unable to connect to server", e);
//            }
//        }
//        return instance;
//    }
//}






// version 2.0

//        package main.java.client;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.List;
//import java.util.concurrent.CopyOnWriteArrayList;
//
///**
// * Singleton wrapper around a persistent socket to the chat server.
// */
//public class ClientConnection {
//
//    /* ========= Singleton ========= */
//    private static ClientConnection INSTANCE;
//    public static ClientConnection getInstance(){
//        if(INSTANCE==null) INSTANCE = new ClientConnection();
//        return INSTANCE;
//    }
//
//    /* ========= Messaging ========= */
//    public interface MessageListener{ void onMessageReceived(String fromUser,String body);}
//    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
//    public void registerListener(MessageListener l){ listeners.add(l);}
//    public void removeListener(MessageListener l){ listeners.remove(l);}
//
//    /* ========= Net ========= */
//    private Socket socket;
//    private PrintWriter out;
//    private String username;
//
//    private ClientConnection(){ }
//
//    public boolean connect(String host,int port,String user){
//        try{
//            socket   = new Socket(host,port);
//            out      = new PrintWriter(socket.getOutputStream(),true);
//            username = user;
//
//            // Inbound reader thread
//            new Thread(this::listen).start();
//            return true;
//        }catch(IOException e){
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /* ---------- Low‑level send ---------- */
//    public void send(String cmd){ out.println(cmd); }
//
//    /* ---------- High‑level helpers ---------- */
//    public void sendPrivateMessage(String to,String body){
//        out.println("PRIVATE|"+username+"|"+to+"|"+body);
//    }
//
//    /* ---------- Listen loop ---------- */
//    private void listen(){
//        try(BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))){
//            String line;
//            while((line = br.readLine())!=null){
//                String[] p = line.split("\\|",4);
//                if(p[0].equals("PRIVATE") && p.length==4){
//                    String from = p[1];
//                    String msg  = p[3];
//                    listeners.forEach(l -> l.onMessageReceived(from,msg));
//                }
//                // handle other server replies as needed
//            }
//        }catch(IOException ignored){}
//    }
//}


////version 3.0
//package main.java.client;
//
//import java.io.*;
//import java.net.Socket;
//import java.util.List;
//import java.util.concurrent.CopyOnWriteArrayList;
//
///**
// * Singleton wrapper around a persistent socket to the chat server.
// */
//public class ClientConnection {
//
//    /* ========= Singleton ========= */
//    private static ClientConnection INSTANCE;
//    public static ClientConnection getInstance(){
//        if(INSTANCE==null) INSTANCE = new ClientConnection();
//        return INSTANCE;
//    }
//
//    /* ========= Messaging ========= */
//    public interface MessageListener{ void onMessageReceived(String fromUser,String body);}
//    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
//    public void registerListener(MessageListener l){ listeners.add(l);}
//    public void removeListener(MessageListener l){ listeners.remove(l);}
//
//    /* ========= Net ========= */
//    private Socket socket;
//    private PrintWriter out;
//    private String username;
//
//    private ClientConnection(){ }
//
//    public boolean connect(String host,int port,String user){
//        try{
//            socket   = new Socket(host,port);
//            out      = new PrintWriter(socket.getOutputStream(),true);
//            username = user;
//
//            // Inbound reader thread
//            new Thread(this::listen).start();
//            return true;
//        }catch(IOException e){
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /* ---------- Low‑level send ---------- */
//    public void send(String cmd){ out.println(cmd); }
//
//    /* ---------- High‑level helpers ---------- */
//    public void sendPrivateMessage(String to,String body){
//        out.println("PRIVATE|"+username+"|"+to+"|"+body);
//    }
//
//    /* ---------- Listen loop ---------- */
//    private void listen(){
//        try(BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))){
//            String line;
//            while((line = br.readLine())!=null){
//                String[] p = line.split("\\|",4);
//                if(p[0].equals("PRIVATE") && p.length==4){
//                    String from = p[1];
//                    String msg  = p[3];
//                    listeners.forEach(l -> l.onMessageReceived(from,msg));
//                }
//                // handle other server replies as needed
//            }
//        }catch(IOException ignored){}
//    }
//}


//VERSION 3.1

package main.java.client;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One persistent socket per running client. Announces ONLINE as soon as it
 * connects so the server can route incoming PRIVATE messages.
 */
public class ClientConnection {

    /* ---------------- singleton ---------------- */
    private static ClientConnection INSTANCE;
    private final Queue<String> pendingMessages = new LinkedList<>();
    private final ExecutorService dispatchPool = Executors.newSingleThreadExecutor();

    public static ClientConnection getInstance() {
        if (INSTANCE == null) INSTANCE = new ClientConnection();
        return INSTANCE;
    }

    /* ---------------- listeners ---------------- */
    public interface MessageListener { void onMessageReceived(String from, String body); }
    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
    public void registerListener(MessageListener l) {
        System.out.println("registering listener");
        listeners.add(l);
        synchronized (pendingMessages) {
            while (!pendingMessages.isEmpty()) {
                String line = pendingMessages.poll();
                System.out.println("Dispatching buffered: " + line);
                dispatch(line);
            }
        }
    }
    public void removeListener(MessageListener l)   { listeners.remove(l); }

    /* ---------------- socket ---------------- */
    private Socket socket;
    private PrintWriter out;
    private String username;

    private ClientConnection() {}

    /**
     * Opens the socket **and immediately sends ONLINE|username** so the server
     * stores this writer in its online map.
     */
    public boolean connect(String host, int port, String user) {
        try {
            socket   = new Socket(host, port);
            out      = new PrintWriter(socket.getOutputStream(), true);
            username = user;

            // start inbound reader
            new Thread(this::listen, "ClientConnection-Reader").start();

            // announce presence
            out.println("ONLINE|" + username);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /* high-level helper */
    public void sendPrivateMessage(String to, String body) {
        out.println("PRIVATE|" + username + "|" + to + "|" + body);
    }

    /* listen loop – dispatches PRIVATE packets to registered listeners */
    private void listen() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (listeners.isEmpty()) {
                    // Buffer messages until listeners are registered
                    synchronized (pendingMessages) {
                        //debugging
                        System.out.println("Buffering message: " + line);
                        pendingMessages.offer(line); // 🧠 buffer until listener is ready
                    }

                    //for debugging
                    System.out.println("[Received] " + line);
                }else {
                    dispatch(line);
                }
            }
        } catch (IOException ignored) {}
    }

    private void dispatch(String line) {
        String[] p = line.split("\\|", 4);
        if ((p[0].equals("PRIVATE") || p[0].equals("OFFLINE_MSG")) && p.length == 4) {
            String from = p[1];
            String msg  = p[3];
            for (MessageListener l : listeners) {
                System.out.println("Dispatching to listeners: " + from + " -> " + msg);
                dispatchPool.submit(() -> l.onMessageReceived(from, msg));
            }
        }
    }
}