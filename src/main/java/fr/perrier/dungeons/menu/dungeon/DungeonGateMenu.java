package fr.perrier.dungeons.menu.dungeon;

import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.menuapi.GlassMenu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class DungeonGateMenu extends GlassMenu {

    @Override
    public int getGlassColor() {
        return 15;
    }

    @Override
    public Map<Integer, Button> getAllButtons(Player player) {
        HashMap<Integer, Button> buttons = new HashMap<>();

        return buttons;
    }

    @Override
    public String getTitle(Player player) {
        return "";
    }

    private class FloorButton extends Button {

        @Override
        public ItemStack getButtonItem(Player player) {
            return null;
        }
    }
}
