package fr.perrier.dungeons.spigot.workflow.action.conditional;

import fr.perrier.dungeons.spigot.workflow.action.Action;

/**
 * Interface for actions that support conditional execution (IF/ELSE branches).
 * This interface eliminates the need for instanceof checks when loading conditional actions.
 * Follows the Interface Segregation Principle by providing a focused contract.
 */
public interface ConditionalAction {
    
    /**
     * Adds an action to be executed when the condition is true (IF branch).
     * 
     * @param action the action to add to the IF branch
     */
    void addIfAction(Action action);
    
    /**
     * Adds an action to be executed when the condition is false (ELSE branch).
     * 
     * @param action the action to add to the ELSE branch
     */
    void addElseAction(Action action);
}
