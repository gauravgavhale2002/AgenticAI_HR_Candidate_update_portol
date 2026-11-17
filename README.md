# TalentFlow AI

A candidate portal and HR recruitment workflow with a Spring Boot API and React frontend.

## Requirements

- Java 21
- Maven 3.9+
- Node.js 20.19+ or 22.12+

## Run the backend

From the repository root in PowerShell:

```powershell
$env:HR_USERNAME = "admin"
$env:HR_PASSWORD = "set-a-local-password"
Set-Location .\backend
mvn spring-boot:run
```

The API starts at `http://localhost:8080`. H2 is configured as an in-memory database, so its data is reset when the backend stops.

Set `HR_USERNAME` and `HR_PASSWORD` in the environment before starting the backend. If `HR_PASSWORD` is not set, Spring generates a random password for that run.

## Run the frontend

In another PowerShell terminal from the repository root:

```powershell
Set-Location .\frontend
npm ci
npm run dev
```

Open the HTTPS Vite URL printed in the terminal (normally `https://localhost:5173`). Vite uses a locally trusted development certificate. If port 5173 is already in use, Vite uses the next available port. The frontend development server proxies API requests to the backend.

## Deploy a public demo for free

The Render Blueprint in `render.yaml` builds the frontend and Spring Boot API into one HTTPS web service. To deploy:

1. Push this project to a GitHub repository that your Render account can access.
2. In Render, choose **New → Blueprint**, connect the repository, and deploy the `render.yaml` Blueprint.
3. Open the generated service URL. The HR username is `manager`; Render generates the HR password, which you can view in the service's environment settings.

This free demo uses an in-memory database and temporary local file storage. Data and uploaded resumes may be lost when the service restarts or redeploys, and the free service may take a little while to wake after inactivity. Use fake candidate information and sample resumes only; do not use real personal data. Share the generated HR password securely with your manager, not in this repository.

## Verify

```powershell
Set-Location .\backend
mvn test

Set-Location ..\frontend
npm run build
```

## Candidate validation

Candidate registration validates names, email, country-specific phone numbers, skills, and job titles in the frontend and backend. Phone numbers are stored in international format. Each skill and job title must contain at least one letter; version numbers are allowed (for example, `Java8` and `Spring Boot Developer 3.8`).
