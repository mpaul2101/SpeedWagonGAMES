package com.cortex.service;

import com.cortex.model.VideoGame;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for integrating with IGDB (Internet Game Database) API.
 * Requires Twitch API credentials.
 */
public class IGDBService {
    private static final Logger logger = LoggerFactory.getLogger(IGDBService.class);
    private static final String IGDB_API_URL = "https://api.igdb.com/v4";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private String accessToken;
    private final String clientId;
    private final String clientSecret;

    public IGDBService(String clientId, String clientSecret) {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        authenticateWithTwitch();
    }

    /**
     * Authenticate with Twitch to get access token for IGDB API.
     */
    private void authenticateWithTwitch() {
        try {
            String url = "https://id.twitch.tv/oauth2/token" +
                    "?client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&grant_type=client_credentials";

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", MediaType.parse("application/json")))
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                this.accessToken = jsonObject.get("access_token").getAsString();
                logger.info("Successfully authenticated with Twitch API");
            } else {
                logger.error("Failed to authenticate with Twitch API");
            }
        } catch (IOException e) {
            logger.error("Error authenticating with Twitch API", e);
        }
    }

    /**
     * Search for games by name.
     */
    public List<VideoGame> searchGames(String query, int limit) {
        List<VideoGame> games = new ArrayList<>();
        
        String requestBody = "search \"" + query + "\";" +
                "fields name, summary, rating, rating_count, cover.url, " +
                "first_release_date, genres.name, themes.name, keywords.name, " +
                "platforms.name, involved_companies.company.name, involved_companies.developer, " +
                "involved_companies.publisher;" +
                "limit " + limit + ";";

        try {
            String responseBody = executeIGDBRequest("/games", requestBody);
            games = parseGamesFromResponse(responseBody);
        } catch (IOException e) {
            logger.error("Error searching games", e);
        }

        return games;
    }

    /**
     * Get popular games.
     */
    public List<VideoGame> getPopularGames(int limit) {
        List<VideoGame> games = new ArrayList<>();
        
        String requestBody = "fields name, summary, rating, rating_count, cover.url, " +
                "first_release_date, genres.name, themes.name, keywords.name, " +
                "platforms.name, involved_companies.company.name, involved_companies.developer, " +
                "involved_companies.publisher;" +
                "where rating_count > 100;" +
                "sort rating desc;" +
                "limit " + limit + ";";

        try {
            String responseBody = executeIGDBRequest("/games", requestBody);
            games = parseGamesFromResponse(responseBody);
        } catch (IOException e) {
            logger.error("Error fetching popular games", e);
        }

        return games;
    }

    /**
     * Get games by genre/tag.
     */
    public List<VideoGame> getGamesByGenre(String genre, int limit) {
        List<VideoGame> games = new ArrayList<>();
        
        String requestBody = "fields name, summary, rating, rating_count, cover.url, " +
                "first_release_date, genres.name, themes.name, keywords.name, " +
                "platforms.name, involved_companies.company.name, involved_companies.developer, " +
                "involved_companies.publisher;" +
                "where genres.name = \"" + genre + "\";" +
                "sort rating desc;" +
                "limit " + limit + ";";

        try {
            String responseBody = executeIGDBRequest("/games", requestBody);
            games = parseGamesFromResponse(responseBody);
        } catch (IOException e) {
            logger.error("Error fetching games by genre", e);
        }

        return games;
    }

    /**
     * Get new releases.
     */
    public List<VideoGame> getNewReleases(int limit) {
        List<VideoGame> games = new ArrayList<>();
        
        long currentTimestamp = System.currentTimeMillis() / 1000;
        long thirtyDaysAgo = currentTimestamp - (30L * 24 * 60 * 60);
        
        String requestBody = "fields name, summary, rating, rating_count, cover.url, " +
                "first_release_date, genres.name, themes.name, keywords.name, " +
                "platforms.name, involved_companies.company.name, involved_companies.developer, " +
                "involved_companies.publisher;" +
                "where first_release_date > " + thirtyDaysAgo + 
                " & first_release_date < " + currentTimestamp + ";" +
                "sort first_release_date desc;" +
                "limit " + limit + ";";

        try {
            String responseBody = executeIGDBRequest("/games", requestBody);
            games = parseGamesFromResponse(responseBody);
        } catch (IOException e) {
            logger.error("Error fetching new releases", e);
        }

        return games;
    }

    /**
     * Execute a request to IGDB API.
     */
    private String executeIGDBRequest(String endpoint, String requestBody) throws IOException {
        Request request = new Request.Builder()
                .url(IGDB_API_URL + endpoint)
                .addHeader("Client-ID", clientId)
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create(requestBody, MediaType.parse("text/plain")))
                .build();

        Response response = httpClient.newCall(request).execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body().string();
        } else {
            throw new IOException("IGDB API request failed: " + response.code());
        }
    }

    /**
     * Parse games from IGDB API response.
     */
    private List<VideoGame> parseGamesFromResponse(String responseBody) {
        List<VideoGame> games = new ArrayList<>();
        
        try {
            JsonArray gamesArray = gson.fromJson(responseBody, JsonArray.class);
            
            for (int i = 0; i < gamesArray.size(); i++) {
                JsonObject gameJson = gamesArray.get(i).getAsJsonObject();
                VideoGame game = new VideoGame();
                
                // Basic info
                game.setIgdbId(gameJson.get("id").getAsLong());
                game.setTitle(gameJson.has("name") ? gameJson.get("name").getAsString() : "Unknown");
                game.setDescription(gameJson.has("summary") ? gameJson.get("summary").getAsString() : "");
                
                // Rating
                if (gameJson.has("rating")) {
                    game.setRating(gameJson.get("rating").getAsDouble() / 20.0); // Convert to 5-star scale
                }
                if (gameJson.has("rating_count")) {
                    game.setRatingCount(gameJson.get("rating_count").getAsInt());
                }
                
                // Cover image
                if (gameJson.has("cover")) {
                    JsonObject cover = gameJson.getAsJsonObject("cover");
                    if (cover.has("url")) {
                        String coverUrl = cover.get("url").getAsString();
                        game.setCoverImageUrl("https:" + coverUrl.replace("t_thumb", "t_cover_big"));
                    }
                }
                
                // Release date
                if (gameJson.has("first_release_date")) {
                    long timestamp = gameJson.get("first_release_date").getAsLong();
                    LocalDate releaseDate = Instant.ofEpochSecond(timestamp)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    game.setReleaseDate(releaseDate);
                }
                
                // Genres, themes, keywords (tags)
                List<String> tags = new ArrayList<>();
                if (gameJson.has("genres")) {
                    JsonArray genres = gameJson.getAsJsonArray("genres");
                    for (int j = 0; j < genres.size(); j++) {
                        JsonObject genre = genres.get(j).getAsJsonObject();
                        if (genre.has("name")) {
                            tags.add(genre.get("name").getAsString());
                        }
                    }
                }
                if (gameJson.has("themes")) {
                    JsonArray themes = gameJson.getAsJsonArray("themes");
                    for (int j = 0; j < themes.size(); j++) {
                        JsonObject theme = themes.get(j).getAsJsonObject();
                        if (theme.has("name")) {
                            tags.add(theme.get("name").getAsString());
                        }
                    }
                }
                if (gameJson.has("keywords")) {
                    JsonArray keywords = gameJson.getAsJsonArray("keywords");
                    for (int j = 0; j < Math.min(keywords.size(), 5); j++) {
                        JsonObject keyword = keywords.get(j).getAsJsonObject();
                        if (keyword.has("name")) {
                            tags.add(keyword.get("name").getAsString());
                        }
                    }
                }
                game.setTags(tags);
                
                // Platforms
                List<String> platforms = new ArrayList<>();
                if (gameJson.has("platforms")) {
                    JsonArray platformsArray = gameJson.getAsJsonArray("platforms");
                    for (int j = 0; j < platformsArray.size(); j++) {
                        JsonObject platform = platformsArray.get(j).getAsJsonObject();
                        if (platform.has("name")) {
                            platforms.add(platform.get("name").getAsString());
                        }
                    }
                }
                game.setPlatforms(platforms);
                
                // Developer/Publisher
                if (gameJson.has("involved_companies")) {
                    JsonArray companies = gameJson.getAsJsonArray("involved_companies");
                    for (int j = 0; j < companies.size(); j++) {
                        JsonObject company = companies.get(j).getAsJsonObject();
                        if (company.has("company")) {
                            JsonObject companyInfo = company.getAsJsonObject("company");
                            String companyName = companyInfo.has("name") ? companyInfo.get("name").getAsString() : "";
                            
                            if (company.has("developer") && company.get("developer").getAsBoolean()) {
                                game.setDeveloper(companyName);
                            }
                            if (company.has("publisher") && company.get("publisher").getAsBoolean()) {
                                game.setPublisher(companyName);
                            }
                        }
                    }
                }
                
                // Set a default price (IGDB doesn't provide pricing)
                game.setPrice(generatePriceBasedOnRating(game.getRating()));
                
                games.add(game);
            }
        } catch (Exception e) {
            logger.error("Error parsing games from response", e);
        }
        
        return games;
    }

    /**
     * Generate a reasonable price based on game rating.
     */
    private double generatePriceBasedOnRating(double rating) {
        if (rating >= 4.5) return 59.99;
        if (rating >= 4.0) return 49.99;
        if (rating >= 3.5) return 39.99;
        if (rating >= 3.0) return 29.99;
        return 19.99;
    }
}
