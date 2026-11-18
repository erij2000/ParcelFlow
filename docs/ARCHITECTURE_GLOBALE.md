# 🏛️ Architecture Globale & Justification Technique – ParcelFlow
**Système Cloud-Native de Suivi de Livraison de Colis**  
**Équipe :** Erij Kacem, Wiem Ghars, Malek Beltaief, Yessine Elfaires  
**Cadre :** Mini-projet SOA & Services Web 2026 (Dr. Ing. Ghada Feki)

---

## 1. Description du Domaine Métier

ParcelFlow est une plateforme événementielle de gestion logistique et de suivi en temps réel de livraison de colis. 
Elle orchestre le cycle de vie complet d'un colis depuis sa création par l'expéditeur, l'estimation tarifaire et la réservation auprès d'un transporteur partenaire, jusqu'au suivi géolocalisé continu par les livreurs et les clients finaux.

### Les 3 Entités Métier Clés (Exigence minimale du sujet)
1. **Order (Commande / Colis)** : Propriétaire du cycle de vie de la demande d'expédition (expéditeur, destinataire, poids, statut, référence unique).
2. **Carrier Quote & Booking (Offre & Réservation Transporteur)** : Détermination tarifaire, délais contractuels et assignation du transporteur.
3. **Tracking Position & Timeline (Géolocalisation & Événements)** : Données temporelles et spatiales (coordonnées GPS, horodatage, centre de tri, livreur).

---

## 2. Découpage en Microservices & Justification des Protocoles

Le découpage respecte le principe du **Single Responsibility Principle (SRP)** et applique une architecture **polyglotte en termes de protocoles**. 
Chaque choix technologique répond à une contrainte technique et d'interopérabilité réelle, conformément à la grille d'évaluation :

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                   CLIENTS EXTERNES                                     │
│                         (Applications Web, Mobiles, Partenaires)                       │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ HTTP / JSON
                               ┌────────────▼────────────┐
                               │       API GATEWAY       │ (Port 8080)
                               │  Spring Cloud Gateway   │ Reverse Proxy Réactif (Netty)
                               └────────────┬────────────┘
                                            │
               ┌────────────────────────────┼────────────────────────────┐
               │ /api/orders/**             │ /api/carrier/**            │ /api/tracking/**
               │                            │                            │
    ┌──────────▼──────────┐      ┌──────────▼──────────┐      ┌──────────▼──────────┐
    │    ORDER SERVICE    │      │   CARRIER SERVICE   │      │   TRACKING SERVICE  │
    │      (Port 8081)    │      │      (Port 8082)    │      │ (Port 8083 & 9090)  │
    │                     │      │                     │      │                     │
    │ Protocole : REST    │      │ Protocole : SOAP    │      │ Protocole : gRPC    │
    │ Format : JSON       │      │ Format : XML (XSD)  │      │ Format : Protobuf   │
    │ Contrat : OpenAPI 3 │      │ Contrat : WSDL 1.1  │      │ Contrat : .proto    │
    └──────────┬──────────┘      └──────────┬──────────┘      └──────────┬──────────┘
               │                            │                            │
    ┌──────────▼──────────┐      ┌──────────▼──────────┐      ┌──────────▼──────────┐
    │      order-db       │      │      carrier-db     │      │     tracking-db     │
    │   PostgreSQL 16     │      │   PostgreSQL 16     │      │   PostgreSQL 16     │
    │     (Port 5433)     │      │     (Port 5434)     │      │     (Port 5435)     │
    └─────────────────────┘      └─────────────────────┘      └─────────────────────┘
```

### Table Comparative des Choix d'Architecture

| Microservice | Protocole | Style d'échange | Justification d'ingénierie face au jury |
|:---|:---:|:---:|:---|
| **Order Service** | **REST (OpenAPI 3)** | Synchrone requête-réponse (JSON) | **Exposition publique standardisée**. REST sur HTTP/JSON est universel, sans état (stateless), idéalement consommable par les applications web (React/Angular) et mobiles, et auto-documenté via la spécification OpenAPI 3. |
| **Carrier Service** | **SOAP 1.1 / WSDL** | Document / Literal (XML) avec XSD | **Interopérabilité avec les systèmes hérités et partenaires B2B**. Les grandes entreprises de fret (ex: DHL, FedEx, transporteurs historiques) exposent des interfaces formelles SOAP avec contrats XSD immuables, garantissant typage fort, validation stricte et standard de fautes (`SOAP Fault`). Approche **Contract-First** obligatoire. |
| **Tracking Service** | **gRPC / HTTP/2** | Binaire Protobuf + Server Streaming | **Haute performance et flux en temps réel**. Le tracking GPS implique l'émission périodique de coordonnées par des boîtiers IoT embarqués ou des smartphones de livreurs. gRPC sur HTTP/2 offre un multiplexage sur une seule connexion TCP, une empreinte binaire minimale (Protobuf), et un streaming serveur natif sans la lourdeur du polling HTTP. |
| **API Gateway** | **Spring Cloud Gateway** | Réactif (Spring WebFlux / Netty) | **Point d'entrée unique et découplage**. Centralise le routage intelligent, la réécriture d'URL (`StripPrefix`), la politique CORS globale, la gestion future des tokens JWT et l'isolation réseau des microservices internes. |

---

## 3. Principes Cloud-Native Appliqués

1. **Database-per-Service (Isolation des données)** :
   - Aucun partage de table ou de base de données.
   - `order-db` (Port hôte 5433), `carrier-db` (Port hôte 5434), `tracking-db` (Port hôte 5435).
   - Indépendance totale des schémas, déploiement et montées de version sans impact collatéral.

2. **Configuration Externalisée (12-Factor App)** :
   - Aucun identifiant, URL ou secret n'est écrit en dur dans le code Java (`@Value("${...}")` et variables d'environnement Spring).
   - Fichier centralisé `.env` chargé dynamiquement par Docker Compose et les profils Spring (`application.yml`).

3. **Services Sans État (Statelessness)** :
   - Les instances des services ne stockent aucune session en mémoire. Toutes les données de travail résident dans PostgreSQL.
   - Permet le passage à l'échelle horizontal immédiat (`docker compose up --scale order-service=3`).

4. **Observabilité & Health Checks** :
   - Endpoints Spring Boot Actuator (`/actuator/health`, `/actuator/info`) sur tous les services.
   - Docker Compose intègre des directives `healthcheck` (`pg_isready`) pour orchestrer l'ordre de démarrage sans condition de course (race condition).

---

## 4. Foire Aux Questions (FAQ) & Réponses Béton pour la Soutenance

### ❓ Question du Prof : *"Pourquoi avez-vous choisi une approche Contract-First pour SOAP plutôt que Code-First ?"*
> **Réponse de l'équipe (Malek / Tous) :**  
> *"Dans une véritable architecture orientée services (SOA), le contrat est la loi qui lie le fournisseur et le consommateur. Avec le Code-First (générer le WSDL depuis le code Java), le contrat change dès qu'un développeur modifie une classe Java, ce qui casse les clients existants.  
> Avec l'approche **Contract-First**, nous avons écrit `carrier.xsd` et `carrier.wsdl` à la main. Le contrat est stable, versionné et indépendant du langage d'implémentation. Le plugin Maven `jaxb2-maven-plugin` génère le code Java à partir du contrat, garantissant une conformité mathématique avec le schéma XML."*

### ❓ Question du Prof : *"Pourquoi utiliser gRPC pour le tracking alors que REST aurait pu suffire ?"*
> **Réponse de l'équipe (Erij / Tous) :**  
> *"Dans un système de suivi de colis en direct, le client web ou mobile a besoin de recevoir les coordonnées GPS au fur et à mesure que le livreur se déplace. En REST, la seule solution classique est le **polling** (faire un GET toutes les 2 secondes), ce qui sature le réseau avec des en-têtes HTTP redondants.  
> Avec **gRPC Server Streaming**, le client ouvre une seule requête TCP (sur HTTP/2), et le serveur pousse continuellement les nouvelles positions sous forme binaire Protobuf. Protobuf est 5 à 10 fois plus léger que le JSON et élimine le coût de sérialisation texte. C'est l'outil parfait pour l'IoT et les flux continus."*

### ❓ Question du Prof : *"Comment gérez-vous la résilience et les pannes si le Carrier Service est éteint ?"*
> **Réponse de l'équipe (Wiem / Yessine) :**  
> *"Au Sprint 1, nous avons posé les fondations et les timeouts stricts. Aux Sprints 2 et 3, nous intégrons **Resilience4j** sur l'Orchestrateur (Order Service) avec un **Circuit Breaker** et une méthode de repli (Fallback) : si le service SOAP ne répond pas au bout de 2 secondes, le circuit s'ouvre, évitant l'épuisement des threads (cascading failure), et une tarification estimative par défaut est appliquée en attendant le rétablissement du service."*
