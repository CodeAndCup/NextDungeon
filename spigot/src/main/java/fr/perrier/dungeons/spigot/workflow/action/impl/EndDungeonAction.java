package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;

@Setter
@Getter
@BlocklyInfo(
        name = "end_dungeon_action",
        color = "#2196F3",
        displayText = "\uD83C\uDFC1\u200B Terminer le donjon",
        tooltip = "Termine le donjon en cours.",
        category = "Actions"
)
public class EndDungeonAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    public EndDungeonAction() {
        super("EndDungeon", "end_dungeon_action");
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        FloorInstance floorInstance = Main.getInstance().getDungeonService().getCurrentInstance();
        floorInstance.complete(true);
        return true;
    }
}
