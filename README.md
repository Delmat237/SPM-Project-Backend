# TechWave Auth Backend - SPM

Ce projet est un service d'authentification et de gestion d'utilisateurs robuste développé avec Spring Boot. Il gère l'enregistrement, la connexion (classique et Google SSO), l'activation de compte par OTP et la réinitialisation de mot de passe.

## 🚀 Technologies
- **Java 17** & **Spring Boot 3.4.5**
- **Base de données** : PostgreSQL
- **Sécurité** : Spring Security & JWT (JSON Web Token)
- **Documentation API** : Swagger UI (SpringDoc OpenAPI)
- **Notifications** : Spring Mail (SMTP Gmail)
- **Outils** : Lombok, Maven, Dotenv

## 🛠️ Installation et Configuration

### 1. Base de données (PostgreSQL)
Assurez-vous que PostgreSQL est installé et démarré.
```bash
# Se connecter à psql
sudo -u postgres psql

# Créer la base de données et l'utilisateur
CREATE DATABASE bd_spm;
CREATE USER user_bd_spm WITH PASSWORD 'votre_mot_de_passe';
GRANT ALL PRIVILEGES ON DATABASE bd_spm TO user_bd_spm;
ALTER DATABASE bd_spm OWNER TO user_bd_spm;
```

### 2. Configuration de l'environnement
Créez ou modifiez le fichier `.env` à la racine du projet :
```env
SERVER_PORT=8082

# Database
URL_BD=jdbc:postgresql://localhost:5432/bd_spm
DB_USERNAME=user_bd_spm
DB_PASSWORD=votre_mot_de_passe

# JWT
JWT_SECRET=votre_secret_tres_long_et_securise
JWT_EXPIRATION=1500000

# Mail (SMTP)
SPRING_MAIL_USERNAME=votre_email@gmail.com
SPRING_MAIL_PASSWORD=votre_mot_de_passe_application

# Google Auth
GOOGLE_CLIENT_ID=votre_google_client_id
```

### 3. Lancement
```bash
mvn clean install
mvn spring-boot:run
```

## 📖 Documentation de l'API (Swagger)
Une fois l'application lancée, la documentation interactive est disponible à l'adresse suivante :
👉 **[http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)**

## 📂 Structure du Projet
- `controller/` : Points d'entrée REST (`Auth`, `User`).
- `service/` : Logique métier (Traitement des données, emails).
- `security/` : Configuration JWT et filtres de sécurité.
- `model/` : Entités JPA (`User`, `Tokens`).
- `repository/` : Interfaces d'accès à la base de données.
- `dao/` : Objets de transfert de données (DTO).

## 🔐 Sécurité & Rôles
- **Public** : Inscription, Connexion, Validation OTP, Reset Password, Swagger UI.
- **Utilisateur (USER)** : Accès à son propre profil.
- **Administrateur (ADMIN)** : Gestion complète des utilisateurs.

---
*Développé pour le projet SPM.*
