package fr.perrier.dungeons.spigot.manager;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CorpsManager {

    public void spawnCorpsOf(Player player) {
        List<EntityData<?>> entityData = new ArrayList<>();
        entityData.add(new EntityData<>(6, EntityDataTypes.ENTITY_POSE, EntityPose.SLEEPING));
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(id, entityData);
        //TODO: Send packet to players around
    }
}
