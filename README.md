# 🚀 SPM Backend — Service d'Authentification

> **SPM** (Solution de Gestion de Projets Modulaire) — Backend d'authentification et de gestion des utilisateurs.  
> Développé par la **Cellule Projet** du **Club Génie Informatique de l'ENSPY**.

---

## 📋 Table des matières

- [Technologies](#-technologies)
- [Architecture](#-architecture)
- [Installation](#️-installation)
- [Configuration](#-configuration)
- [API Endpoints](#-api-endpoints)
- [Sécurité & Rôles](#-sécurité--rôles)
- [Swagger](#-swagger)
- [Docker](#-docker)

---

## 🧰 Technologies

| Catégorie | Technologie |
|-----------|-------------|
| Langage | Java 17 |
| Framework | Spring Boot 3.4.5 |
| Base de données | PostgreSQL |
| Sécurité | Spring Security + JWT (jjwt 0.11.5) |
| OAuth | Google SSO (google-api-client) |
| Email | Spring Mail (SMTP Gmail) |
| Documentation | Swagger UI (SpringDoc OpenAPI 2.8.5) |
| Build | Maven |
| Outils | Lombok, Dotenv |
| Containerisation | Docker (multi-stage build) |

---

## 🏗 Architecture

```
src/main/java/com/techwave/auth/
├── AuthApplication.java                  # Point d'entrée Spring Boot
└── user/
    ├── controller/
    │   ├── AuthController.java           # Endpoints d'authentification
    │   ├── UserController.java           # Endpoints de gestion utilisateurs
    │   └── ErrorResponse.java            # DTO de réponse d'erreur
    ├── dao/
    │   ├── LoginRequest.java             # DTO de connexion
    │   ├── LoginResponse.java            # DTO de réponse (token + user info)
    │   └── RegisterRequest.java          # DTO d'inscription (avec validation)
    ├── model/
    │   ├── User.java                     # Entité JPA (implémente UserDetails)
    │   ├── UserRole.java                 # Enum des rôles (USER, ADMIN)
    │   ├── VerificationToken.java        # Token OTP d'activation de compte
    │   └── PasswordResetToken.java       # Token de réinitialisation de mot de passe
    ├── repository/
    │   ├── UserRepository.java           # Accès BDD utilisateurs
    │   ├── VerificationTokenRepository.java
    │   └── PasswordResetTokenRepository.java
    ├── security/
    │   ├── SecurityConfig.java           # Config Spring Security + CORS
    │   ├── JwtUtil.java                  # Génération & validation JWT (HS512)
    │   └── JwtAuthenticationFilter.java  # Filtre d'interception des requêtes
    └── service/
        ├── UserService.java              # Logique métier utilisateurs (CRUD)
        ├── EmailService.java             # Envoi d'emails HTML (activation, reset)
        └── CustomUserDetailsService.java # Chargement utilisateur pour Spring Security
```

---

## 🛠️ Installation

### Prérequis

- **Java 17+**
- **Maven 3.9+**
- **PostgreSQL 14+**

### 1. Cloner le projet

```bash
git clone <url-du-repo>
cd Backend-SPM
```

### 2. Créer la base de données PostgreSQL

```sql
-- Se connecter à psql
sudo -u postgres psql

-- Créer la base et l'utilisateur
CREATE DATABASE bd_spm;
CREATE USER user_bd_spm WITH PASSWORD 'votre_mot_de_passe';
GRANT ALL PRIVILEGES ON DATABASE bd_spm TO user_bd_spm;
ALTER DATABASE bd_spm OWNER TO user_bd_spm;
```

### 3. Configurer l'environnement

Copier le fichier d'exemple et remplir les valeurs :

```bash
cp .env.example .env
```

### 4. Lancer l'application

```bash
mvn clean install
mvn spring-boot:run
```

L'application démarre sur **http://localhost:8082**

---

## ⚙ Configuration

Créez un fichier `.env` à la racine du projet avec les variables suivantes :

```env
SERVER_PORT=8082

# Base de données PostgreSQL
URL_BD=jdbc:postgresql://localhost:5432/bd_spm
DB_USERNAME=user_bd_spm
DB_PASSWORD=votre_mot_de_passe

# JWT
JWT_SECRET=votre_secret_jwt_tres_long_et_securise_minimum_64_caracteres
JWT_EXPIRATION=1500000

# Email SMTP (Gmail)
SPRING_MAIL_USERNAME=votre_email@gmail.com
SPRING_MAIL_PASSWORD=votre_mot_de_passe_application

# URLs de l'application
APP_BACKEND_URL=http://localhost:8082
APP_FRONTEND_URL=http://localhost:5173

# OAuth Google
GOOGLE_CLIENT_ID=votre_google_client_id
```

> **Note** : Pour Gmail, utilisez un [mot de passe d'application](https://support.google.com/accounts/answer/185833) et non votre mot de passe principal.

---

## 🌐 API Endpoints

### 🔓 Authentification — `/api/auth`

| Méthode | Endpoint | Description | Body / Params |
|---------|----------|-------------|---------------|
| `POST` | `/api/auth/register` | Inscription (envoi OTP par email) | `{ email, password, nom, telephone?, pays? }` |
| `POST` | `/api/auth/login` | Connexion classique | `{ email, password }` |
| `POST` | `/api/auth/google` | Connexion via Google SSO | `{ token }` |
| `POST` | `/api/auth/verify-otp` | Activer le compte avec le code OTP | `?email=...&code=...` |
| `POST` | `/api/auth/resend-activation` | Renvoyer le code OTP | `?email=...` |
| `POST` | `/api/auth/forgot-password` | Demander un lien de reset password | `?email=...` |
| `POST` | `/api/auth/reset-password` | Réinitialiser le mot de passe | `?token=...&newPassword=...` |

### 👤 Utilisateurs — `/api/users`

| Méthode | Endpoint | Rôle requis | Description |
|---------|----------|-------------|-------------|
| `GET` | `/api/users` | `ADMIN` | Lister tous les utilisateurs |
| `POST` | `/api/users` | `ADMIN` | Créer un utilisateur |
| `PUT` | `/api/users/{id}` | `ADMIN` | Modifier un utilisateur |
| `DELETE` | `/api/users/{id}` | `ADMIN` | Supprimer un utilisateur |
| `PUT` | `/api/users/me` | `USER` | Modifier son propre profil |
| `DELETE` | `/api/users/me` | `USER` | Désactiver son propre compte |

### Exemple de réponse login

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "nom": "Jean Dupont",
    "roles": ["ROLE_USER"]
  }
}
```

---

## 🔐 Sécurité & Rôles

### Authentification

- **JWT stateless** signé avec HS512
- Token transmis via header `Authorization: Bearer <token>`
- Sessions désactivées (STATELESS)

### Rôles

| Rôle | Accès |
|------|-------|
| **Public** | Inscription, Connexion, OTP, Reset Password, Swagger |
| **USER** | Accès et modification de son propre profil |
| **ADMIN** | Gestion complète de tous les utilisateurs |

### Flux d'inscription

```
1. POST /register  →  Création compte (disabled) + envoi OTP par email
2. POST /verify-otp  →  Validation du code 6 chiffres → compte activé
3. POST /login  →  Connexion → réception du JWT
```

### Flux de reset password

```
1. POST /forgot-password  →  Envoi d'un lien par email
2. Clic sur le lien  →  Redirection vers le frontend
3. POST /reset-password  →  Nouveau mot de passe enregistré
```

### CORS

Origines autorisées : `http://localhost:5173` (configurable dans `SecurityConfig.java`)

---

## 📖 Swagger

Une fois l'application lancée, la documentation interactive est disponible :

👉 **http://localhost:8082/swagger-ui/index.html**

---

## 🐳 Docker

### Build & Run

```bash
# Construire l'image
docker build -t spm-backend .

# Lancer le conteneur
docker run -p 8082:8082 --env-file .env spm-backend
```

### Dockerfile (multi-stage)

- **Build** : Maven 3.9.6 + Eclipse Temurin 21
- **Runtime** : Eclipse Temurin 21 JDK

---

## 📄 Licence

Projet open source développé par le Club Génie Informatique de l'ENSPY.

---

*Développé avec ❤️ pour le projet SPM — Club GI ENSPY © 2026*
