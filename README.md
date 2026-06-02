# SPM Backend — Plateforme de Gestion de Projets Agile

> **SPM** (Scrum Project Manager) est un backend complet de gestion de projets agile style Jira avec collaboration en temps réel, tableau Kanban, diagramme de Gantt, analytics et exports asynchrones.  
> Développé par la **Cellule Projet** du **Club Génie Informatique de l'ENSPY**.

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.5-6DB33F?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-336791?logo=postgresql)
![JWT](https://img.shields.io/badge/Auth-JWT_HS512-black?logo=jsonwebtokens)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker)
![License](https://img.shields.io/badge/license-MIT-green)

---

## 📋 Table des matières

- [Technologies](#-technologies)
- [Architecture & Modules](#-architecture--modules)
- [Installation](#️-installation)
- [Configuration](#-configuration)
- [API Endpoints](#-api-endpoints)
- [Temps Réel via WebSockets (STOMP)](#-temps-réel-via-websockets-stomp)
- [Documentation interactive (Swagger / OpenAPI)](#-documentation-interactive-swagger--openapi)
- [Docker](#-docker)
- [Script de Démo & Test rapide](#-script-de-démo--test-rapide)

---

## 🧰 Technologies

| Catégorie | Technologie |
|-----------|-------------|
| **Langage** | Java 17 |
| **Framework principal** | Spring Boot 3.4.5 |
| **Base de données** | PostgreSQL 14+ |
| **Sécurité** | Spring Security + JWT (HS512) |
| **OAuth** | Google SSO (ID Token Verification) |
| **Temps Réel** | Spring WebSocket + STOMP Broker |
| **Documentation** | Swagger UI (SpringDoc OpenAPI 2.8.5) |
| **Emailing** | JavaMail (SMTP Gmail) |
| **Build & Docker** | Maven, Multi-stage Dockerfile |

---

## 🏗 Architecture & Modules

Le projet est structuré en plusieurs modules métiers distincts :

```
src/main/java/com/techwave/auth/
├── AuthApplication.java          # Point d'entrée principal
├── admin/                        # Module Administration (Régulation système)
├── analytics/                    # Module Analytics & Rapports (Burndown, Vélocité, Export)
├── collaboration/                # Module Collaboration (Commentaires @mentions, Fichiers, Notifications)
├── common/                       # Gestion des exceptions, DTOs globaux
├── project/                      # Module Projets & Tâches (Membres, Invitations, Gantt, Kanban, FSM)
├── user/                         # Module Utilisateurs (Profils, Authentification locale & Google)
└── websocket/                    # Configuration & Contrôleur WebSockets (STOMP)
```

---

## 🛠️ Installation

### Prérequis

- **Java 17+**
- **Maven 3.9+**
- **PostgreSQL 14+**

### 1. Initialiser la base de données PostgreSQL

```sql
CREATE DATABASE bd_spm;
CREATE USER user_bd_spm WITH PASSWORD 'spm1234';
GRANT ALL PRIVILEGES ON DATABASE bd_spm TO user_bd_spm;
ALTER DATABASE bd_spm OWNER TO user_bd_spm;
```

### 2. Configurer le fichier d'environnement

Copier le fichier d'exemple et modifier les configurations :

```bash
cp .env.example .env
```

### 3. Compiler & Lancer

```bash
mvn clean install
mvn spring-boot:run
```

L'application démarrera par défaut sur le port **8082** : **http://localhost:8082**

---

## ⚙ Configuration (.env)

Créez un fichier `.env` à la racine pour surcharger les valeurs par défaut :

```env
SERVER_PORT=8082

# Base de données PostgreSQL
URL_BD=jdbc:postgresql://localhost:5432/bd_spm
DB_USERNAME=user_bd_spm
DB_PASSWORD=spm1234

# Sécurité & JWT
JWT_SECRET=un_secret_jwt_tres_long_et_securise_minimum_512_bits_pour_HS512
JWT_EXPIRATION=1500000

# Email (SMTP Gmail)
SPRING_MAIL_USERNAME=votre_email@gmail.com
SPRING_MAIL_PASSWORD=votre_mot_de_passe_d_application

# URLs applicatives
APP_BACKEND_URL=http://localhost:8082
APP_FRONTEND_URL=http://localhost:5173
```

---

## 🌐 API Endpoints

Tous les endpoints REST (sauf `/api/auth/**`) requièrent le header `Authorization: Bearer <TOKEN_JWT>`.

### 🔑 1. Authentification — `/api/auth`
- `POST /register` : Inscription d'un utilisateur (génère un OTP)
- `POST /verify-otp` : Vérification du code OTP et activation du compte
- `POST /login` : Authentification locale et retour du JWT
- `POST /google` : Authentification via Google SSO
- `POST /forgot-password` / `POST /reset-password` : Réinitialisation de mot de passe

### 📂 2. Gestion de Projets — `/api/projects`
- `GET /` : Liste des projets de l'utilisateur (paginée)
- `POST /` : Créer un projet (l'auteur devient `OWNER`)
- `GET /{id}` : Détails d'un projet
- `PATCH /{id}` / `DELETE /{id}` : Modifier/Supprimer (soft delete) un projet
- `POST /{id}/members` : Inviter un collaborateur par e-mail
- `PATCH /{id}/members/{userId}` : Modifier le rôle d'un membre (`ADMIN`, `MEMBER`, `READER`)
- `DELETE /{id}/members/{userId}` : Retirer un membre du projet

### 📋 3. Gestion des Tâches — `/api/projects/{projectId}/tasks`
- `GET /` : Liste des tâches (supporte les filtres `status`, `assignee`, `priority`, et les vues `?view=kanban` ou `?view=gantt`)
- `POST /` : Créer une tâche
- `PATCH /{taskId}/status` : Changer le statut d'une tâche via la **Machine à États (FSM)**
- `DELETE /{taskId}` / `PATCH /{taskId}/restore` : Soft-delete et restauration de tâche
- `GET /{taskId}/subtasks` / `POST /{taskId}/subtasks` : Gestion des sous-tâches

### 💬 4. Collaboration & Fichiers
- `GET /api/tasks/{taskId}/comments` / `POST /api/tasks/{taskId}/comments` : Gestion des commentaires (supporte les mentions `@email@domain.com` qui déclenchent des notifications)
- `POST /api/tasks/{taskId}/attachments` : Upload d'un fichier joint (limite 100 Mo)
- `GET /api/attachments/{id}/download` : Récupère une URL temporaire signée et sécurisée
- `GET /api/notifications` : Liste paginée des notifications personnelles de l'utilisateur

### 📊 5. Analytics & Rapports
- `GET /api/projects/{id}/analytics/summary` : Résumé d'avancement (complétées, en retard, etc.)
- `GET /api/projects/{id}/analytics/burndown` : Données du Burndown Chart par dates
- `GET /api/projects/{id}/analytics/velocity` : Vélocité de l'équipe par sprint
- `POST /api/projects/{id}/export` : Lancer un export asynchrone (retourne un `jobId`)
- `GET /api/exports/{jobId}` : Polling du statut de l'export (`PENDING`, `DONE`, `FAILED`)

---

## ⚡ Temps Réel via WebSockets (STOMP)

Le backend diffuse des événements en temps réel via des connexions WebSocket.
- **Endpoint WebSocket** : `/ws` (Handshake HTTP requis, passez `Authorization: Bearer <TOKEN>` dans les headers STOMP)

### Topics de souscription
- `/topic/project/{projectId}` : Événements liés au projet (`task.created`, `task.updated`, `task.moved`, `member.joined`, etc.)
- `/topic/task/{taskId}/comments` : Événements sur les commentaires d'une tâche (`comment.created`, `comment.updated`)
- `/user/queue/notifications` : Notifications instantanées privées destinées à l'utilisateur connecté

---

## 📖 Swagger / OpenAPI

La documentation interactive et exhaustive des endpoints est accessible à l'adresse suivante lorsque l'application est démarrée :

👉 **[http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)**

---

## 🐳 Docker

### Build de l'image
```bash
docker build -t spm-backend .
```

### Exécution du conteneur
```bash
docker run -p 8082:8082 --env-file .env spm-backend
```

---

## 🧪 Script de Test de Démonstration

Un script Python interactif est fourni pour valider l'ensemble du workflow REST (inscription → OTP → login → projets → tâches → polling export).

```bash
# Installer les dépendances Python
pip install requests

# Lancer le script (backend démarré sur :8082)
python3 demo_test.py
```

---

## 👥 Contributeurs

| Nom | Email |
|---|---|
| Azangue Delmat | azangueleonel9@gmail.com |


---

## 🔗 Liens

- **Frontend** : [SPM-Project-Frontend](https://github.com/club-genie-informatique-enspy/SPM-Project-Frontend)
- **Organisation** : [Club GI ENSPY](https://github.com/club-genie-informatique-enspy)
- **Swagger UI** : [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)

---

*Club Génie Informatique — École Nationale Supérieure Polytechnique de Yaoundé — © 2026*
