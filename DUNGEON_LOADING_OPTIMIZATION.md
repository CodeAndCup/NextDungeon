# Prompt — Optimisation du chargement des donjons (BDD ↔ Redis ↔ Lobby)

> **But de ce fichier** : c'est un *prompt d'implémentation* à me redonner plus tard.
> Il contient l'analyse approfondie + validée du flux de chargement actuel et un plan
> de correction ordonné. Quand tu veux que je corrige, dis simplement
> « applique DUNGEON_LOADING_OPTIMIZATION.md » (ou un sous-ensemble des phases).

---

## ⚠️ Contraintes d'exécution (à respecter quand on corrigera)

- **On ne peut pas compiler ni tester ici** (dépendances Maven indisponibles). Voir la mémoire `no-maven-build`.
  → Vérifier chaque changement par lecture (types, signatures, portée de `this`, imports), pas par compilation.
- Préférer des changements **contenus, réversibles, mesurables**. Ce flux est *fonctionnellement correct* aujourd'hui ; on optimise, on ne réécrit pas tout d'un coup.
- Garder la **BDD comme source de vérité durable** et Redis comme cache/canal de sync.

---

## 1. Flux actuel réel (validé dans le code)

Déclenchement au boot **lobby uniquement**, **synchrone sur le main thread** pendant `onEnable` :

```
Main.onEnable()
 ├─ dungeonService = new DungeonService(redissonClient); dungeonService.initialize()   Main.java:187-188
 ├─ databaseManager = DatabaseFactory.createDatabase()                                  Main.java:236
 └─ initializeLobbyServer()                                                             Main.java:270, 666
      ├─ RedisConfigLoader.loadAllDungeonsFromRedis()   (BLOQUANT, main thread)         Main.java:672
      └─ subscribeDashboardSyncChannel()                                                Main.java:675, 682
```

`RedisConfigLoader.loadAllDungeonsFromRedis()` (`RedisConfigLoader.java:28`) :
1. Si `floorsMap` Redis vide → fallback BDD `getAllFloors(0).get()` (`:161,168`) puis `rebuildRedisFromDatabase` (`:201`).
2. Sinon → itère **toute** la map Redis, *heal* des checksums legacy (`:46-69`).
3. Pour **chaque** floor → `loadFloor(fd)` (`:88`, `:246`).
4. `repopulateDashboardDungeonEntries(dbManager)` → `listAllDungeons().get()` bloquant (`:106,119,129`).

`loadFloor(fd)` (`RedisConfigLoader.java:246`) fait, **par floor** :
- `new Floor(fd)` → le constructeur `Floor(FloorData)` **charge les triggers depuis la BDD** si `getTriggers()==null` (`Floor.java:51-53`). Or le map Redis partagé **strip toujours les triggers** (`DungeonService.syncFloor`/`stripTriggersForSharedStorage`, `DungeonService.java:199,454`), donc `getTriggers()` est **toujours null** ⇒ **1 query BDD triggers**.
- `DatabaseTriggersManager.loadTriggers(fd.getId())` **une 2ᵉ fois** (`RedisConfigLoader.java:251-252`) ⇒ **2ᵉ query BDD triggers** (blocking `future.join()`, `DatabaseTriggersManager.java:53`).
- `floor.updateMap()` (`Floor.java:70`) → `DungeonService.syncFloor()` : `floorsMap.fastPut` + `floorMetadataMap.fastPut` + **`syncTopic.publish(FLOOR_UPDATE)`** (`DungeonService.java:209,213,216`).
- `floor.generateTemplate()` (`Floor.java:79`) → async, par floor : si le template CloudNet n'existe pas, **copie tout le template monde + crée une ServiceTask** (`CloudNetProvider.createTemplate`, `:210`, copie de fichiers `copyTemplateFiles`).

Sync live (après boot) : `subscribeDashboardSyncChannel` → `FLOOR_UPDATE` relance `reloadFloorFromRedis(id)` en async (`Main.java:698-699`), qui rappelle `loadFloor` (donc re-2×triggers + updateMap + generateTemplate pour ce floor).

---

## 2. Constats validés (analyse d'origine confirmée + précisée)

| # | Constat | Preuve | Impact | Sévérité |
|---|---------|--------|--------|----------|
| A | **Chargement total au boot, bloquant le main thread** | `loadAllDungeonsFromRedis` appelé en synchrone `Main.java:672` dans `onEnable` | Cold start lent, risque watchdog si beaucoup de floors | Haute |
| B | **N+1 triggers BDD** | `DatabaseTriggersManager.loadTriggers` un par floor, pas de batch (`DatabaseManager` n'a aucune méthode batch triggers) | N requêtes BDD séquentielles au boot | Haute |
| C | **Dual-write réparti** (proxy écrit Redis, spigot persiste BDD) | `DungeonService.handleFloorUpdate` persiste DB puis rollback Redis `:341-398` | Fenêtre d'incohérence Redis↔BDD | Moyenne |
| D | **Tous les lobbies refont le même warmup** | `loadFloor` (triggers+updateMap+template) exécuté par chaque lobby au boot | Duplication de charge BDD/Redis/CloudNet ×nb lobbies | Moyenne/Haute |
| E | **Pas de lock de bootstrap** quand Redis vide | `if (floorsMap.isEmpty())` → chaque lobby fait le rebuild `RedisConfigLoader.java:33-42` | Course entre lobbies après un flush Redis | Moyenne |
| F | **Pas d'observabilité du startup** | aucun timing/metrics agrégé (logs unitaires seulement) | Pas de visibilité sur la régression perf | Basse/Moyenne |

---

## 3. Constats **supplémentaires** trouvés en approfondissant (au-delà de l'analyse d'origine)

| # | Constat | Preuve | Impact | Sévérité |
|---|---------|--------|--------|----------|
| G | **Double chargement des triggers par floor** | `Floor(FloorData)` charge déjà les triggers (`Floor.java:51-53`) **puis** `loadFloor` recharge (`RedisConfigLoader.java:251`) | **2×N** requêtes BDD au lieu de N (et N inutiles) | Haute |
| H | **Tempête de republish au boot** : `updateMap()` publie un `FLOOR_UPDATE` **par floor**, sur **chaque** lobby | `loadFloor`→`updateMap`→`syncTopic.publish` (`DungeonService.java:216`) | N×(nb lobbies) messages pub/sub + désérialisation Kryo sur tous les serveurs, au démarrage | Haute |
| I | **`generateTemplate()` exécuté sur les lobbies** | `loadFloor` appelle `generateTemplate` (`RedisConfigLoader.java:261`) ; provisioning CloudNet = concept *cluster*, pas *par-lobby* | I/O disque lourd (copie template) ×N ×lobbies à froid ; au minimum N checks CloudNet par lobby | Haute |
| J | **`repopulateDashboardDungeonEntries` bloquant au boot** | `listAllDungeons().get()` + boucle get/set buckets sur main thread (`RedisConfigLoader.java:129`) | Ajoute au cold start ; refait par chaque lobby | Moyenne |
| K | **`reloadFloorFromRedis` reproduit tout le coût par event** | `Main.java:698` → `loadFloor` (2×triggers + updateMap + generateTemplate) | Une édition dashboard = re-publish + re-template sur **chaque** lobby | Moyenne |
| L | **Le fast-path « Redis non vide » n'est pas un fast-path** | même quand Redis est plein, `loadFloor` refait triggers+template+republish | l'optimisation Redis attendue n'existe pas vraiment | Haute |

> Note de cohérence : `generateTemplate()` rend un `CompletableFuture` jamais attendu dans `loadFloor` → les erreurs de génération de template sont silencieuses au boot.

---

## 4. Architecture cible (objectif)

1. **Boot lobby = metadata-only.** Le lobby a besoin de la *liste* donjons/floors + métadonnées (pour les menus / file d'attente), **pas** des triggers ni des templates monde.
   - Hydrater depuis `floor_metadata` / `floorsMap` sans toucher BDD triggers ni CloudNet.
2. **Lazy-load du floor complet** (triggers) **à la demande** (au moment de préparer une instance), ou warmup progressif async par priorité — jamais en bloquant `onEnable`.
3. **Sortir la génération de templates du chemin de boot lobby.** Le template est un artefact de provisioning : à générer (a) à la sauvegarde admin/éditeur d'un floor, et/ou (b) par **un seul** acteur via lock distribué, pas par chaque lobby à chaque boot.
4. **Unifier le write-path** : BDD source de vérité + event d'invalidation/version vers Redis (supprimer le republish `updateMap` au boot ; ne publier que sur vrai changement).
5. **Batch triggers** : ajouter une API `loadAllTriggers()` / `loadTriggersFor(Collection<floorId>)` pour tuer le N+1 quand un préchargement est nécessaire.
6. **Lock distribué de bootstrap** (`RLock` Redisson) : un seul lobby reconstruit Redis après un flush.
7. **Observabilité** : métriques de startup (durée, nb floors, hits Redis vs BDD, nb templates générés, nb fallback).

---

## 5. Plan d'implémentation (ordonné, par phases)

> Chaque phase est indépendamment livrable. Commencer par les *quick wins* à fort ROI et faible risque (Phase 1).

### Phase 1 — Quick wins faible risque (corrige G, H, L, partiel I/J)
1. **Supprimer le double chargement triggers** : dans `loadFloor` (`RedisConfigLoader.java:246`), ne **pas** rappeler `DatabaseTriggersManager.loadTriggers` si le `Floor` les a déjà (ou inversement : ne pas charger dans le constructeur au boot lobby). Choisir **une seule** source d'appel.
2. **Ne plus republier au boot** : remplacer `floor.updateMap()` dans `loadFloor` par une mise en cache locale **sans** `syncTopic.publish`. Idée : ajouter `DungeonService.cacheFloorLocalNoPublish(FloorData)` (fastPut éventuel **sans** publish), ou ne rien réécrire du tout quand la donnée vient déjà de Redis (cas fast-path).
3. **Ne pas générer les templates sur les lobbies** : retirer `floor.generateTemplate()` de `loadFloor` (déplacé en Phase 3). Sur un lobby, un template monde ne sert à rien.
4. **Rendre le boot non bloquant** : envelopper l'hydratation lourde dans un `runTaskAsynchronously` après `onEnable`, ou au minimum sortir `repopulateDashboardDungeonEntries` et le fallback BDD du main thread.

### Phase 2 — Boot metadata-only + lazy triggers (corrige A, B, L)
5. Séparer **hydratation métadonnées** (rapide, depuis `floorsMap`/`floor_metadata`) de **hydratation complète** (triggers).
6. **Lazy-load triggers** au point d'usage réel (préparation d'instance / `getCurrentFloor` côté instance, déjà fait via `Floor(FloorData)`), supprimer tout préchargement triggers côté lobby.
7. Ajouter une API DB **batch** dans `DatabaseManager` : `CompletableFuture<Map<String,List<TriggerData>>> loadAllTriggers()` (+ impl). À utiliser **uniquement** si un warmup progressif est conservé.

### Phase 3 — Provisioning templates hors boot (corrige D, I)
8. Déplacer `generateTemplate()` vers le **write-path admin/éditeur** (à la sauvegarde d'un floor) et/ou un service de provisioning dédié.
9. Protéger la (re)génération massive par **lock distribué** Redisson pour qu'un seul acteur la fasse.

### Phase 4 — Cohérence & robustesse (corrige C, E, J, K)
10. **Lock de bootstrap** (`RLock <topic>:bootstrap-lock`) autour du rebuild Redis-vide (`RedisConfigLoader.java:33`), avec `tryLock` + fallback lecture si déjà tenu.
11. **Unifier le write-path** : événement d'invalidation versionné (déjà version/checksum présents sur `FloorData`) ; le republish au boot disparaît (Phase 1), les events ne partent que sur vrai changement.
12. `repopulateDashboardDungeonEntries` : idempotent + sous lock + async (J).
13. `reloadFloorFromRedis` : ne plus générer template ni republier (K) — juste rafraîchir le cache local.

### Phase 5 — Observabilité (corrige F)
14. Métriques de startup agrégées : durée totale, nb donjons/floors, source (Redis vs fallback BDD), nb templates générés, nb triggers chargés. Logguer un résumé unique + exposer au `CacheHealthMonitor`.

---

## 6. Nouvelle API DB nécessaire (Phase 2)

`DatabaseManager` (`spigot/.../database/DatabaseManager.java`) n'a **aucune** méthode batch triggers (seulement `loadTriggers(floorId)` `:27`). Ajouter :

```java
/** Charge tous les triggers groupés par floorId en une requête (anti N+1). */
CompletableFuture<Map<String, List<TriggerData>>> loadAllTriggers();
```

+ implémentation dans la classe concrète (chercher l'impl de `DatabaseManager` — interface seulement ici, voir `DatabaseFactory.createDatabase()` `Main.java:236`).

---

## 7. Décisions à trancher avant implémentation (me demander)

1. **Le lobby a-t-il besoin des triggers ?** (a priori non → lazy-load total côté instance). Confirmer.
2. **Garder un warmup progressif** des floors populaires, ou full lazy ?
3. **Où générer les templates** : à la sauvegarde admin/éditeur, à la création d'instance (premier usage), ou via un job de provisioning dédié ?
4. **Périmètre de la 1ʳᵉ passe** : Phase 1 seule (quick wins) ou Phase 1+2 ?

---

## 8. Critères d'acceptation (cibles)

- Boot lobby : **0** requête BDD triggers (full lazy) ou **1** batch (warmup) — plus jamais N×2.
- Boot lobby : **0** `syncTopic.publish` émis pendant l'hydratation.
- Boot lobby : **0** génération de template monde (déplacée hors boot).
- `onEnable` lobby ne bloque plus le main thread sur l'hydratation des floors.
- Avec N lobbies : la reconstruction Redis (cas flush) faite par **un seul** lobby (lock).
- Un résumé de startup chiffré est loggé.

---

## 9. Fichiers concernés (carte rapide)

- `spigot/.../configuration/RedisConfigLoader.java` — cœur du flux (`loadAllDungeonsFromRedis`, `loadFloor`, `repopulateDashboardDungeonEntries`, `reloadFloorFromRedis`).
- `spigot/.../model/Floor.java` — constructeurs (chargement triggers `:24,29,51-53`), `updateMap` `:70`, `generateTemplate` `:79`.
- `spigot/.../storage/DungeonService.java` — `syncFloor` (publish `:216`), `handleFloorUpdate` (dual-write `:341`), `getFloor`.
- `spigot/.../database/DatabaseManager.java` (+ impl via `DatabaseFactory`) — API batch triggers à ajouter.
- `spigot/.../database/DatabaseTriggersManager.java` — `loadTriggers` blocking `:53`.
- `spigot/.../instance/impl/CloudNetProvider.java` — `templateExists` `:200`, `createTemplate` `:210`.
- `spigot/.../Main.java` — `initializeLobbyServer` `:666`, `subscribeDashboardSyncChannel` `:682`, ordre d'init `:187-270`.
- `spigot/.../monitoring/CacheHealthMonitor.java` — point d'ancrage métriques (Phase 5).
