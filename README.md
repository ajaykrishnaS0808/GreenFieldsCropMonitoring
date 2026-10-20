# GreenFields Crop Monitoring System

A professional dark-green/charcoal web dashboard for the GreenFields Agri Farm college micro-project.

Java Spring Boot crop monitoring and yield management microproject.

## Technology
- Frontend: HTML, CSS, JavaScript, Chart.js
- Backend: Java 17 + Spring Boot REST API
- Database: SQLite (`database/farm.db`)
- Build: Maven

## Features
- Farm overview statistics and charts
- Crop records for paddy, maize, banana, and other crops
- Seasonal harvest reports and yield-per-acre comparison
- Crop activity timeline
- Irrigation, harvest, inspection, and fertilizer reminders
- Mark reminders complete
- Export harvest reports to CSV

## Requirements
Install a 64-bit JDK 17 or newer and Apache Maven 3.9+; VS Code with the Extension Pack for Java is recommended. Internet is needed on the first Maven build to download dependencies and for the hosted Google Fonts/Chart.js assets.

Check in PowerShell:
```powershell
java -version
mvn -version
```

## Windows PowerShell setup
Open PowerShell in the project directory:
```powershell
cd D:\Projects\GreenFieldsCropMonitoring
```
If you extracted the ZIP somewhere else, change the path to that location.

Run the app:
```powershell
mvn spring-boot:run
```

Then open: `http://localhost:8080`

Stop the app with `Ctrl+C` in the terminal. The database is created automatically at `database\farm.db` on first run. Sample rows are inserted only if the crops table is empty. Your saved changes remain in SQLite after restarting the app.

Build a runnable JAR:
```powershell
mvn clean package
java -jar target\greenfields-crop-monitoring.jar
```

## VS Code
1. Open the `GreenFieldsCropMonitoring` folder (not just `src`) using **File → Open Folder**.
2. Install **Extension Pack for Java** if not already installed.
3. Open the integrated terminal (**Terminal → New Terminal**).
4. Run `mvn spring-boot:run`.
5. Visit `http://localhost:8080`.

## API quick check
```powershell
Invoke-RestMethod http://localhost:8080/api/health
Invoke-RestMethod http://localhost:8080/api/crops
Invoke-RestMethod http://localhost:8080/api/stats
```

CSV download:
```powershell
Invoke-WebRequest http://localhost:8080/api/reports.csv -OutFile .\harvest-report.csv
```

## Important notes
- Dashboard seed data is demo data; replace it with verified farm measurements before presenting real-world conclusions.
- Yield per acre is calculated from each harvest record and the crop's currently registered area. If a field's area changes over time, keep a historical area value per harvest for strict year-to-year comparisons.
- The date in the page heading is a sample project heading; operational reminder dates are stored and sorted from SQLite.
- Chart.js and fonts load from public CDNs, so charts/fonts require internet access. The Java backend and SQLite database run locally.
