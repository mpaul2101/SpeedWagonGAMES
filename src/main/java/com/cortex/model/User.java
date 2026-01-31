package com.cortex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a user in the video game library system.
 */
public class User {
    private int id;
    private String username;
    private String email;
    private String passwordHash;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private List<VideoGame> ownedGames;
    private List<VideoGame> wishlist;
    private Set<String> preferredTags;
    private List<GameRating> ratings;

    public User() {
        this.ownedGames = new ArrayList<>();
        this.wishlist = new ArrayList<>();
        this.preferredTags = new HashSet<>();
        this.ratings = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String email, String passwordHash) {
        this();
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public List<VideoGame> getOwnedGames() {
        return ownedGames;
    }

    public void setOwnedGames(List<VideoGame> ownedGames) {
        this.ownedGames = ownedGames;
    }

    public List<VideoGame> getWishlist() {
        return wishlist;
    }

    public void setWishlist(List<VideoGame> wishlist) {
        this.wishlist = wishlist;
    }

    public Set<String> getPreferredTags() {
        return preferredTags;
    }

    public void setPreferredTags(Set<String> preferredTags) {
        this.preferredTags = preferredTags;
    }

    public List<GameRating> getRatings() {
        return ratings;
    }

    public void setRatings(List<GameRating> ratings) {
        this.ratings = ratings;
    }

    // Business methods
    public void addGame(VideoGame game) {
        if (!ownedGames.contains(game)) {
            ownedGames.add(game);
            // Learn from purchased game tags
            preferredTags.addAll(game.getTags());
        }
    }

    public void addToWishlist(VideoGame game) {
        if (!wishlist.contains(game)) {
            wishlist.add(game);
        }
    }

    public void removeFromWishlist(VideoGame game) {
        wishlist.remove(game);
    }

    public void rateGame(VideoGame game, int rating) {
        GameRating gameRating = new GameRating(this.id, game.getId(), rating);
        ratings.add(gameRating);
        
        // Update preferences based on rating
        if (rating >= 4) {
            preferredTags.addAll(game.getTags());
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", ownedGames=" + ownedGames.size() +
                ", wishlist=" + wishlist.size() +
                '}';
    }
}
