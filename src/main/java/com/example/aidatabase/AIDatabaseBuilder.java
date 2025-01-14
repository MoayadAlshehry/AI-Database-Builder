package com.example.aidatabase;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class AIDatabaseBuilder {

    private static final String API_KEY = "YOUR-API-KEY";  // Gemini API Only
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY; // API URL for Gemini API

    private static final String MYSQL_HOST = "localhost";  // MySQL host (usually localhost)
    private static final int MYSQL_PORT = 3306;  // Default MySQL port
    private static final String MYSQL_USER = "YOUR-MySQL-USERNAME";  // MySQL username
    private static final String MYSQL_PASSWORD = "YOUR-MySQL-PASSWORD";  // MySQL password

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Prompt user to describe their project
        System.out.println("Describe your project:");
        String projectDescription = scanner.nextLine();

        // Call Gemini API to get database design suggestions based on the project description
        String geminiResponse = callGeminiAPI(projectDescription);

        DatabaseDesign design = null;

        // Parse the response from Gemini API into a DatabaseDesign object
        if (geminiResponse != null) {
            design = parseGeminiResponse(geminiResponse);
        }

        // If database design suggestions are received, display them to the user
        if (design != null) {
            displaySuggestions(design);

            System.out.println("Do you accept these suggestions? (yes/no)");
            String accept = scanner.nextLine();

            // If the user accepts the suggestions, create the database and tables
            if (accept.equalsIgnoreCase("yes")) {
                createDatabase(design);
                System.out.println("Database and tables created successfully.");
            } else {
                System.out.println("Database creation canceled.");
            }
        } else {
            System.out.println("No suggestions received from Gemini. Please try a different project description.");
        }

        scanner.close();
    }

    /**
     * Calls the Gemini API to get database design suggestions based on a project description.
     * @param prompt The project description provided by the user.
     * @return A JSON response from Gemini API containing database design suggestions.
     */
    private static String callGeminiAPI(String prompt) {
        try {
            // Set up the connection to Gemini API
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Construct the request payload with the user-provided project description
            String jsonInputString = String.format("{\"contents\": [{\"parts\":[{\"text\": \"%s\"}]}]}", 
                    "Given a project description: (" + prompt +"), suggest a database design including databaseName, tableName, and their properties (columnName and type) in JSON format Without any description.");

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read the response from the API
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            } catch (IOException e) {
                System.err.println("Error calling Gemini API: " + e.getMessage());
                return null;
            }
        } catch (IOException e) {
            System.err.println("Error connecting to Gemini API: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses the JSON response from Gemini API and extracts database design suggestions.
     * @param geminiResponse The JSON response from Gemini API.
     * @return A DatabaseDesign object containing the parsed suggestions.
     */
    private static DatabaseDesign parseGeminiResponse(String geminiResponse) {
        Gson gson = new Gson();
        try {
            JsonObject responseJson = gson.fromJson(geminiResponse, JsonObject.class);

            // Extract the "candidates" array from the response
            JsonArray candidates = responseJson.getAsJsonArray("candidates");

            if (candidates != null && candidates.size() > 0) {
                // Access the first candidate (assuming there's only one)
                JsonObject candidate = candidates.get(0).getAsJsonObject();

                // Extract the "content" object from the candidate
                JsonObject content = candidate.getAsJsonObject("content");

                // Extract the parts array from the "content" object
                JsonArray parts = content.getAsJsonArray("parts");

                if (parts != null && parts.size() > 0) {
                    // Access the first part (assuming there's only one)
                    JsonObject part = parts.get(0).getAsJsonObject();

                    // Extract the text field containing the JSON string of the database design
                    String text = part.get("text").getAsString();

                    // Extract the valid JSON string from the text
                    String jsonStr = extractJsonString(text);

                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        // Parse the extracted JSON into a DatabaseDesign object
                        JsonObject jsonObject = gson.fromJson(jsonStr, JsonObject.class);
                        DatabaseDesign design = new DatabaseDesign();

                        // Extract the database name from the JSON
                        JsonElement databaseNameElement = jsonObject.get("databaseName");
                        if (databaseNameElement != null && databaseNameElement.isJsonPrimitive() && databaseNameElement.getAsJsonPrimitive().isString()) {
                            design.databaseName = databaseNameElement.getAsString();
                        } else {
                            System.err.println("Error: 'databaseName' field is not a string or is missing in Gemini response.");
                        }

                        // Extract the table data from the JSON
                        JsonArray tables = jsonObject.getAsJsonArray("tables");

                        if (tables != null) {
                            design.tables = new ArrayList<>();
                            for (JsonElement tableElement : tables) {
                                JsonObject tableObject = tableElement.getAsJsonObject();
                                String tableName = tableObject.get("tableName").getAsString();
                                Table table = new Table(tableName);
                                JsonArray columns = tableObject.getAsJsonArray("columns");

                                if (columns != null) {
                                    // Add columns to the table
                                    for (JsonElement columnElement : columns) {
                                        JsonObject columnObject = columnElement.getAsJsonObject();
                                        String columnName = columnObject.get("columnName").getAsString();
                                        String dataType = columnObject.get("type").getAsString();
                                        table.addColumn(columnName, dataType);
                                    }
                                }

                                design.tables.add(table);
                            }
                        }

                        return design;
                    } else {
                        System.err.println("Error: Could not extract valid JSON string from Gemini response.");
                    }
                }
            }

            return null; 
        } catch (JsonSyntaxException e) {
            System.err.println("Error parsing Gemini response: " + e.getMessage());
            return null; 
        }
    }

    /**
     * Extracts the JSON string from the text returned by the Gemini API.
     * @param text The raw text response from Gemini API.
     * @return The extracted JSON string, or null if no valid JSON is found.
     */
    private static String extractJsonString(String text) {
        int startIndex = text.indexOf("```json");
        int endIndex = text.indexOf("```", startIndex + "```json".length());

        if (startIndex != -1 && endIndex != -1) {
            return text.substring(startIndex + "```json".length(), endIndex);
        } else {
            return null;
        }
    }

    /**
     * Displays the database design suggestions to the user.
     * @param design The DatabaseDesign object containing the suggested database schema.
     */
    private static void displaySuggestions(DatabaseDesign design) {
        if (design != null) {
            System.out.println("\nGemini's Suggestions:");
            if (design.databaseName != null) {
                System.out.println("Database Name: " + design.databaseName);
            } else {
                System.out.println("Database Name: (not provided by Gemini)");
            }
            System.out.println("Tables:");

            if (design.tables != null && !design.tables.isEmpty()) {
                for (Table table : design.tables) {
                    System.out.println("  " + table.name);
                    System.out.println("    Columns:");
                    for (Column column : table.columns) {
                        System.out.println("      " + column.name + " (" + column.dataType + ")");
                    }
                }
            } else {
                System.out.println("  (No tables suggested by Gemini)");
            }
        } else {
            System.out.println("No suggestions available to display.");
        }
    }

    /**
     * Creates the database and tables based on the design suggestions.
     * @param design The DatabaseDesign object containing the database schema to be created.
     */
    private static void createDatabase(DatabaseDesign design) {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/",
                MYSQL_USER,
                MYSQL_PASSWORD
            )) {
            // Create database if it doesn't exist
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + design.databaseName);
            }

            // Switch to the newly created database
            connection.setCatalog(design.databaseName);

            // Create tables based on the design
            for (Table table : design.tables) {
                StringBuilder createTableStatement = new StringBuilder("CREATE TABLE ");
                createTableStatement.append(table.name + " (");

                // Add columns to the table creation statement
                for (int i = 0; i < table.columns.size(); i++) {
                    Column column = table.columns.get(i);
                    createTableStatement.append(column.name + " " + column.dataType);
                    if (i < table.columns.size() - 1) {
                        createTableStatement.append(", ");
                    }
                }

                createTableStatement.append(")");

                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(createTableStatement.toString());
                } catch (SQLException e) {
                    System.err.println("Error creating table " + table.name + ": " + e.getMessage());
                }
            }

        } catch (SQLException e) {
            System.err.println("Error connecting to MySQL or creating database: " + e.getMessage());
        }
    }

    // Represents the database design, including database name and tables.
    private static class DatabaseDesign {
        String databaseName; // Name of the database
        List<Table> tables; // List of tables in the database
    }

    // Represents a table within the database.
    private static class Table {
        String name; // Name of the table
        List<Column> columns; // List of columns in the table

        public Table(String name) {
            this.name = name;
            this.columns = new ArrayList<>();
        }

        public void addColumn(String name, String dataType) {
            this.columns.add(new Column(name, dataType));
        }
    }

    // Represents a column within a table.
    private static class Column {
        String name; // Name of the column
        String dataType; // Data type of the column

        public Column(String name, String dataType) {
            this.name = name;
            this.dataType = dataType;
        }
    }
}
