# Retail Stock Pulse

A college-level Java inventory management project. The current version includes a browser-based dashboard, a plain-Java HTTP API, product management, purchase receipts, sales processing, low-stock alerts, and reports.

> **Current storage mode:** in-memory demonstration storage. All product and transaction changes made while the server is running are lost when the server stops. MySQL/JDBC integration is intentionally deferred to the next development stage.

## Technology

- Java 17+ (the project is compatible with Java 25)
- Maven
- JDK `HttpServer` for HTTP routing
- Gson for JSON serialization/deserialization
- HTML, CSS, and vanilla JavaScript
- In-memory repository adapter (designed to be replaced by JDBC later)

## Run in VS Code on Windows

1. Install a JDK and Maven, and make sure `java -version` and `mvn -version` work in a new terminal.
2. Open this folder (`RetailStockPulse`) in VS Code.
3. Open **Terminal → New Terminal**.
4. Compile the project:

   ```powershell
   mvn clean compile
   ```

5. Start the application:

   ```powershell
   mvn exec:java
   ```

6. Open <http://localhost:8080> in your browser.
7. Check the API at <http://localhost:8080/api/health>.

The first Maven build downloads Gson and Maven plugins, so the first run requires an internet connection. Stop the server with `Ctrl+C`.

### Optional: run with the Windows batch file

Double-click `run.bat`, or execute it from a terminal. It compiles and starts the application.

## Demo data

The application seeds eight sample products on startup across Electronics, Stationery, Grocery, and Home & Living. This makes the dashboard and stock alerts visible immediately. Demo sales and purchase history start empty. Any changes made during a run are held in memory only.

## Features in this version

- Add, edit, search, and deactivate products.
- Track SKU, category, selling price, average purchase cost, current stock, and reorder level.
- Record purchase receipts and increase stock.
- Calculate weighted average cost when new inventory is received.
- Record sales and decrease stock.
- Reject a sale when requested stock is not available.
- Show low-stock and out-of-stock products.
- Display dashboard metrics.
- Generate category-wise inventory valuation, fast-moving products, and sales summaries.

The browser purchase/sale forms currently enter one product line at a time for a compact UI. The Java API accepts multiple line items in each receipt or sale.

## Java concepts demonstrated

| Concept | Usage |
|---|---|
| OOP | Models, repository interface, repository implementation, service, controller, and server classes |
| HashMap / Map | SKU/product lookup support, grouped quantities, and report aggregation |
| Custom exceptions | `InsufficientStockException`, `DuplicateSkuException`, and `ApiException` |
| Lambda expressions | Filtering and sorting products and transactions |
| Streams | Dashboard summaries, category valuation, and fast-moving product reports |
| JDBC | Planned next: replace the in-memory repository with a MySQL implementation |

## Project structure

```text
RetailStockPulse/
├── pom.xml
├── run.bat
├── README.md
└── src/
    └── main/
        ├── java/com/retailstock/
        │   ├── Main.java
        │   ├── config/                 # Application wiring and demo seed data
        │   ├── controller/             # API-facing controller
        │   ├── dto/                    # Request DTOs
        │   ├── exception/              # Application exceptions
        │   ├── model/                  # Product, Purchase, Sale models
        │   ├── repository/             # Storage contract and in-memory adapter
        │   ├── server/                 # JDK HTTP server and handlers
        │   ├── service/                # Inventory business rules and reports
        │   └── util/                   # JSON utilities
        └── resources/static/
            ├── index.html
            ├── css/styles.css
            └── js/                     # API client and UI behaviour
```

## API endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Health check |
| GET | `/api/dashboard` | Dashboard metrics |
| GET | `/api/products` | List products; optional `?search=` |
| GET | `/api/products/{id}` | Get a product |
| POST | `/api/products` | Create a product |
| PUT | `/api/products/{id}` | Update product details |
| DELETE | `/api/products/{id}` | Deactivate a product |
| GET | `/api/low-stock` | Products at/below reorder level |
| GET | `/api/purchases` | Purchase history |
| POST | `/api/purchases` | Record a purchase receipt |
| GET | `/api/sales` | Sales history |
| POST | `/api/sales` | Record a sale |
| GET | `/api/reports/valuation` | Category-wise stock valuation |
| GET | `/api/reports/fast-moving` | Products ranked by units sold |
| GET | `/api/reports/sales` | Sales summary; optional `?from=YYYY-MM-DD&to=YYYY-MM-DD` |

### Example API requests

Create a product:

```json
{
  "sku": "ELC-5001",
  "name": "USB-C Hub",
  "category": "Electronics",
  "sellingPrice": 1499.00,
  "averageCost": 900.00,
  "currentStock": 12,
  "reorderLevel": 4
}
```

Record a sale:

```json
{
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}
```

Record a purchase:

```json
{
  "supplierName": "North Star Distributors",
  "items": [
    { "productId": 1, "quantity": 10, "unitCost": 875.00 }
  ]
}
```

## Next development stage

1. Add JDBC and MySQL Connector/J dependencies.
2. Create the MySQL schema for products, purchase receipts/items, and sales/items.
3. Implement a JDBC repository that satisfies `InventoryRepository`, including the combined purchase/sale save operations.
4. Wire the JDBC repository in `AppConfig` instead of `InMemoryInventoryRepository`.
5. Use one JDBC transaction inside each combined save operation so the receipt/invoice and stock changes commit or roll back together.

This keeps business rules in `InventoryService` and makes the persistence layer replaceable without rewriting the frontend or API contract.

## Notes

- The application has no authentication in this college-project version.
- The server binds to port `8080` by default. Set the `PORT` environment variable to use another port.
- This is a local development/demo application. Do not expose it directly to the public internet without adding authentication, authorization, and production security controls.
