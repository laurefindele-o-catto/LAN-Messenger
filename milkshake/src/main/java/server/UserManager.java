package main.java.server;

import java.io.*;
import java.security.*;

public class UserManager {
    private static final File FILE = new File("users.txt");

    public static String register(String username, String password){
        try{
            if(!FILE.exists())
                FILE.createNewFile();

            if(username.contains(" ")){
                return "ERROR|Username cannot contain spaces.";
            }

            BufferedReader reader = new BufferedReader(new FileReader(FILE));
            String line;

            while((line = reader.readLine()) != null){
                if(line.split(":")[0].equals(username)){
                    reader.close();
                    return "ERROR|Username already exists.";
                }
            }

            reader.close();

            BufferedWriter writer = new BufferedWriter(new FileWriter(FILE, true));
            writer.write(username + ":" + hashPassword(password));
            writer.newLine();
            writer.close();
            return "SUCCESS";

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "ERROR|Username can not be registered in server.";
        }
    }

    public static String login(String username, String password){
        try{
            BufferedReader reader = new BufferedReader(new FileReader(FILE));
            String line;
            String hashedPassword = hashPassword(password);
            boolean found = false;

            while((line = reader.readLine()) != null){
                String [] words = line.split(":");

                if(words[0].equals(username) && words[1].equals(hashedPassword)){
                    reader.close();
                    found = true;
                    break;
                }
            }

            reader.close();
            if(found){
                return "SUCCESS";
            }

            return "ERROR|Invalid credentials. Please try again.";

        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return "ERROR|Server cannot login.";
        }
    }

    private static String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes());

        StringBuilder hexString = new StringBuilder();
        for(byte b:hash){
            String hex = Integer.toHexString(0xff & b);

            if(hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
