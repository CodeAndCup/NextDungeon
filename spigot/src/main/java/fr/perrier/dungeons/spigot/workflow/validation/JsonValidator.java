package fr.perrier.dungeons.spigot.workflow.validation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Utility class for validating JSON data for workflow objects.
 * Helps prevent NullPointerExceptions and provides clear error messages.
 */
public class JsonValidator {
    
    /**
     * Checks if a JSON object has a required field.
     * 
     * @param json the JSON object to check
     * @param fieldName the name of the required field
     * @return true if the field exists and is not null
     */
    public static boolean hasField(JsonObject json, String fieldName) {
        return json != null && json.has(fieldName) && !json.get(fieldName).isJsonNull();
    }
    
    /**
     * Checks if a JSON object has multiple required fields.
     * 
     * @param json the JSON object to check
     * @param fieldNames the names of the required fields
     * @return true if all fields exist and are not null
     */
    public static boolean hasFields(JsonObject json, String... fieldNames) {
        if (json == null) {
            return false;
        }
        
        for (String fieldName : fieldNames) {
            if (!hasField(json, fieldName)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets a string value from JSON with a default fallback.
     * 
     * @param json the JSON object
     * @param fieldName the field name
     * @param defaultValue the default value to return if field is missing
     * @return the string value or default
     */
    public static String getString(JsonObject json, String fieldName, String defaultValue) {
        if (!hasField(json, fieldName)) {
            return defaultValue;
        }
        
        try {
            return json.get(fieldName).getAsString();
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Gets an integer value from JSON with a default fallback.
     * 
     * @param json the JSON object
     * @param fieldName the field name
     * @param defaultValue the default value to return if field is missing
     * @return the integer value or default
     */
    public static int getInt(JsonObject json, String fieldName, int defaultValue) {
        if (!hasField(json, fieldName)) {
            return defaultValue;
        }
        
        try {
            return json.get(fieldName).getAsInt();
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Gets a boolean value from JSON with a default fallback.
     * 
     * @param json the JSON object
     * @param fieldName the field name
     * @param defaultValue the default value to return if field is missing
     * @return the boolean value or default
     */
    public static boolean getBoolean(JsonObject json, String fieldName, boolean defaultValue) {
        if (!hasField(json, fieldName)) {
            return defaultValue;
        }
        
        try {
            return json.get(fieldName).getAsBoolean();
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Gets a double value from JSON with a default fallback.
     * 
     * @param json the JSON object
     * @param fieldName the field name
     * @param defaultValue the default value to return if field is missing
     * @return the double value or default
     */
    public static double getDouble(JsonObject json, String fieldName, double defaultValue) {
        if (!hasField(json, fieldName)) {
            return defaultValue;
        }
        
        try {
            return json.get(fieldName).getAsDouble();
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Gets a float value from JSON with a default fallback.
     * 
     * @param json the JSON object
     * @param fieldName the field name
     * @param defaultValue the default value to return if field is missing
     * @return the float value or default
     */
    public static float getFloat(JsonObject json, String fieldName, float defaultValue) {
        if (!hasField(json, fieldName)) {
            return defaultValue;
        }
        
        try {
            return json.get(fieldName).getAsFloat();
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Gets a long value from JSON with a default fallback.
     * 
     * @param json the JSON object
     * @param fieldName the field name
     * @param defaultValue the default value to return if field is missing
     * @return the long value or default
     */
    public static long getLong(JsonObject json, String fieldName, long defaultValue) {
        if (!hasField(json, fieldName)) {
            return defaultValue;
        }
        
        try {
            return json.get(fieldName).getAsLong();
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Validates that a JSON object has a required type field with the expected value.
     * 
     * @param json the JSON object
     * @param expectedType the expected type value
     * @return true if the type field matches the expected value
     */
    public static boolean validateType(JsonObject json, String expectedType) {
        if (!hasField(json, "type")) {
            return false;
        }
        
        String actualType = getString(json, "type", "");
        return expectedType.equals(actualType);
    }
    
    /**
     * Validates that required fields exist in a JSON object.
     * Throws an exception with a clear message if validation fails.
     * 
     * @param json the JSON object to validate
     * @param objectType the type of object being validated (for error messages)
     * @param requiredFields the required field names
     * @throws ValidationException if validation fails
     */
    public static void requireFields(JsonObject json, String objectType, String... requiredFields) throws ValidationException {
        if (json == null) {
            throw new ValidationException(objectType + " JSON cannot be null");
        }
        
        for (String field : requiredFields) {
            if (!hasField(json, field)) {
                throw new ValidationException(objectType + " missing required field: " + field);
            }
        }
    }
    
    /**
     * Exception thrown when JSON validation fails.
     */
    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}
