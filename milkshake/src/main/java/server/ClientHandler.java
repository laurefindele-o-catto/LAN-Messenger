package main.java.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class ClientHandler implements Runnable{
    private Socket socket;

    public ClientHandler(Socket socket){
        this.socket = socket;
    }

    public void run(){
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String request = in.readLine();
            String [] words = request.split("\\|");

            String task = words[0];
            String username = words[1];
            String password = words[2];

            String response;

            if(task.equalsIgnoreCase("SIGNUP")){
                response = UserManager.register(username, password);
            }
            else if(task.equalsIgnoreCase("LOGIN")){
                response = UserManager.login(username, password);
            }
            else{
                response = "ERROR|Unknown prompt.";
            }

            out.println(response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
