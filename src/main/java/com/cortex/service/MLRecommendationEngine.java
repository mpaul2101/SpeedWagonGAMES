package com.cortex.service;

import com.cortex.model.GameRating;
import com.cortex.model.User;
import com.cortex.model.VideoGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * HYBRID Machine Learning Recommendation Engine combining:
 * 1. Collaborative Filtering (Matrix Factorization) - learns from user ratings
 * 2. Content-Based Filtering (Tag Embeddings) - learns from game tags
 * 
 * This is REAL ML because:
 * - Weights are LEARNED from data, not hardcoded
 * - Model improves with more training data
 * - Uses gradient descent optimization
 * - Learns tag embeddings for content understanding
 */
public class MLRecommendationEngine {
    private static final Logger logger = LoggerFactory.getLogger(MLRecommendationEngine.class);

    // Hyperparameters
    private static final int LATENT_FACTORS = 10;      // Number of hidden features to learn
    private static final double LEARNING_RATE = 0.01;  // How fast the model learns
    private static final double REGULARIZATION = 0.02; // Prevents overfitting
    private static final int MAX_ITERATIONS = 100;     // Training epochs
    private static final int TAG_EMBEDDING_SIZE = 8;   // Size of tag embeddings
    private static final double HYBRID_CF_WEIGHT = 0.6; // Weight for collaborative filtering
    private static final double HYBRID_CB_WEIGHT = 0.4; // Weight for content-based

    // Collaborative Filtering matrices (learned from ratings)
    private double[][] userFactors;    // Users in latent space
    private double[][] gameFactors;    // Games in latent space
    private double[] userBias;         // Per-user bias
    private double[] gameBias;         // Per-game bias
    private double globalMean;         // Global average rating

    // Content-Based matrices (learned from tags)
    private Map<String, double[]> tagEmbeddings;  // Tag → embedding vector
    private double[][] gameContentVectors;         // Game content representation
    private double[][] userPreferenceVectors;      // User preference for tags

    // Mappings
    private Map<Integer, Integer> userIdToIndex;
    private Map<Integer, Integer> gameIdToIndex;
    private Map<Integer, Integer> indexToUserId;
    private Map<Integer, Integer> indexToGameId;
    private List<String> allTags;
    private Map<String, Integer> tagToIndex;

    private boolean isTrained = false;
    private final Random random = new Random(42);

    public MLRecommendationEngine() {
        this.userIdToIndex = new HashMap<>();
        this.gameIdToIndex = new HashMap<>();
        this.indexToUserId = new HashMap<>();
        this.indexToGameId = new HashMap<>();
        this.tagEmbeddings = new HashMap<>();
        this.allTags = new ArrayList<>();
        this.tagToIndex = new HashMap<>();
    }

    /**
     * Train the HYBRID model using:
     * 1. Matrix Factorization with SGD (from ratings)
     * 2. Tag Embedding learning (from game tags and user preferences)
     */
    public void train(List<User> users, List<VideoGame> games, List<GameRating> allRatings) {
        if (allRatings.isEmpty()) {
            logger.warn("No ratings available for training. Model will use fallback recommendations.");
            return;
        }

        logger.info("Starting HYBRID ML training with {} users, {} games, {} ratings", 
                users.size(), games.size(), allRatings.size());

        // Build index mappings
        buildMappings(users, games);
        
        // Extract all unique tags
        extractAllTags(games);

        int numUsers = users.size();
        int numGames = games.size();

        // ===== PHASE 1: Initialize matrices =====
        initializeMatrices(numUsers, numGames);

        // ===== PHASE 2: Learn Tag Embeddings =====
        logger.info("Learning tag embeddings from {} unique tags...", allTags.size());
        learnTagEmbeddings(games);

        // ===== PHASE 3: Build game content vectors from tags =====
        logger.info("Building game content vectors from tags...");
        buildGameContentVectors(games);

        // ===== PHASE 4: Learn user preference vectors from their rated games =====
        logger.info("Learning user preference vectors...");
        buildUserPreferenceVectors(users, games);

        // Calculate global mean
        globalMean = allRatings.stream()
                .mapToDouble(GameRating::getRating)
                .average()
                .orElse(3.0);

        // ===== PHASE 5: STOCHASTIC GRADIENT DESCENT for Collaborative Filtering =====
        logger.info("Training Collaborative Filtering with SGD...");
        trainCollaborativeFiltering(allRatings, numUsers, numGames);

        isTrained = true;
        logger.info("HYBRID ML Training complete!");
        logger.info("  - Latent factors: {}", LATENT_FACTORS);
        logger.info("  - Tag embedding size: {}", TAG_EMBEDDING_SIZE);
        logger.info("  - Unique tags learned: {}", allTags.size());
        logger.info("  - Hybrid weights: CF={}, CB={}", HYBRID_CF_WEIGHT, HYBRID_CB_WEIGHT);
    }

    private void initializeMatrices(int numUsers, int numGames) {
        double scale = Math.sqrt(2.0 / (numUsers + numGames));
        
        // Collaborative Filtering matrices
        userFactors = new double[numUsers][LATENT_FACTORS];
        gameFactors = new double[numGames][LATENT_FACTORS];
        userBias = new double[numUsers];
        gameBias = new double[numGames];

        for (int i = 0; i < numUsers; i++) {
            for (int k = 0; k < LATENT_FACTORS; k++) {
                userFactors[i][k] = random.nextGaussian() * scale;
            }
        }
        for (int j = 0; j < numGames; j++) {
            for (int k = 0; k < LATENT_FACTORS; k++) {
                gameFactors[j][k] = random.nextGaussian() * scale;
            }
        }

        // Content-Based matrices
        gameContentVectors = new double[numGames][TAG_EMBEDDING_SIZE];
        userPreferenceVectors = new double[numUsers][TAG_EMBEDDING_SIZE];
    }

    /**
     * Extract all unique tags from games.
     */
    private void extractAllTags(List<VideoGame> games) {
        Set<String> tagSet = new HashSet<>();
        for (VideoGame game : games) {
            if (game.getTags() != null) {
                tagSet.addAll(game.getTags());
            }
        }
        allTags = new ArrayList<>(tagSet);
        
        for (int i = 0; i < allTags.size(); i++) {
            tagToIndex.put(allTags.get(i), i);
        }
        
        logger.debug("Extracted {} unique tags", allTags.size());
    }

    /**
     * Learn embeddings for each tag using random initialization + co-occurrence learning.
     * Tags that appear together in games should have similar embeddings.
     */
    private void learnTagEmbeddings(List<VideoGame> games) {
        // Initialize random embeddings for each tag
        double scale = Math.sqrt(2.0 / TAG_EMBEDDING_SIZE);
        for (String tag : allTags) {
            double[] embedding = new double[TAG_EMBEDDING_SIZE];
            for (int i = 0; i < TAG_EMBEDDING_SIZE; i++) {
                embedding[i] = random.nextGaussian() * scale;
            }
            tagEmbeddings.put(tag, embedding);
        }

        // Learn from tag co-occurrence (tags in same game should be similar)
        // Using simplified Skip-gram style learning
        for (int iter = 0; iter < 50; iter++) {
            for (VideoGame game : games) {
                List<String> tags = game.getTags();
                if (tags == null || tags.size() < 2) continue;

                // For each pair of tags in the same game, make them more similar
                for (int i = 0; i < tags.size(); i++) {
                    for (int j = i + 1; j < tags.size(); j++) {
                        String tag1 = tags.get(i);
                        String tag2 = tags.get(j);
                        
                        double[] emb1 = tagEmbeddings.get(tag1);
                        double[] emb2 = tagEmbeddings.get(tag2);
                        
                        if (emb1 == null || emb2 == null) continue;

                        // Gradient step to make embeddings more similar
                        for (int k = 0; k < TAG_EMBEDDING_SIZE; k++) {
                            double diff = emb1[k] - emb2[k];
                            emb1[k] -= LEARNING_RATE * 0.1 * diff;
                            emb2[k] += LEARNING_RATE * 0.1 * diff;
                        }
                    }
                }
            }
        }
        
        logger.debug("Tag embeddings learned for {} tags", tagEmbeddings.size());
    }

    /**
     * Build content vectors for each game by averaging tag embeddings.
     */
    private void buildGameContentVectors(List<VideoGame> games) {
        for (VideoGame game : games) {
            Integer gameIdx = gameIdToIndex.get(game.getId());
            if (gameIdx == null) continue;

            List<String> tags = game.getTags();
            if (tags == null || tags.isEmpty()) {
                // No tags - use zero vector
                continue;
            }

            // Average all tag embeddings for this game
            double[] contentVector = new double[TAG_EMBEDDING_SIZE];
            int count = 0;
            
            for (String tag : tags) {
                double[] tagEmb = tagEmbeddings.get(tag);
                if (tagEmb != null) {
                    for (int k = 0; k < TAG_EMBEDDING_SIZE; k++) {
                        contentVector[k] += tagEmb[k];
                    }
                    count++;
                }
            }

            if (count > 0) {
                for (int k = 0; k < TAG_EMBEDDING_SIZE; k++) {
                    contentVector[k] /= count;
                }
            }

            gameContentVectors[gameIdx] = contentVector;
        }
    }

    /**
     * Build user preference vectors based on tags of their highly-rated games.
     */
    private void buildUserPreferenceVectors(List<User> users, List<VideoGame> games) {
        Map<Integer, VideoGame> gameMap = new HashMap<>();
        for (VideoGame game : games) {
            gameMap.put(game.getId(), game);
        }

        for (User user : users) {
            Integer userIdx = userIdToIndex.get(user.getId());
            if (userIdx == null) continue;

            double[] prefVector = new double[TAG_EMBEDDING_SIZE];
            double totalWeight = 0;

            // Learn from rated games (weighted by rating)
            for (GameRating rating : user.getRatings()) {
                VideoGame game = gameMap.get(rating.getGameId());
                if (game == null || game.getTags() == null) continue;

                // Higher ratings = stronger preference
                double weight = rating.getRating() / 5.0;
                
                for (String tag : game.getTags()) {
                    double[] tagEmb = tagEmbeddings.get(tag);
                    if (tagEmb != null) {
                        for (int k = 0; k < TAG_EMBEDDING_SIZE; k++) {
                            prefVector[k] += tagEmb[k] * weight;
                        }
                        totalWeight += weight;
                    }
                }
            }

            // Also learn from owned games
            for (VideoGame game : user.getOwnedGames()) {
                if (game.getTags() == null) continue;
                
                double weight = 0.5; // Lower weight than explicit ratings
                
                for (String tag : game.getTags()) {
                    double[] tagEmb = tagEmbeddings.get(tag);
                    if (tagEmb != null) {
                        for (int k = 0; k < TAG_EMBEDDING_SIZE; k++) {
                            prefVector[k] += tagEmb[k] * weight;
                        }
                        totalWeight += weight;
                    }
                }
            }

            // Normalize
            if (totalWeight > 0) {
                for (int k = 0; k < TAG_EMBEDDING_SIZE; k++) {
                    prefVector[k] /= totalWeight;
                }
            }

            userPreferenceVectors[userIdx] = prefVector;
        }
    }

    /**
     * Train Collaborative Filtering using Stochastic Gradient Descent.
     */
    private void trainCollaborativeFiltering(List<GameRating> allRatings, int numUsers, int numGames) {
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            double totalError = 0.0;
            Collections.shuffle(allRatings);

            for (GameRating rating : allRatings) {
                Integer userIdx = userIdToIndex.get(rating.getUserId());
                Integer gameIdx = gameIdToIndex.get(rating.getGameId());

                if (userIdx == null || gameIdx == null) continue;

                // Predict rating (CF only for training)
                double predicted = predictCF(userIdx, gameIdx);
                double actual = rating.getRating();
                double error = actual - predicted;
                totalError += error * error;

                // Update biases
                userBias[userIdx] += LEARNING_RATE * (error - REGULARIZATION * userBias[userIdx]);
                gameBias[gameIdx] += LEARNING_RATE * (error - REGULARIZATION * gameBias[gameIdx]);

                // Update latent factors
                for (int k = 0; k < LATENT_FACTORS; k++) {
                    double userFactor = userFactors[userIdx][k];
                    double gameFactor = gameFactors[gameIdx][k];

                    userFactors[userIdx][k] += LEARNING_RATE * (error * gameFactor - REGULARIZATION * userFactor);
                    gameFactors[gameIdx][k] += LEARNING_RATE * (error * userFactor - REGULARIZATION * gameFactor);
                }
            }

            double rmse = Math.sqrt(totalError / allRatings.size());
            if (iteration % 20 == 0) {
                logger.info("CF Iteration {}: RMSE = {}", iteration, String.format("%.4f", rmse));
            }

            if (rmse < 0.1) {
                logger.info("Converged at iteration {} with RMSE = {}", iteration, String.format("%.4f", rmse));
                break;
            }
        }
    }

    /**
     * Predict rating using Collaborative Filtering only.
     */
    private double predictCF(int userIdx, int gameIdx) {
        double prediction = globalMean + userBias[userIdx] + gameBias[gameIdx];

        for (int k = 0; k < LATENT_FACTORS; k++) {
            prediction += userFactors[userIdx][k] * gameFactors[gameIdx][k];
        }

        return Math.max(1.0, Math.min(5.0, prediction));
    }

    /**
     * Predict rating using Content-Based filtering (tag similarity).
     */
    private double predictCB(int userIdx, int gameIdx) {
        double[] userPref = userPreferenceVectors[userIdx];
        double[] gameContent = gameContentVectors[gameIdx];

        if (userPref == null || gameContent == null) {
            return globalMean;
        }

        // Cosine similarity between user preference and game content
        double similarity = cosineSimilarity(userPref, gameContent);
        
        // Convert similarity [-1, 1] to rating scale [1, 5]
        return 1.0 + (similarity + 1.0) * 2.0;
    }

    /**
     * HYBRID prediction combining CF and CB.
     */
    private double predictHybrid(int userIdx, int gameIdx) {
        double cfScore = predictCF(userIdx, gameIdx);
        double cbScore = predictCB(userIdx, gameIdx);
        
        return HYBRID_CF_WEIGHT * cfScore + HYBRID_CB_WEIGHT * cbScore;
    }

    /**
     * Get ML-based recommendations for a user using HYBRID approach.
     */
    public List<VideoGame> getRecommendations(User user, List<VideoGame> allGames, int count) {
        if (!isTrained) {
            logger.warn("Model not trained. Returning popular games.");
            return getPopularGamesFallback(allGames, count);
        }

        Integer userIdx = userIdToIndex.get(user.getId());
        if (userIdx == null) {
            logger.info("New user detected. Using cold start strategy with tags.");
            return getColdStartRecommendations(user, allGames, count);
        }

        // Get owned game IDs to exclude
        Set<Integer> ownedGameIds = user.getOwnedGames().stream()
                .map(VideoGame::getId)
                .collect(Collectors.toSet());

        // Predict ratings for all unowned games using HYBRID model
        Map<VideoGame, Double> predictions = new HashMap<>();
        for (VideoGame game : allGames) {
            if (ownedGameIds.contains(game.getId())) continue;

            Integer gameIdx = gameIdToIndex.get(game.getId());
            if (gameIdx != null) {
                double predictedRating = predictHybrid(userIdx, gameIdx);
                predictions.put(game, predictedRating);
            }
        }

        // Return top predicted games
        return predictions.entrySet().stream()
                .sorted(Map.Entry.<VideoGame, Double>comparingByValue().reversed())
                .limit(count)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Find similar games using HYBRID approach (learned embeddings + tag content).
     */
    public List<VideoGame> getSimilarGames(VideoGame targetGame, List<VideoGame> allGames, int count) {
        if (!isTrained) {
            return getPopularGamesFallback(allGames, count);
        }

        Integer targetIdx = gameIdToIndex.get(targetGame.getId());
        if (targetIdx == null) {
            return getPopularGamesFallback(allGames, count);
        }

        double[] targetCFEmbedding = gameFactors[targetIdx];
        double[] targetContentEmbedding = gameContentVectors[targetIdx];
        
        Map<VideoGame, Double> similarities = new HashMap<>();

        for (VideoGame game : allGames) {
            if (game.getId() == targetGame.getId()) continue;

            Integer gameIdx = gameIdToIndex.get(game.getId());
            if (gameIdx != null) {
                // Combine CF similarity and Content similarity
                double cfSim = cosineSimilarity(targetCFEmbedding, gameFactors[gameIdx]);
                double contentSim = cosineSimilarity(targetContentEmbedding, gameContentVectors[gameIdx]);
                
                double hybridSim = HYBRID_CF_WEIGHT * cfSim + HYBRID_CB_WEIGHT * contentSim;
                similarities.put(game, hybridSim);
            }
        }

        return similarities.entrySet().stream()
                .sorted(Map.Entry.<VideoGame, Double>comparingByValue().reversed())
                .limit(count)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Find users with similar taste using learned user embeddings.
     */
    public List<User> findSimilarUsers(User targetUser, List<User> allUsers, int count) {
        if (!isTrained) {
            return Collections.emptyList();
        }

        Integer targetIdx = userIdToIndex.get(targetUser.getId());
        if (targetIdx == null) {
            return Collections.emptyList();
        }

        double[] targetCFEmbedding = userFactors[targetIdx];
        double[] targetPrefEmbedding = userPreferenceVectors[targetIdx];
        
        Map<User, Double> similarities = new HashMap<>();

        for (User user : allUsers) {
            if (user.getId() == targetUser.getId()) continue;

            Integer userIdx = userIdToIndex.get(user.getId());
            if (userIdx != null) {
                double cfSim = cosineSimilarity(targetCFEmbedding, userFactors[userIdx]);
                double prefSim = cosineSimilarity(targetPrefEmbedding, userPreferenceVectors[userIdx]);
                
                double hybridSim = HYBRID_CF_WEIGHT * cfSim + HYBRID_CB_WEIGHT * prefSim;
                similarities.put(user, hybridSim);
            }
        }

        return similarities.entrySet().stream()
                .sorted(Map.Entry.<User, Double>comparingByValue().reversed())
                .limit(count)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Cosine similarity between two vectors.
     */
    private double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Cold start strategy for new users - uses tag preferences.
     */
    private List<VideoGame> getColdStartRecommendations(User user, List<VideoGame> allGames, int count) {
        // Build preference vector from user's preferred tags
        double[] prefVector = new double[TAG_EMBEDDING_SIZE];
        int tagCount = 0;

        if (!user.getPreferredTags().isEmpty()) {
            for (String tag : user.getPreferredTags()) {
                double[] tagEmb = tagEmbeddings.get(tag);
                if (tagEmb != null) {
                    for (int k = 0; k < TAG_EMBEDDING_SIZE; k++) {
                        prefVector[k] += tagEmb[k];
                    }
                    tagCount++;
                }
            }
        }

        if (tagCount > 0) {
            // Normalize
            for (int k = 0; k < TAG_EMBEDDING_SIZE; k++) {
                prefVector[k] /= tagCount;
            }

            // Find games most similar to preference
            Map<VideoGame, Double> scores = new HashMap<>();
            for (VideoGame game : allGames) {
                Integer gameIdx = gameIdToIndex.get(game.getId());
                if (gameIdx != null) {
                    double sim = cosineSimilarity(prefVector, gameContentVectors[gameIdx]);
                    scores.put(game, sim);
                }
            }

            return scores.entrySet().stream()
                    .sorted(Map.Entry.<VideoGame, Double>comparingByValue().reversed())
                    .limit(count)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        // Fallback to popular games
        return getPopularGamesFallback(allGames, count);
    }

    private List<VideoGame> getPopularGamesFallback(List<VideoGame> allGames, int count) {
        return allGames.stream()
                .sorted(Comparator.comparingDouble(VideoGame::getRating).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    private void buildMappings(List<User> users, List<VideoGame> games) {
        userIdToIndex.clear();
        gameIdToIndex.clear();
        indexToUserId.clear();
        indexToGameId.clear();

        int idx = 0;
        for (User user : users) {
            userIdToIndex.put(user.getId(), idx);
            indexToUserId.put(idx, user.getId());
            idx++;
        }

        idx = 0;
        for (VideoGame game : games) {
            gameIdToIndex.put(game.getId(), idx);
            indexToGameId.put(idx, game.getId());
            idx++;
        }
    }

    /**
     * Incremental learning - update model with new rating.
     */
    public void updateWithNewRating(GameRating rating) {
        if (!isTrained) return;

        Integer userIdx = userIdToIndex.get(rating.getUserId());
        Integer gameIdx = gameIdToIndex.get(rating.getGameId());

        if (userIdx == null || gameIdx == null) return;

        // Mini-batch update for CF
        double predicted = predictCF(userIdx, gameIdx);
        double error = rating.getRating() - predicted;

        double onlineLR = LEARNING_RATE * 2;

        userBias[userIdx] += onlineLR * (error - REGULARIZATION * userBias[userIdx]);
        gameBias[gameIdx] += onlineLR * (error - REGULARIZATION * gameBias[gameIdx]);

        for (int k = 0; k < LATENT_FACTORS; k++) {
            double userFactor = userFactors[userIdx][k];
            double gameFactor = gameFactors[gameIdx][k];

            userFactors[userIdx][k] += onlineLR * (error * gameFactor - REGULARIZATION * userFactor);
            gameFactors[gameIdx][k] += onlineLR * (error * userFactor - REGULARIZATION * gameFactor);
        }

        logger.debug("Model updated with new rating: user={}, game={}, rating={}", 
                rating.getUserId(), rating.getGameId(), rating.getRating());
    }

    /**
     * Get the learned embedding for a game.
     */
    public double[] getGameEmbedding(int gameId) {
        Integer idx = gameIdToIndex.get(gameId);
        if (idx == null || !isTrained) return null;
        return Arrays.copyOf(gameFactors[idx], LATENT_FACTORS);
    }

    /**
     * Get the learned content vector for a game (from tags).
     */
    public double[] getGameContentVector(int gameId) {
        Integer idx = gameIdToIndex.get(gameId);
        if (idx == null || !isTrained) return null;
        return Arrays.copyOf(gameContentVectors[idx], TAG_EMBEDDING_SIZE);
    }

    /**
     * Get the learned embedding for a user.
     */
    public double[] getUserEmbedding(int userId) {
        Integer idx = userIdToIndex.get(userId);
        if (idx == null || !isTrained) return null;
        return Arrays.copyOf(userFactors[idx], LATENT_FACTORS);
    }

    /**
     * Get the tag embedding.
     */
    public double[] getTagEmbedding(String tag) {
        return tagEmbeddings.get(tag);
    }

    public boolean isTrained() {
        return isTrained;
    }

    /**
     * Get model statistics.
     */
    public String getModelStats() {
        if (!isTrained) return "Model not trained";
        
        return String.format(
            "HYBRID ML Model Stats:\n" +
            "- Users: %d\n" +
            "- Games: %d\n" +
            "- Unique tags: %d\n" +
            "- CF latent factors: %d\n" +
            "- Tag embedding size: %d\n" +
            "- Hybrid weights: CF=%.1f, CB=%.1f\n" +
            "- Global mean rating: %.2f\n" +
            "- Total learned parameters: %d",
            userIdToIndex.size(),
            gameIdToIndex.size(),
            allTags.size(),
            LATENT_FACTORS,
            TAG_EMBEDDING_SIZE,
            HYBRID_CF_WEIGHT,
            HYBRID_CB_WEIGHT,
            globalMean,
            calculateTotalParameters()
        );
    }

    private int calculateTotalParameters() {
        int cfParams = (userIdToIndex.size() + gameIdToIndex.size()) * LATENT_FACTORS + 
                       userIdToIndex.size() + gameIdToIndex.size(); // biases
        int cbParams = allTags.size() * TAG_EMBEDDING_SIZE + 
                       gameIdToIndex.size() * TAG_EMBEDDING_SIZE +
                       userIdToIndex.size() * TAG_EMBEDDING_SIZE;
        return cfParams + cbParams;
    }
}
