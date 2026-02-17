package fr.perrier.dungeons.spigot.workflow.context;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.registry.TriggersRegistry;
import fr.perrier.dungeons.spigot.workflow.registry.VariableRegistry;

/**
 * Default implementation of WorkflowContext that delegates to Main singleton.
 * This adapter allows gradual migration from direct Main access to dependency injection.
 */
public class DefaultWorkflowContext implements WorkflowContext {
    
    @Override
    public TriggersRegistry getTriggersRegistry() {
        return Main.getInstance().getTriggersRegistry();
    }
    
    @Override
    public VariableRegistry getVariableRegistry() {
        return Main.getInstance().getVariableRegistry();
    }
    
    @Override
    public boolean isDebugEnabled() {
        return Main.getLoggerUtil().isDebugEnabled();
    }
    
    @Override
    public void logInfo(String message) {
        Main.getLoggerUtil().info(message);
    }
    
    @Override
    public void logWarning(String message) {
        Main.getLoggerUtil().warning(message);
    }
    
    @Override
    public void logSevere(String message) {
        Main.getLoggerUtil().severe(message);
    }
}
