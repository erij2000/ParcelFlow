# 📋 Product Backlog & Planification des Sprints – ParcelFlow
**Module :** Architecture SOA et Services Web (2026)  
**Enseignante :** Dr. Ing. Ghada Feki  
**Équipe :** 
- **Erij Kacem** (Tracking Service – gRPC / Protobuf)
- **Wiem Ghars** (Infrastructure & API Gateway – Spring Cloud Gateway / Docker)
- **Malek Beltaief** (Carrier Service – SOAP / WSDL / XSD)
- **Yessine Elfaires** (Order Service – REST / OpenAPI / Orchestration)

---

## 1. Product Backlog Global (User Stories & Estimations)

Total : **53 Story Points**

| ID | User Story (En tant que... je veux... afin de...) | Priorité | Estimation (SP) | Responsable | Sprint Prévu |
|:---|:---|:---:|:---:|:---:|:---:|
| **US01** | En tant que client, je peux créer une demande de livraison avec mes adresses et le colis | Haute | 3 | Yessine | Sprint 1 |
| **US02** | En tant que client/système, je peux consulter le tarif d'expédition via un service de transporteur (SOAP) | Haute | 5 | Malek | Sprint 1 & 2 |
| **US03** | En tant que client, je peux confirmer une commande après avoir pris connaissance du tarif | Haute | 3 | Yessine | Sprint 1 & 2 |
| **US04** | En tant que client, je peux annuler ou supprimer une commande avant sa prise en charge | Haute | 3 | Yessine | Sprint 1 |
| **US05** | En tant que système, je peux réserver une expédition chez un transporteur partenaire via contrat SOAP | Haute | 5 | Malek | Sprint 2 |
| **US06** | En tant que livreur/IoT, je peux enregistrer en continu les positions GPS d'un colis via gRPC | Haute | 5 | Erij | Sprint 1 & 2 |
| **US07** | En tant que client, je peux suivre en temps réel la progression de mon colis via un flux continu (Streaming gRPC) | Haute | 8 | Erij | Sprint 2 |
| **US08** | En tant que système, je mets à jour automatiquement le cycle de vie de la commande (`PENDING` → `CONFIRMED` → `IN_TRANSIT` → `DELIVERED`) | Haute | 5 | Yessine / Erij | Sprint 2 |
| **US09** | En tant que développeur ou client d'API, je dispose d'une documentation interactive OpenAPI / Swagger accessible | Moyenne | 2 | Yessine | Sprint 1 |
| **US10** | En tant que système, je reçois et traite des erreurs métier normalisées via SOAP Faults documentées | Moyenne | 3 | Malek | Sprint 1 & 2 |
| **US11** | En tant qu'administrateur système, toutes les requêtes entrent par une API Gateway réactive unique | Moyenne | 3 | Wiem | Sprint 1 |
| **US12** | En tant que développeur, chaque microservice dispose de sa propre base de données isolée et de sa configuration externalisée | Moyenne | 3 | Wiem / Tous | Sprint 1 |
| **US13** | En tant que système cloud-native, je tolère les pannes réseaux grâce à des timeouts et un Circuit Breaker Resilience4j avec méthode de repli | Moyenne | 5 | Wiem / Yessine | Sprint 3 |

---

## 2. Découpage et Calendrier des Sprints

### 🟢 Sprint 0 : Initialisation, Gouvernance & Infrastructure (Rendu : 15 nov 2026)
*Objectif : Mise en place du socle technique, GitFlow et conventions d'équipe.*
- Initialisation du dépôt GitHub (`erij2000/ParcelFlow`).
- Mise en place de la gouvernance GitFlow (`main`, `develop`, branches `feature/*`).
- Répartition des rôles et Poker Planning des User Stories.
- Configuration initiale des environnements locaux et du README.

### 🔵 Sprint 1 : Fondations Architecture, Contrats & REST OpenAPI (Rendu : 23 nov 2026)
*Objectif : 4 microservices autonomes fonctionnels, 3 BDD isolées, Gateway réactive et premiers contrats.*
- **Wiem** : API Gateway (Spring Cloud Gateway), routage `/api/**`, CORS réactif, Docker Compose multi-conteneurs avec healthchecks.
- **Yessine** : Order Service (CRUD complet REST, OpenAPI/Swagger 3, RFC 7807 ProblemDetail, base PostgreSQL `orderdb`).
- **Malek** : Carrier Service (Approche Contract-First : écriture de `carrier.xsd` et `carrier.wsdl`, serveur Spring-WS, structure SOAP Faults).
- **Erij** : Tracking Service (Contrat Protobuf `tracking.proto` avec RPC unaire et streaming, serveur gRPC port 9090, base PostgreSQL `trackingdb`).

### 🟡 Sprint 2 : RPC, Orchestration Inter-services & Messagerie (Rendu : 30 nov 2026)
*Objectif : Communication inter-services réelle (REST vers SOAP et REST vers gRPC), scénario complet de commande.*
- **Malek** : Implémentation complète de `GetPrice` et `BookCarrier`, gestion avancée des `SOAP Faults` personnalisées.
- **Erij** : Streaming bidirectionnel et serveur gRPC complet (`StreamPositions`), simulation de déplacement GPS temps réel.
- **Yessine** : Orchestration dans `OrderService` (consommation du SOAP Carrier via `WebServiceTemplate` + appel client gRPC).
- **Bonus Wiem / Yessine** : Intégration Apache Kafka pour la publication d'événements asynchrones (`OrderCreatedEvent` consommé par Carrier).

### 🔴 Sprint 3 : Résilience Cloud-Native, Tests & Soutenance (Rendu : 31 déc 2026)
*Objectif : Tolérance aux pannes démontrée, Dockerisation optimisée multi-stage, métriques et soutenance finale.*
- **Yessine / Malek** : Resilience4j (Circuit Breaker, Timeouts stricts, Retry avec Exponential Backoff, Fallback method).
- **Wiem** : Sécurisation JWT sur l'API Gateway, Dockerfiles multi-stage légers, tests d'intégration avec Testcontainers.
- **Erij / Tous** : Traçage distribué (Micrometer + OpenTelemetry / Zipkin), tests de charge.
- **Équipe** : Rédaction du rapport PDF (8 à 12 pages) et répétition de la soutenance (15 min démo + 10 min Q&A).

---

## 3. Matrice de Traçabilité des Rôles

```
                        ┌───────────────────────────────────────────────┐
                        │        CLT / FRONTEND / POSTMAN / CURL        │
                        └───────────────────────┬───────────────────────┘
                                                │ :8080
                                   ┌────────────▼────────────┐
                                   │       API GATEWAY       │  Responsable : WIEM
                                   │  (Spring Cloud Gateway) │
                                   └────────────┬────────────┘
                                                │
             ┌──────────────────────────────────┼──────────────────────────────────┐
             │ :8081                            │ :8082                            │ :8083 & :9090
  ┌──────────▼──────────┐            ┌──────────▼──────────┐            ┌──────────▼──────────┐
  │    ORDER SERVICE    │            │   CARRIER SERVICE   │            │   TRACKING SERVICE  │
  │     (Style REST)    │            │     (Style SOAP)    │            │     (Style gRPC)    │
  │                     │            │                     │            │                     │
  │ Resp : YESSINE      │   SOAP     │ Resp : MALEK        │    gRPC    │ Resp : ERIJ         │
  │ • CRUD Commandes    ├───────────►│ • Tarifs GetPrice   │◄───────────┤ • Positions GPS     │
  │ • OpenAPI/Swagger   │            │ • Réservation WSDL  │            │ • Streaming temps   │
  │ • Orchestrateur     │            │ • SOAP Faults       │            │   réel (.proto)     │
  └──────────┬──────────┘            └──────────┬──────────┘            └──────────┬──────────┘
             │                                  │                                  │
  ┌──────────▼──────────┐            ┌──────────▼──────────┐            ┌──────────▼──────────┐
  │   order-db (PG)     │            │   carrier-db (PG)   │            │   tracking-db (PG)  │
  │   Port: 5433        │            │   Port: 5434        │            │   Port: 5435        │
  └─────────────────────┘            └─────────────────────┘            └─────────────────────┘
```
