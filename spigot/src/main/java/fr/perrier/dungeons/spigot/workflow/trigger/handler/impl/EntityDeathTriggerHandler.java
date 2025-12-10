package fr.perrier.dungeons.spigot.workflow.trigger.handler.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.TriggerEventHandler;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.EntityDeathTrigger;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityDeathTriggerHandler implements TriggerEventHandler<EntityDeathEvent> {

    @Override
    public Class<EntityDeathEvent> getEventType() {
        return EntityDeathEvent.class;
    }

    @Override
    public List<String> getSupportedTriggerTypes() {
        return List.of("entity_death_trigger");
    }

    @Override
    public void handleEvent(EntityDeathEvent event, List<Trigger> triggers) {
        Player player = getPlayerFromEvent(event);

        for (Trigger trigger : triggers) {
            if (trigger instanceof EntityDeathTrigger entityDeathTrigger) {
                if(Main.getInstance().getServer().getPluginManager().getPlugin("MythicMobs") == null) {
                    checkEntityDeathTrigger(player, entityDeathTrigger, event);
                } else {
                    checkMythicMobEntityDeathTrigger(player, entityDeathTrigger, event);
                }
            }
        }
    }

    private void checkEntityDeathTrigger(Player player, EntityDeathTrigger entityDeathTrigger, EntityDeathEvent event) {
        Entity entity = event.getEntity();

        ActiveMob mythicMob = MythicBukkit.inst().getMobManager().getMythicMobInstance(entity);

        if(mythicMob != null && mythicMob.getType().getInternalName().equalsIgnoreCase(entityDeathTrigger.getEntityType())) {
            Map<String, Object> data = extractEventData(event);
            if (entityDeathTrigger.checkConditions(player, data)) {
                entityDeathTrigger.execute(player, entity.getLocation(), data);
            }
        } else {
            checkEntityDeathTrigger(player, entityDeathTrigger, event);
        }
    }

    private void checkMythicMobEntityDeathTrigger(Player player, EntityDeathTrigger entityDeathTrigger, EntityDeathEvent event) {
        Entity entity = event.getEntity();

        if(entity.getType().name().equalsIgnoreCase(entityDeathTrigger.getEntityType())) {
            Map<String, Object> data = extractEventData(event);
            if (entityDeathTrigger.checkConditions(player, data)) {
                entityDeathTrigger.execute(player, entity.getLocation(), data);
            }
        }
    }

    @Override
    public Map<String, Object> extractEventData(EntityDeathEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("event_type", "entity_death");
        data.put("entity", event.getEntity());
        data.put("location", event.getEntity().getLocation());
        data.put("dropped_items", event.getDrops());
        data.put("experience", event.getDroppedExp());
        data.put("killer", event.getEntity().getKiller());
        return data;
    }

    @Override
    public Player getPlayerFromEvent(EntityDeathEvent event) {
        return event.getEntity().getKiller();
    }
}
