package fr.perrier.dungeons.menu.dungeon;


import com.alessiodp.parties.api.interfaces.Party;
import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.menuapi.GlassMenu;
import fr.perrier.cupcodeapi.menuapi.Menu;
import fr.perrier.cupcodeapi.menuapi.buttons.BackButton;
import fr.perrier.cupcodeapi.menuapi.buttons.ConversationButton;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.ItemBuilder;
import fr.perrier.dungeons.parties.DungeonParty;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class PartyBuilderMenu extends GlassMenu {
    private final Menu oldMenu;
    private final String dungeonId;
    private String floorId;
    private int minLevel = -1;
    private String description = "";

    @Override
    public String getTitle(Player player) {
        return "Party Builder";
    }

    @Override
    public int getGlassColor() {
        return 0;
    }

    @Override
    public Map<Integer, Button> getAllButtons(Player player) {
        HashMap<Integer, Button> buttons = new HashMap<>();

        buttons.put(12, new SelectFloorButton());
        buttons.put(13, EditDescriptionButton(player));
        buttons.put(14, EditMinLevelButton(player));

        buttons.put(30, new CancelButton());
        buttons.put(32, new ConfirmButton());

        return buttons;
    }

    public class SelectFloorButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return null;
        }
    }

    public Button EditDescriptionButton(Player player) {
        return new ConversationButton<>(
                new ItemBuilder(Material.PAPER).toItemStack(),
                player,
                ChatUtil.translate("&fPlease enter the description"),
                (target, result) -> {
                    String description = result.getRight();
                    if(description.length() <= 32) {
                        this.description = description;
                        player.sendRawMessage(ChatUtil.translate("&aDescription updated to: &f" + description));
                        this.openMenu(player);
                    } else {
                        player.sendRawMessage(ChatUtil.translate("&cYou can not have more than 32 characters in the description."));
                    }
                }
        );
    }

    public Button EditMinLevelButton(Player player) {
        return new ConversationButton<>(
                new ItemBuilder(Material.EXPERIENCE_BOTTLE).toItemStack(),
                player,
                ChatUtil.translate("&fPlease enter the minimum player level"),
                (target, result) -> {
                    String minLevel = result.getRight();
                    if(StringUtils.isNumeric(minLevel)) {
                        int minLevelInt = Integer.parseInt(minLevel);
                        if(minLevelInt < 0) {
                            player.sendRawMessage(ChatUtil.translate("&cYou can not have a negative minimum player level."));
                            return;
                        }
                        this.minLevel = Integer.parseInt(minLevel);
                        player.sendRawMessage(ChatUtil.translate("&aMinimum player level updated to: &f" + minLevel));
                        this.openMenu(player);
                    } else {
                        player.sendRawMessage(ChatUtil.translate("&cYou must enter a number."));
                    }
                }
        );
    }

    public class CancelButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.REDSTONE_BLOCK).toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            oldMenu.openMenu(player);
        }
    }

    public class ConfirmButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.EMERALD_BLOCK).toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            DungeonParty party = new DungeonParty.Builder()
                    .setDungeonId(dungeonId)
                    .setFloorId(floorId)
                    .setMinLevel(minLevel)
                    .setDescription(description)
                    .setLeader(player)
                    .build();
        }
    }
}
