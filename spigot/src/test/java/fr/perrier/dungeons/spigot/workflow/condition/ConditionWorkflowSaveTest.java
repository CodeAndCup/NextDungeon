package fr.perrier.dungeons.spigot.workflow.condition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.common.workflow.action.ActionData;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.action.factory.ActionFactory;
import fr.perrier.dungeons.spigot.workflow.action.impl.SendMessageAction;
import fr.perrier.dungeons.spigot.workflow.serializer.InstanceSerializer;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.FunctionTrigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de bout en bout pour la sauvegarde/chargement des conditions de workflow.
 *
 * <p>Reproduit le bug rapporté : pour toutes les conditions sauf {@code IfCondition}, ni les
 * branches {@code ifActions}/{@code elseActions} ni les champs (type d'item, quantité…) ne se
 * sauvegardaient. La cause était un décalage de casse dans le JS Blockly généré (clés en
 * minuscules type {@code ifactions}/{@code itemmaterial}) alors que {@link ActionFactory} et Gson
 * utilisent du camelCase.
 *
 * <p>Quatre couches sont couvertes :
 * <ol>
 *   <li><b>Génération JS</b> — le JS Blockly émet/lit des clés camelCase (côté WebEditor).</li>
 *   <li><b>Parsing de sauvegarde</b> — le payload WebEditor est correctement parsé par
 *       {@link ActionFactory} (champs + branches).</li>
 *   <li><b>Sérialisation de chargement</b> — Gson produit des clés camelCase que le JS de
 *       chargement relit (EditorSerializer).</li>
 *   <li><b>Round-trip DB</b> — un trigger complet survit à
 *       {@link InstanceSerializer#serializeTriggers}/{@link InstanceSerializer#deserializeTriggers}.</li>
 * </ol>
 */
@DisplayName("Condition Workflow Save/Load Tests")
class ConditionWorkflowSaveTest {

    /** Gson configuré comme dans EditorSerializer (sérialisation par réflexion des champs). */
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    private static String caseJs(BlocklyAction action) {
        StringBuilder sb = new StringBuilder();
        action.generateCustomActionCase(sb);
        return sb.toString();
    }

    private static String loadingJs(BlocklyAction action) {
        StringBuilder sb = new StringBuilder();
        action.generateCustomActionLoadingCase(sb);
        return sb.toString();
    }

    /** Construit un payload "nested action" tel que getActionsFromStatementInput le produit côté JS. */
    private static JsonObject sendMessage(String target, String message) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "send_message_action");
        o.addProperty("targetPlayer", target);
        o.addProperty("message", message);
        return o;
    }

    /** Ajoute des branches if/else (camelCase) à un payload de condition, comme le JS corrigé. */
    private static JsonObject withBranches(JsonObject condition) {
        JsonArray ifActions = new JsonArray();
        ifActions.add(sendMessage("player", "then-branch"));
        JsonArray elseActions = new JsonArray();
        elseActions.add(sendMessage("@all", "else-branch"));
        condition.add("ifActions", ifActions);
        condition.add("elseActions", elseActions);
        return condition;
    }

    private static void assertBranchesPopulated(List<Action> ifActions, List<Action> elseActions) {
        assertThat(ifActions).as("ifActions").hasSize(1);
        assertThat(ifActions.get(0)).isInstanceOf(SendMessageAction.class);
        assertThat(((SendMessageAction) ifActions.get(0)).getMessage()).isEqualTo("then-branch");

        assertThat(elseActions).as("elseActions").hasSize(1);
        assertThat(elseActions.get(0)).isInstanceOf(SendMessageAction.class);
        assertThat(((SendMessageAction) elseActions.get(0)).getMessage()).isEqualTo("else-branch");
    }

    // ------------------------------------------------------------------------------------------
    // 1. Génération JS — les clés doivent être en camelCase (régression du bug de casse)
    // ------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("Blockly JS generation uses camelCase keys")
    class JsGenerationConsistency {

        @Test
        @DisplayName("All conditions emit camelCase ifActions/elseActions (never lowercase)")
        void branchKeysAreCamelCase() {
            List<BlocklyAction> conditions = List.of(
                    new PlayerHasItemCondition(),
                    new EntityTypeIsCondition(),
                    new TimeOfDayCondition(),
                    new BlockTypeIsCondition(),
                    new PlayerInRegionCondition(),
                    new PlayerPermissionCondition(),
                    new LocationIsSafeCondition()
            );

            for (BlocklyAction condition : conditions) {
                String save = caseJs(condition);
                String load = loadingJs(condition);
                String name = condition.getClass().getSimpleName();

                assertThat(save).as(name + " save JS pushes ifActions/elseActions")
                        .contains("ifActions:", "elseActions:");
                assertThat(save).as(name + " save JS must not use lowercase branch keys")
                        .doesNotContain("ifactions", "elseactions");

                assertThat(load).as(name + " load JS reads action.ifActions/elseActions")
                        .contains("action.ifActions", "action.elseActions");
                assertThat(load).as(name + " load JS must not use lowercase branch keys")
                        .doesNotContain("action.ifactions", "action.elseactions");
            }
        }

        @Test
        @DisplayName("PlayerHasItemCondition emits camelCase field keys")
        void playerHasItemFieldKeysAreCamelCase() {
            PlayerHasItemCondition condition = new PlayerHasItemCondition();
            String save = caseJs(condition);
            String load = loadingJs(condition);

            assertThat(save).contains("itemMaterial:", "minAmount:", "checkName:", "itemName:");
            assertThat(save).doesNotContain("itemmaterial", "minamount", "checkname", "itemname");

            assertThat(load).contains("action.itemMaterial", "action.minAmount",
                    "action.checkName", "action.itemName");
            assertThat(load).doesNotContain("action.itemmaterial", "action.minamount",
                    "action.checkname", "action.itemname");
        }

        @Test
        @DisplayName("Type-specific field keys are camelCase across conditions")
        void otherConditionFieldKeysAreCamelCase() {
            assertThat(caseJs(new EntityTypeIsCondition()))
                    .contains("entityType:").doesNotContain("entitytype");

            String time = caseJs(new TimeOfDayCondition());
            assertThat(time).contains("timePeriod:", "customTime:").doesNotContain("timeperiod", "customtime");

            assertThat(caseJs(new BlockTypeIsCondition()))
                    .contains("blockType:").doesNotContain("blocktype");

            String safe = caseJs(new LocationIsSafeCondition());
            assertThat(safe).contains("checkSolidGround:", "checkDangerousBlocks:")
                    .doesNotContain("checksolidground", "checkdangerousblocks");
        }
    }

    // ------------------------------------------------------------------------------------------
    // 2. Parsing de sauvegarde (WebEditor -> serveur) via ActionFactory
    // ------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("WebEditor save payload is parsed by ActionFactory")
    class WebEditorSaveParsing {

        @Test
        @DisplayName("PlayerHasItemCondition: fields AND if/else branches are saved")
        void playerHasItemFullSave() {
            JsonObject payload = withBranches(new JsonObject());
            payload.addProperty("type", "player_has_item_condition");
            payload.addProperty("itemMaterial", "GOLD_INGOT");
            payload.addProperty("minAmount", 7);
            payload.addProperty("checkName", true);
            payload.addProperty("itemName", "Excalibur");

            Action parsed = ActionFactory.createActionFromJson(payload);

            assertThat(parsed).isInstanceOf(PlayerHasItemCondition.class);
            PlayerHasItemCondition condition = (PlayerHasItemCondition) parsed;
            assertThat(condition.getItemMaterial()).isEqualTo("GOLD_INGOT");
            assertThat(condition.getMinAmount()).isEqualTo(7);
            assertThat(condition.isCheckName()).isTrue();
            assertThat(condition.getItemName()).isEqualTo("Excalibur");
            assertBranchesPopulated(condition.getIfActions(), condition.getElseActions());
        }

        @Test
        @DisplayName("EntityTypeIsCondition: entityType + branches saved")
        void entityTypeSave() {
            JsonObject payload = withBranches(new JsonObject());
            payload.addProperty("type", "entity_type_is_condition");
            payload.addProperty("entityType", "SKELETON");
            payload.addProperty("comparison", "is_not");

            EntityTypeIsCondition condition = (EntityTypeIsCondition) ActionFactory.createActionFromJson(payload);

            assertThat(condition.getEntityType()).isEqualTo("SKELETON");
            assertThat(condition.getComparison()).isEqualTo("is_not");
            assertBranchesPopulated(condition.getIfActions(), condition.getElseActions());
        }

        @Test
        @DisplayName("TimeOfDayCondition: timePeriod/customTime + branches saved")
        void timeOfDaySave() {
            JsonObject payload = withBranches(new JsonObject());
            payload.addProperty("type", "time_of_day_condition");
            payload.addProperty("timePeriod", "custom");
            payload.addProperty("customTime", 18000L);
            payload.addProperty("operator", ">=");

            TimeOfDayCondition condition = (TimeOfDayCondition) ActionFactory.createActionFromJson(payload);

            assertThat(condition.getTimePeriod()).isEqualTo("custom");
            assertThat(condition.getCustomTime()).isEqualTo(18000L);
            assertThat(condition.getOperator()).isEqualTo(">=");
            assertBranchesPopulated(condition.getIfActions(), condition.getElseActions());
        }

        @Test
        @DisplayName("BlockTypeIsCondition: coordinates/blockType + branches saved")
        void blockTypeSave() {
            JsonObject payload = withBranches(new JsonObject());
            payload.addProperty("type", "block_type_is_condition");
            payload.addProperty("x", 10.0);
            payload.addProperty("y", 64.0);
            payload.addProperty("z", -5.0);
            payload.addProperty("blockType", "EMERALD_BLOCK");
            payload.addProperty("comparison", "is");

            BlockTypeIsCondition condition = (BlockTypeIsCondition) ActionFactory.createActionFromJson(payload);

            assertThat(condition.getX()).isEqualTo(10.0);
            assertThat(condition.getY()).isEqualTo(64.0);
            assertThat(condition.getZ()).isEqualTo(-5.0);
            assertThat(condition.getBlockType()).isEqualTo("EMERALD_BLOCK");
            assertBranchesPopulated(condition.getIfActions(), condition.getElseActions());
        }

        @Test
        @DisplayName("PlayerPermissionCondition: permission + branches saved")
        void playerPermissionSave() {
            JsonObject payload = withBranches(new JsonObject());
            payload.addProperty("type", "player_permission_condition");
            payload.addProperty("permission", "dungeons.vip");
            payload.addProperty("comparison", "has_not");

            PlayerPermissionCondition condition = (PlayerPermissionCondition) ActionFactory.createActionFromJson(payload);

            assertThat(condition.getPermission()).isEqualTo("dungeons.vip");
            assertThat(condition.getComparison()).isEqualTo("has_not");
            assertBranchesPopulated(condition.getIfActions(), condition.getElseActions());
        }

        @Test
        @DisplayName("PlayerInRegionCondition: comparison + branches saved")
        void playerInRegionSave() {
            JsonObject payload = withBranches(new JsonObject());
            payload.addProperty("type", "player_in_region_condition");
            payload.addProperty("comparison", "outside");

            PlayerInRegionCondition condition = (PlayerInRegionCondition) ActionFactory.createActionFromJson(payload);

            assertThat(condition.getComparison()).isEqualTo("outside");
            assertBranchesPopulated(condition.getIfActions(), condition.getElseActions());
        }

        @Test
        @DisplayName("LocationIsSafeCondition: flags + branches saved")
        void locationIsSafeSave() {
            JsonObject payload = withBranches(new JsonObject());
            payload.addProperty("type", "location_is_safe_condition");
            payload.addProperty("checkSolidGround", false);
            payload.addProperty("checkDangerousBlocks", true);

            LocationIsSafeCondition condition = (LocationIsSafeCondition) ActionFactory.createActionFromJson(payload);

            assertThat(condition.isCheckSolidGround()).isFalse();
            assertThat(condition.isCheckDangerousBlocks()).isTrue();
            assertBranchesPopulated(condition.getIfActions(), condition.getElseActions());
        }
    }

    // ------------------------------------------------------------------------------------------
    // 3. Sérialisation de chargement (EditorSerializer) — Gson produit du camelCase
    // ------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("Editor load serialization (Gson) yields camelCase keys")
    class EditorLoadSerialization {

        @Test
        @DisplayName("Gson keys match what the loading JS reads")
        void gsonKeysAreCamelCase() {
            PlayerHasItemCondition condition = new PlayerHasItemCondition();
            condition.setItemMaterial("DIAMOND");
            condition.setMinAmount(3);
            condition.setItemName("Holy Sword");
            condition.addIfAction(new SendMessageAction("player", "hi"));

            JsonObject serialized = gson.toJsonTree(condition, condition.getClass()).getAsJsonObject();

            // EditorSerializer relit ces clés côté JS (action.itemMaterial, action.ifActions, ...)
            assertThat(serialized.keySet())
                    .contains("itemMaterial", "minAmount", "checkName", "itemName", "ifActions", "elseActions");
            assertThat(serialized.keySet())
                    .doesNotContain("itemmaterial", "minamount", "ifactions", "elseactions");

            // La branche sérialisée doit recontenir l'action imbriquée avec son "type"
            assertThat(serialized.getAsJsonArray("ifActions")).hasSize(1);
            assertThat(serialized.getAsJsonArray("ifActions").get(0).getAsJsonObject().get("type").getAsString())
                    .isEqualTo("send_message_action");
        }
    }

    // ------------------------------------------------------------------------------------------
    // 4. Round-trip DB complet via l'API publique d'InstanceSerializer
    // ------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("Full database round-trip via InstanceSerializer")
    class DatabaseRoundTrip {

        @Test
        @DisplayName("PlayerHasItemCondition survives serialize -> deserialize with fields + branches")
        void playerHasItemRoundTrip() {
            PlayerHasItemCondition condition = new PlayerHasItemCondition();
            condition.setItemMaterial("GOLD_INGOT");
            condition.setMinAmount(7);
            condition.setCheckName(true);
            condition.setItemName("Excalibur");
            condition.addIfAction(new SendMessageAction("player", "then-branch"));
            condition.addElseAction(new SendMessageAction("@all", "else-branch"));

            FunctionTrigger trigger = new FunctionTrigger("save_test");
            List<ActionData> actions = new ArrayList<>();
            actions.add(condition);
            trigger.setActions(actions);

            List<TriggerData> triggers = new ArrayList<>();
            triggers.add(trigger);

            // Serialize (DB write) then deserialize (DB read)
            String json = InstanceSerializer.serializeTriggers(triggers);
            assertThat(json).as("serialized JSON keeps camelCase branch keys")
                    .contains("ifActions", "elseActions", "itemMaterial");

            List<TriggerData> restored = InstanceSerializer.deserializeTriggers(json);

            assertThat(restored).hasSize(1);
            assertThat(restored.get(0).getActions()).hasSize(1);

            ActionData restoredAction = restored.get(0).getActions().get(0);
            assertThat(restoredAction).isInstanceOf(PlayerHasItemCondition.class);

            PlayerHasItemCondition rc = (PlayerHasItemCondition) restoredAction;
            assertThat(rc.getItemMaterial()).isEqualTo("GOLD_INGOT");
            assertThat(rc.getMinAmount()).isEqualTo(7);
            assertThat(rc.isCheckName()).isTrue();
            assertThat(rc.getItemName()).isEqualTo("Excalibur");
            assertBranchesPopulated(rc.getIfActions(), rc.getElseActions());
        }

        @Test
        @DisplayName("EntityTypeIsCondition survives the round-trip with branches")
        void entityTypeRoundTrip() {
            EntityTypeIsCondition condition = new EntityTypeIsCondition();
            condition.setEntityType("SKELETON");
            condition.setComparison("is_not");
            condition.addIfAction(new SendMessageAction("player", "then-branch"));
            condition.addElseAction(new SendMessageAction("@all", "else-branch"));

            FunctionTrigger trigger = new FunctionTrigger("save_test_entity");
            List<ActionData> actions = new ArrayList<>();
            actions.add(condition);
            trigger.setActions(actions);

            List<TriggerData> triggers = new ArrayList<>();
            triggers.add(trigger);

            List<TriggerData> restored =
                    InstanceSerializer.deserializeTriggers(InstanceSerializer.serializeTriggers(triggers));

            assertThat(restored).hasSize(1);
            assertThat(restored.get(0).getActions()).hasSize(1);
            EntityTypeIsCondition rc = (EntityTypeIsCondition) restored.get(0).getActions().get(0);
            assertThat(rc.getEntityType()).isEqualTo("SKELETON");
            assertThat(rc.getComparison()).isEqualTo("is_not");
            assertBranchesPopulated(rc.getIfActions(), rc.getElseActions());
        }
    }
}
