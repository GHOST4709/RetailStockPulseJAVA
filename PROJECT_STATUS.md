# Project status — Retail Stock Pulse

## Implemented in the first runnable version
- Maven project scaffold.
- Plain Java HTTP server using JDK HttpServer.
- JSON API using Gson.
- Product creation, editing, search, and soft deletion.
- Purchase receiving with weighted-average cost update.
- Sales with stock validation and insufficient-stock rejection.
- Dashboard, low-stock list, valuation, fast-moving, and sales reports.
- Responsive HTML/CSS/vanilla JavaScript frontend.
- Demo catalogue seeded at startup.

## Deliberately postponed
- MySQL database schema.
- JDBC persistence.
- Persistent transaction/rollback implementation.

## Important limitation
The current repository stores data in memory. Application data resets after server restart. The repository interface is the planned seam for adding JDBC later.
