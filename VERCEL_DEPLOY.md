# Deploy Retail Stock Pulse to Vercel

This project uses Vercel's **Container Runtime**. The root `Dockerfile` builds the Java Maven application into an executable JAR and runs its JDK HTTP server on port 80. The same Java process serves both the static frontend and `/api/*` endpoints, so no separate frontend or API rewrite is needed.

## Important limitations of this build

- The current repository adapter is in-memory. Data written while an instance is running is not durable and can disappear when Vercel scales the container down, starts another instance, or creates a new deployment.
- The project currently has no authentication. Anyone who can reach the public URL can use the inventory endpoints. Use this deployment only as a demo with sample data; do not enter real stock, supplier, customer, or financial information.
- Add JDBC + a managed MySQL database and authentication before using this for real inventory.

## 1. Test the executable JAR locally (optional but recommended)

From the project root:

```powershell
mvn clean package
java -jar target/retail-stock-pulse-1.0.0.jar
```

Open `http://localhost:8080` and check `http://localhost:8080/api/health`.

## 2. Push the project to GitHub

Create an empty GitHub repository, then run these commands from this folder (replace the URL with your own repository URL):

```powershell
git init
git add .
git commit -m "Prepare Retail Stock Pulse for deployment"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/RetailStockPulse.git
git push -u origin main
```

## 3. Import it into Vercel

1. Open the Vercel Dashboard and choose **Add New → Project**.
2. Import the GitHub repository you just created.
3. Set the Root Directory to the folder containing `Dockerfile`, `pom.xml`, and `src` (normally `./`).
4. Use the **Container** framework/preset. Vercel's Container Runtime can also be auto-detected from a root `Dockerfile` for a new project.
5. Deploy. No build command, output directory, or environment variables are required for this demo; the Dockerfile sets `PORT=80`.

If Vercel does not select the Container preset automatically, link the project with the Vercel CLI and run:

```powershell
vercel project update --framework container --yes
```

Then redeploy.

## 4. Verify the deployment

Open the deployment URL and check:

- `/` — dashboard frontend
- `/api/health` — backend health endpoint
- Products, purchase, sale, and report pages/actions

## Runtime notes

Vercel's Container Runtime is currently documented as Beta and available on all plans. The container runtime scales down idle instances; because this project stores its data in process memory, changes are not persistent. A hosted database and authentication are required before treating the deployment as a real inventory system.
