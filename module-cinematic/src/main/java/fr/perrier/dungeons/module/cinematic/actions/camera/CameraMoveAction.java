package fr.perrier.dungeons.module.cinematic.actions.camera;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import fr.perrier.dungeons.module.cinematic.action.SimpleCinematicAction;
import fr.perrier.dungeons.module.cinematic.interpolation.PositionInterpolator;
import fr.perrier.dungeons.module.cinematic.model.CameraWaypoint;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Action de mouvement de caméra fluide utilisant l'interpolation Catmull-Rom
 * et une entité TEXT_DISPLAY avec spectate pour un mouvement 100% fluide.
 * <p>
 * Technique (ref: Typewriter DisplayCameraAction.kt):
 * 1. Calcul du chemin interpolé au démarrage du segment
 * 2. Création d'une entité TEXT_DISPLAY avec positionRotationInterpolationDuration
 * 3. Joueur spectate l'entité via WrapperPlayServerCamera
 * 4. Chaque frame: téléporter l'entité → client interpole smoothly
 * 5. Joueur téléporté seulement si distance &gt; 25 blocs ou tous les 10 frames (chunks)
 * 6. Packet interception: fake Y+500, fake inventory, block self-interact
 *
 * @see <a href="https://github.com/gabber235/Typewriter">Typewriter CameraCinematicEntry.kt</a>
 */
public class CameraMoveAction extends SimpleCinematicAction<CameraSegment> {

    private static final double FAKE_Y_OFFSET = 500.0;
    private static final int BASE_INTERPOLATION = 10;
    private static final double MAX_DISTANCE_SQUARED = 625.0; // 25 blocks squared

    private List<CameraSegment> segments = new ArrayList<>();
    private final transient Map<CameraSegment, List<CameraWaypoint>> segmentPaths = new HashMap<>();
    private transient PacketListenerAbstract packetListener = null;
    private transient WrapperEntity cameraEntity = null;

    @Override
    protected void onSegmentStart(Player player, CameraSegment segment) throws Exception {
        // Calculer chemin interpolé avec Catmull-Rom
        List<CameraWaypoint> path = PositionInterpolator.interpolatePath(
                segment.getPathPoints(),
                20 // 20 points par segment pour lisser
        );
        segmentPaths.put(segment, path);

        // Téléporter joueur au point de départ
        if (!path.isEmpty()) {
            CameraWaypoint start = path.get(0);
            Location startLoc = new Location(player.getWorld(),
                    start.getX(), start.getY(), start.getZ(),
                    start.getYaw(), start.getPitch());
            player.teleport(startLoc);
        }

        // Setup joueur pour le vol
        player.setAllowFlight(true);
        player.setFlying(true);

        // Créer et spawn l'entité caméra (ref: Typewriter DisplayCameraAction.kt:340-368)
        if (!path.isEmpty()) {
            createAndSpawnCameraEntity(player, path.get(0));
        }

        // Setup packet interception pour fluidité (skip Bedrock/Floodgate players)
        if (!isFloodgatePlayer(player)) {
            setupPacketInterception(player);
        }
    }

    @Override
    protected void onSegmentTick(Player player, CameraSegment segment, int frame) throws Exception {
        List<CameraWaypoint> path = segmentPaths.get(segment);
        if (path == null || path.isEmpty()) return;

        double percentage = segment.getPercentageAt(frame);
        CameraWaypoint interpolated = PositionInterpolator.interpolateAt(path, percentage);

        Location loc = new Location(player.getWorld(),
                interpolated.getX(), interpolated.getY(), interpolated.getZ(),
                interpolated.getYaw(), interpolated.getPitch());

        // Entité téléportée CHAQUE FRAME → client interpole smoothly via BASE_INTERPOLATION
        if (cameraEntity != null) {
            cameraEntity.teleport(
                    new com.github.retrooper.packetevents.protocol.world.Location(
                            loc.getX(), loc.getY(), loc.getZ(),
                            loc.getYaw(), loc.getPitch()));
        }

        // Joueur téléporté seulement si distance > 25 blocs OU tous les 10 frames (chunk loading)
        double distanceSquared = player.getLocation().distanceSquared(loc);
        if (frame % 10 == 0 || distanceSquared > MAX_DISTANCE_SQUARED) {
            player.teleport(loc);
        }
    }

    @Override
    protected void onSegmentStop(Player player, CameraSegment segment) throws Exception {
        despawnCameraEntity(player);
        segmentPaths.remove(segment);
        cleanupPacketInterception();
        // Restaurer l'affichage inventaire après suppression de l'interception
        player.updateInventory();
    }

    @Override
    public List<CameraSegment> getCinematicSegments() {
        return segments;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Camera entity management (ref: Typewriter DisplayCameraAction.kt:340-437)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Crée et spawn une entité TEXT_DISPLAY utilisée comme "caméra fantôme".
     * Le joueur spectate cette entité via {@link WrapperPlayServerCamera}.
     * L'entité utilise {@code positionRotationInterpolationDuration} pour que
     * le client interpole smoothly entre les positions.
     *
     * @param player   le joueur qui va spectater l'entité
     * @param position la position initiale de l'entité
     */
    private void createAndSpawnCameraEntity(Player player, CameraWaypoint position) {
        if (cameraEntity != null) {
            despawnCameraEntity(player);
        }

        cameraEntity = new WrapperEntity(EntityTypes.TEXT_DISPLAY);

        // Configurer l'interpolation client-side pour un mouvement fluide
        if (cameraEntity.getEntityMeta() instanceof TextDisplayMeta meta) {
            meta.setPositionRotationInterpolationDuration(BASE_INTERPOLATION);
        }

        // Ajouter le joueur comme viewer puis spawn l'entité
        cameraEntity.addViewer(player.getUniqueId());
        cameraEntity.spawn(
                new com.github.retrooper.packetevents.protocol.world.Location(
                        position.getX(), position.getY(), position.getZ(), 0f, 0f));

        // Faire spectater l'entité par le joueur
        spectateEntity(player, cameraEntity);
    }

    /**
     * Despawn l'entité caméra et arrête le spectate du joueur.
     *
     * @param player le joueur qui spectate l'entité
     */
    private void despawnCameraEntity(Player player) {
        if (cameraEntity != null) {
            stopSpectatingEntity(player);
            cameraEntity.despawn();
            cameraEntity.remove();
            cameraEntity = null;
        }
    }

    /**
     * Envoie un packet Camera pour faire spectater une entité par le joueur.
     *
     * @param player le joueur ciblé
     * @param entity l'entité à spectater
     */
    private void spectateEntity(Player player, WrapperEntity entity) {
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(
                    player,
                    new WrapperPlayServerCamera(entity.getEntityId()));
        } catch (Exception e) {
            System.err.println("[Cinematic] Failed to spectate entity: " + e.getMessage());
        }
    }

    /**
     * Envoie un packet Camera pour arrêter le spectate (retour au POV joueur).
     *
     * @param player le joueur ciblé
     */
    private void stopSpectatingEntity(Player player) {
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(
                    player,
                    new WrapperPlayServerCamera(player.getEntityId()));
        } catch (Exception e) {
            System.err.println("[Cinematic] Failed to stop spectating entity: " + e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Packet interception (ref: Typewriter CameraCinematicEntry.kt:251-269)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Configure l'interception de paquets pour créer une position "fake" client-side
     * et masquer l'inventaire du joueur durant la cinématique.
     * <p>
     * <ul>
     *   <li>SERVER→CLIENT: Position Y+500 (pas de self-clipping)</li>
     *   <li>SERVER→CLIENT: Fake inventory vide (immersion totale)</li>
     *   <li>CLIENT→SERVER: Correction Y-500 (vraie position serveur)</li>
     *   <li>CLIENT→SERVER: Blocage clicks inventaire + self-interact</li>
     * </ul>
     */
    private void setupPacketInterception(Player player) {
        if (packetListener != null) return;

        UUID playerUUID = player.getUniqueId();
        int playerEntityId = player.getEntityId();

        packetListener = new PacketListenerAbstract() {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (!playerUUID.equals(event.getUser().getUUID())) return;

                // SERVER→CLIENT: Fake Y position (+500) pour éviter le clipping visuel
                if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
                    WrapperPlayServerPlayerPositionAndLook packet =
                            new WrapperPlayServerPlayerPositionAndLook(event);
                    packet.setY(packet.getY() + FAKE_Y_OFFSET);
                }
                // SERVER→CLIENT: Fake inventory vide (ref: Typewriter keepFakeInventory)
                else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
                    WrapperPlayServerWindowItems packet =
                            new WrapperPlayServerWindowItems(event);
                    int itemCount = packet.getItems().size();
                    List<com.github.retrooper.packetevents.protocol.item.ItemStack> fakeItems =
                            new ArrayList<>(java.util.Collections.nCopies(
                                    itemCount,
                                    com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY
                            ));
                    packet.setItems(fakeItems);
                }
                // SERVER→CLIENT: Fake slot vide (ref: Typewriter keepFakeInventory)
                else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                    WrapperPlayServerSetSlot packet =
                            new WrapperPlayServerSetSlot(event);
                    packet.setItem(com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY);
                }
            }

            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (!playerUUID.equals(event.getUser().getUUID())) return;

                // CLIENT→SERVER: Bloquer clicks inventaire durant cinématique
                if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
                    event.setCancelled(true);
                    return;
                }
                if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW_BUTTON) {
                    event.setCancelled(true);
                    return;
                }

                // CLIENT→SERVER: Corriger Y position (-500) pour la vraie position serveur
                if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
                    WrapperPlayClientPlayerPosition packet =
                            new WrapperPlayClientPlayerPosition(event);
                    Vector3d pos = packet.getPosition();
                    packet.setPosition(new Vector3d(pos.x, pos.y - FAKE_Y_OFFSET, pos.z));
                }
                // CLIENT→SERVER: Corriger Y position + rotation (-500)
                else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                    WrapperPlayClientPlayerPositionAndRotation packet =
                            new WrapperPlayClientPlayerPositionAndRotation(event);
                    Vector3d pos = packet.getPosition();
                    packet.setPosition(new Vector3d(pos.x, pos.y - FAKE_Y_OFFSET, pos.z));
                }
                // CLIENT→SERVER: Empêcher self-interaction durant cinématique
                else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
                    WrapperPlayClientInteractEntity packet =
                            new WrapperPlayClientInteractEntity(event);
                    if (packet.getEntityId() == playerEntityId) {
                        event.setCancelled(true);
                    }
                }
            }
        };

        try {
            PacketEvents.getAPI().getEventManager().registerListener(packetListener);
        } catch (Exception e) {
            System.err.println("[Cinematic] Failed to setup packet interception: " + e.getMessage());
            packetListener = null;
        }
    }

    /**
     * Nettoie l'interception de paquets enregistrée.
     */
    private void cleanupPacketInterception() {
        if (packetListener != null) {
            try {
                PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            } catch (Exception e) {
                System.err.println("[Cinematic] Failed to cleanup packet interception: " + e.getMessage());
            }
            packetListener = null;
        }
    }

    /**
     * Vérifie si le joueur est un joueur Bedrock via Floodgate (Geyser).
     * Les joueurs Bedrock ne supportent pas la manipulation de paquets Java.
     * Détection par préfixe UUID Floodgate: 00000000-0000-0000-0009-*
     */
    private boolean isFloodgatePlayer(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            return uuid.startsWith("00000000-0000-0000-0009-");
        } catch (Exception e) {
            return false;
        }
    }
}
