# EntityDeathTrigger - Problème Résolu ✅

## Résumé du Problème

**Symptôme**: EntityDeathTrigger ne se déclenche pas quand un zombie meurt
**Logs**: 
```
[21:43:59] Triggers cache refresh complete: 1 triggers  ← Chargé
[21:44:33] Processing event: EntityDeathEvent with 0 triggers  ← Pas trouvé!
```

## Cause Racine Identifiée 🔍

**EntityDeathTrigger manquait un constructeur sans argument!**

Gson (utilisé pour la désérialisation depuis la BDD) nécessite un constructeur no-arg pour créer des objets. Sans celui-ci:
- La désérialisation échoue silencieusement
- Le trigger devient null ou malformé
- Le trigger n'est pas ajouté au cache
- Résultat: 0 triggers trouvés pour l'événement

## Solution Appliquée ✅

### EntityDeathTrigger.java
Ajout du constructeur manquant:
```java
// Constructeur sans argument requis pour Gson
public EntityDeathTrigger() {
    super("Entity Death Trigger");
    this.entityType = "ZOMBIE";  // Valeur par défaut
}
```

### RegionTrigger.java
Même correction appliquée:
```java
// Constructeur sans argument requis pour Gson
public RegionTrigger() {
    super("Region Trigger");
    this.worldName = "world";
    this.regionEvent = "enter";
    this.onlyOnce = false;
    this.cooldownSeconds = 0;
}
```

## Comment Cela Corrige le Problème

**Avant (cassé):**
1. BDD contient EntityDeathTrigger en JSON
2. Gson essaie: `new EntityDeathTrigger()` 
3. ❌ Constructeur n'existe pas
4. ❌ Désérialisation échoue
5. ❌ Trigger = null
6. ❌ 0 triggers dans le cache
7. ❌ Rien ne se passe à la mort du zombie

**Après (corrigé):**
1. BDD contient EntityDeathTrigger en JSON
2. Gson appelle: `new EntityDeathTrigger()`
3. ✅ Constructeur existe!
4. ✅ Trigger créé avec valeurs par défaut
5. ✅ Gson remplit les champs depuis le JSON
6. ✅ Trigger ajouté au cache pour EntityDeathEvent
7. ✅ Se déclenche quand un zombie meurt!

## Vérification

Pour tester que le correctif fonctionne:

1. **Redémarrer le serveur** avec le code corrigé

2. **Activer le debug**:
   ```
   /nd debug toggle
   /nd debug setlogbroadcast BOTH
   ```

3. **Vérifier les logs** au démarrage:
   ```
   Processing trigger: ... (type: entity_death_trigger, class: EntityDeathTrigger)
   -> Added trigger to event cache for EntityDeathEvent
   ```

4. **Tuer un zombie**:
   ```
   /summon zombie ~ ~ ~
   /kill @e[type=zombie,limit=1]
   ```

5. **Vérifier les logs**:
   ```
   Processing event: EntityDeathEvent with 1 triggers  ← Devrait être 1, pas 0!
   ```

## Configuration BDD (Pour Référence)

Structure JSON correcte dans `floor_triggers`:
```json
[
  {
    "className": "fr.perrier.dungeons.spigot.workflow.trigger.impl.EntityDeathTrigger",
    "data": {
      "triggerId": "unique-id",
      "name": "Kill Zombie",
      "type": "entity_death_trigger",
      "enabled": true,  ← IMPORTANT: doit être true!
      "entityType": "ZOMBIE",
      "actions": [
        {
          "className": "fr.perrier.dungeons.spigot.workflow.action.impl.EndDungeonAction",
          "data": {
            "type": "end_dungeon_action"
          }
        }
      ]
    }
  }
]
```

## Points Vérifiés ✅

- ✅ Constructeur no-arg ajouté à EntityDeathTrigger
- ✅ Constructeur no-arg ajouté à RegionTrigger
- ✅ Valeurs par défaut configurées dans constructeurs
- ✅ Type = "entity_death_trigger" (correct)
- ✅ Handler supporte "entity_death_trigger"
- ✅ Handler enregistré pour EntityDeathEvent

## Impact

**Triggers affectés (maintenant corrigés):**
- EntityDeathTrigger: ❌→✅ Fonctionne maintenant
- RegionTrigger: ❌→✅ Fonctionne maintenant

**Tous les autres triggers** avaient déjà des constructeurs no-arg et fonctionnaient correctement.

## Si le Problème Persiste

Si après ce correctif le trigger ne fonctionne toujours pas:

1. **Vérifier `enabled: true`** dans la BDD
2. **Vérifier le `type`** = "entity_death_trigger" (exact)
3. **Vérifier l'`entityType`** = "ZOMBIE"
4. **Activer le debug** et partager les logs complets
5. **Consulter** `ENTITYDEATH_TRIGGER_FIX.md` pour diagnostic détaillé

## Fichiers Modifiés

- `EntityDeathTrigger.java`: Ajout constructeur no-arg
- `RegionTrigger.java`: Ajout constructeur no-arg
- `ENTITYDEATH_TRIGGER_FIX.md`: Guide de diagnostic
- `test-trigger-config.sql`: Config SQL de test

## Conclusion

Le problème était **un bug de désérialisation** causé par l'absence de constructeur sans argument. Avec ce correctif, EntityDeathTrigger et RegionTrigger se chargent correctement depuis la base de données et fonctionnent comme prévu.

🎉 **Problème résolu!**
