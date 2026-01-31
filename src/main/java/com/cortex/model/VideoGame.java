package com.cortex.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a video game in the library.
 */
public class VideoGame {
    private int id;
    private long igdbId; // ID from IGDB API
    private String title;
    private String description;
    private double price;
    private String coverImageUrl;
    private LocalDate releaseDate;
    private double rating; // Average rating
    private int ratingCount;
    private List<String> tags; // genres, themes, keywords
    private List<String> platforms;
    private String developer;
    private String publisher;
    private boolean available;

    public VideoGame() {
        this.tags = new ArrayList<>();
        this.platforms = new ArrayList<>();
        this.available = true;
        this.rating = 0.0;
        this.ratingCount = 0;
    }

    public VideoGame(String title, String description, double price) {
        this();
        this.title = title;
        this.description = description;
        this.price = price;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getIgdbId() {
        return igdbId;
    }

    public void setIgdbId(long igdbId) {
        this.igdbId = igdbId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<String> platforms) {
        this.platforms = platforms;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    // Business methods
    public void addRating(int userRating) {
        double totalRating = this.rating * this.ratingCount;
        this.ratingCount++;
        this.rating = (totalRating + userRating) / this.ratingCount;
    }

    public void addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }

    @Override
    public String toString() {
        return "VideoGame{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", price=" + price +
                ", rating=" + String.format("%.2f", rating) +
                ", tags=" + tags +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoGame videoGame = (VideoGame) o;
        return id == videoGame.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
