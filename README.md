# Retail Stock Pulse

A college-level Java inventory management project with a browser-based dashboard, plain-Java HTTP API, product management, purchase receipts, sales processing, low-stock alerts, and reports.

## Technology

- Java 17+ (compatible with Java 25)
- Maven
- JDK `HttpServer` for HTTP routing
- Gson for JSON serialization/deserialization
- HTML, CSS, and vanilla JavaScript
- In-memory repository adapter (designed to be replaced by JDBC later)

## Run locally

```powershell
mvn clean compile
mvn exec:java
```

Open <http://localhost:8080>. API health check: <http://localhost:8080/api/health>.

To create and run the executable, dependency-inclusive JAR:

```powershell
mvn clean package
java -jar target/retail-stock-pulse-1.0.0.jar
```

## Deploy to Vercel

This version includes a root `Dockerfile` for Vercel's Container Runtime. See [VERCEL_DEPLOY.md](VERCEL_DEPLOY.md) for the GitHub-to-Vercel deployment steps and important demo limitations.

## Current demo limitations

- Data is stored in memory and is lost when the application process restarts. Different server instances do not share memory.
- The application has no authentication. Do not expose real inventory or business data until authentication and a persistent database are added.
- MySQL/JDBC integration is intentionally postponed.

## Features

- Add, edit, search, and deactivate products.
- Track SKU, category, selling price, average purchase cost, current stock, and reorder level.
- Record purchase receipts and increase stock.
- Calculate weighted average cost when new inventory is received.
- Record sales and decrease stock.
- Reject a sale when requested stock is not available.
- Show low-stock and out-of-stock products.
- Display dashboard metrics.
- Generate category-wise inventory valuation, fast-moving products, and sales summaries.

## Java concepts demonstrated

| Concept | Usage |
|---|---|
| OOP | Models, repository interface, repository implementation, service, controller, and server classes |
| HashMap / Map | SKU/product lookup support, grouped quantities, and report aggregation |
| Custom exceptions | Insufficient-stock, duplicate-SKU, and API exceptions |
| JDBC | Planned next: replace the in-memory repository with a MySQL implementation |
| Lambda expressions | Filtering and sorting products and transactions |
| Streams | Dashboard summaries, category valuation, and fast-moving product reports |
