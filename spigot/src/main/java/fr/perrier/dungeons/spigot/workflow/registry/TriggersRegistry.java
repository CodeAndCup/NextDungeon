package fr.perrier.dungeons.spigot.workflow.registry;

import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.TriggerEventHandler;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.impl.EntityDeathTriggerHandler;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.impl.RegionTriggerHandler;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.impl.BlockClickTriggerHandler;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.impl.PlayerDamageTriggerHandler;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.impl.ItemPickupTriggerHandler;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.impl.ChatMessageTriggerHandler;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.impl.PlayerJumpTriggerHandler;
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
public class TriggersRegistry implements Listener {

    // Cache des triggers par type
    private final Map<String, List<Trigger>> triggersByType = new ConcurrentHashMap<>();

    // Cache des triggers par type d'événement
    private final Map<Class<? extends Event>, List<Trigger>> triggersByEventType = new ConcurrentHashMap<>();

    // Handlers enregistrés - multiple handlers per event type
    private final Map<Class<? extends Event>, List<TriggerEventHandler<?>>> handlers = new HashMap<>();

    private final Map<String, FunctionTrigger> registeredFunctions = new HashMap<>();


    // Instance singleton
    private static TriggersRegistry instance;

    public TriggersRegistry() {
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
            } catch (Exception e) {
                // Handle registration failures
                if(Main.getLoggerUtil().isDebugEnabled()) {
                    Main.getLoggerUtil().severe("Failed to register event listener for: " + eventClass.getSimpleName() + " - " + e.getMessage());
                }
            }
        }

        Main.getLoggerUtil().info("GlobalTriggerManager initialized with " + handlers.size() + " handlers");
    }

    /**
     * Registers the default trigger handlers.
     */
    private void registerDefaultHandlers() {
        registerHandler(new RegionTriggerHandler());
        registerHandler(new EntityDeathTriggerHandler());
        registerHandler(new BlockClickTriggerHandler());
        registerHandler(new PlayerDamageTriggerHandler());
        registerHandler(new ItemPickupTriggerHandler());
        registerHandler(new ChatMessageTriggerHandler());
        registerHandler(new PlayerJumpTriggerHandler());
        
        if(Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Registered handlers:");
            for (Map.Entry<Class<? extends Event>, List<TriggerEventHandler<?>>> entry : handlers.entrySet()) {
                for (TriggerEventHandler<?> handler : entry.getValue()) {
                    Main.getLoggerUtil().info("  - " + handler.getClass().getSimpleName() + " for " + entry.getKey().getSimpleName() + " (supports: " + handler.getSupportedTriggerTypes() + ")");
                }
            }
        }
    }

    /**
     * Registers a new trigger handler.
     *
     * @param handler the handler to register
     * @param <T>     the event type
     */
    public <T extends Event> void registerHandler(TriggerEventHandler<T> handler) {
        handlers.computeIfAbsent(handler.getEventType(), k -> new ArrayList<>()).add(handler);
        if (Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Handler registered for: " + handler.getEventType().getSimpleName());
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
                    Main.getLoggerUtil().warning("Invalid TriggerData in cache: " + triggerData.getName());
                    continue;
                }

                if (!trigger.isEnabled()) {
                    if(Main.getLoggerUtil().isDebugEnabled()) {
                        Main.getLoggerUtil().info("Skipping disabled trigger: " + trigger.getName());
                    }
                    continue;
                }

                // Cache par type de trigger
                triggersByType.computeIfAbsent(trigger.getType(), k -> new ArrayList<>()).add(trigger);
                
                if(Main.getLoggerUtil().isDebugEnabled()) {
                    Main.getLoggerUtil().info("Processing trigger: " + trigger.getName() + " (type: " + trigger.getType() + ", class: " + trigger.getClass().getSimpleName() + ")");
                }

                // Cache par type d'événement
                boolean addedToEventCache = false;
                for (Map.Entry<Class<? extends Event>, List<TriggerEventHandler<?>>> entry : handlers.entrySet()) {
                    for (TriggerEventHandler<?> handler : entry.getValue()) {
                        if(Main.getLoggerUtil().isDebugEnabled()) {
                            Main.getLoggerUtil().info("  Checking handler: " + handler.getClass().getSimpleName() + " (event: " + entry.getKey().getSimpleName() + ", supports: " + handler.getSupportedTriggerTypes() + ")");
                        }
                        if (handler.getSupportedTriggerTypes().contains(trigger.getType())) {
                            triggersByEventType.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(trigger);
                            addedToEventCache = true;
                            if(Main.getLoggerUtil().isDebugEnabled()) {
                                Main.getLoggerUtil().info("  -> Added trigger to event cache for " + entry.getKey().getSimpleName());
                            }
                            break; // Add trigger only once per event type
                        }
                    }
                }
                
                if(!addedToEventCache && Main.getLoggerUtil().isDebugEnabled()) {
                    Main.getLoggerUtil().warning("Trigger " + trigger.getName() + " (type: " + trigger.getType() + ") was not added to any event cache!");
                }
            }

            Main.getLoggerUtil().info("Triggers cache refresh complete: " + allTriggers.size() + " triggers");
            if(Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("Cache contents - triggersByType: " + triggersByType.keySet());
                Main.getLoggerUtil().info("Cache contents - triggersByEventType: " + triggersByEventType.keySet().stream().map(Class::getSimpleName).toList());
            }

        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error refreshing cache: " + e.getMessage());
            e.printStackTrace(System.err);
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
        Main.getLoggerUtil().info("Function registered: " + name);
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
            Main.getLoggerUtil().info("Function removed: " + name);
        }
    }

    /**
     * Clears all registered functions.
     */
    public void clearFunctions() {
        int count = registeredFunctions.size();
        registeredFunctions.clear();
        if (Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Cleared " + count + " registered functions");
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
        List<TriggerEventHandler<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null && !eventHandlers.isEmpty()) {
            List<Trigger> triggers = getTriggersForEventType(event.getClass());
            if(Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("Processing event: " + event.getEventName() + " with " + triggers.size() + " triggers");
            }
            if (!triggers.isEmpty()) {
                for (TriggerEventHandler<?> handler : eventHandlers) {
                    if(Main.getLoggerUtil().isDebugEnabled()) {
                        Main.getLoggerUtil().info("Invoking handler: " + handler.getClass().getSimpleName() + " for event: " + event.getEventName());
                    }
                    TriggerEventHandler<T> typedHandler = (TriggerEventHandler<T>) handler;
                    typedHandler.handleEvent(event, triggers);
                }
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
