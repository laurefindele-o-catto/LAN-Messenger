package main.java.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class ClientConnection {
    public static String send(String message) throws IOException {
        try(Socket socket = new Socket("localhost", 12345);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())))
        {

            out.println(message);
            return in.readLine();
        }
        catch (IOException e) {
            e.printStackTrace();
            return "ERROR|Cannot connect to server";
        }
    }
}
