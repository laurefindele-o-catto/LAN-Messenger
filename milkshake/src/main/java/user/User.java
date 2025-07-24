package main.java.user;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

// model/User.java

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private String password;
    private String photoPath; // path to photo on disk
    private String firstName;
    private String lastName;
    private LocalDate birthday;
    private String bio;
    private String email;
    /** Avatar stored as Base-64 PNG so it travels easily through sockets / JSON. */
    private String avatarBase64;

    // Friendship and request tracking by username
    private Set<String> friends = Collections.synchronizedSet(new HashSet<>());
    private Set<String> friendRequestsSent = Collections.synchronizedSet(new HashSet<>());
    private Set<String> friendRequestsReceived = Collections.synchronizedSet(new HashSet<>());

    public User(String username) {
        this.username = username;
    }

    // Existing Getters and Setters
    public String getUsername() { return username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarBase64() { return avatarBase64; }
    public void setAvatarBase64(String avatarBase64) { this.avatarBase64 = avatarBase64; }

    // Friendship operations
    /**
     * Sends a friend request to another user.
     */

    public static User loadUser(String username) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("users/" + username + "/data.ser"))) {
            User user = (User) in.readObject();
            user.ensureSetFields(); // 🔥 This is critical
            return user;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    private void ensureSetFields() {
        if (friends == null) friends = Collections.synchronizedSet(new HashSet<>());
        if (friendRequestsSent == null) friendRequestsSent = Collections.synchronizedSet(new HashSet<>());
        if (friendRequestsReceived == null) friendRequestsReceived = Collections.synchronizedSet(new HashSet<>());
    }



//    public void sendFriendRequest(User other) {
//        if (other == null || other.username.equals(this.username)) return;
//        if (friends.contains(other.username) || friendRequestsSent.contains(other.username)) return;
//        friendRequestsSent.add(other.username);
//        other.friendRequestsReceived.add(this.username);
//        System.out.println("sending friend request to " + other.getUsername());
//    }

    public void sendFriendRequest(User other) {
        System.out.println("== In sendFriendRequest ==");
        System.out.println("Already friends? " + friends.contains(other.username));
        System.out.println("Already sent? " + friendRequestsSent.contains(other.username));

        if (other == null || other.username.equals(this.username)) return;
        if (friends.contains(other.username) || friendRequestsSent.contains(other.username)) {
            System.out.println("Request skipped due to already sent or already friends.");
            return;
        }

        friendRequestsSent.add(other.username);
        other.friendRequestsReceived.add(this.username);
        System.out.println("sending friend request to " + other.getUsername());
    }


    /**
     * Accepts a pending friend request from another user.
     */
    public void acceptFriendRequest(User other) {
        if (other == null || !friendRequestsReceived.contains(other.username)) return;
        friendRequestsReceived.remove(other.username);
        other.friendRequestsSent.remove(this.username);
        friends.add(other.username);
        other.friends.add(this.username);

        System.out.println("accepting friend request from " + other.getUsername());
    }

    /**
     * Declines a pending friend request from another user.
     */
    public void declineFriendRequest(User other) {
        if (other == null || !friendRequestsReceived.contains(other.username)) return;
        friendRequestsReceived.remove(other.username);
        other.friendRequestsSent.remove(this.username);

        System.out.println("declining friend request from " + other.getUsername());
    }

    /**
     * Removes an existing friend relationship.
     */
    public void removeFriend(User other) {
        if (other == null || !friends.contains(other.username)) return;
        friends.remove(other.username);
        other.friends.remove(this.username);
    }

    // Accessors for friend lists (defensive copies)
    //public Set<String> getFriends() { return new HashSet<>(friends); }
    public Set<String> getFriends() {
        if (friends == null) {
            friends = Collections.synchronizedSet(new HashSet<>());
        }
        return friends;
    }

    public Set<String> getFriendRequestsSent() {
        if (friendRequestsSent == null) {
            friendRequestsSent = Collections.synchronizedSet(new HashSet<>());
        }
        return new HashSet<>(friendRequestsSent);
    }
    public Set<String> getFriendRequestsReceived() {
        if (friendRequestsReceived == null) {
            friendRequestsReceived = Collections.synchronizedSet(new HashSet<>());
        }
        return new HashSet<>(friendRequestsReceived);
    }

    @Override
    public String toString() {
        return firstName + " " + lastName + " (" + username + ")";
    }
}
