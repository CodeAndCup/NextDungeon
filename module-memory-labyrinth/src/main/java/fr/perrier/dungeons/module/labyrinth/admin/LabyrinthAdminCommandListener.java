package fr.perrier.dungeons.module.labyrinth.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthRunManager;
import fr.perrier.dungeons.module.labyrinth.manager.LootTableRegistry;
import fr.perrier.dungeons.module.labyrinth.manager.RoomTemplateRegistry;
import fr.perrier.dungeons.module.labyrinth.model.LootTable;
import fr.perrier.dungeons.module.labyrinth.model.RoomTemplate;
import fr.perrier.dungeons.module.labyrinth.model.RoomType;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Lightweight admin CRUD surface for Memory Labyrinth, exposed as a
 * chat command and intercepted via {@link PlayerCommandPreprocessEvent}
 * (same pattern as the revive / resume prompt listeners — avoids
 * registering a Bukkit command from a URLClassLoader-loaded module).
 *
 * <p>Supports both player chat ({@code /labyrinth admin ...}) and
 * console ({@code labyrinth admin ...}). Player commands require
 * the {@code nextdungeon.admin} permission.</p>
 *
 * <p>Subcommands :</p>
 * <ul>
 *   <li>{@code list-rooms} — dump the loaded room pool</li>
 *   <li>{@code list-loot-tables} — dump cached loot tables</li>
 *   <li>{@code list-saves} — query DB and list every Infinite save</li>
 *   <li>{@code import-rooms <dir>} — upsert every {@code *.json} file
 *       in {@code plugins/NextDungeon/labyrinth/<dir>}/</li>
 *   <li>{@code import-loot &lt;floorId&gt; &lt;file&gt;} — upsert loot
 *       table from a JSON file</li>
 *   <li>{@code reload} — reload room pool + loot tables from DB</li>
 *   <li>{@code stats} — module runtime stats (active runs, registry size)</li>
 * </ul>
 *
 * <p>The full REST/panel surface (CDC §7) is not implemented in v1 ; it
 * requires extending the {@code WebEditorRequestPacket} protocol and
 * adding HTTP routes in {@code ProxyWebEditorServer} (see CDC §10
 * « hors-scope » for follow-up).</p>
 */
public class LabyrinthAdminCommandListener implements Listener {

    public static final String COMMAND = "labyrinth";
    public static final String ADMIN_SUB = "admin";
    public static final String PERMISSION = "nextdungeon.admin";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final RoomTemplateRegistry rooms;
    private final LootTableRegistry lootTables;
    private final LabyrinthRunManager runManager;

    public LabyrinthAdminCommandListener(RoomTemplateRegistry rooms,
                                         LootTableRegistry lootTables,
                                         LabyrinthRunManager runManager) {
        this.rooms = rooms;
        this.lootTables = lootTables;
        this.runManager = runManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayer(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || !message.startsWith("/" + COMMAND)) return;
        Player sender = event.getPlayer();
        if (!sender.hasPermission(PERMISSION)) return;
        String tail = message.substring(("/" + COMMAND).length()).trim();
        if (!tail.startsWith(ADMIN_SUB)) return;
        event.setCancelled(true);
        dispatch(sender, tail.substring(ADMIN_SUB.length()).trim());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onConsole(ServerCommandEvent event) {
        String message = event.getCommand();
        if (message == null || !message.startsWith(COMMAND)) return;
        String tail = message.substring(COMMAND.length()).trim();
        if (!tail.startsWith(ADMIN_SUB)) return;
        event.setCancelled(true);
        dispatch(event.getSender(), tail.substring(ADMIN_SUB.length()).trim());
    }

    private void dispatch(CommandSender sender, String args) {
        if (args.isEmpty()) {
            help(sender);
            return;
        }
        String[] parts = args.split("\\s+");
        String sub = parts[0].toLowerCase();
        switch (sub) {
            case "help" -> help(sender);
            case "list-rooms" -> listRooms(sender);
            case "list-loot-tables" -> listLootTables(sender);
            case "list-saves" -> listSaves(sender);
            case "import-rooms" -> importRooms(sender, slice(parts, 1));
            case "import-loot" -> importLoot(sender, slice(parts, 1));
            case "reload" -> reload(sender);
            case "stats" -> stats(sender);
            default -> {
                send(sender, "§cSous-commande inconnue : " + sub);
                help(sender);
            }
        }
    }

    private void help(CommandSender sender) {
        send(sender, "§6▶ §eMemory Labyrinth — Admin commands");
        send(sender, "§7/" + COMMAND + " " + ADMIN_SUB + " §flist-rooms");
        send(sender, "§7/" + COMMAND + " " + ADMIN_SUB + " §flist-loot-tables");
        send(sender, "§7/" + COMMAND + " " + ADMIN_SUB + " §flist-saves");
        send(sender, "§7/" + COMMAND + " " + ADMIN_SUB + " §fimport-rooms §o<directory>");
        send(sender, "§7/" + COMMAND + " " + ADMIN_SUB + " §fimport-loot §o<floorId> <file.json>");
        send(sender, "§7/" + COMMAND + " " + ADMIN_SUB + " §freload");
        send(sender, "§7/" + COMMAND + " " + ADMIN_SUB + " §fstats");
    }

    private void listRooms(CommandSender sender) {
        send(sender, "§6▶ §eRoom pool §7(" + rooms.size() + " entries)");
        for (RoomType type : RoomType.values()) {
            List<RoomTemplate> list = rooms.getByType(type);
            send(sender, "§7" + type + " §8| §f" + list.size());
            for (RoomTemplate r : list) {
                String tags = r.getTags() == null ? "" : String.join(",", r.getTags());
                send(sender, "  §f" + r.getId() + " §7tags=[" + tags + "] icon=" + r.getFixedIcon());
            }
        }
    }

    private void listLootTables(CommandSender sender) {
        send(sender, "§6▶ §eLoot tables §7(" + lootTables.size() + " entries)");
        for (String floorId : lootTables.floorIds()) {
            LootTable table = lootTables.getByFloor(floorId);
            int items = table.getItems() == null ? 0 : table.getItems().size();
            send(sender, "  §f" + floorId + " §7baseGold=" + table.getBaseGold()
                    + " baseItemRolls=" + table.getBaseItemRolls()
                    + " entries=" + items);
        }
    }

    private void listSaves(CommandSender sender) {
        DatabaseManager db = Main.getInstance().getDatabaseManager();
        if (db == null) {
            send(sender, "§cDatabaseManager unavailable.");
            return;
        }
        send(sender, "§6▶ §eListing saves...");
        db.listLabyrinthSaves().thenAccept(rows -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            send(sender, "§7" + rows.size() + " save(s) found");
            for (String[] row : rows) {
                send(sender, "  §f" + row[0] + " §7floor=" + row[1] + " party=" + shortHash(row[2]));
            }
        }));
    }

    private void importRooms(CommandSender sender, String[] args) {
        if (args.length < 1) {
            send(sender, "§cUsage : import-rooms <directory>");
            return;
        }
        Path base = baseDir().resolve(args[0]);
        if (!Files.isDirectory(base)) {
            send(sender, "§cDirectory not found : " + base);
            return;
        }
        DatabaseManager db = Main.getInstance().getDatabaseManager();
        if (db == null) {
            send(sender, "§cDatabaseManager unavailable.");
            return;
        }
        try (Stream<Path> stream = Files.walk(base, 1)) {
            int imported = 0;
            for (Path file : (Iterable<Path>) stream::iterator) {
                if (!Files.isRegularFile(file)) continue;
                if (!file.getFileName().toString().endsWith(".json")) continue;
                String json = Files.readString(file, StandardCharsets.UTF_8);
                RoomTemplate room = GSON.fromJson(json, RoomTemplate.class);
                if (room == null || room.getId() == null) {
                    send(sender, "§7- skip " + file.getFileName() + " (no id)");
                    continue;
                }
                String type = room.getType() != null ? room.getType().name() : "COMBAT";
                String tagsCsv = room.getTags() == null ? "" : String.join(",", room.getTags());
                db.saveLabyrinthRoom(room.getId(), type, tagsCsv, json);
                rooms.upsert(room);
                imported++;
            }
            send(sender, "§a▶ Imported " + imported + " room(s) from " + base);
        } catch (IOException | RuntimeException e) {
            send(sender, "§cImport failed : " + e.getMessage());
        }
    }

    private void importLoot(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "§cUsage : import-loot <floorId> <file.json>");
            return;
        }
        Path file = baseDir().resolve(args[1]);
        if (!Files.isRegularFile(file)) {
            send(sender, "§cFile not found : " + file);
            return;
        }
        DatabaseManager db = Main.getInstance().getDatabaseManager();
        if (db == null) {
            send(sender, "§cDatabaseManager unavailable.");
            return;
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            LootTable table = GSON.fromJson(json, LootTable.class);
            if (table == null) {
                send(sender, "§cInvalid loot table JSON");
                return;
            }
            table.setFloorId(args[0]);
            String payload = GSON.toJson(table);
            db.saveLootTable(args[0], payload);
            lootTables.upsert(table);
            send(sender, "§a▶ Loot table for floor '" + args[0] + "' imported");
        } catch (IOException | RuntimeException e) {
            send(sender, "§cImport failed : " + e.getMessage());
        }
    }

    private void reload(CommandSender sender) {
        send(sender, "§6▶ §eReloading room pool + loot tables...");
        rooms.clear();
        lootTables.clear();
        rooms.loadAll().thenCompose(v -> lootTables.loadAll())
                .thenRun(() -> Bukkit.getScheduler().runTask(Main.getInstance(),
                        () -> send(sender, "§a▶ Reload complete (rooms=" + rooms.size()
                                + ", lootTables=" + lootTables.size() + ")")))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(Main.getInstance(),
                            () -> send(sender, "§cReload failed : " + ex.getMessage()));
                    return null;
                });
    }

    private void stats(CommandSender sender) {
        send(sender, "§6▶ §eMemory Labyrinth — runtime stats");
        send(sender, "§7active runs §8| §f" + runManager.getActiveRunCount());
        send(sender, "§7rooms in pool §8| §f" + rooms.size());
        send(sender, "§7loot tables §8| §f" + lootTables.size());
    }

    private static Path baseDir() {
        File data = Main.getInstance().getDataFolder();
        Path labDir = data.toPath().resolve("labyrinth");
        try {
            if (!Files.exists(labDir)) Files.createDirectories(labDir);
        } catch (IOException ignored) {
            // Falls back to data dir if labyrinth dir cannot be created.
            return data.toPath();
        }
        return labDir;
    }

    private static String[] slice(String[] parts, int from) {
        if (from >= parts.length) return new String[0];
        String[] out = new String[parts.length - from];
        System.arraycopy(parts, from, out, 0, out.length);
        return out;
    }

    private static String shortHash(String hash) {
        if (hash == null) return "?";
        return hash.length() > 12 ? hash.substring(0, 12) + "…" : hash;
    }

    private static void send(CommandSender sender, String message) {
        if (sender instanceof Player p) p.sendMessage(message);
        else if (sender instanceof ConsoleCommandSender c) c.sendMessage(message);
        else sender.sendMessage(message);
    }
}
