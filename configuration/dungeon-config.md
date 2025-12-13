---
description: Here is an example configuration for a dungeon with multiple floor and steeps.
icon: gears
---

# Dungeon Config

```yaml
# ╔════════════════════════════════════════════════════════════════════════╗
# ║                       Dungeons Plugin Configuration                    ║
# ║         This file defines one dungeon and its floors configuration.    ║
# ╚════════════════════════════════════════════════════════════════════════╝

dungeon:
  id: "example"
  name: "Dungeon Exemple"
  floors:
    - id: "floor1"
      name: "Les Cryptes Oubliées"
      description: "Explorez les anciennes cryptes\nremplies de mystères et de dangers."

      world:
        difficulty: "normal"
        spawn: { x: 0, y: 100, z: 0 }

      requirements:
        retry_cooldown: "15m" # Time before retrying
        required_floor: [] # List of required floor(s) (id)
        minimum_level: 0
        party:
          min_size: 2
          max_size: 25
        required_items:
          - "Old Key"
        forbidden_items:
          - "Magic Wand"

      rules:
        death_ban: "15m"
        gamemode: "SURVIVAL"
        allow_flight: false

      steps:
        - id: "step1"
          name: "Entrée des Catacombes"
          region:
            pos1: { x: 0, y: 0, z: 0 }
            pos2: { x: 0, y: 0, z: 0 }

        - id: "step2"
          name: "Galerie Sombre"
          region:
            pos1: { x: 0, y: 0, z: 0 }
            pos2: { x: 0, y: 0, z: 0 }

        - id: "step3"
          name: "Salle Piégée"
          region:
            pos1: { x: 0, y: 0, z: 0 }
            pos2: { x: 0, y: 0, z: 0 }

        - id: "step4"
          name: "Boss Intermédiaire"
          region:
            pos1: { x: 0, y: 0, z: 0 }
            pos2: { x: 0, y: 0, z: 0 }

        - id: "step5"
          name: "Salle du Seigneur Fantôme"
          region:
            pos1: { x: 0, y: 0, z: 0 }
            pos2: { x: 0, y: 0, z: 0 }
    - id: "floor2"
      name: "Le Labyrinthe des Ombres"
      description: "Naviguez à travers un labyrinthe\nrempli de pièges et de créatures."

      world:
        difficulty: "hard"
        spawn: { x: 0, y: 100, z: 0 }

      requirements:
        retry_cooldown: "30m" # Time before retrying
        required_floor: ["example_floor1"] # List of required floor(s) (id)
        minimum_level: 2
        party:
          min_size: 3
          max_size: 20
        required_items:
          - "Silver Key"
        forbidden_items:
          - "Fire Sword"

      rules:
        death_ban: "30m"
        gamemode: "SURVIVAL"
        allow_flight: false

      steps:
        - id: "step1"
          name: "Entrée du Labyrinthe"
          region:
            pos1: { x: 0, y: 0, z: 0 }
            pos2: { x: 0, y: 0, z: 0 }

        - id: "step2"
          name: "Couloirs Sinueux"
          region:
            pos1: { x: 0, y: 0, z: 0 }
            pos2: { x: 0, y: 0, z: 0 }

        - id: "step3"
          name: "Salle des Illusions"
          region:
            pos1: { x: 0, y: 0, z: 0 }
            pos2: { x: 0, y: 0, z: 0 }

        - id: "step4"
          name: "Boss du Labyrinthe"
          region:
            pos1: { x: 0, y: 0, z: 0 }
            pos2: { x: 0, y: 0, z: 0 }
```
