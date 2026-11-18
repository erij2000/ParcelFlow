# 🚀 Guide Rapide de Lancement Docker – Équipe ParcelFlow
**Message à copier/coller directement sur WhatsApp / Discord à toute l'équipe (Wiem, Yessine, Malek) !**

---

Salut l'équipe ! Voici la procédure exacte et testée pour cloner le projet, lancer toute l'architecture en une seule commande avec Docker et tester vos services respectifs pour la présentation de demain.

---

## 📥 Étape 1 : Récupérer le code sur votre machine

```bash
# 1. Ouvrir votre terminal (PowerShell, Git Bash ou Terminal Mac/Linux)
# 2. Cloner le repository officiel :
git clone https://github.com/erij2000/ParcelFlow.git

# 3. Entrer dans le dossier :
cd ParcelFlow

# 4. Basculer sur votre branche de travail :
# -> Pour Erij    : git checkout feature/erij/tracking-grpc-proto
# -> Pour Yessine : git checkout feature/yessine/order-rest-swagger
# -> Pour Malek   : git checkout feature/malek/carrier-soap-wsdl
# -> Pour Wiem    : git checkout feature/wiem/gateway-infrastructure
```

---

## ⚙️ Étape 2 : Prérequis & Fichier `.env`

Assurez-vous que **Docker Desktop** est bien démarré et que son icône indique que le moteur tourne (vert).

Le fichier `.env` est déjà présent à la racine du projet avec les ports et mots de passe de test. Vérifiez simplement qu'il existe à la racine (`ls -a` ou `dir`).

---

## 🐳 Étape 3 : Lancer tout le système avec Docker

À la racine du dossier `ParcelFlow`, exécutez la commande magique :

```bash
docker compose up --build -d
```

*(Note : La première fois, le téléchargement des images Maven et la compilation peuvent prendre 3 à 5 minutes selon votre connexion).*

### Vérifier que tous les conteneurs sont bien démarrés :
```bash
docker compose ps
```

Vous devez voir **7 conteneurs** avec le statut `running` ou `healthy` :
1. `parcelflow-api-gateway` (Port 8080)
2. `parcelflow-order-service` (Port 8081)
3. `parcelflow-carrier-service` (Port 8082)
4. `parcelflow-tracking-service` (Ports 8083 et 9090)
5. `parcelflow-order-db` (Port 5433)
6. `parcelflow-carrier-db` (Port 5434)
7. `parcelflow-tracking-db` (Port 5435)

---

## 🌐 Étape 4 : Les URLs pour tester vos services respectifs

| Qui | Quoi tester | URL dans votre navigateur |
|:---|:---|:---|
| **Wiem** | **API Gateway & Health** | `http://localhost:8080/actuator/health` |
| **Yessine** | **Swagger UI / OpenAPI** | `http://localhost:8081/swagger-ui.html` |
| **Malek** | **WSDL SOAP en direct** | `http://localhost:8082/ws/carrier.wsdl` |
| **Erij** | **Tracking Service Health** | `http://localhost:8083/actuator/health` |
| **Erij** | **Port gRPC dédié** | `localhost:9090` (accessible via grpcurl) |

---

## 📋 Scénario de Démonstration en Direct (Ordre de passage)

1. **Wiem** lance la commande `docker compose ps` pour montrer au professeur que les 4 services et les 3 bases de données PostgreSQL indépendantes tournent de manière isolée.
2. **Wiem** ouvre `http://localhost:8080/actuator/health` pour prouver le fonctionnement de la Gateway.
3. **Yessine** ouvre `http://localhost:8081/swagger-ui.html`, clique sur `POST /orders`, saisit un JSON de test et clique sur **Execute** : une commande avec la référence `PF-2026-00001` est créée avec un code HTTP **201 Created**.
4. **Malek** ouvre `http://localhost:8082/ws/carrier.wsdl` dans le navigateur pour montrer le contrat WSDL généré et explique son schéma `carrier.xsd` Contract-First.
5. **Erij** montre le fichier `src/main/proto/tracking.proto`, présente le port gRPC `9090` et explique le principe du streaming de positions GPS.

---

## 🛠️ Commandes Utiles en cas de problème

```bash
# Voir les logs d'un service qui pose problème (ex: order-service) :
docker compose logs -f order-service

# Redémarrer un seul service sans toucher aux autres :
docker compose restart order-service

# Arrêter proprement tout le système à la fin :
docker compose down

# Si vous voulez tout réinitialiser à zéro (supprime aussi les bases de données) :
docker compose down -v
```
