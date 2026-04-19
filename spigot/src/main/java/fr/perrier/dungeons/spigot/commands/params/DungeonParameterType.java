package fr.perrier.dungeons.spigot.commands.params;

import fr.perrier.cupcodeapi.commands.annotations.ParameterType;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Dungeon;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class DungeonParameterType implements ParameterType<Dungeon> {
    @Override
    public Dungeon transform(CommandSender commandSender, String source) {
        try {
            return Main.getInstance().getDungeonService().getDungeon(source);
        }catch (Exception e){
            commandSender.sendMessage(ChatUtil.translate("&cAucun donjon trouvé avec ce nom"));
            return null;
        }
    }

    @Override
    public List<String> tabComplete(Player player, Set<String> set, String s) {
        return Main.getInstance().getDungeonService().getDungeonsMap().values().stream().map(Dungeon::getName).toList();
    }
}
