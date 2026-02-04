package fr.perrier.dungeons.spigot.workflow.blocks;

import com.google.gson.JsonObject;

/**
 * Helper class to parse LocationBlock from JSON data
 */
public class LocationBlockParser {

    /**
     * Parse a LocationBlock from JSON data
     * @param data The JSON object containing location data
     * @param fieldName The name of the field in the JSON (e.g., "location", "position")
     * @return A LocationBlock or null if no location data is present
     */
    public static LocationBlock parseFromJson(JsonObject data, String fieldName) {
        if (!data.has(fieldName)) {
            return null;
        }

        JsonObject locData = data.getAsJsonObject(fieldName);
        if (locData == null || locData.isJsonNull()) {
            return null;
        }

        LocationBlock block = new LocationBlock();

        // Parse coordinates
        if (locData.has("x")) {
            block.setX(locData.get("x").getAsDouble());
        }
        if (locData.has("y")) {
            block.setY(locData.get("y").getAsDouble());
        }
        if (locData.has("z")) {
            block.setZ(locData.get("z").getAsDouble());
        }

        // Parse world if present
        if (locData.has("hasWorld") && locData.get("hasWorld").getAsBoolean()) {
            block.setHasWorld(true);
            if (locData.has("world")) {
                block.setWorldName(locData.get("world").getAsString());
            }
        }

        // Parse rotation if present
        if (locData.has("hasRotation") && locData.get("hasRotation").getAsBoolean()) {
            block.setHasRotation(true);
            if (locData.has("yaw")) {
                block.setYaw(locData.get("yaw").getAsFloat());
            }
            if (locData.has("pitch")) {
                block.setPitch(locData.get("pitch").getAsFloat());
            }
        }

        return block;
    }

    /**
     * Parse location with fallback to individual coordinates
     * This supports both old format (x, y, z separate fields) and new format (location object)
     */
    public static LocationBlock parseWithFallback(JsonObject data, String locationField,
                                                  String xField, String yField, String zField,
                                                  String worldField, String yawField, String pitchField) {
        // Try new format first
        LocationBlock block = parseFromJson(data, locationField);
        if (block != null) {
            return block;
        }

        // Fallback to old format with individual fields
        block = new LocationBlock();
        boolean hasData = false;

        if (data.has(xField)) {
            block.setX(data.get(xField).getAsDouble());
            hasData = true;
        }
        if (data.has(yField)) {
            block.setY(data.get(yField).getAsDouble());
            hasData = true;
        }
        if (data.has(zField)) {
            block.setZ(data.get(zField).getAsDouble());
            hasData = true;
        }

        if (data.has(worldField)) {
            block.setWorldName(data.get(worldField).getAsString());
            block.setHasWorld(true);
            hasData = true;
        }

        if (data.has(yawField)) {
            block.setYaw(data.get(yawField).getAsFloat());
            hasData = true;
        }
        if (data.has(pitchField)) {
            block.setPitch(data.get(pitchField).getAsFloat());
            block.setHasRotation(true);
            hasData = true;
        }

        return hasData ? block : null;
    }
}
