# Dashboard UI Mockup

## Visual Description

The NextDungeon Dashboard follows the same visual style as the existing Blockly web editor, featuring a modern, gradient-based design.

### Color Scheme
- **Background**: Linear gradient from `#667eea` (blue-purple) to `#764ba2` (deep purple)
- **Cards**: White with 95% opacity (`rgba(255,255,255,0.95)`)
- **Primary Accent**: Gradient from `#667eea` to `#764ba2`
- **Success**: Green gradient (`#4caf50` to `#66bb6a`)
- **Warning**: Orange gradient (`#ff9800` to `#ffb74d`)
- **Info**: Blue gradient (`#2196f3` to `#42a5f5`)
- **Danger**: Red gradient (`#f44336` to `#ef5350`)

### Layout Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                     📊 NextDungeon Dashboard                    │
│          Vue d'ensemble des dungeons, instances et éditeurs     │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│     123      │  │      45      │  │       8      │
│ 🏰 Floors    │  │ 🎮 Instances │  │ ✏️ Sessions  │
│    Total     │  │   Actives    │  │  d'Édition   │
└──────────────┘  └──────────────┘  └──────────────┘

┌────────────────────────┐  ┌────────────────────────┐
│ 📈 Distribution des    │  │ 🔧 Floors les Plus     │
│    Instances par Floor │  │       Édités           │
│                        │  │                        │
│   [BAR CHART]          │  │   [DOUGHNUT CHART]     │
│                        │  │                        │
└────────────────────────┘  └────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 🏰 Liste des Floors                                             │
├─────────────────────────────────────────────────────────────────┤
│ ID       │ Nom          │ Description  │ Instances │ Sessions  │
│──────────┼──────────────┼──────────────┼───────────┼───────────│
│ floor_1  │ First Floor  │ Entry level  │    [5]    │    [2]    │
│ floor_2  │ Boss Arena   │ Final battle │    [3]    │    [1]    │
│ ...      │ ...          │ ...          │    ...    │    ...    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 🎮 Instances Actives                                            │
├─────────────────────────────────────────────────────────────────┤
│ Instance ID           │ Floor ID │ Statut  │ Joueurs           │
│───────────────────────┼──────────┼─────────┼───────────────────│
│ 123e4567-e89b-12d3... │ floor_1  │ [Ready] │ [3 joueur(s)]     │
│ 987f6543-c21a-98b7... │ floor_2  │ [Prep.] │ [1 joueur(s)]     │
│ ...                   │ ...      │ ...     │ ...               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ ✏️ Sessions d'Édition Actives                                   │
├─────────────────────────────────────────────────────────────────┤
│ Session ID  │ Dungeon │ Floor  │ Éditeur │ Serveur │ Créé le   │
│─────────────┼─────────┼────────┼─────────┼─────────┼───────────│
│ floor1-a1b2 │ Main    │ floor1 │ Player1 │ spigot1 │ 10:30:45  │
│ floor2-c3d4 │ Main    │ floor2 │ Player2 │ spigot2 │ 11:15:22  │
│ ...         │ ...     │ ...    │ ...     │ ...     │ ...       │
└─────────────────────────────────────────────────────────────────┘

                                                    ┌──────────────┐
                                                    │ 🔄 Actualiser│
                                                    └──────────────┘
```

### Interactive Elements

1. **Floor Row Click**: Clicking any row in the Floors table opens a modal overlay
2. **Modal Overlay**: 
   ```
   ┌────────────────────────────────────────┐
   │ Configuration: First Floor         [×] │
   ├────────────────────────────────────────┤
   │ Informations Générales                 │
   │ ┌────────────────────────────────────┐ │
   │ │ ID: floor_1                        │ │
   │ │ Nom: First Floor                   │ │
   │ │ Description: Entry level dungeon   │ │
   │ └────────────────────────────────────┘ │
   │                                        │
   │ Configuration du Monde                 │
   │ ┌────────────────────────────────────┐ │
   │ │ {                                  │ │
   │ │   "world": "dungeon_world",        │ │
   │ │   "spawn": { "x": 0, "y": 64 }     │ │
   │ │ }                                  │ │
   │ └────────────────────────────────────┘ │
   │                                        │
   │ [More sections...]                     │
   └────────────────────────────────────────┘
   ```

3. **Refresh Button**: Fixed position in bottom-right, floating button with gradient background

### Charts

1. **Instance Distribution Chart** (Bar Chart)
   - X-axis: Floor names
   - Y-axis: Number of instances
   - Bars: Blue-purple gradient
   - Shows which floors have the most active instances

2. **Most Edited Floors Chart** (Doughnut Chart)
   - Segments: Different colored slices for each floor
   - Legend: Right side showing floor names
   - Shows top 10 most frequently edited floors

### Responsive Design

- Cards adapt to screen size using CSS Grid
- Tables are scrollable on smaller screens
- Modal is centered and responsive
- Charts resize automatically

### Typography

- **Headers**: Segoe UI, 2.5em for main title
- **Section Headers**: 1.8em with bottom border
- **Body Text**: 1em Segoe UI
- **Code/IDs**: Monospace font with background

### Badges

Status indicators use colored badges:
- **Success (Green)**: Ready instances, active counts > 0
- **Warning (Orange)**: Preparing instances
- **Info (Blue)**: General counts, player counts
- **Danger (Red)**: Error states (not shown in normal operation)

### Animations

- Cards: Slight elevation on hover (`transform: translateY(-5px)`)
- Buttons: Elevation on hover with enhanced shadow
- Modal: Fade-in animation on open
- Status updates: Smooth transitions every 30 seconds

### Accessibility

- Semantic HTML structure
- Proper heading hierarchy
- Color contrast meets WCAG standards
- Keyboard navigation support for modal close
