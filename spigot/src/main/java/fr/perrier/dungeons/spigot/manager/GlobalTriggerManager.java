package fr.perrier.dungeons.spigot.manager;

import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.TriggerEventHandler;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.impl.EntityDeathTriggerHandler;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.impl.RegionTriggerHandler;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.FunctionTrigger;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global manager for all types of triggers.
 * Handles registration, caching, and event processing for triggers and their handlers.
 */
public class GlobalTriggerManager implements Listener {

    // Cache des triggers par type
    private final Map<String, List<Trigger>> triggersByType = new ConcurrentHashMap<>();

    // Cache des triggers par type d'événement
    private final Map<Class<? extends Event>, List<Trigger>> triggersByEventType = new ConcurrentHashMap<>();

    // Handlers enregistrés
    private final Map<Class<? extends Event>, TriggerEventHandler<?>> handlers = new HashMap<>();

    private final Map<String, FunctionTrigger> registeredFunctions = new HashMap<>();


    // Instance singleton
    private static GlobalTriggerManager instance;

    public GlobalTriggerManager() {
        registerDefaultHandlers();
    }

    /**
     * Initializes the global trigger manager and registers event listeners for all handlers.
     */
    public void initialize() {
        // Enregistrer ce manager comme listener principal
        Listener dummyListener = new Listener() {};
        EventExecutor executor = (listener, event) -> {
            processEvent(event);
        };

        for (Class<? extends Event> eventClass : handlers.keySet()) {
            try {
                Bukkit.getPluginManager().registerEvent(eventClass, dummyListener, EventPriority.NORMAL, executor, Main.getInstance());
            } catch (Exception ignored) {
                // Handle registration failures
            }
        }

        Main.getInstance().getLogger().info("GlobalTriggerManager initialized with " + handlers.size() + " handlers");
    }

    /**
     * Registers the default trigger handlers.
     */
    private void registerDefaultHandlers() {
        registerHandler(new RegionTriggerHandler());
        registerHandler(new EntityDeathTriggerHandler());
    }

    /**
     * Registers a new trigger handler.
     *
     * @param handler the handler to register
     * @param <T>     the event type
     */
    public <T extends Event> void registerHandler(TriggerEventHandler<T> handler) {
        handlers.put(handler.getEventType(), handler);
        if (Main.isDebug()) {
            Main.getInstance().getLogger().info("Handler registered for: " + handler.getEventType().getSimpleName());
        }
    }

    /**
     * Refreshes the trigger cache from the current floor's triggers.
     * Populates caches by trigger type and event type.
     */
    public void refreshTriggerCache() {
        triggersByType.clear();
        triggersByEventType.clear();

        try {
            List<TriggerData> allTriggers = Main.getInstance().getDungeonService().getCurrentFloor().getTriggers();

            for (TriggerData triggerData : allTriggers) {
                if( !(triggerData instanceof Trigger trigger)) {
                    Main.getInstance().getLogger().warning("&eInvalid TriggerData in cache: " + triggerData.getName());
                    continue;
                }

                if (!trigger.isEnabled()) continue;

                // Cache par type de trigger
                triggersByType.computeIfAbsent(trigger.getType(), k -> new ArrayList<>()).add(trigger);

                // Cache par type d'événement
                for (Map.Entry<Class<? extends Event>, TriggerEventHandler<?>> entry : handlers.entrySet()) {
                    if (entry.getValue().getSupportedTriggerTypes().contains(trigger.getType())) {
                        triggersByEventType.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(trigger);
                    }
                }
            }

            Main.getInstance().getLogger().info("Triggers cache refresh complete: " + allTriggers.size() + " triggers");

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Error refreshing cache: " + e.getMessage());
        }
    }

    /**
     * Registers a function trigger definition.
     *
     * @param function the function trigger to register
     */
    public void registerFunction(FunctionTrigger function) {
        if (function == null || function.getFunctionName() == null) {
            return;
        }

        String name = function.getFunctionName().trim();
        if (name.isEmpty()) {
            return;
        }

        registeredFunctions.put(name, function);
        Main.getInstance().getLogger().info("Function registered: " + name);
    }

    /**
     * Gets a registered function by its name.
     *
     * @param name the function name
     * @return the FunctionTrigger, or null if not found
     */
    public FunctionTrigger getFunction(String name) {
        if (name == null) {
            return null;
        }
        return registeredFunctions.get(name.trim());
    }

    /**
     * Removes a registered function by its name.
     *
     * @param name the function name
     */
    public void removeFunction(String name) {
        if (name != null) {
            registeredFunctions.remove(name.trim());
            Main.getInstance().getLogger().info("Function removed: " + name);
        }
    }

    /**
     * Clears all registered functions.
     */
    public void clearFunctions() {
        int count = registeredFunctions.size();
        registeredFunctions.clear();
        if (Main.isDebug()) {
            Main.getInstance().getLogger().info("Cleared " + count + " registered functions");
        }
    }

    /**
     * Gets all registered function names.
     *
     * @return an array of function names
     */
    public String[] getFunctionNames() {
        return registeredFunctions.keySet().toArray(new String[0]);
    }

    /**
     * Gets triggers of a specific type.
     *
     * @param type the trigger type
     * @return a list of triggers of the given type
     */
    public List<Trigger> getTriggersByType(String type) {
        return triggersByType.getOrDefault(type, Collections.emptyList());
    }

    /**
     * Gets triggers for a specific event type.
     *
     * @param eventType the event class
     * @return a list of triggers for the given event type
     */
    public List<Trigger> getTriggersForEventType(Class<? extends Event> eventType) {
        return triggersByEventType.getOrDefault(eventType, Collections.emptyList());
    }

    /**
     * Processes a generic event - called automatically by the handlers.
     *
     * @param event the event to process
     * @param <T>   the event type
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> void processEvent(T event) {
        TriggerEventHandler<T> handler = (TriggerEventHandler<T>) handlers.get(event.getClass());
        if (handler != null) {
            List<Trigger> triggers = getTriggersForEventType(event.getClass());
            if (!triggers.isEmpty()) {
                handler.handleEvent(event, triggers);
            }
        }
    }

    /**
     * Returns cache statistics.
     *
     * @return a map containing cache statistics
     */
    public Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total_triggers", triggersByType.values().stream().mapToInt(List::size).sum());
        stats.put("trigger_types", triggersByType.size());
        stats.put("event_types", triggersByEventType.size());
        stats.put("handlers", handlers.size());
        return stats;
    }
}
