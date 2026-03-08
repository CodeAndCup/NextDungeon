package fr.perrier.dungeons.spigot.commands.params;

import fr.perrier.cupcodeapi.commands.annotations.ParameterType;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.common.module.NextDungeonModule;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.module.ModuleLoader;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class ModuleParameterType implements ParameterType<NextDungeonModule> {
    @Override
    public NextDungeonModule transform(CommandSender commandSender, String source) {
        try {
            ModuleLoader loader = Main.getInstance().getModuleLoader();
            return loader.getLoadedModules().get(source);
        }catch (Exception e){
            commandSender.sendMessage(ChatUtil.translate("&cAucun module trouvé avec ce nom"));
            return null;
        }
    }

    @Override
    public List<String> tabComplete(Player player, Set<String> set, String s) {
        ModuleLoader loader = Main.getInstance().getModuleLoader();
        return loader.getLoadedModules().values().stream().map(NextDungeonModule::getId).toList();
    }
}
