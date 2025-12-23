# NextDungeon - Modifications Récentes

## Version 1.0.2-SNAPSHOT - 23 Décembre 2024

### 🔧 Correctif de la Communication Redis

#### Problème Résolu
La communication Redis entre les serveurs de jeu et les proxies n'était pas fiable. Lorsqu'une requête était envoyée via Redis, **tous les serveurs** recevaient et pouvaient traiter la requête, au lieu que seul le serveur concerné ne le fasse.

#### Solution Implémentée

1. **Ajout du champ `targetServerId`** dans `WebEditorRequestPacket` :
   - Permet de spécifier explicitement quel serveur Spigot doit traiter la requête
   - Ajouté dans les trois modules : spigot, bungeecord, velocity

2. **Filtrage côté serveur** dans `WebEditorRequestSubscriber` :
   - Vérifie si le message est destiné à ce serveur avant traitement
   - Ignore les messages destinés à d'autres serveurs
   - Log les requêtes ignorées pour le debugging

3. **Mise à jour de la communication proxy** :
   - `SpigotCommunicationService` (BungeeCord et Velocity) passe maintenant le serveur cible
   - Logs améliorés pour tracer les requêtes vers le bon serveur

4. **Configuration serveur** :
   - Nouveau paramètre `server-name` dans `config.yml`
   - Permet d'identifier de manière unique chaque serveur Spigot
   - Exemple : `server-name: "dungeon-server-1"`

#### Fichiers Modifiés

**Spigot :**
- `spigot/src/main/java/fr/perrier/dungeons/spigot/messaging/packets/webeditor/WebEditorRequestPacket.java`
- `spigot/src/main/java/fr/perrier/dungeons/spigot/messaging/subscribers/WebEditorRequestSubscriber.java`
- `spigot/src/main/resources/config.yml`

**BungeeCord :**
- `bungeecord/src/main/java/fr/perrier/dungeons/bungee/messaging/packets/webeditor/WebEditorRequestPacket.java`
- `bungeecord/src/main/java/fr/perrier/dungeons/bungee/messaging/SpigotCommunicationService.java`

**Velocity :**
- `velocity/src/main/java/fr/perrier/dungeons/velocity/messaging/packets/webeditor/WebEditorRequestPacket.java`
- `velocity/src/main/java/fr/perrier/dungeons/velocity/messaging/SpigotCommunicationService.java`

#### Configuration Requise

Dans chaque serveur Spigot, définir un identifiant unique dans `config.yml` :

```yaml
# ─────────────────────────────────────────────────────
# 🔧 CONFIGURATION SERVEUR
# ─────────────────────────────────────────────────────
# Identifiant unique de ce serveur Spigot pour la communication Redis
# Doit être unique pour chaque serveur de jeu
server-name: "dungeon-server-1"
```

### 🎨 Refonte Graphique du Webeditor et Dashboard

#### Nouveau Thème Sombre

Interface complètement redessinée avec un thème sombre moderne, inspiré du style de [Spark](https://spark.lucko.me/).

#### Caractéristiques du Design

**Palette de Couleurs :**
- Fond principal : `#1a1d29`
- Fond secondaire : `#242837`
- Fond tertiaire : `#2d3348`
- Accent principal : `#5e72e4` (bleu)
- Accent secondaire : `#11cdef` (cyan)
- Succès : `#2dce89` (vert)
- Avertissement : `#fb6340` (orange)
- Danger : `#f5365c` (rouge)
- Texte principal : `#e9ecef` (blanc cassé)
- Texte secondaire : `#adb5bd` (gris clair)

**Améliorations Visuelles :**
- Bordures arrondies modernes (6-8px)
- Ombres douces pour la profondeur
- Transitions fluides sur les interactions
- Typographie moderne (système fonts)
- Cartes avec bordures subtiles
- Badges colorés pour les statuts
- Graphiques adaptés au thème sombre

#### Fichiers Modifiés

**BungeeCord :**
- `bungeecord/src/main/resources/webserver/index.html` (Webeditor)
- `bungeecord/src/main/resources/webserver/dashboard.html` (Dashboard)

**Velocity :**
- `velocity/src/main/resources/webserver/index.html` (Webeditor)
- `velocity/src/main/resources/webserver/dashboard.html` (Dashboard)

#### Composants Affectés

**Webeditor :**
- En-tête avec informations de session
- Barre de contrôles (Charger, Sauvegarder, Vider, Générer Code)
- Zone de l'éditeur Blockly
- Messages de statut (succès, erreur, info)
- Messages de debug

**Dashboard :**
- En-tête du dashboard
- Cartes de statistiques (floors, instances, sessions)
- Tableaux de données
- Graphiques (barres et donut)
- Modales de configuration
- Bouton de rafraîchissement

### 🧪 Tests Recommandés

#### Communication Redis

1. **Test multi-serveurs :**
   - Démarrer plusieurs serveurs Spigot avec des `server-name` différents
   - Envoyer des requêtes ciblées depuis le proxy
   - Vérifier que seul le serveur cible répond

2. **Test webeditor :**
   - Créer une session d'édition depuis un serveur Spigot
   - Charger/sauvegarder des triggers
   - Vérifier les logs pour confirmer le routage correct

3. **Test de fallback :**
   - Envoyer une requête à un serveur inexistant
   - Vérifier le timeout et la gestion d'erreur

#### Interface Utilisateur

1. **Test d'affichage :**
   - Vérifier l'apparence sur différentes résolutions
   - Tester la responsive sur mobile/tablette
   - Vérifier le contraste des couleurs

2. **Test fonctionnel :**
   - Tester tous les boutons et interactions
   - Vérifier l'affichage des graphiques
   - Tester l'ouverture/fermeture des modales

3. **Test de performance :**
   - Vérifier le temps de chargement
   - Tester avec de grandes quantités de données
   - Vérifier la fluidité des animations

### 📝 Notes Importantes

1. **Compatibilité :**
   - Les anciennes versions du plugin ne sont pas compatibles avec cette version
   - Tous les serveurs (Spigot, BungeeCord, Velocity) doivent être mis à jour ensemble

2. **Migration :**
   - Ajouter le paramètre `server-name` dans la configuration de chaque serveur Spigot
   - Redémarrer tous les serveurs après la mise à jour

3. **Débogage :**
   - Les logs contiennent maintenant l'identifiant du serveur cible
   - Les requêtes ignorées sont loguées au niveau `FINE`
   - Utiliser les logs pour diagnostiquer les problèmes de routage

### 🔜 Améliorations Futures Suggérées

1. **Communication Redis :**
   - Ajouter un système de heartbeat pour détecter les serveurs actifs
   - Implémenter un cache des serveurs disponibles
   - Ajouter des métriques de performance

2. **Interface Utilisateur :**
   - Ajouter un mode clair/sombre commutable
   - Implémenter des animations plus poussées
   - Ajouter des tooltips explicatifs
   - Améliorer l'accessibilité (ARIA labels, navigation clavier)

3. **Monitoring :**
   - Ajouter des graphiques en temps réel
   - Implémenter un système d'alertes
   - Ajouter des logs d'audit

### 📚 Ressources

- Documentation Spark : https://spark.lucko.me/
- Guide de configuration : Voir `config.yml`
- Rapport de bugs : GitHub Issues
- Support : Discord du projet

---

**Auteurs :** Copilot AI Assistant, PerrierBouteille  
**Date :** 23 Décembre 2024  
**Version :** 1.0.2-SNAPSHOT
