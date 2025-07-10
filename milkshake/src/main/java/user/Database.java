// Files are stored under ./users/<username>.ser
package main.java.user;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Database {
    private static final String ROOT = "users";   // folder created

    //check that users folder exists
    static {
        try { Files.createDirectories(Paths.get(ROOT)); }
        catch (IOException e) { e.printStackTrace(); }
    }

    /** Serialize a user to disk. */

    public static void saveUser(User u) {
        File file = fileOf(u.getUsername());
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(u);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    //Load a profile or return null

    public static User loadUser(String username) {
        File file = fileOf(username);
        if (!file.exists()) return null;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (User) ois.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        return null;
    }


    private static File fileOf(String username) {
        return Paths.get(ROOT, username + ".ser").toFile();
    }

    private Database() {}
}
