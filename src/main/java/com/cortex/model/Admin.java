package com.cortex.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents an administrator with extended privileges.
 */
public class Admin extends User {
    private String adminLevel; // SUPER_ADMIN, MODERATOR, CONTENT_MANAGER
    private LocalDateTime promotedAt;

    public Admin() {
        super();
        this.adminLevel = "MODERATOR";
        this.promotedAt = LocalDateTime.now();
    }

    public Admin(String username, String email, String passwordHash, String adminLevel) {
        super(username, email, passwordHash);
        this.adminLevel = adminLevel;
        this.promotedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }

    public LocalDateTime getPromotedAt() {
        return promotedAt;
    }

    public void setPromotedAt(LocalDateTime promotedAt) {
        this.promotedAt = promotedAt;
    }

    // Admin-specific methods
    public void addGameToStore(VideoGame game) {
        // Logic to add a game to the store catalog
        System.out.println("Admin " + getUsername() + " added game: " + game.getTitle());
    }

    public void removeGameFromStore(VideoGame game) {
        // Logic to remove a game from the store catalog
        System.out.println("Admin " + getUsername() + " removed game: " + game.getTitle());
    }

    public void moderateUser(User user, String action) {
        // Logic to moderate users (ban, warn, etc.)
        System.out.println("Admin " + getUsername() + " performed action '" + action + "' on user: " + user.getUsername());
    }

    public void updateGamePrice(VideoGame game, double newPrice) {
        game.setPrice(newPrice);
        System.out.println("Admin " + getUsername() + " updated price for " + game.getTitle() + " to $" + newPrice);
    }

    public void manageGameTags(VideoGame game, List<String> tags) {
        game.setTags(tags);
        System.out.println("Admin " + getUsername() + " updated tags for " + game.getTitle());
    }

    @Override
    public String toString() {
        return "Admin{" +
                "username='" + getUsername() + '\'' +
                ", adminLevel='" + adminLevel + '\'' +
                ", promotedAt=" + promotedAt +
                '}';
    }
}
