package fr.perrier.dungeons.spigot.commands.params;

import fr.perrier.cupcodeapi.commands.annotations.ParameterType;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.Floor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class FloorParameterType implements ParameterType<FloorData> {
    @Override
    public Floor transform(CommandSender commandSender, String source) {
        try {
            return Main.getInstance().getDungeonService().getFloor(source);
        }catch (Exception e){
            commandSender.sendMessage(ChatUtil.translate("&cAucun donjon trouvé avec ce nom"));
            return null;
        }
    }

    @Override
    public List<String> tabComplete(Player player, Set<String> set, String s) {
        return Main.getInstance().getDungeonService().getFloorsMap().values().stream().map(FloorData::getId).toList();
    }
}
