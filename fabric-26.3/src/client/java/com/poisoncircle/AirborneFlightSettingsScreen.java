package com.poisoncircle;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;

/** Flight values are validated again by the authoritative server. */
public final class AirborneFlightSettingsScreen extends AirborneAdminScreen {
    private EditBox heightBox,speedBox;
    private Button apply;
    private String heightValue,speedValue;
    public AirborneFlightSettingsScreen(){super("飞行设置");}
    @Override protected void build(){
        if(heightValue==null){JsonObject state=liveSnapshot.has("flight")&&liveSnapshot.get("flight").isJsonObject()?liveSnapshot.getAsJsonObject("flight"):liveSnapshot;heightValue=text(state,"height","200");speedValue=text(state,"speed","0.8");}
        heightBox=field("飞行高度",12,58,cardWidth-24,64,heightValue);heightBox.setResponder(v->heightValue=v);
        speedBox=field("飞行速度",12,110,cardWidth-24,16,speedValue);speedBox.setResponder(v->speedValue=v);
        apply=button("应用飞行参数",12,154,cardWidth-24,this::save);
        button("返回",12,188,cardWidth-24,()->open(new AirborneControlScreen()));updateControls();
    }
    private void save(){try{double height=Double.parseDouble(heightBox.getValue().strip()),speed=Double.parseDouble(speedBox.getValue().strip());if(!AirborneUiRules.validFlight(height,speed))throw new IllegalArgumentException();JsonObject d=new JsonObject();d.addProperty("height",height);d.addProperty("speed",speed);send("flight_settings",d);}catch(RuntimeException e){feedback="高度须为有限正数，速度须为0.2–2";}}
    @Override protected void updateControls(){if(apply!=null){apply.active=!locked();heightBox.setEditable(!locked());speedBox.setEditable(!locked());}}
    @Override protected void drawDetails(GuiGraphicsExtractor g){line(g,"高度 正数（默认200）",42);line(g,"速度 0.2–2",94);}
}
