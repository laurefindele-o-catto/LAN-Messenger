package main.java.user;

import java.io.Serializable;

// model/User.java
import java.time.LocalDate;

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

    public User(String username){
        this.username = username;
    }

    // Getters and Setters
    public String getUsername() { return username; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String   getAvatarBase64()            { return avatarBase64; }
    public void     setAvatarBase64(String b64)  { this.avatarBase64 = b64; }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override public String toString() {
        return firstName + " " + lastName + " (" + username + ")";
    }
}
