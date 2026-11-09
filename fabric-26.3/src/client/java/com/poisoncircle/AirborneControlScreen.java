package com.poisoncircle;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Six entry points; mode and shared group are selected independently. */
public final class AirborneControlScreen extends AirborneAdminScreen {
    private Button start, stop, solo, duo;
    public AirborneControlScreen() { super("空降管理"); }
    static void clearSession() { }
    @Override protected void build() {
        int half=(cardWidth-30)/2;
        start=button("开始",12,74,half,()->send("start",new JsonObject()));
        stop=button("强制结束",18+half,74,half,()->send("stop",new JsonObject()));
        button("点位管理",12,100,cardWidth-24,()->open(new AirborneGroupsScreen(null)));
        solo=button("单人",12,126,half,()->open(new AirborneGroupsScreen("solo")));
        duo=button("多人",18+half,126,half,()->open(new AirborneGroupsScreen("duo")));
        button("飞行",12,152,cardWidth-24,()->open(new AirborneFlightSettingsScreen()));
        updateControls();
    }
    @Override protected void updateControls() { if(start!=null){start.active=!locked();stop.active=locked();solo.active=!locked();duo.active=!locked();} }
    @Override protected void drawDetails(GuiGraphicsExtractor g) {
        line(g,"当前模式 "+AirborneUiRules.modeLabel(AirborneUiRules.activeMode(liveSnapshot))+" / 分组 "+groupName(liveSnapshot),34);
        line(g,"状态 "+text(liveSnapshot,"status","未同步")+"  玩家 "+text(liveSnapshot,"participants","0")+"  倒数 "+text(liveSnapshot,"countdown","0")+"s",49);
    }
    static boolean isLockedStatus(String status) { return AirborneUiRules.locked(status); }
    static void snapshotChanged() {
        if(Minecraft.getInstance().gui.screen() instanceof AirborneAdminScreen s) s.updateSnapshot();
    }
    static void report(String message) {
        Minecraft c=Minecraft.getInstance();
        if(c.gui.screen() instanceof AirborneAdminScreen s)s.feedback=message;
        else if(c.gui.screen() instanceof AirbornePointsScreen s)s.report(message);
        else if(c.player!=null)c.gui.hud.setOverlayMessage(Component.literal(message),false);
    }
    static String groupName(JsonObject state) {
        String id=AirborneUiRules.selectedGroup(state,AirborneUiRules.activeMode(state));
        for(var e:array(state,"groups"))if(e.isJsonObject()&&text(e.getAsJsonObject(),"id","").equals(id))return text(e.getAsJsonObject(),"name",id);
        return id.isBlank()?"未选择":id;
    }
}

/** Shared look, live state and error reporting for the manager's three pages. */
abstract class AirborneAdminScreen extends Screen {
    static final int INK=0xFF2D190F,BORDER=0xFF6B4028,PANEL=0xFFF0D9A6,HOVER=0xFFE1BC78;
    protected int left,top,cardWidth;
    protected JsonObject liveSnapshot=new JsonObject();
    protected String feedback="";
    private String lastServerMessage="";
    private int ticks;
    private final List<EditBox> fields=new ArrayList<>();
    AirborneAdminScreen(String title){super(Component.literal(title));}
    @Override public boolean isPauseScreen(){return false;}
    @Override protected void init(){liveSnapshot=AirborneAdminClient.snapshot();fields.clear();cardWidth=Math.min(400,width-16);left=(width-cardWidth)/2;top=Math.max(0,(height-244)/2);build();acceptMessage();}
    protected abstract void build();
    protected abstract void updateControls();
    protected abstract void drawDetails(GuiGraphicsExtractor g);
    protected void stateChanged(){updateControls();}
    final void updateSnapshot(){liveSnapshot=AirborneAdminClient.snapshot();acceptMessage();stateChanged();}
    private void acceptMessage(){String m=text(liveSnapshot,"message","");if(!m.isBlank()&&!m.equals(lastServerMessage))feedback=m;lastServerMessage=m;}
    protected boolean locked(){return AirborneUiRules.locked(text(liveSnapshot,"status",""));}
    protected void open(Screen screen){Minecraft.getInstance().gui.setScreen(screen);}
    protected void send(String action,JsonObject data){feedback="请求已发送";AirborneAdminClient.send(action,data);}
    protected Button button(String text,int x,int y,int w,Runnable run){return addRenderableWidget(new Button(left+x,top+y,w,20,Component.literal(text),b->run.run(),n->n.get()){
        @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float pt){g.fill(getX(),getY(),getRight(),getBottom(),BORDER);g.fill(getX()+1,getY()+1,getRight()-1,getBottom()-1,!active?0xFFD0C2A4:isHoveredOrFocused()?HOVER:PANEL);var m=net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(getMessage(),getWidth()-8));g.text(font,m,getX()+(getWidth()-font.width(m))/2,getY()+6,INK,false);}
    });}
    protected EditBox field(String hint,int x,int y,int w,int max,String value){EditBox b=new EditBox(font,left+x+4,top+y+4,w-8,14,Component.literal(hint));b.setMaxLength(max);b.setBordered(false);b.setTextColor(INK);b.setTextShadow(false);b.setValue(value);fields.add(b);addRenderableWidget(b);return b;}
    protected void line(GuiGraphicsExtractor g,String text,int y){g.text(font,font.plainSubstrByWidth(text,cardWidth-24),left+12,top+y,INK,false);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float pt){g.fill(0,0,width,height,0xA008101A);g.fill(left,top,left+cardWidth,top+244,BORDER);g.fill(left+2,top+2,left+cardWidth-2,top+242,PANEL);line(g,getTitle().getString(),12);drawDetails(g);for(EditBox b:fields){g.fill(b.getX()-4,b.getY()-4,b.getRight()+4,b.getBottom()+4,BORDER);g.fill(b.getX()-3,b.getY()-3,b.getRight()+3,b.getBottom()+3,0xFFF8E9C9);}line(g,feedback,228);super.extractRenderState(g,mx,my,pt);}
    @Override public void tick(){if(++ticks%40==0)AirborneAdminClient.send("sync",new JsonObject());}
    static JsonArray array(JsonObject o,String key){return o.has(key)&&o.get(key).isJsonArray()?o.getAsJsonArray(key):new JsonArray();}
    static String text(JsonObject o,String key,String fallback){try{return o.has(key)?o.get(key).getAsString():fallback;}catch(RuntimeException e){return fallback;}}
    static JsonObject data(String key,String value){JsonObject d=new JsonObject();d.addProperty(key,value);return d;}
}
