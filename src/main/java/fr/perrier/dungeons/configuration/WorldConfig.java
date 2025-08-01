package fr.perrier.dungeons.configuration;

import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.exceptions.NewerFormatException;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.utils.ASWMUtil;
import fr.perrier.dungeons.utils.Position;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.UUID;

@Getter
public class WorldConfig {

    private String name;
    private UUID id;
    private SlimeWorld slimeWorld;
    private SlimePropertyMap properties;

    public WorldConfig(String name) {
        this.name = name;
    }

    public void setProperties(String difficulty, Position spawn) {
        SlimePropertyMap properties = new SlimePropertyMap();
        properties.setValue(SlimeProperties.DIFFICULTY,difficulty);
        properties.setValue(SlimeProperties.SPAWN_X,spawn.getX());
        properties.setValue(SlimeProperties.SPAWN_Y,spawn.getY());
        properties.setValue(SlimeProperties.SPAWN_Z,spawn.getZ());
        this.properties = properties;
    }

    public void loadWorld() {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                // ATTEMPT TO LOAD WORLD
                long start = System.currentTimeMillis();
                SlimeLoader loader = ASWMUtil.getLoaderType();

                UUID id = UUID.randomUUID();
                SlimeWorld slimeWorld = Main.getInstance().getAswmAPI().loadWorld(loader, name, true, properties).clone(name + "_" + id);
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    try {
                        Main.getInstance().getAswmAPI().generateWorld(slimeWorld);
                    } catch (IllegalArgumentException ex) {
                        Bukkit.getLogger().severe(ChatColor.RED + "Failed to generate world " + name + ": " + ex.getMessage() + ".");
                        return;
                    }
                    Bukkit.getLogger().info("Generated world " + name + " in " + (System.currentTimeMillis() - start) + "ms.");
                });
            } catch (CorruptedWorldException ex) {
                Bukkit.getLogger().severe("Failed to load world " + name + ": world seems to be corrupted.");
            } catch (NewerFormatException ex) {
                Bukkit.getLogger().severe(ChatColor.RED + "Failed to load world " + name + ": this world" +
                        " was serialized with a newer version of the Slime Format (" + ex.getMessage() + ") that SWM cannot understand.");
            } catch (UnknownWorldException ex) {
                Bukkit.getLogger().severe(ChatColor.RED + "Failed to load world " + name +
                        ": world could not be found.");
            } catch (WorldInUseException ex) {
                Bukkit.getLogger().severe(ChatColor.RED + "Failed to load world " + name +
                        ": world is already in use. If you think this is a mistake, please wait some time and try again.");
            } catch (IllegalArgumentException ex) {
                Bukkit.getLogger().severe(ChatColor.RED + "Failed to load world " + name +
                        ": " + ex.getMessage());
            } catch (IOException ex) {
                Bukkit.getLogger().severe("Failed to load world " + name + ":");
                ex.fillInStackTrace();
            }
        });
    }
}
