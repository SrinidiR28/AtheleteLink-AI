# 🏅 AthleteLink AI

AI-powered scholarship management & athlete development platform.

## Features
- **Athlete Registration & Authentication** (JWT-secured)
- **5 Scholarship Categories**: Under 18, Above 18, Women, Defence Personnel, Single Parents
- **Trial Management**: Create, browse, apply for athletic trials
- **Booking System**: Schedule trial sessions with confirmation emails
- **Coach Connections**: Connect athletes with coaches
- **Admin Dashboard**: Manage users, scholarships, trials, and announcements
- **Rich HTML Email Notifications**: Branded emails for submissions, approvals, bookings
- **Browser Confirmation Pages**: Beautiful HTML confirmation pages for bookings/trials

## Tech Stack
- **Backend**: Java 17, Spring Boot 3.3.4, Spring Security (JWT), Spring JDBC
- **Database**: MySQL 8+
- **Frontend**: Vanilla HTML/CSS/JS (served from Spring Boot static resources)
- **Email**: Gmail SMTP with App Passwords

---

## 🚀 Deploy on Railway (Recommended)

### 1. Create a Railway Account
Go to [railway.com](https://railway.com) and sign up (free trial available).

### 2. Add a MySQL Database
- Click **"New Project"** → **"Provision MySQL"**
- Railway auto-creates `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE` env vars

### 3. Deploy from GitHub
- Click **"New Service"** → **"GitHub Repo"** → select `AtheleteLink-AI`
- Railway will auto-detect the `Dockerfile` and build

### 4. Set Environment Variables
In Railway service settings → **Variables**, add:

| Variable | Value |
|---|---|
| `DB_URL` | `jdbc:mysql://${MYSQLHOST}:${MYSQLPORT}/${MYSQLDATABASE}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | (use Railway's `MYSQLUSER` reference: `${{MySQL.MYSQLUSER}}`) |
| `DB_PASSWORD` | (use Railway's `MYSQLPASSWORD` reference: `${{MySQL.MYSQLPASSWORD}}`) |
| `JWT_SECRET` | Any random 64+ character string |
| `MAIL_USERNAME` | Your Gmail address |
| `MAIL_PASSWORD` | Your Gmail App Password |
| `PUBLIC_BASE_URL` | Your Railway public URL (e.g. `https://your-app.up.railway.app`) |
| `CORS_ORIGINS` | Your Railway public URL |

### 5. Generate Domain
- Go to **Settings** → **Networking** → **Generate Domain**
- Your app is now live at `https://your-app.up.railway.app`

---

## 🖥️ Run Locally

### Prerequisites
- Java 17 (OpenJDK / Temurin)
- Maven 3.8+
- MySQL 8+

### Steps

```bash
# 1. Create MySQL database
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS athletelink_db;"

# 2. Run the app
cd backend
mvn spring-boot:run

# 3. Open browser
# http://localhost:8080
```

### Local Config
Edit `backend/src/main/resources/application.properties` to set your MySQL credentials:
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

---

## 📁 Project Structure
```
AtheleteLink-AI/
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/athletelink/
│       │   ├── config/          # SecurityConfig, CorsConfig
│       │   ├── controller/      # PlatformController, AdminController, AuthController
│       │   ├── dto/             # Request/Response DTOs
│       │   ├── filter/          # JWT Auth Filter
│       │   └── service/         # PlatformService, AdminService, EmailService
│       └── resources/
│           ├── application.properties
│           ├── schema.sql
│           └── static/          # Frontend (served by Spring Boot)
│               ├── index.html
│               ├── admin.html
│               └── js/
├── frontend/                    # Frontend source (mirrored in static/)
├── database/
│   └── schema.sql
├── Dockerfile                   # Railway/Docker deployment
├── railway.json                 # Railway config
└── README.md
```

## 👤 Default Admin
After first run, register a user and then manually promote to admin:
```sql
UPDATE users SET role='ADMIN' WHERE email='your-email@example.com';
```

---

Built with ❤️ by Vaibhav | AthleteLink AI
