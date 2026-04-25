# Memory Labyrinth — Module CDC

> Module dynamique NextDungeon implémentant un type de donjon procédural inspiré de **Hades** & **Archero** : enchaînement de salles avec choix de portes, icônes de récompense visibles au choix, boss tous les 10 paliers, mode infini avec checkpoints.

---

## 1. Vue d'ensemble

### 1.1 Concept

Le **Memory Labyrinth** est un *type de donjon* fourni par le module `module-memory-labyrinth`. Il se présente comme un donjon NextDungeon classique exposant plusieurs **floors** correspondant à des niveaux de difficulté ; chaque floor enchaîne des **salles** générées de manière procédurale à partir d'un pool de salles pré-construites.

Le donjon peut se jouer **solo** ou **en groupe**.

### 1.2 Boucle de jeu

```
[Lobby] → [Salle 1] → choix porte (icônes visibles) → [Salle 2] → … → [Salle 9] → [Boss salle 10]
                                                                                       │
                                                                       (checkpoint dans Infinite)
                                                                                       ▼
                                                                       [Salle 11] → … → [Boss 20] → …
```

- **Lobby** : 1ʳᵉ salle, sans mob, sert de point d'entrée et permet (en Infinite) de choisir entre `Reprendre une save` et `Nouvelle partie`.
- **Salle de combat** (1 à 9 modulo 10) : contient des mobs ; les portes restent verrouillées tant que tous les mobs ne sont pas tués. À la fin, **deux portes** sont proposées avec **leur icône de récompense visible** ; le **leader de la party** choisit (style Hades).
- **Salle de blessing** (optionnelle, peut être insérée aléatoirement entre les salles de combat) : pas de mob, une porte avec une icône de bénédiction (non implémentée en v1) offrant un bonus pour les salle suivante.
- **Salle de boss** (toutes les 10 salles, soit `roomIndex % 10 == 0`) : une seule porte de sortie. Tuer le boss déclenche :
  - le revive d'un coéquipier mort (sélection via chat),
  - la sauvegarde du checkpoint (Infinite uniquement).

### 1.3 Floors / Difficultés

| Floor | Type | Nombre de salles | Save |
|---|---|---|---|
| `easy` | Fini | N (ex. 30) | non |
| `normal` | Fini | N (ex. 50) | non |
| `hard` | Fini | N (ex. 70) | non |
| `infinite` | **Infini** | ∞ | **oui** (à chaque boss) |

> Les valeurs `N` sont configurables par floor.

### 1.4 Sauvegardes (Infinite Labyrinth)

- Sauvegarde **automatique** à la fin de chaque combat de boss (toutes les 10 salles).
- La save mémorise : numéro de salle atteint, composition du groupe (UUIDs au démarrage), seed/route empruntée, modificateur de difficulté courant, **compteurs d'icônes accumulés**.
- **Reprise** : la save n'est rechargeable que si la **composition de groupe est identique à celle du démarrage** (mêmes UUIDs). Si un joueur du groupe initial ne revient jamais, la save reste bloquée — les autres peuvent toujours créer une nouvelle partie.
- **Mort totale** (solo : le joueur meurt / groupe : tout le groupe est mort sans pouvoir revive) → la save est invalidée et supprimée. Le joueur recommence à zéro.

### 1.5 Mort & Revive

- **Mort en cours de salle** : le joueur passe en mode fantôme (système existant `InstancePlayerDeathListener`).
- **Après un boss** : si un ou plusieurs joueurs sont morts, un message dans le chat propose la liste des joueurs morts ; un joueur vivant clique sur un pseudo pour le revive. **Un seul revive par boss**.
- Si **tout le groupe** meurt avant de tuer un boss → fail de l'instance, save Infinite supprimée, **loot calculé sur les icônes accumulées depuis la dernière save**.

### 1.6 Récompenses (Loot)

Le floor à une loot table avec des items et leur pourcentage de drop de base.
Inspiré de Hades : chaque salle de combat obtient une **icône de récompense** tirée aléatoirement, **affichée au-dessus de la porte** au moment du choix. Les icônes s'accumulent pendant la run et déterminent le loot **en fin de run**.
Pour le donjon infini, le loot n'est donné qu'à la fin de la run (mort totale ou sortie volontaire -> [On ne pourra plus recommencer cette run, elle sera "delete"]); les icônes accumulées sont persistées dans la save et survivent aux reprises.

Icônes disponibles en v1 :

| Icône | Effet sur le loot final |
|---|---|
| 🪙 `GOLD` | Multiplie l'or final |
| ✨ `BLESSING` | *Réservé — non implémenté en v1* |

> **Pas de drop au sol pendant la run.** Le loot n'est calculé et donné qu'à la **fin du run** (mort, complétion, ou sortie d'un Infinite). Les icônes accumulées sont **persistées dans la save** en Infinite et survivent aux save/resume.

### 1.7 Règle commune

> **Aucune porte ne s'ouvre tant que tous les mobs de la salle ne sont pas morts.** Exception : le lobby (pas de mobs).

---

## 2. Faisabilité dans NextDungeon

**Verdict : faisable comme module dynamique** sur le modèle de `module-cinematic` / `module-worldedit`. L'architecture existante couvre la majorité des besoins ; la logique propre au labyrinthe (boucle de salle, portes, loot) vit **entièrement dans le module**.

### 2.1 Ce que le cœur fournit déjà (réutilisable)

| Besoin | Brique existante |
|---|---|
| Chargement dynamique de modules | `ModuleLoader` + `NextDungeonModule` (`common/.../module/`) |
| Enregistrement de blocks Blockly | `ModuleBlockRegistry`, `ModuleBlockDescriptor`, `ModuleContext.fireTrigger()` |
| Triggers à base de régions | `RegionTrigger` (`spigot/.../workflow/trigger/impl/`) |
| Détection de mort de mob | `EntityDeathTrigger` + `InstanceMobKillListener` |
| Modèle de salle | `Step` (CuboidRegion nommé dans `FloorData`) |
| Instance de donjon multi-joueurs | `FloorInstance` (players, stats, lives, originInstances) |
| Système de fantôme & revive | `InstancePlayerDeathListener.revivePlayer()` |
| Persistence Mongo/MySQL | `DatabaseManager` (async, retry, transaction) |
| Sync cross-server | Redis via `DungeonService` |
| Profil joueur (stats, complétions) | `ProfileData` |
| Parties | `IDungeonParty` (interne + Alessio) |

### 2.2 Ce que le module doit ajouter

1. **Catalogue de salles (`RoomTemplate`)** — pool de salles pré-construites, avec géométrie, points de spawn, points de portes, type (combat/boss/lobby).
2. **Générateur procédural** — pioche 2 salles à chaque transition et **roll une icône** par salle.
3. **État de run** (`LabyrinthRun`) — décompte de salle, route empruntée, mobs vivants par salle, modificateur de difficulté, compteurs d'icônes.
4. **Système de portes** — verrou/déverrou en fonction des mobs vivants, hologramme d'icône au-dessus.
5. **Save Infinite** (`LabyrinthSave`) — stockée en DB, retrouvée par hash de composition de groupe.
6. **Calcul de loot** — RNG pondéré sur une `LootTable` par floor, à la fin du run uniquement.
7. **UI revive post-boss** + **UI lobby resume/new** (chat clickable).
8. **Hooks Blockly** — triggers / conditions / values exposés à l'éditeur (Option C : *aucune action exposée*).

### 2.3 Points d'attention

- Le système actuel n'a pas de notion de « salle qui se charge à la volée ». **v1 : salles statiques** dans un monde pré-fait (le picker téléporte les joueurs entre régions). Paste runtime via `module-worldedit` reste ouvert pour une v2.
- Les checkpoints n'existent pas dans `ProfileData` ; on ajoute une **table dédiée au module** (`labyrinth_saves`), qui garde le module isolé.
- Logique « mobs morts → portes ouvertes » : **primitive native du module**, pas de Blockly à câbler.

---

## 3. Architecture du module

### 3.1 Arborescence Maven

Calquée sur `module-cinematic/` :

```
module-memory-labyrinth/
├── pom.xml                                  ← parent: NextDungeon, Common en provided
└── src/main/java/fr/perrier/dungeons/module/labyrinth/
    ├── MemoryLabyrinthModule.java          ← implémente NextDungeonModule
    ├── manager/
    │   ├── LabyrinthRunManager.java        ← état des runs en cours (par instance)
    │   ├── RoomTemplateRegistry.java       ← chargement du pool de salles depuis DB
    │   ├── LabyrinthSaveManager.java       ← CRUD des saves Infinite
    │   └── LootTableRegistry.java          ← chargement des loot tables par floor
    ├── model/
    │   ├── RoomTemplate.java
    │   ├── RoomType.java                   ← LOBBY | COMBAT | BOSS
    │   ├── RewardIcon.java                 ← GOLD | BLESSING (TODO v2)
    │   ├── DoorChoice.java                 ← (RoomTemplate, RewardIcon) × 2
    │   ├── LabyrinthRun.java               ← runtime (currentRoom, route, iconCounts…)
    │   ├── LabyrinthSave.java              ← snapshot persistant Infinite
    │   ├── LootTable.java                  ← config loot par floor
    │   └── DifficultyModifier.java         ← scaling HP/dmg par tier
    ├── generator/
    │   ├── RoomPicker.java                 ← tirage des 2 salles (avec contraintes)
    │   └── IconRoller.java                 ← roll d'icône à la proposition de porte
    ├── lifecycle/                          ← orchestration interne (non exposée)
    │   ├── LabyrinthRoomLifecycle.java     ← entrée/spawn/cleared/transition
    │   ├── DoorController.java             ← lock/unlock + hologramme d'icône
    │   └── BossEncounterHandler.java       ← boss → revive UI → save → next
    ├── loot/
    │   └── LootCalculator.java             ← RNG en fin de run
    ├── trigger/                            ← exposés à Blockly
    │   ├── OnRoomEnteredTrigger.java
    │   ├── OnRoomClearedTrigger.java
    │   ├── OnBossKilledTrigger.java
    │   ├── OnDoorsProposedTrigger.java
    │   ├── OnRunEndedTrigger.java
    │   ├── OnCheckpointSavedTrigger.java
    │   └── OnSaveInvalidatedTrigger.java
    ├── condition/
    │   ├── IsBossRoomCondition.java
    │   ├── IsInfiniteFloorCondition.java
    │   └── HasResumableSaveCondition.java
    ├── value/
    │   ├── CurrentTierValue.java
    │   ├── CurrentRoomIndexValue.java
    │   ├── IconCountValue.java
    │   ├── GoldEarnedValue.java
    │   └── ItemsRolledValue.java
    └── ui/
        ├── DoorIconHologram.java           ← icône au-dessus des portes
        ├── ResumeOrNewPrompt.java          ← chat clickable (lobby Infinite, leader only)
        └── RevivePromptComponent.java      ← chat clickable (post-boss)
```

### 3.2 Interactions avec le cœur

```
┌─────────────────────────────────────────────┐
│  module-memory-labyrinth (JAR)              │
│  ┌─────────────────────────────────────┐    │
│  │ MemoryLabyrinthModule.onEnable()    │    │
│  │  • register triggers/conditions/     │   │
│  │    values (Option C - no actions)   │   │
│  │  • subscribe Bukkit listeners        │   │
│  │    (room transitions, mob death,     │   │
│  │    boss death, player death)         │   │
│  └─────────────────────────────────────┘    │
└────────────────────┬────────────────────────┘
                     ▼
        ModuleContext (host)
                     ▼
   ┌──────────────────────────────────┐
   │ Core NextDungeon                  │
   │  FloorInstance · RegionTrigger    │
   │  DatabaseManager · IDungeonParty  │
   │  ProfileData                      │
   └──────────────────────────────────┘
```

> Le module **ne reçoit jamais d'actions Blockly** — il pilote sa boucle nativement et émet des triggers que les workflows admin peuvent écouter pour greffer des effets de bord (cinematic, message, son…).

---

## 4. Modèle de données

### 4.1 `RoomTemplate`

```json
{
  "id": "combat_basic_01",
  "type": "COMBAT",
  "worldId": "labyrinth_pool",
  "region": { "min": [0, 60, 0], "max": [16, 80, 16] },
  "playerSpawn": { "x": 8, "y": 61, "z": 2 },
  "doors": [
    { "id": "exit_left",  "anchor": [3,  61, 14] },
    { "id": "exit_right", "anchor": [13, 61, 14] }
  ],
  "mobSpawns": [
    { "mobId": "skeleton_t1", "x": 5,  "y": 61, "z": 8, "count": 2 },
    { "mobId": "zombie_t1",   "x": 11, "y": 61, "z": 8, "count": 3 }
  ],
  "tags": ["t1", "combat", "easy", "normal"],
  "fixedIcon": null
}
```

- `type` : `LOBBY` | `COMBAT` | `BOSS`
- `tags` : utilisés par le picker pour filtrer (tier de difficulté + nom de floor + rôle).
- `mobSpawns[].mobId` : référence à un mob MMOCore/MythicMobs existant. Le module applique le scaling tier au runtime (voir §6.4).
- `fixedIcon` :
  - `null` pour `COMBAT` → le module roll une icône au moment de la proposition de porte (Q5.3).
  - `NONE` pour `LOBBY` (forcé).
  - `RewardIcon` non-null pour `BOSS` (Q5.6 — fixe par boss).

### 4.2 `LabyrinthRun` (runtime, en mémoire)

```java
class LabyrinthRun {
  UUID instanceId;
  String floorId;                                  // easy | normal | hard | infinite
  int currentRoomIndex;                            // 0 = lobby, 1..N
  RoomTemplate currentRoom;
  RewardIcon currentRoomIcon;                      // rolled for COMBAT, fixed for BOSS, NONE for LOBBY
  List<RoomTemplate> routeHistory;
  DoorChoice pendingChoice;                        // null si pas en fin de salle
  Map<UUID, Integer> aliveMobsByRoom;
  Set<UUID> deadPlayers;                           // résolus à chaque boss
  DifficultyModifier currentModifier;
  long seed;
  Map<RewardIcon, Integer> iconCounts;             // accumulé pendant la run
  Set<UUID> initialPlayerUuids;                    // figé au démarrage (Q2 = A)
}
```

### 4.3 `LabyrinthSave` (persisté, Infinite uniquement)

```json
{
  "id": "uuid",
  "floorId": "infinite",
  "partyHash": "sha256(sorted(initialPlayerUuids))",
  "playerUuids": ["uuid1", "uuid2"],
  "lastBossClearedRoom": 30,
  "difficultyTier": 3,
  "seed": 1234567890,
  "iconCounts": { "GOLD": 7 },
  "checksum": "sha256(payload)",
  "createdAt": "2026-04-25T12:00:00Z",
  "updatedAt": "2026-04-25T12:35:00Z"
}
```

- `partyHash` est calculé sur **la composition initiale du run** (Q2 = A). Si un joueur du groupe initial ne revient jamais, la save reste inaccessible — pas de reset automatique, les joueurs créent une nouvelle partie.
- `iconCounts` est **persisté** : les icônes accumulées survivent aux save/resume (Q5.4).
- `lastBossClearedRoom` est toujours un multiple de 10.
- `checksum` anti-tamper recalculé à chaque update.

### 4.4 `LootTable` (par floor)

```json
{
  "floorId": "infinite",
  "baseGold": 100,
  "goldPerIcon": 0.15,
  "baseItemRolls": 1,
  "items": [
    { "itemId": "epic_sword",       "weight": 5, "minTier": 1 },
    { "itemId": "legendary_armor",  "weight": 1, "minTier": 3 }
  ]
}
```

- `goldPerIcon` : multiplicateur appliqué par icône `GOLD` accumulée.
- `items[].minTier` : utilisé en Infinite pour gater les drops par palier de difficulté.
- Pool **séparé par floor** (Q5.2) — `easy` ne drop pas les items légendaires d'`infinite`.

### 4.5 Schéma DB

#### MySQL

```sql
CREATE TABLE IF NOT EXISTS labyrinth_rooms (
    id VARCHAR(64) PRIMARY KEY,
    type VARCHAR(16) NOT NULL,
    payload_json MEDIUMTEXT NOT NULL,
    tags VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS labyrinth_saves (
    id VARCHAR(36) PRIMARY KEY,
    floor_id VARCHAR(64) NOT NULL,
    party_hash CHAR(64) NOT NULL,
    payload_json MEDIUMTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_party (party_hash, floor_id)
);

CREATE TABLE IF NOT EXISTS labyrinth_loot_tables (
    floor_id VARCHAR(64) PRIMARY KEY,
    payload_json MEDIUMTEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### MongoDB

- `labyrinth_rooms` — `{ _id, type, payload, tags, updated_at }`
- `labyrinth_saves` — `{ _id, floor_id, party_hash, payload, updated_at }` + index sur `(party_hash, floor_id)`
- `labyrinth_loot_tables` — `{ _id (= floor_id), payload, updated_at }`

---

## 5. Hooks Blockly exposés (Option C)

> **Aucune action.** Le module pilote sa boucle nativement. Les workflows admin **écoutent** les triggers ci-dessous pour greffer des effets de bord (cinematic, son, titre, message global, etc.).

### 5.1 Triggers

| Block ID | Variables exposées au workflow |
|---|---|
| `labyrinth.on_room_entered` | `roomIndex`, `roomType`, `rewardIcon`, `playerUuid` |
| `labyrinth.on_room_cleared` | `roomIndex`, `rewardIcon`, `clearTimeMs` |
| `labyrinth.on_doors_proposed` | `iconLeft`, `iconRight`, `nextIsBoss` (bool) |
| `labyrinth.on_boss_killed` | `roomIndex`, `tier`, `playersAlive` |
| `labyrinth.on_run_ended` | `success` (bool), `goldEarned`, `itemsRolled` (list), `iconCounts`, `tier` |
| `labyrinth.on_checkpoint_saved` | `roomIndex`, `tier` |
| `labyrinth.on_save_invalidated` | `reason` (`ALL_DEAD`) |

### 5.2 Conditions

| Block ID | Description |
|---|---|
| `labyrinth.is_boss_room` | True si `currentRoomIndex % 10 == 0` (et > 0) |
| `labyrinth.is_infinite_floor` | True si floor courant = `infinite` |
| `labyrinth.has_resumable_save` | True si une save existe pour la compo actuelle |

### 5.3 Values

| Block ID | Description |
|---|---|
| `labyrinth.current_tier` | Palier de difficulté actuel |
| `labyrinth.current_room_index` | Numéro de salle courant |
| `labyrinth.icon_count` | Compteur pour une icône (input dropdown : GOLD, BLESSING) |
| `labyrinth.gold_earned` | Or final calculé (valide uniquement dans `on_run_ended`) |
| `labyrinth.items_rolled` | Liste d'items rollés (idem) |

> **Couleur/catégorie suggérée** : `#3F51B5` ("Memory Labyrinth").

---

## 6. Flow d'exécution end-to-end

### 6.1 Démarrage d'un run

```
Joueur lance /dungeon … memory_labyrinth infinite
  → FloorInstance créée (cœur)
  → MemoryLabyrinthModule détecte une instance de type labyrinth
  → LabyrinthRunManager.startRun(instance)
      ├── initialPlayerUuids = current party
      ├── Si floor == infinite ET has_resumable_save :
      │     ResumeOrNewPrompt → leader clique (Q1 = A)
      │       ├── "Reprendre" → charge save, applique tier, skip à room (lastBossClearedRoom + 1)
      │       └── "Nouvelle" → invalide save existante, démarre fresh
      └── TP joueurs au lobby (RoomType.LOBBY, sans mob, 1 porte)
```

### 6.2 Boucle de salle

```
Entrée salle X
  → fire labyrinth.on_room_entered
  → spawn mobs (mobSpawns + tier scaling)         (incrémente aliveMobs[X])
  → DoorController.lock(X)
  → InstanceMobKillListener (cœur) → décrémente aliveMobs[X]
      → quand aliveMobs[X] == 0 :
          fire labyrinth.on_room_cleared
          → DoorController.unlock(X)
          → RoomPicker.pickNext(currentRoomIndex)
              ├── Si X+1 % 10 == 0 (prochaine = boss) → 1 porte vers BOSS room
              └── Sinon                              → 2 portes COMBAT
          → IconRoller.roll() pour chaque porte (sauf boss = fixedIcon)
          → DoorIconHologram.show() au-dessus de chaque porte
          → fire labyrinth.on_doors_proposed
  → Joueur traverse la porte choisie (RegionTrigger sur la zone porte)
      → currentRoomIndex++
      → iconCounts[chosenIcon]++
      → TP au playerSpawn de la salle suivante
```

### 6.3 Boss & checkpoint

```
Salle boss : currentRoomIndex % 10 == 0
  → spawn boss (RoomTemplate.type=BOSS, fixedIcon visible mais pas comptabilisé)
  → boss meurt → fire labyrinth.on_boss_killed
      ├── BossEncounterHandler.run() :
      │     ├── Si deadPlayers non-vide ET mode groupe :
      │     │     RevivePromptComponent dans le chat
      │     │       → 1er clic d'un joueur vivant → revive ce target
      │     │       → réutilise InstancePlayerDeathListener.revivePlayer()
      │     │     (1 seul revive par boss)
      │     ├── currentModifier.tier++
      │     └── Si floor == infinite :
      │           LabyrinthSaveManager.upsert(run)
      │           → fire labyrinth.on_checkpoint_saved
      └── Une seule porte vers la salle suivante (icône rollée si COMBAT)
```

### 6.4 Mort & invalidation

```
Joueur meurt → InstancePlayerDeathListener (cœur) → ghost
deadPlayers.add(uuid)

Tous morts (avant boss) :
  → FloorInstance.fail()
  → fire labyrinth.on_run_ended (success=false, loot calculé sur iconCounts actuels)
  → Si floor == infinite :
        LabyrinthSaveManager.delete(saveId)
        → fire labyrinth.on_save_invalidated (reason=ALL_DEAD)
```

### 6.5 Fin de run & loot

```
Run terminée (succès finite OU mort totale OU sortie volontaire infinite)
  → LootCalculator.compute(iconCounts, tier, lootTable)
        gold      = baseGold × (1 + goldPerIcon × iconCounts.GOLD) × tierMultiplier
        itemRolls = baseItemRolls
        for r in 0..itemRolls :
            pickWeightedFromLootTable(filter: minTier <= currentTier)
  → distribuer à chaque joueur (loot individuel — chacun son roll)
  → fire labyrinth.on_run_ended
        variables: goldEarned, itemsRolled, iconCounts, tier, success
```

### 6.6 Scaling de mobs (Q4 = C)

```
Au spawn d'un mob :
  attach metadata { instanceId, tier }
  → setMaxHealth(base × modifier.hpMult(tier))
  → setAttackDamage(base × modifier.dmgMult(tier))
DifficultyModifier table (proposition initiale, à équilibrer) :
  tier 1 : ×1.0 / ×1.0
  tier 2 : ×1.3 / ×1.15
  tier 3 : ×1.7 / ×1.30
  tier N : ×(1 + 0.3·(N-1)) / ×(1 + 0.15·(N-1))
```

---

## 7. Spec API (extension du panel)

| Méthode | Endpoint | Rôle |
|---|---|---|
| GET | `/labyrinth/rooms` | Liste des `RoomTemplate` |
| GET | `/labyrinth/rooms/{id}` | Détail d'une salle |
| POST | `/labyrinth/rooms` | Créer |
| PUT | `/labyrinth/rooms/{id}` | Modifier |
| DELETE | `/labyrinth/rooms/{id}` | Supprimer |
| GET | `/labyrinth/loot-tables/{floorId}` | Récupérer la loot table d'un floor |
| PUT | `/labyrinth/loot-tables/{floorId}` | Upsert |
| GET | `/labyrinth/saves?partyHash=…` | Lister les saves (debug/admin) |

> Suivre le pattern existant des endpoints `/cinematics`.

---

## 8. Sécurité, perf, edge cases

- **Composition de groupe** : `partyHash = sha256(sorted(initialPlayerUuids))`. Composition figée au démarrage de la run.
- **Cross-server** : la save est stockée en DB partagée ; récupérable depuis n'importe quel serveur.
- **Concurrence** : `LabyrinthRun` est en mémoire sur le serveur de l'instance ; seule la `LabyrinthSave` est synchronisée via DB.
- **Limite de pool** : prévoir au moins **8 salles `COMBAT` distinctes par tier** pour limiter les répétitions visibles. Picker logge un warning si pool trop maigre.
- **Cas dégénéré** : si le pool ne contient qu'une salle d'un type requis, `RoomPicker` peut proposer la même salle des deux côtés (warning, fallback acceptable).
- **TTL save Infinite** : 30 jours d'inactivité → purge automatique en arrière-plan.
- **Anti-cheat checkpoint** : `LabyrinthSave.checksum = sha256(payload sans le champ checksum)`. Si modifiée à la main, refus de reprise.
- **Save bloquée** : si un joueur du groupe initial ne revient jamais, la save reste inaccessible. **Pas de reset admin** — les joueurs créent une nouvelle partie. Comportement assumé.
- **Loot vs déconnexion** : si un joueur déconnecté n'est plus en ligne au moment du `on_run_ended`, son loot est mis en pending sur son `ProfileData` et délivré au prochain login.

---

## 9. Roadmap / découpage

| Phase | Lot | Contenu |
|---|---|---|
| **P0** | Squelette module | `pom.xml` parent, `MemoryLabyrinthModule.onEnable`, scaffolding packages |
| **P1** | Modèles + DB | `RoomTemplate`, `LabyrinthSave`, `LootTable`, DAO MySQL & Mongo, migrations |
| **P2** | Pool & RoomPicker | Registry chargé au boot, picker avec contraintes (lobby/boss/combat) |
| **P3** | Runtime de salle | `LabyrinthRunManager`, spawn mobs (avec scaling), tracking `aliveMobs` |
| **P4** | Portes & navigation | `DoorController` (lock/unlock), `IconRoller`, `DoorIconHologram` |
| **P5** | Boss & revive UI | `BossEncounterHandler`, `RevivePromptComponent` chat-clickable |
| **P6** | Save Infinite | `LabyrinthSaveManager`, `partyHash`, `checksum`, `ResumeOrNewPrompt` |
| **P7** | Difficulté infinie | `DifficultyModifier` (HP/dmg scaling par tier) appliqué au spawn |
| **P8** | Loot & end-of-run | `LootCalculator`, distribution individuelle, pending offline |
| **P9** | Hooks Blockly | Tous les triggers/conditions/values exposés au panel (Option C) |
| **P10** | Endpoints panel | CRUD `RoomTemplate` + `LootTable` + listing saves |
| **P11** | QA & polish | Tests bout-en-bout solo + groupe, doc utilisateur, logs admin |

---

## 10. Hors-scope (v1)

- **Icône `BLESSING`** : enum réservée mais *non implémentée en v1*.
- **Génération de salles à la volée** (paste de schématiques runtime). À évaluer en v2 via `module-worldedit`.
- **Mode PvP** dans le labyrinthe.
- **Marketplace de pools** (partage de `RoomTemplate` entre serveurs).
- **Skill tree / méta-progression** (mirror of night à la Hades) — possible plus tard via un autre module abonné à `on_boss_killed` / `on_run_ended`.
- **Reset admin de save** — pas de besoin identifié, on s'en passe.

---

## 11. Décisions actées

| # | Question | Décision |
|---|---|---|
| Q1 | Lobby Infinite : qui décide reprendre vs nouvelle ? | **Leader de la party** |
| Q2 | `partyHash` basé sur quelle composition ? | **Composition au démarrage** (figée) |
| Q3 | Pool de `RoomTemplate` par floor ou partagé ? | **Pool unique + tags** (`tier`, `floor`, `combat`/`boss`/`lobby`) |
| Q4 | Mobs : Mythic/MMO ref vs descriptor ? | **Hybride** : ref `mobId` + scaling auto via `DifficultyModifier` |
| Q5 | Récompenses : Blockly vs primitive ? | **Système d'icônes natif** : roll par salle, accumulation, RNG sur `LootTable` en fin de run, distribution individuelle |
| Q5.1 | Icônes disponibles | **`GOLD`** (v1), **`BLESSING`** (réservée, non implémentée) |
| Q5.2 | Pool d'items | **Un par floor** |
| Q5.3 | Icône d'une salle COMBAT | **Tirée aléatoirement** au moment de la proposition de porte |
| Q5.4 | Loot pendant la run | **Non** — calculé et distribué uniquement en fin de run ; `iconCounts` persistés dans la save |
| Q5.5 | Icône lobby | **NONE** |
| Q5.6 | Icône boss | **Fixe**, définie dans la `RoomTemplate` du boss |

claude --resume 27d06130-53ae-4ded-a081-4031b8a7ee54