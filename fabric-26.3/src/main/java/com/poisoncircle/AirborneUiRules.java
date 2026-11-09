package com.poisoncircle;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Pure validation and snapshot selection shared by the airborne forms. */
final class AirborneUiRules {
    private AirborneUiRules() { }
    static boolean locked(String status) {
        return "running".equalsIgnoreCase(status) || "active".equalsIgnoreCase(status) || "countdown".equalsIgnoreCase(status);
    }
    static boolean validFlight(double height, double speed) {
        return Double.isFinite(height) && height > 0 && Double.isFinite(speed) && speed >= .2 && speed <= 2;
    }
    static String activeMode(JsonObject state) { return "duo".equals(text(state,"activeMode","solo"))?"duo":"solo"; }
    static String modeLabel(String mode) { return "duo".equals(mode)?"多人":"单人"; }
    static String selectedGroup(JsonObject state,String mode) {
        return text(state,"selectedGroup","");
    }
    static JsonArray groups(JsonObject state,String mode) {
        JsonArray result=new JsonArray();
        for(var value:array(state,"groups"))if(value.isJsonObject())result.add(value);
        return result;
    }
    static JsonArray points(JsonObject state,String group) {
        if(group.isEmpty())return array(state,"draftPoints");
        for(var value:groups(state,null))if(group.equals(text(value.getAsJsonObject(),"id","")))return array(value.getAsJsonObject(),"points");
        return new JsonArray();
    }
    static JsonObject pointRequest(String group,String id) {
        JsonObject data=new JsonObject();data.addProperty("group",group);data.addProperty("id",id);return data;
    }
    private static JsonArray array(JsonObject state,String key) { return state.has(key)&&state.get(key).isJsonArray()?state.getAsJsonArray(key):new JsonArray(); }
    private static String text(JsonObject state,String key,String fallback) { try{return state.has(key)?state.get(key).getAsString():fallback;}catch(RuntimeException e){return fallback;} }
}
