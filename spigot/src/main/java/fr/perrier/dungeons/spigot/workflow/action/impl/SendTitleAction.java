package fr.perrier.dungeons.spigot.workflow.action.impl;

import com.cryptomorin.xseries.messages.Titles;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.Serial;
import java.util.Map;

/**
 * Action pour envoyer un title à un joueur
 */
@Setter
@Getter
@BlocklyInfo(
        name = "send_title_action",
        color = "#2196F3",
        displayText = "💬 Envoyer title",
        tooltip = "Envoie un title à un joueur spécifique\n{player} = joueur déclencheur\n{target} = joueur cible\n& = codes couleur",
        category = "Actions"
)
public class SendTitleAction extends Action implements BlocklyAction {
    @Serial
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "À:",
            options = "player,@all", defaultValue = "player", order = 1)
    private String targetPlayer;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Title (optionnel):",
            defaultValue = "&#00FF00Bonjour {player}!", order = 2)
    private String title;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Subtitle (optionnel):",
            defaultValue = "", order = 3)
    private String subtitle;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Fade In (ticks):",
            defaultValue = "10", order = 4)
    private int fadeIn;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Stay (ticks):",
            defaultValue = "70", order = 5)
    private int stay;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Fade Out (ticks):",
            defaultValue = "20", order = 6)
    private int fadeOut;

    public SendTitleAction() {
        super("SendTitle", "send_title_action");
        this.targetPlayer = "player";
        this.title = "&#00FF00Bonjour {player}!";
        this.subtitle = "";
        this.fadeIn = 10;
        this.stay = 70;
        this.fadeOut = 20;
    }

    public SendTitleAction(String targetPlayer, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        super("SendTitle", "send_title_action");
        this.targetPlayer = targetPlayer;
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }
    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (title == null || subtitle == null) {
            return false;
        }

        Player target = null;

        // Déterminer le joueur cible
        if (targetPlayer == null || targetPlayer.isEmpty() || "player".equals(targetPlayer)) {
            target = triggerPlayer; // Le joueur qui a déclenché le trigger
        } else if("@all".equals(targetPlayer)) {
            // Envoyer à tous les joueurs en ligne

            String finalTitle = title
                    .replace("{player}", triggerPlayer != null ? triggerPlayer.getName() : "Unknown")
                    .replace("{target}", "Tous")
                    .replace("{trigger}", data.getOrDefault("trigger_name", "Unknown").toString());

            String finalSubtitle = subtitle
                    .replace("{player}", triggerPlayer != null ? triggerPlayer.getName() : "Unknown")
                    .replace("{target}", "Tous")
                    .replace("{trigger}", data.getOrDefault("trigger_name", "Unknown").toString());

            for(Player p : Bukkit.getOnlinePlayers()) {
                Titles.sendTitle(p,fadeIn,stay,fadeOut,ChatUtil.translate(finalTitle), ChatUtil.translate(finalSubtitle));
            }
            return true;
        } else {
            // Chercher un joueur par nom exact
            target = Bukkit.getPlayerExact(targetPlayer);
        }

        if (target != null && target.isOnline()) {
            String finalTitle = title
                    .replace("{player}", triggerPlayer != null ? triggerPlayer.getName() : "Unknown")
                    .replace("{target}", "Tous")
                    .replace("{trigger}", data.getOrDefault("trigger_name", "Unknown").toString());

            String finalSubtitle = subtitle
                    .replace("{player}", triggerPlayer != null ? triggerPlayer.getName() : "Unknown")
                    .replace("{target}", "Tous")
                    .replace("{trigger}", data.getOrDefault("trigger_name", "Unknown").toString());

            Titles.sendTitle(target,fadeIn,stay,fadeOut,ChatUtil.translate(finalTitle), ChatUtil.translate(finalSubtitle));
            return true;
        }

        return false;
    }
}
