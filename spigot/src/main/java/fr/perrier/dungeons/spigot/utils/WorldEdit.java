package fr.perrier.dungeons.spigot.utils;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import fr.perrier.dungeons.spigot.Main;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

public class WorldEdit {

    /**
     * Apply a schematic at the given coordinates in the "world" world.
     *
     * @param schematicName the name of the schematic file (without .schematic extension)
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    public static void applySchematic(String schematicName, int x, int y, int z) {
        File file = new File(Main.getInstance().getDataFolder() + File.separator + "schematics" + File.separator + schematicName + ".schematic");
        if (!file.exists()) {
            Main.getInstance().getLogger().severe("The schematic file " + schematicName + ".schematic does not exist.");
            return;
        }

        BlockVector3 to = BlockVector3.at(x, y, z);

        World weWorld = BukkitAdapter.adapt(Objects.requireNonNull(Bukkit.getWorld("world")));
        Clipboard clipboard;

        // Detect correct format
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            Main.getInstance().getLogger().severe("Could not detect schematic format for file " + schematicName + ".schematic");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             ClipboardReader reader = format.getReader(fis)) {

            clipboard = reader.read();

            // Prepare the EditSession
            try (EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(weWorld)) {

                // Paste the clipboard
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                Operation operation = holder
                        .createPaste(editSession)
                        .to(to)
                        // .ignoreAirBlocks(true)
                        .build();

                Operations.complete(operation);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}