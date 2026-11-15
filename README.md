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

Open the Vite URL printed in the terminal (normally `http://localhost:5173`). The frontend development server proxies API requests to the backend.

## Verify

```powershell
Set-Location .\backend
mvn test

Set-Location ..\frontend
npm run build
```

## Candidate validation

Candidate registration validates names, email, country-specific phone numbers, skills, and job titles in the frontend and backend. Phone numbers are stored in international format. Each skill and job title must contain at least one letter; version numbers are allowed (for example, `Java8` and `Spring Boot Developer 3.8`).
