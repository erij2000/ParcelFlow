# 🟧 Fiche Technique & Guide de Soutenance – Malek Beltaief
**Microservice :** `carrier-service` (Tarification & Réservation de Transporteurs)  
**Spécialité :** Protocole SOAP, Approche Contract-First, Schémas XSD, Contrat WSDL, SOAP Faults, Spring-WS  
**Dépôt GitHub :** [github.com/erij2000/ParcelFlow](https://github.com/erij2000/ParcelFlow)  
**Branche Git :** `feature/malek/carrier-soap-wsdl`

---

## 🎤 1. Ton Pitch Oral pour Demain (À dire mot pour mot au professeur)

> *"Bonjour Madame / Monsieur.  
> Je suis responsable du **Carrier Service**, le microservice dédié à l'intégration B2B avec les transporteurs logistiques partenaires pour la tarification d'expédition et la réservation de créneaux.
>
> Pour ce microservice, j'ai implémenté le protocole **SOAP (Simple Object Access Protocol)** avec une approche rigoureuse **Contract-First**.  
> Contrairement à l'approche naïve Code-First qui dérive le contrat à partir de classes Java (rendant le service fragile et instable), nous avons rédigé les contrats **`carrier.xsd`** et **`carrier.wsdl`** avant d'écrire la moindre ligne de code applicatif.
>
> Le schéma XML (`carrier.xsd`) définit de manière immuable les types complexes, les énumérations (`STANDARD`, `EXPRESS`, `OVERNIGHT`) et les contraintes de validation sur les éléments (ex: poids positif strict).  
> Le contrat WSDL expose les opérations métier : **`GetPrice`** pour l'estimation tarifaire et **`BookCarrier`** pour la réservation formelle d'expédition.
>
> J'ai également apporté une attention particulière à la gestion des erreurs selon le standard **SOAP Fault**. Au lieu de masquer les pannes ou de renvoyer de simples codes HTTP 500 génériques, j'ai configuré l'annotation `@SoapFault` sur des exceptions métier (`CarrierSoapFaultException`), qui génèrent des enveloppes XML `<soap:Fault>` explicites conformes au contrat.
>
> Le service est configuré avec **Spring-WS** et publie son WSDL en direct sur `http://localhost:8082/ws/carrier.wsdl`. Sa base de données `carrierdb` tourne sur PostgreSQL de manière autonome sur le port 5434."*

---

## 📂 2. Explication Détaillée Fichier par Fichier

### 📄 Fichier 1 : `src/main/resources/wsdl/carrier.xsd`
*Le schéma de données XML (La Loi du Service).*

- **Rôle :** Définit la grammaire XML de tous les messages échangés.
- **Éléments clés :**
  - `<xs:simpleType name="CarrierType">` : Restriction par énumération des modes d'expédition autorisés (`STANDARD`, `EXPRESS`, `OVERNIGHT`). Si un client envoie `DRONE`, la validation XML échoue immédiatement avant même d'atteindre le code Java !
  - `<xs:element name="GetPriceRequest">` : Contient l'origine, la destination, le type de transporteur et le poids (`weightKg`).
  - Validation du poids : `<xs:restriction base="xs:decimal"><xs:minExclusive value="0"/><xs:maxInclusive value="1000"/></xs:restriction>`. Le contrat interdit formellement les poids négatifs ou dépassant 1 tonne.
  - `<xs:element name="CarrierServiceFault">` : Définit la structure XML de l'erreur métier retournée (`faultCode`, `faultMessage`, `details`).

---

### 📄 Fichier 2 : `src/main/resources/wsdl/carrier.wsdl`
*Le contrat formel d'interface Web Service.*

- **Structure standard en 5 sections :**
  1. `<wsdl:types>` : Importe le fichier `carrier.xsd`.
  2. `<wsdl:message>` : Définit les messages d'entrée et de sortie (`GetPriceRequest`, `GetPriceResponse`, `BookCarrierRequest`, `BookCarrierResponse`, `CarrierServiceFault`).
  3. `<wsdl:portType>` : L'interface abstraite. Contient les opérations `GetPrice` et `BookCarrier`, associant chaque entrée, sortie et faute.
  4. `<wsdl:binding>` : La liaison concrète avec le protocole SOAP 1.1 sur HTTP (`style="document"`, `transport="http://schemas.xmlsoap.org/soap/http"`).
  5. `<wsdl:service>` : L'adresse réseau concrète du port d'écoute (`http://localhost:8082/ws`).

---

### 📄 Fichier 3 : `src/main/java/com/parcelflow/carrier/config/WebServiceConfig.java`
*La configuration du moteur Spring-WS.*

- **Annotation `@EnableWs` :** Active la mécanique des Web Services SOAP dans Spring Boot.
- **Bean `ServletRegistrationBean<MessageDispatcherServlet>` :**  
  Enregistre le servlet central de Spring-WS sur le pattern d'URL `/ws/*`. Toutes les requêtes SOAP entrantes sont captées par ce servlet qui les analyse et les route vers le bon `@Endpoint`.
- **Bean `DefaultWsdl11Definition` :**  
  Construit dynamiquement le document WSDL 1.1 à partir du XSD et le rend accessible sur `http://localhost:8082/ws/carrier.wsdl`.

---

### 📄 Fichier 4 : `src/main/java/com/parcelflow/carrier/ws/CarrierEndpoint.java`
*Le contrôleur SOAP.*

- **Annotation `@Endpoint` :** Équivalent de `@RestController`, mais pour le monde SOAP.
- **Annotation `@PayloadRoot` :**
  ```java
  @PayloadRoot(namespace = "http://parcelflow.com/carrier", localPart = "GetPriceRequest")
  @ResponsePayload
  public GetPriceResponseStub getPrice(@RequestPayload GetPriceRequestStub request) { ... }
  ```
  Spring-WS n'utilise pas les URL pour router les requêtes, mais inspecte le premier élément XML du `<soap:Body>` (ici `GetPriceRequest`). C'est le principe du **Payload-based Routing**.
- **Méthodes implémentées :**
  - `getPrice(...)` : Valide les paramètres, appelle `PricingService` et construit la réponse avec prix calculé et délai estimé.
  - `bookCarrier(...)` : Réserve le transporteur, génère un identifiant de réservation UUID unique et confirme le statut.

---

### 📄 Fichier 5 : `src/main/java/com/parcelflow/carrier/ws/CarrierSoapFaultException.java`
*Génération automatique des SOAP Faults.*

- **Annotation `@SoapFault(faultCode = FaultCode.SERVER)` :**  
  Indique à Spring-WS que dès que cette exception est levée, le framework doit intercepter la réponse et encapsuler l'erreur dans un élément XML standard :
  ```xml
  <SOAP-ENV:Fault>
      <faultcode>SOAP-ENV:Server</faultcode>
      <faultstring>Le poids doit être strictement positif</faultstring>
  </SOAP-ENV:Fault>
  ```

---

### 📄 Fichier 6 : `src/main/java/com/parcelflow/carrier/service/PricingService.java`
*L'algorithme métier de tarification.*

- Formule de calcul :
  $$\text{Prix Total} = \text{Tarif de Base} + (\text{Poids} \times 1.50\text{ €}) + \text{Surcharge Inter-ville}$$
- `STANDARD` : base 5.00 € (4 jours)
- `EXPRESS` : base 12.00 € (1 jour)
- `OVERNIGHT` : base 25.00 € (livraison le jour même/nuit)

---

## 🛡️ 3. Questions Pièges du Professeur & Réponses de Malek

1. **"Malek, quelle est la différence entre SOAP Document/Literal et RPC/Encoded ?"**  
   > *Réponse :* "Le style **RPC/Encoded** lie fortement le payload XML à un format de sérialisation spécifique d'un langage (ex: Java ou .NET) et a été déprécié par le WS-I Basic Profile pour des raisons d'interopérabilité.  
   > Nous utilisons le style **Document/Literal**, qui est la norme moderne de facto : le corps du message SOAP est un document XML pur, validé mot pour mot par notre schéma XSD (`Literal`), garantissant une interopérabilité universelle entre n'importe quelles technologies."

2. **"Comment le client sait-il quelles méthodes appeler ?"**  
   > *Réponse :* "Le client télécharge notre WSDL public disponible sur `/ws/carrier.wsdl`. Les outils clients (ex: `wsimport` en Java, SoapUI, ou le futur `WebServiceTemplate` de Yessine) génèrent automatiquement les stubs clients et les objets de sérialisation à partir de ce contrat."
