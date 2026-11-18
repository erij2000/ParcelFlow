# 🟩 Fiche Technique & Guide de Soutenance – Yessine Elfaires
**Microservice :** `order-service` (Gestion des Commandes & Orchestrateur)  
**Spécialité :** Architecture RESTful, OpenAPI 3 / Swagger, DTO Pattern, RFC 7807, Spring Data JPA  
**Dépôt GitHub :** [github.com/erij2000/ParcelFlow](https://github.com/erij2000/ParcelFlow)  
**Branche Git :** `feature/yessine/order-rest-swagger`

---

## 🎤 1. Ton Pitch Oral pour Demain (À dire mot pour mot au professeur)

> *"Bonjour Madame / Monsieur.  
> Je suis responsable de l'**Order Service**, qui constitue à la fois le point focal de gestion des commandes et le **futur orchestrateur métier** de notre architecture SOA.
>
> Pour l'interaction avec les clients (portails web, applications mobiles des expéditeurs), j'ai conçu une **API RESTful rigoureuse**, respectant les principes d'indépendance de statut (stateless), le bon usage des verbes HTTP (POST pour création, GET pour lecture, PATCH pour modification partielle d'état, DELETE pour annulation), et des codes de retour standardisés (201 Created avec entête d'URI, 200 OK, 204 No Content, 400 Bad Request, 404 Not Found).
>
> J'ai appliqué une séparation des responsabilités stricte en 3 couches (**Controller**, **Service Métier**, **Repository JPA**), en isolant totalement les entités de base de données du monde extérieur grâce au **pattern DTO** (`CreateOrderRequest`, `OrderResponse`).
>
> Pour la documentation, j'ai intégré la spécification **OpenAPI 3 / Swagger UI** via `springdoc-openapi-starter-webmvc-ui`. Toute l'API est auto-documentée et testable en direct sur `/swagger-ui.html`.
>
> Enfin, pour les erreurs, j'ai implémenté le standard moderne **RFC 7807 ProblemDetail**, recommandé par Spring Boot 3, qui garantit des réponses d'erreur JSON normalisées et prévisibles.
>
> Au Sprint 2, ce service deviendra le chef d'orchestre : lors de la confirmation d'une commande, il contactera le Carrier Service via un client SOAP (`WebServiceTemplate`) pour valider le tarif et réserver le transport, puis enregistrera le colis auprès du Tracking Service d'Erij via gRPC."*

---

## 📂 2. Explication Détaillée Fichier par Fichier

### 📄 Fichier 1 : `src/main/java/com/parcelflow/order/controller/OrderController.java`
*La porte d'entrée HTTP du service.*

- **Annotations :**
  - `@RestController` : Combine `@Controller` et `@ResponseBody` pour sérialiser automatiquement les retours Java en JSON.
  - `@RequestMapping("/orders")` : Définit le préfixe de base de l'URI.
  - `@Tag(name = "Orders", description = "...")` : Catégorise les endpoints dans le Swagger UI.
- **Les Endpoints implémentés :**
  1. `POST /orders` : Crée une commande. Reçoit `@Valid @RequestBody CreateOrderRequest`. Retourne HTTP `201 Created` avec l'objet créé.
  2. `GET /orders` : Récupère la liste de toutes les commandes (`200 OK`).
  3. `GET /orders/{id}` : Récupère une commande par son identifiant unique UUID.
  4. `GET /orders/tracking/{ref}` : Endpoint public de recherche par référence de suivi (ex: `PF-2026-00001`).
  5. `PATCH /orders/{id}/status` : Met à jour uniquement le statut (`PENDING`, `CONFIRMED`, `IN_TRANSIT`, etc.). On utilise `PATCH` et non `PUT`, car seule une fraction de la ressource est modifiée.
  6. `DELETE /orders/{id}` : Supprime une commande (`204 No Content`).
- **Annotations OpenAPI :**
  - `@Operation(summary = "...", description = "...")` : Rédige une description humaine pour chaque méthode dans la documentation.
  - `@ApiResponse(...)` : Décrit les codes HTTP possibles et les schémas retournés.

---

### 📄 Fichier 2 : `src/main/java/com/parcelflow/order/dto/CreateOrderRequest.java` et `OrderResponse.java`
*Les objets de transfert de données (Data Transfer Objects).*

- **Pourquoi ne pas exposer directement l'entité JPA `Order` ?**  
  1. **Sécurité :** Empêcher le client d'injecter des champs sensibles (ex: injecter un faux statut `DELIVERED` ou écraser un `id`).
  2. **Découplage :** Si la structure de la table SQL change, l'API publique ne casse pas.
  3. **Performance :** Évite les boucles infinies de sérialisation JSON avec Jackson lors de relations bidirectionnelles JPA.
- **Validation Bean (`jakarta.validation.*`) :**
  - `@NotBlank(message = "Le nom de l'expéditeur est obligatoire")` : Interdit les chaînes nulles ou composées uniquement d'espaces.
  - `@Positive(message = "Le poids doit être positif")` : Empêche les valeurs négatives ou nulles.
  - `@DecimalMax("1000.000")` : Règle métier interdisant les colis dépassant 1 tonne.

---

### 📄 Fichier 3 : `src/main/java/com/parcelflow/order/service/OrderService.java`
*La logique métier pure.*

- **Annotation `@Service` & `@Transactional` :**  
  Délimite les transactions de base de données. En cas d'exception non contrôlée, la transaction fait un rollback automatique.
- **Génération de la Référence Unique :**  
  Méthode `generateTrackingReference()` : Génère un format lisible `PF-2026-00001`.
- **Méthode `createOrder(...)` :**  
  Transforme le DTO entrant en entité `Order`, assigne le statut initial `OrderStatus.PENDING`, sauvegarde via le repository, et renvoie le DTO `OrderResponse`.

---

### 📄 Fichier 4 : `src/main/java/com/parcelflow/order/exception/GlobalExceptionHandler.java`
*Gestionnaire d'erreurs centralisé selon la RFC 7807.*

- **Annotation `@RestControllerAdvice` :**  
  Intercepteur global d'exceptions pour tous les contrôleurs REST.
- **Classe `ProblemDetail` (Standard Spring Boot 3 / RFC 7807) :**  
  Au lieu de renvoyer une stack trace illisible de 50 lignes ou un JSON custom bricolé, on renvoie une structure internationale standardisée :
  ```json
  {
    "type": "about:blank",
    "title": "Commande introuvable",
    "status": 404,
    "detail": "Commande non trouvée : f3b4-...",
    "timestamp": "2026-11-18T14:00:00Z"
  }
  ```
- **Méthode `handleValidationErrors(MethodArgumentNotValidException ex)` :**  
  Attrape les violations de `@Valid` et extrait le nom exact du champ fautif pour l'afficher au client avec un code `400 Bad Request`.

---

### 📄 Fichier 5 : `src/main/java/com/parcelflow/order/config/OpenApiConfig.java`
*Configuration globale de la spécification Swagger.*

- Configure les métadonnées de l'API (Titre, version 1.0.0-SNAPSHOT, licence, contact de l'équipe ParcelFlow).
- Accessible immédiatement dans le navigateur : `http://localhost:8081/swagger-ui.html`.

---

## 🛡️ 3. Questions Pièges du Professeur & Réponses de Yessine

1. **"Yessine, pourquoi utiliser `PATCH` plutôt que `PUT` pour changer le statut d'une commande ?"**  
   > *Réponse :* "Selon les spécifications RFC de HTTP, le verbe `PUT` est idempotent et remplace **l'intégralité** de la ressource cible par le payload fourni (tous les champs non mentionnés sont remis à null). Le verbe `PATCH` applique une **modification partielle** sur un sous-ensemble de champs, ce qui correspond exactement au cas d'usage où l'on change uniquement la valeur de l'attribut `status` sans renvoyer l'expéditeur, le destinataire ou le poids."

2. **"Comment fonctionne l'orchestration dans une architecture SOA ?"**  
   > *Réponse :* "L'Orchestration repose sur un coordinateur central (ici `OrderService`) qui détient la logique du processus métier et invoque séquentiellement ou en parallèle les services participants (Carrier en SOAP, Tracking en gRPC). Cela s'oppose à la chorégraphie purement réactive, et offre une meilleure traçabilité du workflow de commande."
