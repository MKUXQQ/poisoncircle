package com.poisoncircle;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import org.junit.jupiter.api.Test;

final class AirborneGroupRulesTest {
    @Test void repeatedCallbacksRecognizeIdenticalPointsDespiteNewIdsNamesAndOrder() {
        JsonArray a=points("a",1),b=points("new-id",1);
        b.get(0).getAsJsonObject().addProperty("name","renamed");
        assertTrue(AirborneGroupRules.samePoints(a,b));
        a.add(points("second",2).get(0));b.add(points("copy",2).get(0));
        JsonArray reversed=new JsonArray();reversed.add(b.get(1));reversed.add(b.get(0));
        assertTrue(AirborneGroupRules.samePoints(a,reversed));
        assertFalse(AirborneGroupRules.samePoints(a,points("a",1)));
        b.get(0).getAsJsonObject().addProperty("x",3);
        assertFalse(AirborneGroupRules.samePoints(a,b));
        b=points("a",1);b.get(0).getAsJsonObject().addProperty("dimension","minecraft:the_nether");
        assertFalse(AirborneGroupRules.samePoints(points("a",1),b));
    }
    @Test void migrationRetainsBothLegacyGroupsAndEveryLegacyDraftAndIsIdempotent() {
        JsonObject o=JsonParser.parseString("""
          {"activeMode":"duo","selectedGroup":"b","selectedGroups":{"solo":"a","duo":"b"},
           "groups":[{"id":"a","name":"1","mode":"solo","points":[]},{"id":"b","name":"2","mode":"duo","points":[]}],
           "drafts":{"solo":[],"duo":[]}}
          """).getAsJsonObject();
        o.getAsJsonObject("drafts").add("solo",points("s",1));o.getAsJsonObject("drafts").add("duo",points("d",2));
        String before=o.toString();JsonObject migrated=AirborneGroupRules.sharedConfig(o);JsonArray groups=migrated.getAsJsonArray("groups");
        assertEquals(before,o.toString());assertEquals(4,groups.size());
        assertEquals("b",migrated.get("selectedGroup").getAsString());
        assertEquals("s",groups.get(2).getAsJsonObject().getAsJsonArray("points").get(0).getAsJsonObject().get("id").getAsString());
        assertEquals("d",groups.get(3).getAsJsonObject().getAsJsonArray("points").get(0).getAsJsonObject().get("id").getAsString());
        assertEquals("3",groups.get(2).getAsJsonObject().get("name").getAsString());
        assertFalse(migrated.has("selectedGroups"));assertFalse(migrated.has("drafts"));
        assertEquals(migrated,AirborneGroupRules.sharedConfig(migrated));
    }
    @Test void selectedLegacyDraftBecomesVisibleWithoutLosingTheOtherDraft() {
        JsonObject o=new JsonObject();o.addProperty("activeMode","solo");JsonObject drafts=new JsonObject();drafts.add("solo",points("s",1));drafts.add("duo",points("d",2));o.add("drafts",drafts);
        JsonObject migrated=AirborneGroupRules.sharedConfig(o);
        assertEquals("legacy-draft-solo",migrated.get("selectedGroup").getAsString());
        assertEquals(2,migrated.getAsJsonArray("groups").size());
    }
    @Test void migrationReusesAGroupContainingTheSameLegacyDraftLocations() {
        JsonObject o=new JsonObject();o.addProperty("activeMode","solo");JsonArray groups=new JsonArray();JsonObject group=new JsonObject();group.addProperty("id","existing");group.addProperty("name","1");group.add("points",points("old-id",4));groups.add(group);o.add("groups",groups);
        JsonObject drafts=new JsonObject();drafts.add("solo",points("new-id",4));drafts.add("duo",points("other-id",4));o.add("drafts",drafts);
        JsonObject migrated=AirborneGroupRules.sharedConfig(o);
        assertEquals(1,migrated.getAsJsonArray("groups").size());assertEquals("existing",migrated.get("selectedGroup").getAsString());
    }
    @Test void sharedWorkingDraftSurvivesRoundTrip() {
        JsonObject o=new JsonObject();o.add("draftPoints",points("working",8));
        assertEquals(o.getAsJsonArray("draftPoints"),AirborneGroupRules.sharedConfig(o).getAsJsonArray("draftPoints"));
    }
    @Test void packingDraftStartsTheNextGroupAndRejectsAllEmptyRetries() {
        JsonObject input=state();input.add("draftPoints",points("first",1));String before=input.toString();
        JsonObject saved=AirborneGroupRules.packGroup(input,8);assertEquals(before,input.toString());assertBlankDraft(saved);assertEquals(1,saved.getAsJsonArray("groups").size());
        JsonObject first=saved.getAsJsonArray("groups").get(0).getAsJsonObject().deepCopy();
        for(int callback=0;callback<4;callback++)assertThrows(IllegalArgumentException.class,()->AirborneGroupRules.packGroup(saved,8));
        assertEquals(first,saved.getAsJsonArray("groups").get(0));
        saved.add("draftPoints",points("next",2));JsonObject second=AirborneGroupRules.packGroup(saved,8);assertBlankDraft(second);assertEquals(2,second.getAsJsonArray("groups").size());assertEquals(first,second.getAsJsonArray("groups").get(0));
        assertEquals("duo",second.get("activeMode").getAsString());assertEquals(200,second.get("height").getAsInt());
    }
    @Test void intentionalSameLocationsReuseExistingGroupAndStillClearDraftAtCapacity() {
        JsonObject input=state();input.add("draftPoints",points("first",1));JsonObject saved=AirborneGroupRules.packGroup(input,1);
        saved.add("draftPoints",points("new-id",1));JsonObject reused=AirborneGroupRules.packGroup(saved,1);assertBlankDraft(reused);assertEquals(saved.get("groups"),reused.get("groups"));
        reused.addProperty("selectedGroup",reused.getAsJsonArray("groups").get(0).getAsJsonObject().get("id").getAsString());
        JsonObject selectedSaved=AirborneGroupRules.packGroup(reused,1);assertBlankDraft(selectedSaved);assertEquals(reused.get("groups"),selectedSaved.get("groups"));
    }
    @Test void capacityAndHiddenLegacyDraftFailuresKeepOriginalData() {
        JsonObject input=state();input.add("draftPoints",points("first",1));JsonObject saved=AirborneGroupRules.packGroup(input,1);saved.add("draftPoints",points("different",2));
        String before=saved.toString();assertThrows(IllegalArgumentException.class,()->AirborneGroupRules.packGroup(saved,1));assertEquals(before,saved.toString());
        saved.addProperty("selectedGroup",saved.getAsJsonArray("groups").get(0).getAsJsonObject().get("id").getAsString());before=saved.toString();
        assertTrue(assertThrows(IllegalArgumentException.class,()->AirborneGroupRules.packGroup(saved,8)).getMessage().contains("未保存草稿"));assertEquals(before,saved.toString());
        saved.add("draftPoints",points("legacy-copy",1));assertBlankDraft(AirborneGroupRules.packGroup(saved,1));
    }
    @Test void sourceContractKeepsBothSetterCallbacksOnTheSharedTransactionalSavePath() throws Exception {
        String source=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/poisoncircle/AirborneGame.java"));
        assertTrue(source.contains("case \"pack_group\"->packGroup();"));
        assertTrue(source.contains("private static void packGroup(){applyConfig(AirborneGroupRules.packGroup(config(),MAX_GROUPS));}"));
        assertTrue(source.contains("JsonObject before=config();"));assertTrue(source.contains("}persist();}catch(IOException|RuntimeException e){applyConfig(before);"));
        String setter=source.substring(source.indexOf("private static final class SetterItem"));assertEquals(2,setter.split("new AirborneAdminPayload\\(\"pack_group\",\"\\{\\}\"\\)",-1).length-1);
        assertTrue(setter.contains("Group group=working();"));assertTrue(source.contains("当前组已保存，已切换到空白草稿；右键地面设置下一组"));
    }
    private static JsonObject state() {
        JsonObject o=new JsonObject();o.add("groups",new JsonArray());o.add("draftPoints",new JsonArray());o.addProperty("selectedGroup","");o.addProperty("activeMode","duo");o.addProperty("height",200);return o;
    }
    private static void assertBlankDraft(JsonObject o) {assertEquals("",o.get("selectedGroup").getAsString());assertTrue(o.getAsJsonArray("draftPoints").isEmpty());}
    private static JsonArray points(String id,double x) {
        JsonObject q=new JsonObject();q.addProperty("id",id);q.addProperty("name",id);q.addProperty("dimension","minecraft:overworld");q.addProperty("x",x);q.addProperty("y",64);q.addProperty("z",0);JsonArray a=new JsonArray();a.add(q);return a;
    }
}
