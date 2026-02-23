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
import fr.perrier.dungeons.common.model.player.PlayerStats;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
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
     * Handles the player death event.
     * Turns the player into a ghost, creates a corpse NPC, and starts the ghost timer.
     *
     * @param event the player death event
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        player.setRespawnLocation(player.getLocation().clone().add(0,2,0));

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
                                .resolveProfile(Profile.unresolved(target.getUniqueId()))
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

                // Créer la barre de santé au-dessus du NPC
                Location npcLoc = ghostData.getDeathLocation().clone().add(0, 1.05, 0);
               TextDisplay healthBar = ghostData.getDeathLocation().getWorld().spawn(npcLoc, org.bukkit.entity.TextDisplay.class, display -> {
                    display.setText(ChatUtil.translate("&f" + ChatUtil.toSmallCaps(player.getName() + " will died in") + "\n" + "&#ff0000❤ &#BB0000⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛ &f15s"));
                    display.setAlignment(TextDisplay.TextAlignment.CENTER);
                    display.setBillboard(Display.Billboard.CENTER);
                    display.setLineWidth(200);
                });
                ghostData.setHealthBarDisplay(healthBar);
            }, 5L);

            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    GhostData data = GHOST_DATA.get(player.getUniqueId());
                    if(data != null && !data.isRevived()) {
                        data.setTimeLeftAsGhost(data.getTimeLeftAsGhost() - 1);
                        if(data.getTimeLeftAsGhost() == 1)
                            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, false, false,false));
                            });
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
                            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                                updateHealthBar(player, data);
                            });
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
     *
     * @param player the player to revive
     */
    public static void revivePlayer(Player player) {
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
                player.teleport(data.getDeathLocation());
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
