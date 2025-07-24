package main.java.user;

public class Session {
    private static User currentUser;
    private static String selectedUser;

    public static void setSelectedUser(String u) { selectedUser = u; }
    public static String getSelectedUser() { return selectedUser; }

    public static void setUser(User user){
        currentUser = user;
    }

    public static User getUser(){
        return currentUser;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void refreshCurrentUser() {
        if (currentUser != null) {
            currentUser = Database.loadUser(currentUser.getUsername());
        }
    }

    public static void clear() {
        currentUser = null;
        selectedUser = null;
    }
}