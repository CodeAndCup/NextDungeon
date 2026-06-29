package fr.perrier.dungeons.spigot.listener.dungeons;

import com.cryptomorin.xseries.messages.Titles;
import com.github.juliarn.npclib.api.Npc;
import com.github.juliarn.npclib.api.Position;
import com.github.juliarn.npclib.api.event.ShowNpcEvent;
import com.github.juliarn.npclib.api.profile.Profile;
import com.github.juliarn.npclib.api.profile.ProfileProperty;
import com.github.juliarn.npclib.api.protocol.enums.EntityPose;
import com.github.juliarn.npclib.api.protocol.meta.EntityMetadataFactory;
import com.mojang.authlib.GameProfile;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.common.model.dungeon.FloorType;
import fr.perrier.dungeons.common.model.player.PlayerStats;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;

/**
 * Listener for player death events in a dungeon instance.
 * Handles ghost mechanics, corpse NPC creation, health bar display, and revival or ban logic.
 */
public class InstancePlayerDeathListener implements Listener {

    /**
     * Stores ghost data for each player by UUID.
     */
    @Getter
    private static final Map<UUID,GhostData> GHOST_DATA = new HashMap<>();

    /**
     * Data class representing a player's ghost state.
     */
    @Setter
    @Getter
    @AllArgsConstructor
    public static class GhostData {
        private final UUID playerUUID;
        private final Location deathLocation;
        private int timeLeftAsGhost;
        private Npc<World, Player, ItemStack, Plugin> corpseNpc;
        private int taskId;
        private boolean revived;
        private org.bukkit.entity.TextDisplay healthBarDisplay;
    }

    /**
     * Registers the NPC event handler for corpse NPCs.
     */
    public InstancePlayerDeathListener() {
        Main.getInstance().getNpcLibPlatform().eventManager().registerEventHandler(ShowNpcEvent.class, showNpcEvent -> {
            var npc = showNpcEvent.npc();
            Player player = showNpcEvent.player();

            npc.changeMetadata(EntityMetadataFactory.skinLayerMetaFactory(), true).schedule(player);
            npc.changeMetadata(EntityMetadataFactory.entityPoseMetaFactory(), (new Random().nextBoolean() ? EntityPose.SLEEPING : EntityPose.SWIMMING)).schedule(player);
            npc.changeMetadata(EntityMetadataFactory.shakingMetaFactory(),true).schedule(player);
        });
    }

    /**
     * Cleans up ghost state when a player disconnects to prevent memory leaks.
     *
     * @param event the player quit event
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GhostData data = GHOST_DATA.remove(player.getUniqueId());
        if (data != null) {
            // Cancel the ghost timer task
            if (data.getTaskId() != -1) {
                Bukkit.getScheduler().cancelTask(data.getTaskId());
            }
            // Remove the health bar display entity
            if (data.getHealthBarDisplay() != null && !data.getHealthBarDisplay().isDead()) {
                data.getHealthBarDisplay().remove();
            }
            // Unlink the corpse NPC
            if (data.getCorpseNpc() != null) {
                data.getCorpseNpc().unlink();
            }
        }

        // Drop the player's per-player entries (stats, lives, origin, membership) from the
        // instance state so they do not linger in Redis after the player disconnects.
        // Run on the main thread (same pattern as InstanceJoinListener) so the backing maps
        // are never mutated concurrently with other instance listeners.
        Main.getInstance().getDungeonService().removePlayerFromInstanceState(player.getUniqueId());
    }

    /**
     * Prevents ghost players (dead, awaiting revive) from dealing damage.
     * Ghosts are invulnerable so they cannot be hit, but nothing stops them
     * from hitting mobs or other players — this cancels any damage whose
     * attacker is a ghost, covering both melee and projectiles they fired.
     *
     * @param event the entity-vs-entity damage event
     */
    @EventHandler(ignoreCancelled = true)
    public void onGhostDealDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker != null && Main.getInstance().getGhostFactory().isGhost(attacker)) {
            event.setCancelled(true);
        }
    }

    /**
     * Resolves the player responsible for a damage source, if any.
     *
     * @param damager the entity that dealt the damage (a player or a projectile)
     * @return the attacking player, or {@code null} if the source is not a player
     */
    private static Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    /**
     * Handles the player death event.
     * Turns the player into a ghost, creates a corpse NPC, and starts the ghost timer.
     *
     * @param event the player death event
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        player.setRespawnLocation(player.getLocation().clone().add(0,2,0));

        // Floors with a custom revive flow (e.g. Memory Labyrinth) keep the
        // dead player as a ghost until a teammate revives them at the boss
        // room or the run ends — the standard 15s ghost-timeout auto-revive
        // must NOT run, otherwise the player comes back to life on his own.
        FloorInstance currentInstance = Main.getInstance().getDungeonService().getCurrentInstance();
        boolean customRevive = currentInstance != null
                && currentInstance.getFloor() != null
                && currentInstance.getFloor().getFloorType() == FloorType.LABYRINTH;

        if (customRevive) {
            // Custom-revive floors have no "lives" mechanic — the player simply
            // stays down until a teammate revives them. Skip the lives-based
            // core death message and announce a fall instead.
            Bukkit.broadcastMessage(ChatUtil.translate(
                    "&c☠ " + player.getName() + " has fallen — a teammate can revive them."));
        } else {
            Bukkit.broadcastMessage(ChatUtil.translate(
                    Objects.requireNonNull(Main.getInstance().getConfig().getString("ReviveSystem.deathMessage"))
                            .replace("{player}", player.getName())
                            .replace("{lives}", String.valueOf(
                                    Main.getInstance().getDungeonService()
                                            .getCurrentInstance()
                                            .getPlayerCurrentLives()
                                            .getOrDefault(player.getUniqueId(), 0)
                            ))
            ));
        }

        if(!Main.getInstance().getGhostFactory().isGhost(player)) {
            int ghostDuration = Main.getInstance().getConfig().getInt("ReviveSystem.ghostDuration", 15);
            GhostData ghostData = new GhostData(player.getUniqueId(), player.getLocation(), ghostDuration, null, -1, false, null);
            GHOST_DATA.put(player.getUniqueId(), ghostData);

            UUID uniqueId = UUID.randomUUID();
            GameProfile profile = ((CraftPlayer)player).getProfile();

            Npc<World, Player, ItemStack, Plugin> npc = Main.getInstance().getNpcLibPlatform().newNpcBuilder()
                    .npcSettings(builder ->
                            builder.profileResolver((target, spawnedNpc) ->
                                Main.getInstance().getNpcLibPlatform().profileResolver()
                                .resolveProfile(Profile.unresolved(player.getUniqueId()))
                                .thenApply(resolvedProfile -> spawnedNpc.profile().withProperties(resolvedProfile.properties()))
                            )
                    )
                    .profile(Profile.resolved(
                            ChatUtil.toSmallCaps(player.getName()),
                            uniqueId,
                            profile.getProperties().values().stream()
                                    .map(prop -> ProfileProperty.property(prop.name(), prop.value(), prop.signature()))
                                    .collect(Collectors.toSet())))
                    .position(Position.position(
                            player.getLocation().getX(),
                            player.getLocation().getY(),
                            player.getLocation().getZ(),
                            Main.getInstance().getNpcLibPlatform().worldAccessor().extractWorldIdentifier(player.getWorld())
                    ))
                    .buildAndTrack();

            ghostData.setCorpseNpc(npc);

            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                player.teleport(player.getLocation().clone().add(0, 2, 0));
                player.spigot().respawn();
                Main.getInstance().getGhostFactory().addPlayer(player);
                player.setAllowFlight(true);
                player.setFlying(true);
                player.setInvulnerable(true);

                // Créer la barre de santé au-dessus du NPC. Custom-revive
                // floors show a static "awaiting revive" label since there is
                // no death countdown — the player stays dead until revived.
                Location npcLoc = ghostData.getDeathLocation().clone().add(0, 1.05, 0);
                String corpseText = customRevive
                        ? "&f" + ChatUtil.toSmallCaps(player.getName()) + "\n&#ff0000☠ " + ChatUtil.toSmallCaps("awaiting revive")
                        : "&f" + ChatUtil.toSmallCaps(player.getName() + " will died in") + "\n" + "&#ff0000❤ &#BB0000⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛ &f15s";
               TextDisplay healthBar = ghostData.getDeathLocation().getWorld().spawn(npcLoc, org.bukkit.entity.TextDisplay.class, display -> {
                    display.setText(ChatUtil.translate(corpseText));
                    display.setAlignment(TextDisplay.TextAlignment.CENTER);
                    display.setBillboard(Display.Billboard.CENTER);
                    display.setLineWidth(200);
                });
                ghostData.setHealthBarDisplay(healthBar);
            }, 5L);

            // Custom-revive floors own the death resolution — skip the core
            // ghost-timeout countdown entirely so the player remains a ghost.
            if (customRevive) {
                return;
            }

            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    GhostData data = GHOST_DATA.get(player.getUniqueId());
                    if(data != null && !data.isRevived()) {
                        data.setTimeLeftAsGhost(data.getTimeLeftAsGhost() - 1);
                        if(data.getTimeLeftAsGhost() == 1)
                            Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, false, false,false))
                            );
                        if(data.getTimeLeftAsGhost() <= 0) {
                            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                                if(!data.isRevived()) {
                                    Main.getInstance().getGhostFactory().removePlayer(player);
                                    if(player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                                        player.setFlying(false);
                                        player.setAllowFlight(false);
                                    }
                                    player.setInvulnerable(false);
                                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                                    player.teleport(data.getDeathLocation());
                                    applyDeathTo(player);
                                    if(data.getCorpseNpc() != null)
                                        data.getCorpseNpc().unlink();
                                    if(data.getHealthBarDisplay() != null)
                                        data.getHealthBarDisplay().remove();
                                    GHOST_DATA.remove(player.getUniqueId());
                                }
                            });
                            this.cancel();
                        } else {
                            Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                                updateHealthBar(player, data)
                            );
                        }
                    }
                }
            };
            int taskId = task.runTaskTimerAsynchronously(Main.getInstance(), 20L, 20L).getTaskId();
            ghostData.setTaskId(taskId);
        }
    }

    /**
     * Updates the ghost health bar display for the player.
     *
     * @param player the ghost player
     * @param data the ghost data
     */
    private void updateHealthBar(Player player, GhostData data) {
        int totalTime = Main.getInstance().getConfig().getInt("ReviveSystem.ghostDuration", 15);
        int timeLeft = data.getTimeLeftAsGhost();
        int barLength = 10;

        // Calcul de la barre de santé
        int filledBlocks = Math.max(0, (timeLeft * barLength) / totalTime);

        String barBuilder = "&#ff0000❤ " +
                            "&#BB0000⬛".repeat(filledBlocks) +
                            "&#111111⬛".repeat(Math.max(0, barLength - filledBlocks));

        // Afficher la barre au joueur
        Titles.sendTitle(player, 0, 22, 0,
                ChatUtil.toSmallCaps(ChatUtil.translate("&7You are a ghost!")),
                ChatUtil.translate(barBuilder + " &f" + timeLeft + "s")
        );

        // Mettre à jour la barre au-dessus du NPC
        if(data.getHealthBarDisplay() != null && !data.getHealthBarDisplay().isDead()) {
            String displayText = ChatUtil.translate("&f" + ChatUtil.toSmallCaps(player.getName() + " will died in") + "\n" + barBuilder + " &f" + timeLeft + "s");
            data.getHealthBarDisplay().setText(displayText);
        }
    }

    /**
     * Revives a ghost player, removing ghost effects and cleaning up NPCs and displays.
     * The player is teleported back to where they died.
     *
     * @param player the player to revive
     */
    public static void revivePlayer(Player player) {
        revivePlayer(player, null);
    }

    /**
     * Revives a ghost player, removing ghost effects and cleaning up NPCs and displays.
     *
     * @param player the player to revive
     * @param target where to teleport the revived player ; when {@code null} the
     *               player is sent back to their death location. Custom-revive
     *               floors (e.g. Memory Labyrinth) pass the reviver's location so
     *               the player rejoins the group instead of an old, far-away room.
     */
    public static void revivePlayer(Player player, Location target) {
        GhostData data = GHOST_DATA.get(player.getUniqueId());
        if(data != null && !data.isRevived()) {
            data.setRevived(true);

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                // Arrêter la tâche du timer
                Bukkit.getScheduler().cancelTask(data.getTaskId());

                // Retirer le joueur du mode fantôme
                Main.getInstance().getGhostFactory().removePlayer(player);
                if(player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
                player.setInvulnerable(false);
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                player.removePotionEffect(PotionEffectType.BLINDNESS);

                // Téléporter et afficher message
                player.teleport(target != null ? target : data.getDeathLocation());
                String reviveMessage = Objects.requireNonNull(Main.getInstance().getConfig().getString("ReviveSystem.reviveMessage"));
                Bukkit.broadcastMessage(ChatUtil.translate(reviveMessage.replace("{player}", player.getName())));

                // Nettoyer
                if(data.getCorpseNpc() != null) {
                    data.getCorpseNpc().unlink();
                }
                GHOST_DATA.remove(player.getUniqueId());
            });
        }
    }

    /**
     * Applies death logic to a player (decrement lives or ban if no lives left).
     *
     * @param player the player to process
     */
    private void applyDeathTo(Player player) {
        FloorInstance instance = Main.getInstance().getDungeonService().getCurrentInstance();

        PlayerStats stats = instance.getPlayerStats().get(player.getUniqueId());
        if(stats != null) {
            stats.incrementDeaths();
        }

        if(instance.getPlayerCurrentLives().containsKey(player.getUniqueId()) && instance.getPlayerCurrentLives().get(player.getUniqueId()) > 0) {
            instance.getPlayerCurrentLives().compute(player.getUniqueId(), (k, currentLives) -> currentLives - 1);
        } else {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    Objects.requireNonNull(Main.getInstance().getConfig().getString("ReviveSystem.banCommand"))
                            .replace("{player}", player.getName())
                            .replace("{time}", instance.getFloor().getRules().getDeathBanDuration())
                            .replace("{reason}", Objects.requireNonNull(Main.getInstance().getConfig().getString("ReviveSystem.banReason")))
            );
        }
        instance.syncInstance();

        if(instance.isAllPlayersDead()) {
            instance.fail();
        }
    }
}
