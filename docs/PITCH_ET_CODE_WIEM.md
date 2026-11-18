# 🟦 Fiche Technique & Guide de Soutenance – Wiem Ghars
**Microservice :** `api-gateway` & Infrastructure Globale  
**Spécialité :** Spring Cloud Gateway (WebFlux / Netty), Docker & Docker Compose, Stratégie Réseau, Healthchecks  
**Dépôt GitHub :** [github.com/erij2000/ParcelFlow](https://github.com/erij2000/ParcelFlow)  
**Branche Git :** `feature/wiem/gateway-infrastructure`

---

## 🎤 1. Ton Pitch Oral pour Demain (À dire mot pour mot au professeur)

> *"Bonjour Madame / Monsieur.  
> Je suis responsable de l'**Infrastructure DevOps et de l'API Gateway**, qui constitue la colonne vertébrale et le point d'accès unifié de l'écosystème ParcelFlow.
>
> Pour l'API Gateway, j'ai sélectionné **Spring Cloud Gateway**, qui repose sur une architecture **réactive non-bloquante** avec **Project Reactor et le serveur Netty**, et non sur le modèle traditionnel bloquant Tomcat/Servlet (Spring MVC).  
> Ce choix garantit un débit très élevé (high throughput) et une faible empreinte mémoire, même face à des milliers de connexions simultanées.
>
> J'ai configuré les routes de manière programmatique dans **`GatewayRoutesConfig.java`**. Le Gateway intercepte les requêtes sur le port `8080`, applique le filtre **`StripPrefix(1)`** pour nettoyer le segment `/api`, injecte un en-tête de traçabilité `X-Gateway-Source`, et achemine le trafic vers le bon service (`order-service`, `carrier-service`, `tracking-service`).
>
> J'ai également centralisé la politique **CORS (Cross-Origin Resource Sharing)** dans la Gateway via `CorsConfig.java`. Ainsi, aucun microservice interne n'a besoin de se soucier des autorisations de domaine ou des en-têtes pré-flight `OPTIONS`.
>
> Sur le plan de la conteneurisation, j'ai conçu un fichier **`docker-compose.yml`** orchestrant les 4 microservices et les 3 bases de données PostgreSQL isolées, respectant le pattern **Database-per-Service**.  
> Pour éliminer les conditions de course au démarrage, j'ai mis en place des **Healthchecks** sur chaque base PostgreSQL (`pg_isready`) et des dépendances conditionnelles (`condition: service_healthy`) sur les conteneurs applicatifs.
>
> L'ensemble du système démarre de façon 100% autonome et reproductible en une unique commande : `docker compose up --build`."*

---

## 📂 2. Explication Détaillée Fichier par Fichier

### 📄 Fichier 1 : `src/main/java/com/parcelflow/gateway/config/GatewayRoutesConfig.java`
*Le routeur intelligent du système.*

- **Bean `RouteLocator` & `RouteLocatorBuilder` :**  
  Permet de définir les routes directement en code Java typé plutôt qu'en simple fichier YAML. Cela permet des tests unitaires sur les routes et l'injection dynamique de prédicats et filtres.
- **La Route Order Service :**
  ```java
  .route("order-service", r -> r
      .path("/api/orders/**")
      .filters(f -> f
          .stripPrefix(1) // /api/orders/123 -> /orders/123
          .addRequestHeader("X-Gateway-Source", "parcelflow-gateway"))
      .uri(orderServiceUrl))
  ```
  - **`path("/api/orders/**")`** : Capture toutes les requêtes destinées aux commandes.
  - **`stripPrefix(1)`** : Supprime le premier préfixe `/api` car le microservice `order-service` écoute sur `/orders`.
  - **`addRequestHeader`** : Permet aux microservices internes de valider que la requête provient bien de la Gateway officielle et non d'un accès pirate direct.

---

### 📄 Fichier 2 : `src/main/java/com/parcelflow/gateway/config/CorsConfig.java`
*Sécurité et compatibilité avec les clients web frontaux.*

- **Classe `CorsWebFilter` (Package `org.springframework.web.cors.reactive`) :**  
  **Attention piège :** On n'utilise **pas** `CorsFilter` de Spring MVC (qui bloquerait Netty), mais la version réactive dédiée à WebFlux.
- Autorise les méthodes HTTP : `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS`.
- Centralisation : Évite la duplication de code et élimine le risque d'oublier de configurer le CORS sur un nouveau microservice.

---

### 📄 Fichier 3 : `docker-compose.yml` (Racine du projet)
*L'orchestration des conteneurs.*

- **Réseau dédié `parcelflow-net` (driver bridge) :**  
  Isole le réseau des conteneurs. Les microservices communiquent entre eux en utilisant leurs noms de service Docker comme noms d'hôtes DNS (ex: `http://order-service:8081`).
- **Volumes persistants (`order-db-data`, `carrier-db-data`, `tracking-db-data`) :**  
  Garantit que même si un conteneur PostgreSQL est arrêté ou recréé, les données insérées en base ne sont jamais perdues.
- **Orchestration avec Healthcheck :**
  ```yaml
  order-db:
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${ORDER_DB_USER} -d ${ORDER_DB_NAME}"]
      interval: 10s
      timeout: 5s
      retries: 5

  order-service:
    depends_on:
      order-db:
        condition: service_healthy
  ```
  Sans cette directive, `order-service` tenterait de se connecter à la base avant que PostgreSQL n'ait fini d'initialiser son socket, provoquant un crash immédiat `Connection refused` au lancement !

---

### 📄 Fichier 4 : `Dockerfile` (Présent dans chaque module)
*Compilation multi-stage et sécurité.*

- **Stage 1 (Builder) :**  
  Image lourde `maven:3.9.6-eclipse-temurin-17` qui compile le code et produit le fichier `.jar`.
- **Stage 2 (Runner) :**  
  Image minimale légère `eclipse-temurin:17-jre-alpine` (~180 Mo au lieu de 800 Mo).
- **Sécurité :**  
  Création d'un groupe et utilisateur système sans privilèges :  
  `RUN addgroup -S parcelflow && adduser -S parcelflow -G parcelflow`  
  `USER parcelflow`  
  Le conteneur ne tourne **jamais** en utilisateur root !

---

### 📄 Fichier 5 : `.env` (Variables d'environnement)
*Principe Cloud-Native (12-Factor App).*

- Centralise tous les ports (`GATEWAY_PORT=8080`, `ORDER_SERVICE_PORT=8081`, etc.) et les identifiants de bases de données.
- Aucun mot de passe n'est stocké en dur dans le code Java ou dans les fichiers de configuration versionnés.

---

## 🛡️ 3. Questions Pièges du Professeur & Réponses de Wiem

1. **"Wiem, pourquoi ne peut-on pas ajouter la dépendance `spring-boot-starter-web` dans l'API Gateway ?"**  
   > *Réponse :* "Spring Cloud Gateway est construit nativement sur **Spring WebFlux et le serveur asynchrone Netty**. Si l'on ajoute `spring-boot-starter-web`, Spring Boot tentera de démarrer le serveur Tomcat bloquant classique sur le même port, ce qui provoque une collision de classpath fatale et empêche le démarrage de l'application."

2. **"Comment la Gateway sait-elle où trouver les services dans Docker ?"**  
   > *Réponse :* "Grâce au serveur DNS interne embarqué dans Docker Compose. Lorsque les conteneurs partagent le réseau `parcelflow-net`, le nom du service (ex: `order-service`) est automatiquement résolu par Docker en l'adresse IP interne du conteneur correspondant."
