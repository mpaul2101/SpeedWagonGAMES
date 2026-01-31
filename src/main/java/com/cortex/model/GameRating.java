package com.cortex.model;

import java.time.LocalDateTime;

/**
 * Represents a rating given by a user to a game.
 */
public class GameRating {
    private int id;
    private int userId;
    private int gameId;
    private int rating; // 1-5 stars
    private String review;
    private LocalDateTime createdAt;

    public GameRating() {
        this.createdAt = LocalDateTime.now();
    }

    public GameRating(int userId, int gameId, int rating) {
        this();
        this.userId = userId;
        this.gameId = gameId;
        this.rating = rating;
    }

    public GameRating(int userId, int gameId, int rating, String review) {
        this(userId, gameId, rating);
        this.review = review;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        this.rating = rating;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "GameRating{" +
                "userId=" + userId +
                ", gameId=" + gameId +
                ", rating=" + rating +
                ", createdAt=" + createdAt +
                '}';
    }
}
