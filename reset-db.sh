#!/bin/bash
# Script to reset the database if you encounter issues

echo "Cortex Database Reset Utility"
echo "=============================="
echo ""

# Check if database file exists
if [ -f "cortex_library.db" ]; then
    echo "Found existing database file: cortex_library.db"
    read -p "Do you want to delete it and start fresh? (yes/no): " response
    
    if [ "$response" = "yes" ] || [ "$response" = "y" ]; then
        rm cortex_library.db
        echo "✓ Database deleted successfully!"
        echo ""
        echo "Next time you run the application, a fresh database will be created."
    else
        echo "Database kept unchanged."
    fi
else
    echo "No existing database found."
    echo "A new database will be created when you run the application."
fi

echo ""
echo "To run the application: mvn javafx:run"
