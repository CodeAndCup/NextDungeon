---
description: Learn how to safely update NextDungeon to the latest version.
icon: arrow-up-from-bracket
---

# Updating

## Before You Update

### 1. Back Up Your Data

Always create backups before updating:

* **Plugin config**: `plugins/NextDungeon/config.yml`
* **Database**: Export your MySQL/MongoDB database
* **Redis**: If you have important live data, snapshot your Redis instance (`BGSAVE`)

```bash
# Example: back up the plugin folder
cp -r plugins/NextDungeon/ plugins/NextDungeon-backup-$(date +%Y%m%d)/
```

### 2. Check the Changelog

Review the release notes for the new version. Pay special attention to:

* New required configuration keys in `config.yml`
* Database schema changes
* New or renamed Redis keys
* Removed or renamed commands and permissions

### 3. Verify Dependency Compatibility

Confirm that MMOCore, packetevents, CloudNet, and any other integration plugins are still compatible with the new NextDungeon version.

***

## Update Procedure

### Step 1: Stop Your Server

Gracefully shut down all game servers and the proxy before replacing files:

```
/stop
```

### Step 2: Replace the Plugin JAR

1. Delete the old `NextDungeon.jar` from `plugins/`
2. Place the new `NextDungeon.jar` into `plugins/`
3. Do the same for `NextDungeon-Velocity.jar` or `NextDungeon-BungeeCord.jar` on your proxy

### Step 3: Migrate Configuration Changes

Check the new default `config.yml` (generated on first start) and compare it to your existing config. Add any new keys with appropriate values.

Common areas to check between versions:

| Section                     | What to look for                                |
| --------------------------- | ----------------------------------------------- |
| `RedisConfiguration`        | New keys (e.g. `database` field added in 1.0.4) |
| `InstanceProvider`          | New provider types or renamed options           |
| `PartyProvider`             | New provider options                            |
| `ReviveSystem`              | New revive/ghost configuration keys             |
| `NotificationConfiguration` | New notification type options                   |

### Step 4: Start Your Server

Start the server and check the console for:

* Successful Redis connection
* Database connection confirmation
* `NextDungeon X.X.X started in N ms` message
* Any `SEVERE` or `WARNING` messages indicating configuration issues

### Step 5: Verify Functionality

Run the following checks:

```
/dungeon list
/dungeon admin status
/dungeon admin queue status
```

Test that an existing dungeon floor can be entered by running:

```
/dungeon admin test <dungeonId> <floorId>
```

***

## Rolling Back

If the update introduces issues:

1. Stop the server
2. Restore the old JAR and configuration backup
3. If Redis data was modified, restore from your Redis snapshot
4. Restart the server

***

## Common Post-Update Issues

| Symptom                  | Likely Cause                                    | Fix                                                       |
| ------------------------ | ----------------------------------------------- | --------------------------------------------------------- |
| Plugin fails to load     | Missing hard dependency (MMOCore, packetevents) | Install/update the missing dependency                     |
| Redis connection error   | Host/port/password changed or wrong             | Update `RedisConfiguration` in `config.yml`               |
| Commands not found       | New command alias added/removed                 | Check `plugin.yml` in the new release                     |
| Database errors on start | Schema change in new version                    | Check release notes for migration SQL or MongoDB commands |
