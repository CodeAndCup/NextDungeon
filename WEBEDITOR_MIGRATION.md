# Éditeur Web Centralisé - NextDungeon

## Architecture Migree

L'éditeur web Blockly a été migré du système distribué (un serveur par Spigot) vers un système centralisé sur le proxy BungeeCord.

### Avantages
- ✅ **Un seul port** : Plus besoin de ports multiples pour chaque serveur Spigot
- ✅ **Gestion centralisée** : Toutes les sessions d'édition sont gérées sur le proxy
- ✅ **URLs uniques** : Chaque session a son URL unique (`localhost:8080/{floorId-uuid}/editor/`)
- ✅ **Scalabilité** : Supporte plusieurs serveurs Spigot simultanément

### Architecture

```
┌─────────────────┐     HTTP     ┌──────────────────┐
│   Proxy (8080)  │◄────────────►│  Spigot (8081)   │
│                 │              │                  │
│ ProxyWebEditor  │              │ SpigotProxyBridge│
│ SessionManager  │              │ MessageHandler   │
└─────────────────┘              └──────────────────┘
         │
         ▼
    Web Interface
   (Blockly Editor)
```

## Utilisation

### Depuis un serveur Spigot en mode édition

```bash
/dungeon admin webeditor start
```

Cette commande va maintenant :
1. Demander au proxy de créer une session d'édition
2. Retourner une URL unique : `http://localhost:8080/{floorId-uuid}/editor/`
3. Le pont Spigot (port 8081) reste actif pour répondre aux requêtes du proxy

### URLs disponibles

- **Interface d'édition** : `http://localhost:8080/{sessionId}/editor/`
- **API Triggers** : `http://localhost:8080/{sessionId}/api/triggers`
- **API Sauvegarde** : `http://localhost:8080/{sessionId}/api/save`
- **API Types** : `http://localhost:8080/{sessionId}/api/trigger-types`
- **API Blockly** : `http://localhost:8080/{sessionId}/api/blockly.js`
- **API Floor Info** : `http://localhost:8080/{sessionId}/api/floor-info`

### Gestion des sessions (proxy)

```bash
/webeditor-proxy list          # Liste les sessions actives
/webeditor-proxy info          # Infos de votre session
/webeditor-proxy stop          # Ferme votre session
```

## Composants

### Proxy (BungeeCord)
- `ProxyWebEditorServer` - Serveur web principal (port 8080)
- `EditorSessionManager` - Gestion des sessions actives
- `SpigotCommunicationService` - Communication avec les serveurs Spigot
- `WebEditorProxyCommand` - Commandes de gestion

### Spigot
- `DungeonWebEditorManager` - Gestionnaire modifié pour utiliser le proxy
- `SpigotProxyBridge` - Serveur API local (port 8081)
- `ProxyBridgeService` - Client de communication avec le proxy
- `ProxyEditorMessageHandler` - Traitement des requêtes proxy

## Migration

### Ancien système
```
Spigot1:8080 ── WebEditorServer ── Player1
Spigot2:8080 ── WebEditorServer ── Player2  ❌ Conflit de ports
Spigot3:8080 ── WebEditorServer ── Player3
```

### Nouveau système
```
                    ┌── Spigot1:8081 ── Player1
Proxy:8080 ── SessionManager ──┼── Spigot2:8081 ── Player2  ✅ Un seul port public
                    └── Spigot3:8081 ── Player3
```

## Compatibilité

- ✅ Toutes les fonctionnalités existantes sont préservées
- ✅ La logique métier (triggers, actions, sauvegarde) reste identique
- ✅ L'interface Blockly reste inchangée
- ✅ Rétrocompatible avec les triggers existants