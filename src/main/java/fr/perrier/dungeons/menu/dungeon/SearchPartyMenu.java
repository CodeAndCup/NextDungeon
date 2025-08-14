package fr.perrier.dungeons.menu.dungeon;

import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.menuapi.pagination.PaginatedMenu;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class SearchPartyMenu extends PaginatedMenu {
    @Override
    public String getPrePaginatedTitle(Player player) {
        return "";
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        HashMap<Integer, Button> buttons = new HashMap<>();

        return buttons;
    }
}
