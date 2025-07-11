package main.java.user;

public class Session {
    private static User currentUser;

    public static void setUser(User user){
        currentUser = user;
    }

    public static User getUser(){
        return currentUser;
    }

    // Redundant naming
    public static void setCurrentUser(User currentUser) {
        Session.currentUser = currentUser;
    }

    public static User getCurrentUser() {
        return currentUser;
    }
}
