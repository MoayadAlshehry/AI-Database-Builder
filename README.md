
# AI Database Builder

This project allows users to describe their project and get database design suggestions based on the description, which are then created in MySQL. The project uses Maven for dependency management and the Gemini API for database design suggestions.

## Getting Started

### Prerequisites

Before running the project, make sure you have the following installed:

- **JDK 8** or higher
- **Maven**
- **Eclipse IDE** (recommended)

### Setup

1. **Clone the Repository**

   First, clone the repository to your local machine using Git.

   ```bash
   git clone https://github.com/MoayadAlshehry/ai-database-builder.git
   ```
   Or you can just download it from this link https://github.com/MoayadAlshehry/ai-database-builder.git
   
2. **Import the Project into Eclipse**

   - Open Eclipse.
   - Go to `File` > `Import...`.
   - Select `Maven` > `Existing Maven Projects`, then click `Next`.
   - Browse to the cloned project directory and select it.
   - Click `Finish`.

3. **Add your API Key and Database Credentials**

   - Replace the placeholders for the Gemini API key and MySQL credentials in `AIDatabaseBuilder.java`:
      You can get your Gemini API key through this link https://aistudio.google.com/app/apikey
      
     ```java
     private static final String API_KEY = "YOUR-API-KEY";  // Gemini API Only
     private static final String MYSQL_USER = "YOUR-MySQL-USERNAME";  // MySQL username
     private static final String MYSQL_PASSWORD = "YOUR-MySQL-PASSWORD";  // MySQL password
     ```

4. **Build the Project with Maven**

   - Right-click the project in Eclipse > `Run As` > `Maven build`.
   - In the dialog that appears, set the **Goals** field to `clean install` and click `Run`. This will download the dependencies and build the project.

### Running the Program

1. **Navigate to the Main Program**

   The main program file can be found here:

   ```
   AI-Database-Builder\ai-database-builder\src\main\java\com\example\aidatabase\AIDatabaseBuilder.java
   ```

2. **Run the Program**

   - Right-click `AIDatabaseBuilder.java` in Eclipse.
   - Select `Run As` > `Java Application`.


### How It Works

- When you run the program, it asks for a project description. The description is sent to the Gemini API to generate database design suggestions.
- If the user accepts the design, it will create a MySQL database and tables based on the suggestions.
  

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
