package com.poisoncircle;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;

/** Every page shares one group selection; mode confirmation changes only the mode. */
public final class AirborneGroupsScreen extends AirborneAdminScreen {
    private final String mode;
    private String chosen, signature="";
    private int page;
    private boolean browsed;
    private EditBox name;
    private Button rename,delete,openPoints,select,previous,next;
    private final List<Button> rows=new ArrayList<>();
    public AirborneGroupsScreen(String mode){super(mode==null?"点位分组管理":"选择"+AirborneUiRules.modeLabel(mode)+"分组");this.mode=mode;}
    private JsonArray groups(){return AirborneUiRules.groups(liveSnapshot,mode);}
    @Override protected void build(){
        rows.clear();
        if(chosen==null)chosen=mode==null?text(liveSnapshot,"selectedGroup",""):AirborneUiRules.selectedGroup(liveSnapshot,mode);
        if(!chosen.isEmpty()&&group()==null)chosen="";
        JsonArray groups=groups();page=Math.max(0,Math.min(page,Math.max(0,(groups.size()-1)/3)));
        for(int i=0;i<3&&page*3+i<groups.size();i++){
            JsonObject group=groups.get(page*3+i).getAsJsonObject();String id=text(group,"id","");
            String label=(id.equals(chosen)?"▶ ":"")+text(group,"name",id)+"  "+array(group,"points").size()+"点";
            rows.add(button(label,12,34+i*24,cardWidth-24,()->{chosen=id;browsed=true;send("select_group",data("group",id));rebuildWidgets();}));
        }
        previous=button("上一页",12,108,(cardWidth-30)/2,()->{page--;rebuildWidgets();});
        next=button("下一页",18+(cardWidth-30)/2,108,(cardWidth-30)/2,()->{page++;rebuildWidgets();});
        if(mode==null){
            name=field("分组名称",12,136,(cardWidth-30)/2,32,group()==null?"":text(group(),"name",""));
            button("新建点位",18+(cardWidth-30)/2,136,(cardWidth-30)/2,()->{if(locked())return;send("select_group",data("group",""));open(new AirbornePointsScreen(""));});
            rename=button("命名",12,162,(cardWidth-30)/2,()->{String value=name.getValue().strip();if(value.isEmpty()){feedback="请输入分组名称";return;}JsonObject d=data("group",chosen);d.addProperty("name",value);send("rename_group",d);});
            delete=button("删除组",18+(cardWidth-30)/2,162,(cardWidth-30)/2,()->send("delete_group",data("group",chosen)));
            openPoints=button("打开组内点位",12,188,(cardWidth-30)/2,()->{send("open_points",data("group",chosen));open(new AirbornePointsScreen(chosen));});
        }else select=button("选择此模式",12,162,cardWidth-24,()->{JsonObject d=data("mode",mode);send("mode",d);open(new AirborneControlScreen());});
        button("返回",mode==null?18+(cardWidth-30)/2:12,188,mode==null?(cardWidth-30)/2:cardWidth-24,()->open(new AirborneControlScreen()));
        signature=groups.toString();updateControls();
    }
    private JsonObject group(){for(var e:groups())if(text(e.getAsJsonObject(),"id","").equals(chosen))return e.getAsJsonObject();return null;}
    @Override protected void stateChanged(){String selected=mode==null?text(liveSnapshot,"selectedGroup",""):AirborneUiRules.selectedGroup(liveSnapshot,mode);boolean selectionChanged=!browsed&&!selected.equals(chosen);if(selectionChanged)chosen=selected;if(selectionChanged||!signature.equals(groups().toString()))rebuildWidgets();else updateControls();}
    @Override protected void updateControls(){boolean editable=!locked()&&group()!=null;for(Button row:rows)row.active=!locked();if(rename!=null)rename.active=editable;if(delete!=null)delete.active=editable;if(openPoints!=null)openPoints.active=editable;if(select!=null)select.active=!locked();if(name!=null)name.setEditable(editable);if(previous!=null)previous.active=page>0;if(next!=null)next.active=(page+1)*3<groups().size();}
    @Override protected void drawDetails(GuiGraphicsExtractor g){if(groups().isEmpty())line(g,"尚无分组：用设置器加点并蹲右键建组",48);if(mode!=null)line(g,"点击分组立即选中，确认只切换模式",140);}
}
