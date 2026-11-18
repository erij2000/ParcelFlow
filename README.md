# ParcelFlow – Système de Suivi de Livraison de Colis
### Mini-projet SOA 2026 | Java 17 · Spring Boot 3.3.x · Docker

---

## 🏗️ Architecture

```
                        ┌──────────────────────────────────────┐
                        │           API GATEWAY :8080           │
                        │      (Spring Cloud Gateway – Wiem)    │
                        └────┬──────────────┬──────────────┬───┘
                             │              │              │
                    /api/orders/**   /api/carrier/**  /api/tracking/**
                             │              │              │
              ┌──────────────▼──┐  ┌────────▼────────┐  ┌─▼──────────────────┐
              │  ORDER SERVICE   │  │ CARRIER SERVICE  │  │  TRACKING SERVICE  │
              │   REST + Swagger │  │   SOAP / WSDL    │  │  gRPC + Protobuf   │
              │   :8081 (Yessine)│  │   :8082 (Malek)  │  │  :8083/:9090 (Erij)│
              └──────────────┬──┘  └────────┬────────┘  └────────────────────┘
                             │              │                      │
              ┌──────────────▼──┐  ┌────────▼────────┐  ┌────────▼────────┐
              │   order-db      │  │   carrier-db     │  │   tracking-db   │
              │  PostgreSQL:5433│  │ PostgreSQL:5434  │  │ PostgreSQL:5435 │
              └─────────────────┘  └──────────────────┘  └─────────────────┘
```

## 👥 Équipe

| Membre | Service | Technologie |
|--------|---------|-------------|
| **Wiem** | API Gateway + Infrastructure | Spring Cloud Gateway, Docker Compose |
| **Yessine** | Order Service | REST, OpenAPI/Swagger, Spring Data JPA |
| **Malek** | Carrier Service | SOAP, WSDL, XSD Contract-First |
| **Erij** | Tracking Service | gRPC, Protobuf, Streaming |

## 🚀 Démarrage rapide

```bash
# 1. Cloner le projet
git clone https://github.com/parcelflow/parcelflow-soa.git
cd parcelflow-soa

# 2. Copier le fichier d'environnement (NE PAS committer .env !)
cp .env.example .env

# 3. Lancer tout le stack en une commande
docker compose up --build

# 4. Accéder aux services
# API Gateway     : http://localhost:8080
# Swagger UI      : http://localhost:8081/swagger-ui.html
# WSDL SOAP       : http://localhost:8082/ws/carrier.wsdl
# gRPC (grpcurl)  : grpcurl -plaintext localhost:9090 list
```

## 📋 Sprints

| Sprint | Objectif | Statut |
|--------|----------|--------|
| **Sprint 1** | Architecture, REST, OpenAPI, SOAP base | ✅ En cours |
| **Sprint 2** | gRPC streaming, orchestration, Kafka | ⏳ Planifié |
| **Sprint 3** | Résilience, JWT, CI/CD, Testcontainers | ⏳ Planifié |

## 📡 Endpoints API

### Order Service (REST)
| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/orders` | Créer une commande |
| GET | `/api/orders` | Lister toutes les commandes |
| GET | `/api/orders/{id}` | Détail d'une commande |
| GET | `/api/orders/tracking/{ref}` | Suivi par référence publique |
| PATCH | `/api/orders/{id}/status` | Mise à jour du statut |
| DELETE | `/api/orders/{id}` | Supprimer une commande |

### Carrier Service (SOAP)
Endpoint : `http://localhost:8082/ws`

| Opération | Description |
|-----------|-------------|
| `GetPrice` | Calcul du tarif de transport |
| `BookCarrier` | Réservation d'un transporteur |

### Tracking Service (gRPC)
Port : `9090`

| Méthode | Type | Description |
|---------|------|-------------|
| `GetLastPosition` | Unaire | Dernière position GPS |
| `GetHistory` | Unaire | Historique complet |
| `RecordPosition` | Unaire | Enregistrer une position |
| `StreamPositions` | Server Streaming | Flux GPS en temps réel |

## 🌿 GitFlow

```bash
# Chaque membre travaille sur sa branche feature
git checkout -b feature/wiem/gateway-routes
git checkout -b feature/yessine/order-crud
git checkout -b feature/malek/carrier-soap
git checkout -b feature/erij/tracking-grpc

# Merge vers develop après review
git checkout develop
git merge --no-ff feature/yessine/order-crud
```

## 🔮 Fonctionnalités avancées (Sprint 2 & 3)
- 🔐 Sécurité JWT sur la Gateway
- 📨 Kafka pour les événements asynchrones
- 📊 OpenTelemetry + Micrometer (tracing distribué)
- 🧪 Testcontainers pour les tests d'intégration
- 🚀 GitHub Actions CI/CD pipeline
