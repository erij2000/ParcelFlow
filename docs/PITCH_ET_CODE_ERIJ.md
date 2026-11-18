# 🟥 Fiche Technique & Guide de Soutenance – Erij Kacem
**Microservice :** `tracking-service` (Suivi GPS en Temps Réel)  
**Spécialité :** gRPC, Protobuf IDL, Server Streaming, PostgreSQL  
**Dépôt GitHub :** [github.com/erij2000/ParcelFlow](https://github.com/erij2000/ParcelFlow)  
**Branche Git :** `feature/erij/tracking-grpc-proto`

---

## 🎤 1. Ton Pitch Oral pour Demain (À dire mot pour mot au professeur)

> *"Bonjour Madame / Monsieur.  
> Dans le projet ParcelFlow, je suis responsable du **Tracking Service**, le microservice dédié à la capture et à la diffusion en temps réel des coordonnées géographiques des colis.
>
> Pour ce microservice, j'ai choisi le protocole **gRPC sur HTTP/2 avec sérialisation binaire Protocol Buffers (Protobuf)**.  
> Ce choix technique est directement justifié par la nature des données de télémétrie : un livreur ou un capteur GPS embarqué émet des coordonnées fréquemment. Utiliser du polling HTTP/REST traditionnel aurait généré une surcharge réseau excessive (en-têtes HTTP répétés, sérialisation texte JSON lourde, handshakes répétés).
>
> Avec gRPC, j'ai défini un contrat strict dans le fichier **`tracking.proto`**. Ce contrat spécifie non seulement des appels **unaires** (`GetLastPosition`, `RecordPosition`, `GetHistory`), mais surtout du **Server Streaming** (`StreamPositions`). Grâce au streaming sur une seule connexion TCP multiplexée, le client reçoit un flux continu de positions dès que le colis se déplace, sans jamais avoir à réinterroger le serveur.
>
> Côté persistance, j'ai implémenté le pattern **Database-per-Service** avec une base PostgreSQL dédiée (`trackingdb` sur le port 5435), isolée des autres services. Le serveur gRPC tourne sur le port `9090` via `grpc-spring-boot-starter`, en parallèle du port HTTP `8083` pour les health checks Actuator.
>
> Au Sprint 2, je finalise le streaming dynamique avec simulation de trajet GPS, que Yessine consommera directement depuis l'Orchestrateur via un stub gRPC client."*

---

## 📂 2. Explication Détaillée Fichier par Fichier

### 📄 Fichier 1 : `src/main/proto/tracking.proto`
*Le cœur de ton service – le contrat d'interface (Interface Definition Language).*

- **Rôle :** C'est l'équivalent du WSDL pour SOAP ou de la spec OpenAPI pour REST. Il est indépendant de tout langage de programmation.
- **Pourquoi `syntax = "proto3";` ?**  
  La version 3 de Protobuf est la plus moderne, simplifie la gestion des champs par défaut et supprime le mot-clé verbeux `required`.
- **Pourquoi `option java_multiple_files = true;` ?**  
  Demande au compilateur `protoc` de générer une classe Java distincte par message et par service, évitant d'avoir un fichier Java géant de 3000 lignes difficile à maintenir.
- **Les Messages clés :**
  - `TrackingPosition` : encapsule `parcel_id`, `latitude`, `longitude`, `city`, `status`, `timestamp`.
  - Chaque champ possède un tag numérique (`= 1`, `= 2`). **Pourquoi ?** En Protobuf, les noms de variables ne sont **jamais** envoyés sur le réseau ! Seuls les numéros de tags et les octets binaires transitent, d'où la compacité extrême (gain de bande passante de 70% par rapport à JSON).
- **Les Méthodes du Service `TrackingService` :**
  - `rpc GetLastPosition(TrackParcelRequest) returns (TrackingPosition);` → **Appel Unaire** (1 requête → 1 réponse).
  - `rpc StreamPositions(TrackParcelRequest) returns (stream TrackingPosition);` → **Server Streaming** (Le mot-clé `stream` indique que le serveur envoie une série d'objets `TrackingPosition` jusqu'à fermeture du flux par `onCompleted()`).

---

### 📄 Fichier 2 : `pom.xml` de `tracking-service`
*Configuration Maven, dépendances et génération de code.*

- **Dépendance `net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE` :**  
  Active le serveur gRPC embarqué dans Spring Boot et scanne automatiquement les beans annotés `@GrpcService`.
- **Dépendances `io.grpc:grpc-netty-shaded`, `grpc-protobuf`, `grpc-stub` :**  
  Fournissent le moteur réseau haute performance Netty optimisé pour HTTP/2, ainsi que les abstractions de stubs.
- **Plugin `kr.motd.maven:os-maven-plugin` :**  
  Détecte automatiquement le système d'exploitation de la machine (Windows x86_64, Linux, Mac M1/ARM) pour télécharger le bon binaire de compilation `protoc`.
- **Plugin `org.xolstice.maven.plugins:protobuf-maven-plugin` :**  
  Prend le fichier `tracking.proto` et génère les classes Java lors de l'étape `mvn compile` dans le dossier `target/generated-sources/protobuf`.

---

### 📄 Fichier 3 : `src/main/java/com/parcelflow/tracking/grpc/TrackingGrpcService.java`
*L'implémentation du service gRPC.*

- **Annotation `@GrpcService` :**  
  Indique à Spring Boot que cette classe est un contrôleur gRPC. Spring l'enregistre sur le port configuré (9090).
- **Interface `StreamObserver<T>` :**  
  C'est le pattern réactif utilisé par gRPC. Il expose 3 méthodes fondamentales :
  1. `observer.onNext(reponse)` : envoie une donnée au client. En streaming, on peut appeler `onNext()` plusieurs fois d'affilée !
  2. `observer.onError(statusException)` : envoie un code d'erreur standard gRPC (ex: `Status.NOT_FOUND`, `Status.INVALID_ARGUMENT`).
  3. `observer.onCompleted()` : informe le client que le flux est terminé et ferme la communication proprement.
- **Méthode `getLastPosition(...)` :**  
  Interroge `TrackingPositionRepository` pour trouver la dernière position du colis. Si trouvée, fait `onNext()` puis `onCompleted()`. Si introuvable, déclenche `observer.onError(Status.NOT_FOUND)`.
- **Méthode `streamPositions(...)` :**  
  Récupère l'historique complet et boucle dessus pour faire une série de `onNext(position)`, simulant un envoi continu en direct.

---

### 📄 Fichier 4 : `src/main/java/com/parcelflow/tracking/model/TrackingPosition.java`
*Entité JPA pour la persistance.*

- **Annotations :** `@Entity`, `@Table(name = "tracking_positions")`.
- **Indexation :** `@Index(name = "idx_parcel_id", columnList = "parcel_id")`.  
  **Pourquoi ?** Dans un système de tracking, 99% des requêtes se font par `parcel_id`. L'index B-Tree en base garantit un temps de réponse en $O(\log N)$ même avec des millions de positions enregistrées.
- **Champs GPS :** `latitude` (`Double`), `longitude` (`Double`).

---

### 📄 Fichier 5 : `src/main/resources/application.yml`
*Configuration du microservice.*

- `server.port: 8083` : Port pour le serveur web Tomcat (endpoints Actuator de supervision `/actuator/health`).
- `grpc.server.port: 9090` : Port TCP dédié au serveur gRPC HTTP/2.
- `spring.datasource.url` : Connexion vers PostgreSQL `tracking-db:5432/trackingdb` injectée via variables d'environnement.

---

## 🛡️ 3. Questions Pièges du Professeur & Réponses d'Erij

1. **"Erij, comment teste-t-on un service gRPC si Postman ou un navigateur classique ne comprend pas le binaire ?"**  
   > *Réponse :* "On utilise l'outil CLI **`grpcurl`** (l'équivalent de `curl` pour gRPC) ou les versions récentes de Postman qui supportent gRPC en lui fournissant notre fichier `tracking.proto`. On peut exécuter : `grpcurl -plaintext -proto tracking.proto -d '{\"parcel_id\": \"PF-2026-00001\"}' localhost:9090 tracking.TrackingService/GetLastPosition`."

2. **"Pourquoi avoir deux ports différents (8083 et 9090) dans votre service ?"**  
   > *Réponse :* "Le port 8083 gère les requêtes HTTP/1.1 classiques pour les sondes de santé Spring Boot Actuator (`/health`) utilisées par Docker Compose et la passerelle. Le port 9090 est optimisé pour gRPC en HTTP/2 binaire avec multiplexage de trames, garantissant l'isolation des flux de transport."
