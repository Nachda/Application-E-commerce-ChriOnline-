# 🛒 ChriOnline - Application E-commerce Sécurisée

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![MySQL](https://img.shields.io/badge/MySQL-9.x-blue)
![License](https://img.shields.io/badge/License-Academic-lightgrey)

Application e-commerce complète développée en Java, basée sur une architecture
client-serveur native avec sockets TCP/UDP, intégrant des mécanismes de
sécurité avancés (chiffrement hybride AES/RSA, authentification admin par
défi-réponse, protection anti-brute force, anti-rejeu et anti-DoS).

Projet GSTR2 "Génie des Systèmes de Télécommunications et Réseau" (BAC+4)
— ENSA Tétouan, année académique **2025-2026**.

## 📋 Description

ChriOnline simule un processus d'achat en ligne complet (consultation de
produits, panier, paiement par carte, gestion des commandes) côté client,
avec un espace d'administration séparé (gestion des produits, catégories,
commandes, utilisateurs, statistiques et notifications de stock).

La communication client/serveur est chiffrée de bout en bout grâce à un
échange de clé AES protégé par RSA (principe similaire à TLS/HTTPS),
et l'authentification de l'administrateur se fait sans mot de passe,
via un mécanisme de signature RSA de type défi-réponse (similaire à SSH).

## ✨ Fonctionnalités principales

### 🛍️ Cœur e-commerce

| Module | Description |
|---|---|
| 👤 Utilisateurs | Enregistrement, authentification sécurisée, gestion de session |
| 📦 Produits | Consultation catalogue, gestion des catégories, détails (nom, prix, stock) |
| 🛒 Panier | Ajout/suppression de produits, calcul automatique du total |
| 💳 Paiement | Simulation de paiement par carte bancaire, avec cartes enregistrées |
| 📄 Commandes | Validation, historique, gestion des statuts |
| 🔔 Notifications | Alertes de stock et notifications email (javax.mail) |
| 📊 Statistiques admin | Tableau de bord temps réel (ventes, stock, utilisateurs) |

### 🔒 Sécurité avancée

| Protection | Mécanisme implémenté |
|---|---|
| 🔐 Chiffrement hybride | Échange de clé AES via RSA, chiffrement des données échangées (SecureChannel) |
| 🛡️ Anti-brute force | Limitation des tentatives d'authentification (BruteForceProtection) |
| 🔁 Anti-rejeu | Protection par challenge/timestamp (ReplayProtection) |
| 👑 Authentification admin sans mot de passe | Défi-réponse avec signature RSA (ChallengeGenerator, Signer, Verifier) |
| ✅ Validation des entrées | Filtrage et validation des données utilisateur (InputValidator) |
| 🔑 Stockage sécurisé | Clés RSA protégées dans un keystore PKCS12 (KeystoreManager) |

## 🏗️ Architecture technique

\\\
src/
├── client/        # Points d'entrée client et admin (ClientMain, AdminMain)
├── config/        # Configuration serveur/application
├── dao/           # Accès aux données (DAO - Data Access Object)
├── database/       # Connexion et gestion MySQL
├── model/         # Entités métier (Order, Product, SavedCard, ...)
├── security/       # Cryptographie et sécurité réseau (RSA, challenge-response, anti-DoS)
├── server/        # Logique serveur, gestion des clients (ClientHandler)
├── service/        # Logique métier (StockService, ProductService, CardService, ...)
├── ui/
│   ├── admin/      # Interface d'administration (Swing)
│   ├── client/     # Interface client (boutique, paiement)
│   ├── common/      # Composants partagés
│   ├── components/  # Composants UI réutilisables (tables, badges, dialogs)
│   └── theme/      # Thème graphique de l'application
└── utils/         # Utilitaires (validation mots de passe, etc.)
\\\

## 🛠️ Stack technique

- **Langage** : Java (JDK 17+)
- **Réseau** : Sockets TCP (\Socket\/\ServerSocket\) + UDP (\DatagramSocket\), multi-threadé
- **Base de données** : MySQL (via \mysql-connector-j\)
- **Cryptographie** : Java Cryptography Architecture (JCA) – AES, RSA 2048, SHA256withRSA
- **Stockage sécurisé** : Keystore PKCS12 pour les clés privées (admin/serveur)
- **Logging** : Log4j2
- **Notifications** : JavaMail API (\javax.mail\)
- **Interface graphique** : Java Swing
- **Versionning** : Git + GitHub

## 🧠 Compétences mobilisées

- Programmation réseau bas niveau (sockets TCP/UDP, protocoles applicatifs)
- Cryptographie appliquée (chiffrement hybride, signatures numériques, gestion de clés)
- Sécurité des systèmes (anti-brute force, anti-rejeu, anti-DoS)
- Conception logicielle en couches (DAO / Service / UI)
- Bases de données relationnelles (MySQL, JDBC)
- Développement d'interfaces graphiques (Swing)
- Travail collaboratif avec Git/GitHub

## 🚀 Installation et exécution

### Prérequis
- Java JDK 17 ou supérieur
- Serveur MySQL actif (base de données à configurer)
- Git (optionnel, pour cloner)

### 1. Cloner le repository
\\\ash
git clone https://github.com/Nachda/Application-E-commerce-ChriOnline-.git
cd Application-E-commerce-ChriOnline-
\\\

### 2. Compiler les sources
\\\ash
javac -cp "lib/*" -d bin src/*.java src/**/*.java
\\\

### 3. Lancer le serveur
\\\ash
java -cp "bin;lib/*" server.Server
\\\

### 4. Lancer un client (dans un autre terminal)
\\\ash
java -cp "bin;lib/*" client.ClientMain
\\\

### 5. Lancer l'interface admin (dans un autre terminal)
\\\ash
java -cp "bin;lib/*" client.AdminMain
\\\

> 💡 **Note** : La première exécution nécessite la génération des clés RSA
> (serveur/admin) et la configuration de la base MySQL (voir \esources/\
> pour les fichiers de configuration).

## 📂 Structure du projet

\\\
ChriOnline/
├── src/           # Code source Java complet (voir architecture ci-dessus)
├── resources/      # Fichiers de configuration
├── lib/           # Dépendances externes (Log4j2, JavaMail, MySQL Connector)
├── bin/           # Classes compilées (ignoré par Git)
├── logs/          # Traces d'exécution (ignoré par Git)
├── image/         # Assets pour l'interface graphique
├── .gitignore     # Fichiers exclus du versionnement
├── LICENSE        # Licence académique
└── README.md      # Ce fichier
\\\

## 👥 Équipe & Membres du projet

Projet réalisé par les étudiants de la filière GSTR2 (ENSA Tétouan),
année académique 2025-2026 :

- **Nachda Nourouddine**
- **YAMEOGO Ariel Barthelemy Wendtoin**
- **Omar Hassan Abdoul-Fatah**
