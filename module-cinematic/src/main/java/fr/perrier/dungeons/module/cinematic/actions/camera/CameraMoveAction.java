package fr.perrier.dungeons.module.cinematic.actions.camera;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import fr.perrier.dungeons.module.cinematic.action.SimpleCinematicAction;
import fr.perrier.dungeons.module.cinematic.interpolation.PositionInterpolator;
import fr.perrier.dungeons.module.cinematic.model.CameraWaypoint;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Action de mouvement de caméra fluide utilisant l'interpolation Catmull-Rom.
 * <p>
 * Technique:
 * 1. Calcul du chemin interpolé au démarrage du segment
 * 2. Chaque frame: interpoler la position sur le chemin
 * 3. Tous les 10 frames: téléporter le joueur réel (pour le chargement de chunks)
 * 4. Packet interception: fake Y+500 client-side pour éviter le clipping visuel
 *
 * @see <a href="https://github.com/gabber235/Typewriter">Typewriter CameraCinematicEntry.kt</a>
 */
public class CameraMoveAction extends SimpleCinematicAction<CameraSegment> {

    private static final double FAKE_Y_OFFSET = 500.0;

    private List<CameraSegment> segments = new ArrayList<>();
    private final transient Map<CameraSegment, List<CameraWaypoint>> segmentPaths = new HashMap<>();
    private transient PacketListenerAbstract packetListener = null;

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

        // Téléporter joueur tous les 10 frames pour le chargement de chunks
        if (frame % 10 == 0) {
            player.teleport(loc);
        }
    }

    @Override
    protected void onSegmentStop(Player player, CameraSegment segment) throws Exception {
        segmentPaths.remove(segment);
        cleanupPacketInterception();
    }

    @Override
    public List<CameraSegment> getCinematicSegments() {
        return segments;
    }

    /**
     * Configure l'interception de paquets pour créer une position "fake" client-side.
     * <p>
     * Référence: Typewriter CameraCinematicEntry.kt lignes 251-269
     * <ul>
     *   <li>SERVER→CLIENT: Position Y+500 (client pense joueur est haut, pas de self-clipping)</li>
     *   <li>CLIENT→SERVER: Correction Y-500 (serveur gère vraie position)</li>
     *   <li>CLIENT→SERVER: Blocage self-interact (joueur ne peut pas interact avec lui-même)</li>
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
            }

            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (!playerUUID.equals(event.getUser().getUUID())) return;

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
     */
    private boolean isFloodgatePlayer(Player player) {
        try {
            return player.getClass().getName().contains("Floodgate");
        } catch (Exception e) {
            return false;
        }
    }
}
