# Yatayat Security and Environment Setup

Yatayat keeps database, email, Google OAuth, and administrator credentials outside tracked source files. The backend reads them from operating-system environment variables.

> [!IMPORTANT]
> Never commit real credentials, local `.env` files, or local Spring configuration files. The repository's `.env.example` is documentation only; Spring Boot and Vite do not automatically load it.

## Required environment variables

| Variable | Purpose | Safe local example |
| --- | --- | --- |
| `DB_URL` | MySQL JDBC connection URL | `jdbc:mysql://localhost:3306/yatayat_db` |
| `DB_USERNAME` | MySQL account username | `root` |
| `DB_PASSWORD` | MySQL account password | Use your local database password |
| `MAIL_USERNAME` | SMTP account username | Use the project email account |
| `MAIL_PASSWORD` | SMTP app password | Use a newly generated app password |
| `GOOGLE_CLIENT_ID` | Google OAuth client identifier | Use the ID from Google Cloud Console |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret | Use a newly generated client secret |
| `ADMIN_EMAIL` | Seed administrator email | Use the intended administrator address |
| `ADMIN_PASSWORD` | Seed administrator password | Use a strong, unique password |

Copy variable names from `.env.example`, but replace every `replace_with_...` value locally. Do not put working credentials in `.env.example`.

## Configure IntelliJ IDEA

1. Open the backend project at `Backend/yatayat-backend`.
2. Open **Run > Edit Configurations**.
3. Select the Spring Boot configuration for `YatayatBackendApplication`. If none exists, add a **Spring Boot** configuration and select that main class.
4. Locate **Environment variables** and add all variables from the table above.
5. Keep credentials in the local run configuration. Do not save or share the configuration as a project file.
6. Apply the configuration and start the backend.

If IntelliJ offers to store the run configuration as a project file, leave that option disabled for configurations containing secrets.

## Configure PowerShell

Set variables for the current PowerShell window before running the backend:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/yatayat_db"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_local_database_password"
$env:MAIL_USERNAME = "your_smtp_username"
$env:MAIL_PASSWORD = "your_smtp_app_password"
$env:GOOGLE_CLIENT_ID = "your_google_client_id"
$env:GOOGLE_CLIENT_SECRET = "your_google_client_secret"
$env:ADMIN_EMAIL = "your_admin_email"
$env:ADMIN_PASSWORD = "your_strong_admin_password"

Set-Location Backend\yatayat-backend
.\mvnw.cmd spring-boot:run
```

These values exist only in that PowerShell process and processes started from it. Close the window to clear them.

To remove one earlier in the same window:

```powershell
Remove-Item Env:DB_PASSWORD
```

## Configure Command Prompt

Set variables for the current Command Prompt window before starting the backend:

```bat
set "DB_URL=jdbc:mysql://localhost:3306/yatayat_db"
set "DB_USERNAME=root"
set "DB_PASSWORD=your_local_database_password"
set "MAIL_USERNAME=your_smtp_username"
set "MAIL_PASSWORD=your_smtp_app_password"
set "GOOGLE_CLIENT_ID=your_google_client_id"
set "GOOGLE_CLIENT_SECRET=your_google_client_secret"
set "ADMIN_EMAIL=your_admin_email"
set "ADMIN_PASSWORD=your_strong_admin_password"

cd Backend\yatayat-backend
mvnw.cmd spring-boot:run
```

These values last only for that Command Prompt process. Close the window to clear them.

## Configure production

1. Create a dedicated production MySQL account with only the permissions Yatayat requires. Do not use the MySQL root account.
2. Configure all required variables in the deployment platform's protected environment-variable or secret settings.
3. Do not place production values in source files, build scripts, frontend variables, images, logs, or deployment documentation.
4. Use separate database, SMTP, OAuth, and administrator credentials for development and production.
5. Configure the Google OAuth client with the exact production redirect origins and callback URLs required by the deployed application.
6. Restrict production CORS origins through `YATAYAT_CORS_ALLOWED_ORIGINS`; do not use a wildcard when credentials are enabled.
7. Serve the application over HTTPS and configure secure session-cookie behavior at the application or hosting layer.
8. Limit access to deployment settings and rotate credentials when a team member no longer needs access.
9. Confirm that application logs do not contain passwords, OAuth secrets, OTP values, session identifiers, or personal documents.
10. Back up the production database securely and test restoration using a non-production environment.

Production variables must be available to the backend process at startup. Frontend builds must never receive backend secrets through `VITE_` variables because Vite exposes those values to the browser.

## Credential rotation checklist

Credentials previously committed to Git must be rotated even after they are removed from the current files because older commits retain them.

- [ ] Change the MySQL password and update `DB_PASSWORD` in authorized run/deployment configurations.
- [ ] Revoke the exposed Gmail/SMTP app password, create a replacement, and update `MAIL_PASSWORD`.
- [ ] Rotate the Google OAuth client secret and update `GOOGLE_CLIENT_SECRET`.
- [ ] Review the Google OAuth client's authorized origins and redirect URIs.
- [ ] Replace the administrator password and update `ADMIN_PASSWORD`.
- [ ] Reset the existing database administrator account if it was already created; changing the seed variable alone does not update an existing user's password.
- [ ] Confirm that development and production use different credentials.
- [ ] Remove old credentials from IDE configurations, terminal profiles, CI settings, and hosting settings.
- [ ] Review access logs for unexpected use of the old credentials.
- [ ] Decide separately whether Git-history cleanup is necessary before publishing or sharing the repository.

## Local development checklist

- [ ] Install Java 17 and Node.js versions compatible with the project.
- [ ] Start MySQL and create the `yatayat_db` database.
- [ ] Configure all required environment variables using IntelliJ, PowerShell, or Command Prompt.
- [ ] Use local or development-only SMTP and OAuth credentials.
- [ ] Confirm that no real secret was added to `.env.example` or another tracked file.
- [ ] Verify the Maven wrapper with `mvnw.cmd -version`.
- [ ] Compile the backend with `mvnw.cmd clean compile`.
- [ ] Start the backend and confirm it can connect to MySQL.
- [ ] Build the frontend with `npm run build` from `Frontend/yatayat-frontend`.
- [ ] Confirm that the frontend API URL points to the intended backend environment.
- [ ] Run `git status` and inspect every changed or untracked file before committing.
- [ ] Run a credential scan and `git diff --check` before preparing a commit.

## If a credential is exposed

1. Do not post or copy the credential into an issue, chat, screenshot, or commit message.
2. Revoke or rotate it at the provider immediately.
3. Update the protected local or production environment setting.
4. Verify the application with the replacement credential.
5. Review Git history and provider logs to determine the remaining exposure.

