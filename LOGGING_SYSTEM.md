# Système de Logging NextDungeon

## Vue d'ensemble

Le système de logging de NextDungeon permet de contrôler où et comment les logs sont affichés, avec un mode debug optionnel pour des logs détaillés.

## Configuration

Dans `config.yml`:

```yaml
DebugMode:
    activated: false        # Active le mode debug pour des logs détaillés
    logType: "CONSOLE"     # Où afficher les logs (CONSOLE, IN_GAME, BOTH)
```

### Options de logType

- **CONSOLE**: Les logs sont affichés uniquement dans la console du serveur
- **IN_GAME**: Les logs sont envoyés aux joueurs avec la permission `nextdungeon.debug` ou OPs
- **BOTH**: Les logs sont affichés à la fois dans la console ET en jeu

## Commandes

### Activer/Désactiver le mode debug

```
/nd debug toggle
```

Active ou désactive le mode debug. Quand activé, des logs supplémentaires sont affichés pour diagnostiquer les problèmes.

### Changer le type de log

```
/nd debug setlogbroadcast <type>
```

Change où les logs sont affichés. Types valides: `CONSOLE`, `IN_GAME`, `BOTH`

**Exemples:**
```
/nd debug setlogbroadcast CONSOLE
/nd debug setlogbroadcast IN_GAME
/nd debug setlogbroadcast BOTH
```

## Utilisation dans le code

### Méthodes disponibles

```java
// Récupérer l'instance du logger
LoggerUtil logger = Main.getLoggerUtil();

// Logger un message info
logger.info("Message d'information");

// Logger un avertissement
logger.warning("Message d'avertissement");

// Logger une erreur sévère
logger.severe("Message d'erreur critique");

// Vérifier si le mode debug est activé
if (logger.isDebugEnabled()) {
    logger.info("Message de debug détaillé");
}
```

### Bonnes pratiques

1. **Utiliser les logs debug pour les détails**:
   ```java
   if (Main.getLoggerUtil().isDebugEnabled()) {
       Main.getLoggerUtil().info("Trigger: " + trigger.getName() + " (type: " + trigger.getType() + ")");
   }
   ```

2. **Toujours logger les erreurs importantes**:
   ```java
   try {
       // Code risqué
   } catch (Exception e) {
       Main.getLoggerUtil().severe("Error processing trigger: " + e.getMessage());
       e.printStackTrace(System.err);
   }
   ```

3. **Utiliser warning pour les situations anormales**:
   ```java
   if (trigger == null) {
       Main.getLoggerUtil().warning("Trigger not found: " + triggerId);
       return;
   }
   ```

## Niveaux de logs

### INFO
Messages informatifs normaux sur l'état du plugin:
- Démarrage du plugin
- Chargement de ressources
- Initialisation de systèmes
- Compteurs (nombre de triggers, dungeons, etc.)

### WARNING
Situations anormales mais non critiques:
- Configuration manquante ou invalide (utilise des valeurs par défaut)
- Ressources non trouvées
- Opérations qui ont échoué mais ne bloquent pas le fonctionnement

### SEVERE
Erreurs critiques qui impactent le fonctionnement:
- Impossibilité de se connecter à la base de données
- Échec de chargement de composants essentiels
- Exceptions non gérées

## Permissions

- `nextdungeon.debug`: Permet de recevoir les logs en jeu quand `logType` est `IN_GAME` ou `BOTH`
- Les OPs reçoivent automatiquement les logs en jeu

## Initialisation

Le système de logging est initialisé automatiquement au démarrage du plugin:

1. Le fichier de configuration par défaut est créé si absent
2. LoggerUtil est initialisé avec `CONSOLE` par défaut
3. La configuration est chargée et appliquée
4. Si la configuration est invalide, `CONSOLE` est utilisé comme fallback

## Sécurité

- Le système inclut des checks défensifs pour éviter les NullPointerException
- Les valeurs de configuration invalides sont détectées et remplacées par des valeurs par défaut
- Les logs ne contiennent jamais d'informations sensibles (mots de passe, tokens, etc.)

## Dépannage

### Les logs n'apparaissent pas en jeu

1. Vérifier que `logType` est `IN_GAME` ou `BOTH` dans config.yml
2. Vérifier que le joueur a la permission `nextdungeon.debug` ou est OP
3. Essayer `/nd debug setlogbroadcast BOTH`

### Les logs de debug n'apparaissent pas

1. Vérifier que `DebugMode.activated` est `true` dans config.yml
2. Ou activer avec `/nd debug toggle`

### Erreur au démarrage du plugin

Si vous voyez une erreur liée au logging au démarrage:
1. Vérifier que config.yml existe et est valide
2. Vérifier que la clé `DebugMode.logType` existe
3. Supprimer config.yml et redémarrer pour régénérer le fichier par défaut
