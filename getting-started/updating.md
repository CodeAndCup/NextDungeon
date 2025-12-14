---
description: Learn how to safely update NextDungeon to the latest version.
icon: arrow-up-from-bracket
---

# Updating

Keeping NextDungeon up to date ensures you have the latest features, bug fixes, and security improvements. Follow this guide to safely update your installation.

## Before You Update

### 1. Backup Your Data

**Always** create backups before updating:

* **Configuration files**: `plugins/NextDungeon/`
* **Dungeon configurations**: `plugins/NextDungeon/dungeons/`
* **Database**: Export your MySQL/MongoDB database
* **World files**: If using vanilla instance provider

```bash
# Example backup command
cp -r plugins/NextDungeon/ plugins/NextDungeon-backup-$(date +%Y%m%d)/
```

### 2. Check Compatibility

Before updating, verify that:

* Your Minecraft server version is supported
* Required dependencies are compatible with the new version
* CloudNet version meets requirements (if applicable)
* Java version is correct (Java 21+ required)

### 3. Read the Changelog

Review the [release notes](https://github.com/SAOFR-DEV/Dungeons/releases) for:

* Breaking changes that may affect your configuration
* New features you might want to use
* Deprecated features that need migration
* Known issues or migration notes

## Update Process

### Step 1: Stop Your Server

Properly shut down your Minecraft server to prevent data corruption:

```bash
# Using screen/tmux
stop

# Or if running as a service
systemctl stop minecraft
```

> **Important:** Do not force-kill the server process. Allow it to save all data gracefully.

### Step 2: Download the New Version

1. Visit the [releases page](https://github.com/SAOFR-DEV/Dungeons/releases)
2. Download the latest version JAR file
3. Verify the file integrity (checksum if provided)

### Step 3: Replace the Plugin File

```bash
# Navigate to your plugins folder
cd /path/to/your/server/plugins/

# Remove the old version
rm NextDungeon-*.jar

# Move the new version into the plugins folder
mv /path/to/download/NextDungeon-1.0.1-SNAPSHOT.jar .
```

### Step 4: Update Dependencies

Check if any dependency plugins also need updating:

* **CloudNet**: Update to required version (4.0.0-RC13+)
* **Parties**: Ensure compatibility
* **MMOCore**: Check for updates
* **MythicMobs**: Verify version compatibility

### Step 5: Review Configuration Changes

Some updates may introduce new configuration options:

1. Start your server once to generate new default configurations
2. Compare your backup with the new default configs
3. Merge any new options into your existing configuration
4. Update deprecated settings as noted in the changelog

> **Tip:** Use a diff tool to compare configuration files:
> ```bash
> diff plugins/NextDungeon/config.yml plugins/NextDungeon-backup-*/config.yml
> ```

### Step 6: Start and Verify

1. Start your server
2. Monitor the console for errors or warnings
3. Check that the plugin loaded successfully:
   ```
   /dungeon debug list
   ```
4. Test a dungeon instance to ensure everything works

### Step 7: Test Thoroughly

Before allowing players back on:

* Test dungeon creation and loading
* Verify instance provider functionality
* Check database connectivity
* Test party functionality
* Verify custom configurations still work
* Test integrations (MythicMobs, MMOCore, etc.)

## Version-Specific Notes

### Updating to 1.0.1-SNAPSHOT

Changes in version 1.0.1-SNAPSHOT:

* Enhanced web editor functionality
* Improved CloudNet integration
* New database configuration options
* Revive system improvements

**Migration steps:**
1. Review new `ReviveSystem` configuration options
2. Update `webeditor.proxy-port` if needed
3. Check new database configuration structure

### Updating from Pre-1.0 Versions

If updating from a version before 1.0:

* Configuration file structure has changed significantly
* Database schema may require migration
* Some commands have been renamed or restructured
* Consider a fresh installation for major version jumps

## Rollback Procedure

If something goes wrong after updating:

### Step 1: Stop the Server

```bash
stop
```

### Step 2: Restore Plugin File

```bash
cd /path/to/your/server/plugins/
rm NextDungeon-*.jar
cp ../NextDungeon-backup-*/NextDungeon-*.jar .
```

### Step 3: Restore Configuration

```bash
rm -rf NextDungeon/
cp -r ../NextDungeon-backup-*/ NextDungeon/
```

### Step 4: Restore Database (if needed)

Import your database backup using MySQL/MongoDB tools.

### Step 5: Restart Server

Start your server with the previous version restored.

## Automatic Updates

> **Warning:** Automatic updates are **not recommended** for production servers. Always test updates in a staging environment first.

## Update Checklist

- [ ] Server and database backed up
- [ ] Changelog reviewed
- [ ] Compatibility verified
- [ ] Dependencies updated
- [ ] Plugin file replaced
- [ ] Configuration files reviewed and updated
- [ ] Server started successfully
- [ ] Plugin loaded without errors
- [ ] Dungeons tested and functional
- [ ] Integrations working correctly
- [ ] Players notified of any changes

## Getting Help

If you encounter issues during the update:

1. Check the console logs for specific error messages
2. Review the [troubleshooting guide](#) (if available)
3. Search for similar issues on GitHub
4. Join the Discord community for support
5. Create a bug report with full details and logs

***

Stay up to date to enjoy the latest features and improvements!

