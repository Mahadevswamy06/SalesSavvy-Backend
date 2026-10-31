# Sales Savvy Backend

Spring Boot REST API for Sales Savvy. Java 17+, Maven, and MySQL are required.

## Run locally

1. Create a MySQL database named `salessavvy`.
2. Set the environment variables listed in `.env.example` in your IDE/run configuration. Spring Boot does **not** automatically read a `.env` file.
3. Use local development values for `FRONTEND_ORIGIN` (`http://localhost:5174`) and `COOKIE_SECURE` (`false`).
4. Run `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).

The app listens on port `9090` by default.

## Deployment configuration

Set these environment variables in the backend hosting provider:

- `DB_URL`: production JDBC URL, using the host/provider's private database connection where available.
- `DB_USERNAME`, `DB_PASSWORD`: dedicated application database credentials (do not use MySQL root in production).
- `JWT_SECRET`: unique, random secret at least 64 bytes long for HS512.
- `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`: credentials from the Razorpay dashboard. The secret belongs only on the backend.
- `FRONTEND_ORIGIN`: exact deployed frontend origin, including `https://` and no path.
- `COOKIE_SECURE=true` when using HTTPS.
- `COOKIE_SAME_SITE`: usually `Lax` for same-site deployments; if frontend and backend are on different sites and cross-site cookies are required, configure `None` and `COOKIE_SECURE=true`.
- `DDL_AUTO`: use `validate` in production after creating/updating the schema through reviewed migrations.

Do not commit real credentials. `.env.example` contains placeholders only and is documentation; it is not automatically loaded by Spring Boot.

## Database scripts

The `database/` directory contains manual table/migration scripts for the existing schema. Review each script and back up production data before running a migration. Do not run migrations blindly against a live database.

## Before production

- Rotate credentials that were previously exposed in source files or repository history.
- Confirm protected endpoints enforce authentication and role authorization.
- Test login/logout cookies, CORS, checkout, payment verification, image uploads, and order flows over HTTPS.
- Confirm database backups and user data are not in the public repository.
