package com.poisoncircle;

import com.google.gson.*;
import java.util.*;

/** Pure migration and semantic identity for shared point groups. */
final class AirborneGroupRules {
    private AirborneGroupRules() { }
    static String nextName(JsonArray groups) {
        Set<String> names=new HashSet<>();
        for(JsonElement e:groups)names.add(e.getAsJsonObject().get("name").getAsString());
        int n=1;while(names.contains(Integer.toString(n)))n++;return Integer.toString(n);
    }
    static JsonObject packGroup(JsonObject input,int maxGroups) {
        JsonObject o=input.deepCopy();JsonArray groups=o.getAsJsonArray("groups"),draft=o.getAsJsonArray("draftPoints");
        String selected=o.get("selectedGroup").getAsString();JsonArray source=draft;
        if(!selected.isEmpty()){
            JsonObject group=null;for(JsonElement e:groups)if(selected.equals(e.getAsJsonObject().get("id").getAsString())){group=e.getAsJsonObject();break;}
            if(group==null)throw new IllegalArgumentException("分组不存在");source=group.getAsJsonArray("points");
            if(!draft.isEmpty()&&!samePoints(source,draft))throw new IllegalArgumentException("另有未保存草稿，请先切换到草稿并打包，再保存当前组");
        }
        if(source.isEmpty())throw new IllegalArgumentException("当前为空白草稿，请先添加点位");
        boolean existing=false;for(JsonElement e:groups)if(samePoints(source,e.getAsJsonObject().getAsJsonArray("points"))){existing=true;break;}
        if(!existing){
            if(groups.size()>=maxGroups)throw new IllegalArgumentException("最多"+maxGroups+"个分组");
            JsonObject group=new JsonObject();group.addProperty("id",UUID.randomUUID().toString());group.addProperty("name",nextName(groups));JsonArray points=source.deepCopy();
            for(JsonElement e:points)e.getAsJsonObject().addProperty("id",UUID.randomUUID().toString());group.add("points",points);groups.add(group);
        }
        o.addProperty("selectedGroup","");o.add("draftPoints",new JsonArray());return o;
    }
    static boolean samePoints(JsonArray a,JsonArray b) {
        if(a.size()!=b.size())return false;
        List<String> left=locations(a),right=locations(b);Collections.sort(left);Collections.sort(right);return left.equals(right);
    }
    private static List<String> locations(JsonArray points) {
        List<String> result=new ArrayList<>();
        for(JsonElement e:points){JsonObject q=e.getAsJsonObject();result.add(q.get("dimension").getAsString()+":"+Double.toHexString(q.get("x").getAsDouble())+":"+Double.toHexString(q.get("y").getAsDouble())+":"+Double.toHexString(q.get("z").getAsDouble()));}
        return result;
    }
    static JsonObject sharedConfig(JsonObject input) {
        JsonObject o=input.deepCopy();JsonArray groups=o.has("groups")?o.getAsJsonArray("groups"):new JsonArray();o.add("groups",groups);
        String mode=o.has("activeMode")?o.get("activeMode").getAsString():"solo";
        String selected=o.has("selectedGroup")?o.get("selectedGroup").getAsString():"";
        if(!o.has("selectedGroup")&&o.has("selectedGroups")){JsonObject choices=o.getAsJsonObject("selectedGroups");if(choices.has(mode))selected=choices.get(mode).getAsString();}
        if(o.has("drafts")){
            JsonObject drafts=o.getAsJsonObject("drafts");
            for(String kind:List.of("solo","duo"))if(drafts.has(kind)&&!drafts.getAsJsonArray(kind).isEmpty()){
                JsonArray points=drafts.getAsJsonArray(kind);String existing="";
                for(JsonElement e:groups){JsonObject g=e.getAsJsonObject();if(samePoints(points,g.getAsJsonArray("points"))){existing=g.get("id").getAsString();break;}}
                if(!existing.isEmpty()){if(selected.isEmpty()&&kind.equals(mode))selected=existing;continue;}
                JsonObject group=new JsonObject();String id="legacy-draft-"+kind;int suffix=1;
                while(containsId(groups,id))id="legacy-draft-"+kind+"-"+(suffix++);
                group.addProperty("id",id);group.addProperty("name",nextName(groups));group.add("points",drafts.getAsJsonArray(kind).deepCopy());groups.add(group);
                if(selected.isEmpty()&&kind.equals(mode))selected=id;
            }
        }
        o.addProperty("selectedGroup",selected);o.remove("selectedGroups");o.remove("drafts");
        if(!o.has("draftPoints"))o.add("draftPoints",new JsonArray());
        return o;
    }
    private static boolean containsId(JsonArray groups,String id){for(JsonElement e:groups)if(id.equals(e.getAsJsonObject().get("id").getAsString()))return true;return false;}
}
