# SPEED WAGON- Video Game Library

A comprehensive video game library application built with Java and JavaFX, featuring IGDB API integration and AI-powered game recommendations.

## Features

### Core Functionality
- **User Management**: Registration, login, and profile management
- **Admin Panel**: Administrative controls for managing games and users
- **Game Catalog**: Browse and search video games
- **IGDB Integration**: Fetch game data from the Internet Game Database API
- **Shopping System**: Purchase games and build your library
- **Wishlist**: Save games for later

### AI-Powered Recommendations
The application includes an intelligent recommendation engine that:
- Learns from your game preferences and tags
- Tracks your purchase history
- Analyzes your game ratings
- Provides personalized game suggestions
- Uses collaborative filtering for similar game recommendations
- Implements implicit feedback learning from user interactions

### Technologies Used
- **Java 17**: Core programming language
- **JavaFX 21**: Modern UI framework
- **SQLite**: Lightweight database
- **IGDB API**: Game data source
- **Maven**: Build and dependency management
- **BCrypt**: Password hashing
- **OkHttp**: HTTP client for API calls
- **Gson**: JSON parsing
- **SLF4J & Logback**: Logging

## Project Structure

```
Cortex/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cortex/
│   │   │       ├── Main.java                      # Application entry point
│   │   │       ├── model/                         # Data models
│   │   │       │   ├── User.java                  # User model
│   │   │       │   ├── Admin.java                 # Admin model (extends User)
│   │   │       │   ├── VideoGame.java             # Video game model
│   │   │       │   └── GameRating.java            # Rating model
│   │   │       ├── database/                      # Database layer
│   │   │       │   ├── DatabaseManager.java       # Database connection
│   │   │       │   ├── UserDAO.java               # User data access
│   │   │       │   └── VideoGameDAO.java          # Game data access
│   │   │       ├── service/                       # Business logic
│   │   │       │   ├── AuthenticationService.java # User auth
│   │   │       │   ├── IGDBService.java           # IGDB API integration
│   │   │       │   └── RecommendationEngine.java  # AI recommendations
│   │   │       └── ui/                            # JavaFX views
│   │   │           ├── LoginView.java             # Login/register screen
│   │   │           └── MainView.java              # Main application UI
│   │   └── resources/
│   │       ├── styles/
│   │       │   └── main.css                       # Application styling
│   │       └── config.properties                  # Configuration
│   └── test/
│       └── java/                                  # Unit tests
├── pom.xml                                        # Maven configuration
├── .gitignore                                     # Git ignore rules
└── README.md                                      # This file
```

## Setup Instructions

### Prerequisites
- Java Development Kit (JDK) 17 or higher
- Maven 3.6 or higher
- Twitch Developer Account (for IGDB API access)

### Installation

1. **Navigate to project directory**
   ```bash
   cd /home/paul/ProiectPI/Cortex
   ```

2. **Configure IGDB API Credentials** (Optional - sample games work without this)
   - Go to https://dev.twitch.tv/console/apps
   - Create a new application
   - Copy your Client ID and Client Secret
   - Edit `src/main/resources/config.properties`:
     ```properties
     igdb.client.id=YOUR_CLIENT_ID_HERE
     igdb.client.secret=YOUR_CLIENT_SECRET_HERE
     ```

3. **Build the project**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn javafx:run
   ```

## Usage Guide

### First Time Setup

1. **Register an Account**
   - Launch the application
   - Click "Don't have an account? Register here"
   - Fill in username, email, and password (min 6 characters)
   - Click "Register"

2. **Login**
   - Enter your username and password
   - Click "Login"

3. **Load Games**
   - Navigate to "Browse Games"
   - Sample games are created automatically
   - Or configure IGDB API to load real game data

### Using the Application

**Browse Games**
- View all available games
- Search for specific titles
- Click "View Details" to see game information
- Purchase games or add to wishlist

**Recommendations**
- Get AI-powered personalized recommendations
- Based on your gaming preferences and history
- Updates as you purchase and rate games

**My Library**
- View all games you own
- Rate games to improve recommendations
- Access game details

**Wishlist**
- Save games you're interested in
- Easy access to desired games

### Admin Features

To create an admin, you can modify the registration in the code or database.

## AI Recommendation System

The recommendation engine uses multiple factors:

1. **Tag-Based Filtering**
   - Analyzes game genres, themes, and keywords
   - Matches with user preferences

2. **Collaborative Filtering**
   - Finds similar games based on shared attributes
   - Considers developer/publisher relationships

3. **Behavior Learning**
   - Tracks game views, purchases, and wishlist additions
   - Adjusts preferences based on ratings

4. **Scoring Algorithm**
   - Combines tag matching, game ratings, and popularity
   - Includes recency boost for new releases

## Database Schema

The application uses SQLite with the following main tables:
- `users`: User accounts and admin info
- `video_games`: Game catalog
- `game_tags`: Game genre/theme tags
- `game_platforms`: Supported platforms
- `user_owned_games`: User's game library
- `user_wishlist`: User's wishlist
- `user_preferred_tags`: Learned user preferences
- `game_ratings`: User ratings and reviews

## Troubleshooting

**"Failed to authenticate with IGDB API"**
- This is normal if you haven't configured API credentials
- Sample games will still work for testing

**"Database errors"**
- Delete `cortex_library.db` to reset the database
- Check file permissions

**UI not displaying correctly**
- Ensure JavaFX is properly installed
- Check Java version (requires Java 17+)

## Credits

- **IGDB API**: Game data provided by IGDB (https://www.igdb.com/)
- **JavaFX**: UI framework
- **SQLite**: Database engine