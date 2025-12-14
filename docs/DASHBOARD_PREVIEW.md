# Dashboard Visual Preview

## Browser Window Preview

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ ← → ⟳  http://localhost:7734/dashboard                                   ☰ ≡ │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ╔═══════════════════════════════════════════════════════════════════════╗  │
│  ║                    📊 NextDungeon Dashboard                           ║  │
│  ║     Vue d'ensemble des dungeons, instances et éditeurs actifs         ║  │
│  ╚═══════════════════════════════════════════════════════════════════════╝  │
│                                                                              │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐ │
│  │                     │  │                     │  │                     │ │
│  │        123          │  │         45          │  │          8          │ │
│  │    🏰 Floors        │  │   🎮 Instances      │  │    ✏️ Sessions      │ │
│  │      Total          │  │     Actives         │  │     d'Édition       │ │
│  │                     │  │                     │  │                     │ │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘ │
│                                                                              │
│  ┌──────────────────────────────────┐  ┌──────────────────────────────────┐│
│  │ 📈 Distribution des Instances    │  │ 🔧 Floors les Plus Édités        ││
│  │      par Floor                   │  │                                  ││
│  │                                  │  │        ╱─────╲                   ││
│  │      ┃                           │  │       ╱       ╲                  ││
│  │      ┃                           │  │      ┃   🟣   ┃                  ││
│  │  ┃   ┃        ┃                  │  │      ┃  🔵 🟠  ┃                  ││
│  │  ┃   ┃   ┃    ┃   ┃              │  │       ╲   🟢  ╱                   ││
│  │  ┃   ┃   ┃    ┃   ┃              │  │        ╲─────╱                    ││
│  │ ─┸───┸───┸────┸───┸──────────    │  │  ◼ Floor 1  ◼ Floor 2            ││
│  │ F1  F2  F3   F4  F5              │  │  ◼ Floor 3  ◼ Floor 4            ││
│  └──────────────────────────────────┘  └──────────────────────────────────┘│
│                                                                              │
│  ╔═══════════════════════════════════════════════════════════════════════╗  │
│  ║ 🏰 Liste des Floors                                                   ║  │
│  ╠═════════╦══════════════╦════════════════╦═══════════╦═════════════════╣  │
│  ║ ID      ║ Nom          ║ Description    ║ Instances ║ Sessions        ║  │
│  ╠═════════╬══════════════╬════════════════╬═══════════╬═════════════════╣  │
│  ║ floor_1 ║ First Floor  ║ Entry level... ║    ⟨ 5 ⟩  ║    ⟨ 2 ⟩        ║  │
│  ║ floor_2 ║ Boss Arena   ║ Final battle.. ║    ⟨ 3 ⟩  ║    ⟨ 1 ⟩        ║  │
│  ║ floor_3 ║ Secret Room  ║ Hidden area... ║    ⟨ 0 ⟩  ║    ⟨ 0 ⟩        ║  │
│  ║ floor_4 ║ Puzzle Hall  ║ Mind games.... ║    ⟨ 2 ⟩  ║    ⟨ 3 ⟩        ║  │
│  ╚═════════╩══════════════╩════════════════╩═══════════╩═════════════════╝  │
│                                                                              │
│  ╔═══════════════════════════════════════════════════════════════════════╗  │
│  ║ 🎮 Instances Actives                                                  ║  │
│  ╠═══════════════════════╦══════════╦═══════════╦══════════════════════╣  │
│  ║ Instance ID           ║ Floor ID ║ Statut    ║ Joueurs              ║  │
│  ╠═══════════════════════╬══════════╬═══════════╬══════════════════════╣  │
│  ║ 123e4567-e89b...      ║ floor_1  ║ ⟨ Ready ⟩ ║ ⟨ 3 joueur(s) ⟩      ║  │
│  ║ 987f6543-c21a...      ║ floor_2  ║ ⟨ Prep. ⟩ ║ ⟨ 1 joueur(s) ⟩      ║  │
│  ║ 456a7890-d34e...      ║ floor_1  ║ ⟨ Ready ⟩ ║ ⟨ 4 joueur(s) ⟩      ║  │
│  ╚═══════════════════════╩══════════╩═══════════╩══════════════════════╝  │
│                                                                              │
│  ╔═══════════════════════════════════════════════════════════════════════╗  │
│  ║ ✏️ Sessions d'Édition Actives                                         ║  │
│  ╠═════════════╦═════════╦════════╦═════════╦═════════╦══════════════════╣  │
│  ║ Session ID  ║ Dungeon ║ Floor  ║ Éditeur ║ Serveur ║ Créé le          ║  │
│  ╠═════════════╬═════════╬════════╬═════════╬═════════╬══════════════════╣  │
│  ║ floor1-a1b2 ║ Main    ║ floor1 ║ Player1 ║ spigot1 ║ 14/12 10:30:45   ║  │
│  ║ floor2-c3d4 ║ Main    ║ floor2 ║ Player2 ║ spigot2 ║ 14/12 11:15:22   ║  │
│  ║ floor4-e5f6 ║ Bonus   ║ floor4 ║ Admin   ║ spigot1 ║ 14/12 12:00:00   ║  │
│  ╚═════════════╩═════════╩════════╩═════════╩═════════╩══════════════════╝  │
│                                                                              │
│                                                           ┌──────────────┐   │
│                                                           │ 🔄 Actualiser│   │
│                                                           └──────────────┘   │
└──────────────────────────────────────────────────────────────────────────────┘
```

## Modal Overlay (When Floor is Clicked)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│    ╔═════════════════════════════════════════════════════════════════╗      │
│    ║ Configuration: First Floor                                   [×]║      │
│    ╠═════════════════════════════════════════════════════════════════╣      │
│    ║                                                                 ║      │
│    ║  Informations Générales                                         ║      │
│    ║  ┌──────────────────────────────────────────────────────────┐  ║      │
│    ║  │ ID: floor_1                                              │  ║      │
│    ║  │ Nom: First Floor                                         │  ║      │
│    ║  │ Description: Entry level dungeon for new players        │  ║      │
│    ║  └──────────────────────────────────────────────────────────┘  ║      │
│    ║                                                                 ║      │
│    ║  Configuration du Monde                                         ║      │
│    ║  ┌──────────────────────────────────────────────────────────┐  ║      │
│    ║  │ {                                                        │  ║      │
│    ║  │   "world": "dungeon_world",                              │  ║      │
│    ║  │   "spawn": {                                             │  ║      │
│    ║  │     "x": 0,                                              │  ║      │
│    ║  │     "y": 64,                                             │  ║      │
│    ║  │     "z": 0                                               │  ║      │
│    ║  │   },                                                     │  ║      │
│    ║  │   "difficulty": "NORMAL"                                 │  ║      │
│    ║  │ }                                                        │  ║      │
│    ║  └──────────────────────────────────────────────────────────┘  ║      │
│    ║                                                                 ║      │
│    ║  Prérequis                                                      ║      │
│    ║  ┌──────────────────────────────────────────────────────────┐  ║      │
│    ║  │ {                                                        │  ║      │
│    ║  │   "minLevel": 1,                                         │  ║      │
│    ║  │   "maxPlayers": 4                                        │  ║      │
│    ║  │ }                                                        │  ║      │
│    ║  └──────────────────────────────────────────────────────────┘  ║      │
│    ║                                                                 ║      │
│    ║  Règles                                                         ║      │
│    ║  ┌──────────────────────────────────────────────────────────┐  ║      │
│    ║  │ {                                                        │  ║      │
│    ║  │   "pvp": false,                                          │  ║      │
│    ║  │   "respawn": true,                                       │  ║      │
│    ║  │   "lives": 3                                             │  ║      │
│    ║  │ }                                                        │  ║      │
│    ║  └──────────────────────────────────────────────────────────┘  ║      │
│    ║                                                                 ║      │
│    ║  Étapes                                                         ║      │
│    ║  ┌──────────────────────────────────────────────────────────┐  ║      │
│    ║  │ 5 étape(s) configurée(s)                                 │  ║      │
│    ║  └──────────────────────────────────────────────────────────┘  ║      │
│    ║                                                                 ║      │
│    ║  Triggers                                                       ║      │
│    ║  ┌──────────────────────────────────────────────────────────┐  ║      │
│    ║  │ 12 trigger(s) configuré(s)                               │  ║      │
│    ║  └──────────────────────────────────────────────────────────┘  ║      │
│    ║                                                                 ║      │
│    ╚═════════════════════════════════════════════════════════════════╝      │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

## Color Scheme Reference

**Background Gradient**: Purple-blue gradient (#667eea → #764ba2)

**Badge Colors**:
- 🟢 Green (Success): Active instances, ready state
- 🟠 Orange (Warning): Preparing state, active sessions
- 🔵 Blue (Info): Counts and general information
- 🔴 Red (Danger): Error states (rarely shown)

**Chart Colors**:
- Bar Chart: Blue-purple gradient matching theme
- Doughnut Chart: Multi-color palette (purple, blue, orange, green, etc.)

## Responsive Behavior

**Desktop (1400px+)**
```
[Summary Cards: 3 columns]
[Charts: 2 columns]
[Tables: Full width]
```

**Tablet (768px)**
```
[Summary Cards: 2 columns]
[Charts: 1 column stacked]
[Tables: Scrollable horizontally]
```

**Mobile (375px)**
```
[Summary Cards: 1 column stacked]
[Charts: 1 column stacked]
[Tables: Scrollable with pinned first column]
```

## Interactive Elements

1. **Hover States**: Cards slightly elevate, tables highlight rows
2. **Click Events**: Floor rows open modal, refresh button updates data
3. **Auto-Refresh**: Visual indicator not shown, happens silently every 30s
4. **Loading States**: "Chargement..." shown while fetching data
5. **Empty States**: "Aucun(e) ... trouvé(e)" when no data available

## Accessibility Features

- Semantic HTML structure (headers, nav, main, section)
- ARIA labels for interactive elements
- Keyboard navigation support (Tab, Enter, Escape)
- High contrast text (black on white cards)
- Scalable font sizes (em units)
- Screen reader friendly table structure
