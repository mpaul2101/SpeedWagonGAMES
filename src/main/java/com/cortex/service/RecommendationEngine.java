package com.cortex.service;

import com.cortex.model.User;
import com.cortex.model.VideoGame;
import com.cortex.model.GameRating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-based recommendation system that learns from user preferences and behaviors.
 * Now powered by real Machine Learning using Matrix Factorization!
 */
public class RecommendationEngine {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationEngine.class);
    
    private static final int MIN_RECOMMENDATIONS = 5;
    private static final int MAX_RECOMMENDATIONS = 20;
    private static final double TAG_WEIGHT = 1.0;
    private static final double RATING_WEIGHT = 1.5;
    private static final double OWNERSHIP_WEIGHT = 0.5;

    // ML Engine for advanced recommendations
    private final MLRecommendationEngine mlEngine;
    private boolean useML = false;

    public RecommendationEngine() {
        this.mlEngine = new MLRecommendationEngine();
    }

    /**
     * Train the ML model with user data. Call this when the application starts
     * or when you have new rating data.
     */
    public void trainMLModel(List<User> users, List<VideoGame> games, List<GameRating> ratings) {
        if (ratings.size() >= 10) { // Need minimum data for ML to work
            mlEngine.train(users, games, ratings);
            useML = mlEngine.isTrained();
            logger.info("ML Model training complete. Using ML: {}", useML);
        } else {
            logger.info("Not enough ratings for ML training (need >= 10). Using rule-based recommendations.");
            useML = false;
        }
    }

    /**
     * Update ML model incrementally with a new rating (online learning).
     */
    public void onNewRating(GameRating rating) {
        if (useML) {
            mlEngine.updateWithNewRating(rating);
        }
    }

    /**
     * Get personalized game recommendations for a user.
     * Uses ML model if trained, otherwise falls back to rule-based system.
     */
    public List<VideoGame> getRecommendations(User user, List<VideoGame> allGames, int count) {
        // Use ML if available
        if (useML) {
            logger.debug("Using ML-based recommendations for user {}", user.getUsername());
            return mlEngine.getRecommendations(user, allGames, count);
        }

        // Fallback to rule-based system
        logger.debug("Using rule-based recommendations for user {}", user.getUsername());
        return getRuleBasedRecommendations(user, allGames, count);
    }

    /**
     * Original rule-based recommendations (fallback).
     */
    private List<VideoGame> getRuleBasedRecommendations(User user, List<VideoGame> allGames, int count) {
        if (user.getOwnedGames().isEmpty() && user.getRatings().isEmpty()) {
            // For new users, recommend popular games
            return getPopularGames(allGames, count);
        }

        // Build user preference profile
        Map<String, Double> tagPreferences = buildTagPreferenceProfile(user);
        
        // Score all games
        Map<VideoGame, Double> gameScores = new HashMap<>();
        Set<Integer> ownedGameIds = user.getOwnedGames().stream()
                .map(VideoGame::getId)
                .collect(Collectors.toSet());

        for (VideoGame game : allGames) {
            // Skip games the user already owns
            if (ownedGameIds.contains(game.getId())) {
                continue;
            }

            double score = calculateGameScore(game, tagPreferences, user);
            gameScores.put(game, score);
        }

        // Sort games by score and return top recommendations
        return gameScores.entrySet().stream()
                .sorted(Map.Entry.<VideoGame, Double>comparingByValue().reversed())
                .limit(count)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Build a tag preference profile based on user's owned games and ratings.
     */
    private Map<String, Double> buildTagPreferenceProfile(User user) {
        Map<String, Double> tagPreferences = new HashMap<>();

        // Learn from owned games
        for (VideoGame game : user.getOwnedGames()) {
            for (String tag : game.getTags()) {
                tagPreferences.merge(tag, OWNERSHIP_WEIGHT, Double::sum);
            }
        }

        // Learn from ratings (higher weight for highly rated games)
        for (GameRating rating : user.getRatings()) {
            // Find the rated game
            VideoGame ratedGame = user.getOwnedGames().stream()
                    .filter(g -> g.getId() == rating.getGameId())
                    .findFirst()
                    .orElse(null);

            if (ratedGame != null) {
                double ratingWeight = (rating.getRating() / 5.0) * RATING_WEIGHT;
                for (String tag : ratedGame.getTags()) {
                    tagPreferences.merge(tag, ratingWeight, Double::sum);
                }
            }
        }

        // Learn from explicit user preferences
        for (String tag : user.getPreferredTags()) {
            tagPreferences.merge(tag, TAG_WEIGHT, Double::sum);
        }

        // Normalize preferences
        double maxValue = tagPreferences.values().stream()
                .max(Double::compareTo)
                .orElse(1.0);

        if (maxValue > 0) {
            tagPreferences.replaceAll((tag, value) -> value / maxValue);
        }

        return tagPreferences;
    }

    /**
     * Calculate a recommendation score for a game based on user preferences.
     */
    private double calculateGameScore(VideoGame game, Map<String, Double> tagPreferences, User user) {
        double score = 0.0;

        // Tag matching score
        double tagScore = 0.0;
        int matchingTags = 0;
        for (String tag : game.getTags()) {
            if (tagPreferences.containsKey(tag)) {
                tagScore += tagPreferences.get(tag);
                matchingTags++;
            }
        }
        
        if (matchingTags > 0) {
            tagScore /= matchingTags; // Average tag score
            score += tagScore * 10.0; // Scale up tag score
        }

        // Game rating boost
        score += game.getRating() * 2.0;

        // Popularity boost (based on rating count)
        if (game.getRatingCount() > 100) {
            score += Math.log10(game.getRatingCount()) * 0.5;
        }

        // Recency boost for newer games
        if (game.getReleaseDate() != null) {
            long daysSinceRelease = java.time.temporal.ChronoUnit.DAYS.between(
                    game.getReleaseDate(), 
                    java.time.LocalDate.now()
            );
            
            if (daysSinceRelease < 90) { // Released in last 3 months
                score += (90 - daysSinceRelease) / 90.0 * 2.0;
            }
        }

        return score;
    }

    /**
     * Get popular games for new users or when preferences are not available.
     */
    private List<VideoGame> getPopularGames(List<VideoGame> allGames, int count) {
        return allGames.stream()
                .sorted(Comparator.comparingDouble(VideoGame::getRating).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Get similar games to a specific game.
     * Uses ML embeddings if available, otherwise Jaccard similarity.
     */
    public List<VideoGame> getSimilarGames(VideoGame targetGame, List<VideoGame> allGames, int count) {
        // Use ML if available (uses learned game embeddings)
        if (useML) {
            logger.debug("Using ML-based similarity for game {}", targetGame.getTitle());
            return mlEngine.getSimilarGames(targetGame, allGames, count);
        }

        // Fallback to Jaccard similarity
        return getRuleBasedSimilarGames(targetGame, allGames, count);
    }

    /**
     * Original rule-based similar games (fallback).
     */
    private List<VideoGame> getRuleBasedSimilarGames(VideoGame targetGame, List<VideoGame> allGames, int count) {
        Map<VideoGame, Double> similarityScores = new HashMap<>();
        Set<String> targetTags = new HashSet<>(targetGame.getTags());

        for (VideoGame game : allGames) {
            if (game.getId() == targetGame.getId()) {
                continue; // Skip the target game itself
            }

            double similarity = calculateSimilarity(targetTags, new HashSet<>(game.getTags()));
            
            // Boost similarity if same developer/publisher
            if (targetGame.getDeveloper() != null && 
                targetGame.getDeveloper().equals(game.getDeveloper())) {
                similarity += 0.2;
            }
            if (targetGame.getPublisher() != null && 
                targetGame.getPublisher().equals(game.getPublisher())) {
                similarity += 0.1;
            }

            similarityScores.put(game, similarity);
        }

        return similarityScores.entrySet().stream()
                .sorted(Map.Entry.<VideoGame, Double>comparingByValue().reversed())
                .limit(count)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Calculate Jaccard similarity between two sets of tags.
     */
    private double calculateSimilarity(Set<String> tags1, Set<String> tags2) {
        Set<String> intersection = new HashSet<>(tags1);
        intersection.retainAll(tags2);

        Set<String> union = new HashSet<>(tags1);
        union.addAll(tags2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    /**
     * Update user preferences based on interaction (implicit feedback).
     */
    public void updatePreferencesFromInteraction(User user, VideoGame game, InteractionType type) {
        switch (type) {
            case VIEW:
                // Light weight for viewing
                game.getTags().forEach(tag -> {
                    if (Math.random() > 0.7) { // 30% chance to add tag from viewed games
                        user.getPreferredTags().add(tag);
                    }
                });
                break;
                
            case ADD_TO_WISHLIST:
                // Medium weight for wishlisting
                game.getTags().forEach(tag -> {
                    if (Math.random() > 0.4) { // 60% chance
                        user.getPreferredTags().add(tag);
                    }
                });
                break;
                
            case PURCHASE:
                // High weight for purchasing
                user.getPreferredTags().addAll(game.getTags());
                break;
                
            case RATE_HIGH:
                // Very high weight for high ratings
                user.getPreferredTags().addAll(game.getTags());
                break;
                
            case RATE_LOW:
                // Remove tags from low-rated games
                game.getTags().forEach(tag -> {
                    if (Math.random() > 0.6) { // 40% chance to remove
                        user.getPreferredTags().remove(tag);
                    }
                });
                break;
        }
        
        logger.debug("Updated preferences for user {} based on {} interaction with {}",
                user.getUsername(), type, game.getTitle());
    }

    /**
     * Types of user interactions for implicit feedback.
     */
    public enum InteractionType {
        VIEW,
        ADD_TO_WISHLIST,
        PURCHASE,
        RATE_HIGH,
        RATE_LOW
    }

    /**
     * Get trending games based on recent activity (placeholder for future implementation).
     */
    public List<VideoGame> getTrendingGames(List<VideoGame> allGames, int count) {
        // For now, return games with high ratings and recent release dates
        return allGames.stream()
                .filter(game -> game.getReleaseDate() != null)
                .filter(game -> game.getReleaseDate().isAfter(
                        java.time.LocalDate.now().minusMonths(6)))
                .sorted(Comparator.comparingDouble(VideoGame::getRating).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Check if ML model is being used.
     */
    public boolean isUsingML() {
        return useML;
    }

    /**
     * Get ML model statistics.
     */
    public String getMLStats() {
        return mlEngine.getModelStats();
    }

    /**
     * Get the ML engine for advanced operations.
     */
    public MLRecommendationEngine getMLEngine() {
        return mlEngine;
    }
}
