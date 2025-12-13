---
description: Here is the main config file to set up on your spigot/paper server.
icon: gears
---

# Main Config File

```yaml
# =====================================================
# NextDungeon Configuration File
# =====================================================
# Bienvenue dans la configuration de NextDungeon !
# Cette configuration contrôle tous les aspects du plugin.
# =====================================================

# ─────────────────────────────────────────────────────
# 🔴 CONFIGURATION REDIS
# ─────────────────────────────────────────────────────
# Redis est utilisé pour la communication inter-serveurs
# et le stockage des données temps réel.
RedisConfiguration:
    host: "127.0.0.1"
    port: 6379
    username: "default"
    password: ""
    topic: "dungeons:packets" # ⚠️ Ne pas modifier sans savoir ce que vous faites

# ─────────────────────────────────────────────────────
# 🌐 CONFIGURATION WEB EDITOR
# ─────────────────────────────────────────────────────
# Configuration du serveur web proxy pour l'éditeur
webeditor:
    proxy-port: 7734 # Port du serveur web proxy (Velocity/BungeeCord)

# ─────────────────────────────────────────────────────
# 💾 CONFIGURATION BASE DE DONNÉES
# ─────────────────────────────────────────────────────
# Types disponibles: mongodb, mysql
# Choisissez le type de base de données qui correspond
# à votre infrastructure.
DatabaseConfiguration:
    type: "mysql"

    mysql:
        host: "localhost"
        port: 3306
        database: "dungeons"
        username: "root"
        password: "root"

    mongodb:
        host: "localhost"
        port: 27017
        database: "dungeons"

# ─────────────────────────────────────────────────────
# ☁️  CONFIGURATION DU PROVIDER D'INSTANCES
# ─────────────────────────────────────────────────────
# Gère le système de création et gestion des instances.
#
# Types disponibles:
#   • CLOUDNET   - Utilise CloudNet (nécessite CloudNet installé)
#   • ASP        - Advanced Slime World Manager (léger & performant)
#   • VANILLA    - Système natif de Minecraft (toujours disponible)
#
InstanceProvider:
    type: "CLOUDNET"

    # Configuration ASP (Advanced Slime World Manager)
    ASP:
        loaderType: "FILE" # FILE, MYSQL, ou MONGODB

        mysql:
            url: "jdbc:mysql://localhost:3306/asm"
            host: "localhost"
            port: 3306
            database: "asm"
            useSSL: false
            username: "root"
            password: "root"

        mongodb:
            database: "asm"
            collection: "worlds"
            username: ""
            password: ""
            authSource: ""
            host: "localhost"
            port: 27017
            uri: ""

# ─────────────────────────────────────────────────────
# ⚰️  SYSTÈME DE RÉSURRECTION
# ─────────────────────────────────────────────────────
# Gère le système de fantôme et de résurrection quand
# un joueur meurt. Les coéquipiers peuvent le ressusciter
# en utilisant un item spécial avant que le timer s'écoule.
#
ReviveSystem:

    # 📦 ITEM DE RÉSURRECTION
    # L'item utilisé pour ressusciter un joueur décédé
    ReviveItem:
        type: "BEETROOT_SOUP"           # Type d'item Minecraft
        displayName: "&c&lRevive Item"  # Nom affiché (supporte les codes couleur)
        lore:                           # Description de l'item
            - "&7Use this to revive a fallen teammate"

    # ⏱️  DURÉE DU MODE FANTÔME
    # Temps en secondes avant que le joueur réapparaisse
    # (et consomme une vie, ou est banni s'il n'en a plus)
    ghostDuration: 15

    # 📢 MESSAGE DE RÉSURRECTION
    # Message affiché à tous les joueurs lors d'une résurrection
    # {player} sera remplacé par le nom du joueur
    reviveMessage: "&a{player} has been revived!"

    # 🚫 BANNISSEMENT DÉFINITIF
    # Utilisé quand un joueur n'a plus de vies restantes
    banCommand: "litebans:ban {player} {time} {reason}"
    banReason: "You have died permanently in the dungeon."

# =====================================================
# 💡 CONSEILS DE CONFIGURATION
# =====================================================
# • Changer les couleurs: utilisez les codes Minecraft
#   &0=noir &1=bleu &2=vert &3=cyan &4=rouge &5=magenta
#   &6=jaune &7=gris &8=gris foncé &9=bleu clair &a=vert clair
#   &b=cyan clair &c=rouge clair &d=magenta clair &e=jaune clair &f=blanc
#   &l=gras &o=italique &n=souligné &m=barré
#   Ou utilisez les codes hexadécimaux: &#RRGGBB
#
# • Tester votre configuration: relancez le serveur et vérifiez les logs
# • Besoin d'aide? Consultez la documentation: https://cupcode-1.gitbook.io/nextdungeon/
#
# =====================================================

```
