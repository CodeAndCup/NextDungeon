package fr.perrier.dungeons.spigot.workflow.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.EntityDeathTrigger;

import java.lang.reflect.Modifier;

/**
 * Simple test to verify EntityDeathTrigger serialization/deserialization.
 * Run this to debug why entityType is not persisting.
 */
public class EntityDeathTriggerSerializationTest {
    
    public static void main(String[] args) {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
                .setPrettyPrinting()
                .create();
        
        System.out.println("=== EntityDeathTrigger Serialization Test ===\n");
        
        // Test 1: Create trigger with custom entityType
        System.out.println("TEST 1: Creating trigger with entityType='TEST'");
        EntityDeathTrigger trigger1 = new EntityDeathTrigger("Test Trigger");
        trigger1.setEntityType("TEST");
        trigger1.setEnabled(true);
        
        System.out.println("Created trigger - entityType: " + trigger1.getEntityType());
        
        // Test 2: Serialize to JSON
        System.out.println("\nTEST 2: Serializing to JSON");
        JsonObject json1 = gson.toJsonTree(trigger1, EntityDeathTrigger.class).getAsJsonObject();
        System.out.println("JSON: " + json1.toString());
        System.out.println("JSON contains 'entityType': " + json1.has("entityType"));
        if (json1.has("entityType")) {
            System.out.println("JSON entityType value: " + json1.get("entityType").getAsString());
        }
        
        // Test 3: Deserialize from JSON
        System.out.println("\nTEST 3: Deserializing from JSON");
        EntityDeathTrigger trigger2 = gson.fromJson(json1, EntityDeathTrigger.class);
        System.out.println("Deserialized trigger - entityType: " + trigger2.getEntityType());
        
        // Test 4: Verify values match
        System.out.println("\nTEST 4: Verification");
        boolean success = "TEST".equals(trigger2.getEntityType());
        System.out.println("entityType preserved: " + success);
        
        if (!success) {
            System.err.println("\n❌ FAILED: entityType was not preserved!");
            System.err.println("Expected: TEST");
            System.err.println("Got: " + trigger2.getEntityType());
        } else {
            System.out.println("\n✅ SUCCESS: entityType was preserved correctly!");
        }
        
        // Test 5: Simulate web editor format
        System.out.println("\nTEST 5: Simulating web editor JSON format");
        String webEditorJson = "{\"type\":\"entity_death_trigger\",\"name\":\"Kill Test Entity\",\"entityType\":\"TEST\",\"enabled\":true}";
        System.out.println("Web editor JSON: " + webEditorJson);
        
        JsonObject webJson = gson.fromJson(webEditorJson, JsonObject.class);
        EntityDeathTrigger trigger3 = gson.fromJson(webJson, EntityDeathTrigger.class);
        System.out.println("Deserialized from web format - entityType: " + trigger3.getEntityType());
        
        boolean webSuccess = "TEST".equals(trigger3.getEntityType());
        System.out.println("Web format entityType preserved: " + webSuccess);
        
        if (!webSuccess) {
            System.err.println("\n❌ FAILED: Web editor format not working!");
        } else {
            System.out.println("\n✅ SUCCESS: Web editor format working!");
        }
    }
}
