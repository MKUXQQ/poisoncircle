package com.poisoncircle;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Point editing and setter access for the selected group. */
public final class AirbornePointsScreen extends Screen {
    private static final int INK=0xFF2D190F,BORDER=0xFF6B4028,PANEL=0xFFF0D9A6,HOVER=0xFFE1BC78;
    private static final Map<String,String> VALUES=new HashMap<>();
    static void clearSession(){VALUES.clear();}
    private int left,top,cardWidth,index,ticks;
    private String lastServerMessage="";
    private EditBox nameBox;
    private String feedback="";
    private JsonObject liveSnapshot=new JsonObject();
    private Button rename,teleport,delete;
    private final String groupId;
    public AirbornePointsScreen(){this(null);}
    public AirbornePointsScreen(String groupId){super(Component.literal("空降点位"));this.groupId=groupId;}
    private String selectedGroupId(){return groupId==null?text(liveSnapshot,"selectedGroup",""):groupId;}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){liveSnapshot=AirborneAdminClient.snapshot();acceptMessage();build();}
    private void build(){clearWidgets();cardWidth=Math.min(400,width-16);left=(width-cardWidth)/2;top=Math.max(0,(height-228)/2);JsonObject point=selectedPoint();
        button("上一个",12,8,(cardWidth-30)/2,()->{index--;build();});button("下一个",18+(cardWidth-30)/2,8,(cardWidth-30)/2,()->{index++;build();});
        nameBox=field("name","点位名称",12,63,cardWidth-24,64,point==null?"":text(point,"name",""));
        rename=button("重命名",12,102,cardWidth-24,this::editName);teleport=button("管理员传送",12,136,(cardWidth-30)/2,()->sendPoint("teleport_point"));delete=button("删除点位",18+(cardWidth-30)/2,136,(cardWidth-30)/2,()->sendPoint("delete_point"));
        button("管理器",12,174,(cardWidth-30)/2,()->Minecraft.getInstance().gui.setScreen(new AirborneControlScreen()));button("关闭",18+(cardWidth-30)/2,174,(cardWidth-30)/2,this::onClose);updateButtons();
    }
    private void updateSnapshot(){String previous=identity();liveSnapshot=AirborneAdminClient.snapshot();acceptMessage();if(!previous.equals(identity()))build();updateButtons();}
    private void updateButtons(){boolean has=!points().isEmpty(),editable=has&&!AirborneUiRules.locked(text(liveSnapshot,"status",""));if(rename!=null)rename.active=editable;if(teleport!=null)teleport.active=editable;if(delete!=null)delete.active=editable;if(nameBox!=null)nameBox.setEditable(editable);}
    static void snapshotChanged(){Minecraft c=Minecraft.getInstance();if(c.gui.screen() instanceof AirbornePointsScreen s){s.updateSnapshot();}}
    private void acceptMessage(){String message=text(liveSnapshot,"message","");if(!message.isBlank()&&!message.equals(lastServerMessage))feedback=message;lastServerMessage=message;}
    void report(String message){feedback=message;}
    private String groupName(){if(selectedGroupId().isEmpty())return "工作点位";for(var e:array(liveSnapshot,"groups"))if(e.isJsonObject()&&text(e.getAsJsonObject(),"id","").equals(selectedGroupId()))return text(e.getAsJsonObject(),"name",selectedGroupId());return selectedGroupId();}
    private JsonArray points(){return AirborneUiRules.points(liveSnapshot,selectedGroupId());}
    private JsonObject selectedPoint(){JsonArray p=points();if(p.isEmpty())return null;index=Math.floorMod(index,p.size());return p.get(index).getAsJsonObject();}
    private String identity(){JsonObject p=selectedPoint();return selectedGroupId()+":"+(p==null?"none":text(p,"id",""))+":";}
    private void editName(){JsonObject d=base();if(d!=null){d.addProperty("name",value(nameBox));send("rename_point",d);}}
    private void sendPoint(String action){JsonObject d=base();if(d!=null)send(action,d);}
    private JsonObject base(){JsonObject p=selectedPoint();if(p==null){feedback="请使用设置器添加点位";return null;}return AirborneUiRules.pointRequest(selectedGroupId(),text(p,"id",""));}
    private void send(String action,JsonObject data){feedback="请求已发送: "+action;AirborneAdminClient.send(action,data);}
    private EditBox field(String suffix,String hint,int x,int y,int w,int max,String fallback){EditBox b=new EditBox(font,left+x+4,top+y+4,w-8,14,Component.literal(hint));b.setMaxLength(max);b.setBordered(false);b.setTextColor(INK);b.setTextShadow(false);String fieldKey=identity()+suffix;b.setValue(VALUES.getOrDefault(fieldKey,fallback));b.setResponder(v->VALUES.put(fieldKey,v));addRenderableWidget(b);return b;}
    private Button button(String s,int x,int y,int w,Runnable r){return addRenderableWidget(new Button(left+x,top+y,w,20,Component.literal(s),b->r.run(),n->n.get()){@Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float pt){g.fill(getX(),getY(),getRight(),getBottom(),BORDER);g.fill(getX()+1,getY()+1,getRight()-1,getBottom()-1,isHoveredOrFocused()?HOVER:PANEL);var m=net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(getMessage(),getWidth()-8));g.text(font,m,getX()+(getWidth()-font.width(m))/2,getY()+6,INK,false);}});}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float pt){g.fill(0,0,width,height,0xA008101A);g.fill(left,top,left+cardWidth,top+220,BORDER);g.fill(left+2,top+2,left+cardWidth-2,top+218,PANEL);JsonObject p=selectedPoint();String heading=p==null?"当前分组暂无点位":text(p,"name","点位")+"  "+text(p,"x","?")+", "+text(p,"y","?")+", "+text(p,"z","?");g.text(font,font.plainSubstrByWidth(heading,cardWidth-24),left+12,top+34,INK,false);g.text(font,font.plainSubstrByWidth("分组 "+groupName()+" 点位 "+(points().isEmpty()?0:index+1)+"/"+points().size(),cardWidth-24),left+12,top+49,INK,false);for(EditBox box:new EditBox[]{nameBox})if(box!=null){g.fill(box.getX()-4,box.getY()-4,box.getRight()+4,box.getBottom()+4,BORDER);g.fill(box.getX()-3,box.getY()-3,box.getRight()+3,box.getBottom()+3,0xFFF8E9C9);}g.text(font,font.plainSubstrByWidth(feedback,cardWidth-24),left+12,top+207,INK,false);super.extractRenderState(g,mx,my,pt);}
    @Override public void tick(){if(ticks++%40==0)AirborneAdminClient.send("sync",new JsonObject());}
    private static String value(EditBox b){return b==null?"":b.getValue().strip();}
    private static JsonArray array(JsonObject o,String k){return o.has(k)&&o.get(k).isJsonArray()?o.getAsJsonArray(k):new JsonArray();}
    private static String text(JsonObject o,String k,String f){try{return o.has(k)?o.get(k).getAsString():f;}catch(RuntimeException e){return f;}}
}
