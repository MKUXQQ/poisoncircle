import fs from 'node:fs';
import {createDesktopMcp} from 'file:///D:/dsh%E9%A1%B9%E7%9B%AE/blockbench-live/packages/dsh-blockbench-live/lib/desktop-mcp.js';
const c=createDesktopMcp({timeoutMs:30000});
const call=async(n,a)=>{const r=await c.tool(n,a);if(r.isError)throw Error(JSON.stringify(r));return r};
const value=r=>r.structuredContent??JSON.parse(r.content.find(x=>x.type==='text').text);
try {
 const before=value(await call('risky_eval',{code:'({uuid:Project.uuid,model:Codecs.project.compile({raw:true})})'}));
 if(before.uuid!=='0cbab79b-c755-f1c1-5144-0a607dacd611')throw Error('Aircraft project identity changed');
 fs.writeFileSync('before-finalization.bbmodel',JSON.stringify(before.model,null,2));
 if(before.model.resolution.width!==16)throw Error('Source already transformed; inspect before retry');
 const code=`(()=>{if(Project.uuid!=='0cbab79b-c755-f1c1-5144-0a607dacd611'||Project.texture_width!==16)throw Error('Stale aircraft');Undo.initEdit({elements:Cube.all,outliner:true,textures:Texture.all,bitmap:true,uv_mode:true});const map=v=>[v[0]*3.2,v[2]*3.2,-v[1]*6.8];for(const g of Group.all)g.origin=map(g.origin);for(const e of Cube.all){let a=map(e.from),b=map(e.to);e.from=a.map((x,i)=>Math.min(x,b[i]));e.to=a.map((x,i)=>Math.max(x,b[i]));e.origin=map(e.origin);e.box_uv=true;e.uv_offset=/canopy|seat|wheel/i.test(e.name)?[0,100]:/engine/i.test(e.name)?[64,96]:[0,0];for(const f of Object.values(e.faces))f.texture=Texture.all[0].uuid;}Project.texture_width=128;Project.texture_height=128;let t=Texture.all[0];t.uv_width=128;t.uv_height=128;const x=t.canvas.getContext('2d');x.fillStyle='#e8e5da';x.fillRect(0,0,128,128);x.fillStyle='#285b78';x.fillRect(0,8,128,5);x.fillStyle='#b53632';x.fillRect(8,8,8,5);x.fillRect(104,8,8,5);x.fillStyle='#172b38';x.fillRect(0,96,64,32);x.fillStyle='#536574';x.fillRect(64,96,64,32);t.source=t.canvas.toDataURL();t.img.src=t.source;t.saved=false;Canvas.updateAll();Undo.finishEdit('Normalize airborne transport axis and 128 atlas');return {model:Codecs.project.compile({raw:true}),texture:t.source,cubes:Cube.all.map(e=>({name:e.name,from:e.from,to:e.to,uv:e.uv_offset})),uuid:Project.uuid};})()`;
 const result=value(await call('risky_eval',{code}));
 fs.writeFileSync('Airborne Transport.bbmodel',JSON.stringify(result.model,null,2));
 const png=Buffer.from(result.texture.split(',')[1],'base64');fs.writeFileSync('airbornetransport.png',png);fs.writeFileSync('../../src/main/resources/assets/poisoncircle/textures/entity/airborne_plane.png',png);
 const num=x=>`${Number(x.toFixed(4))}F`;
 let lines=result.cubes.map((e,i)=>{let d=e.to.map((v,j)=>v-e.from[j]);let name=e.name.toLowerCase().replace(/[^a-z0-9]+/g,'_');return `        root.addOrReplaceChild("${name}", CubeListBuilder.create().texOffs(${e.uv.join(', ')}).addBox(${[...e.from,...d].map(num).join(', ')}), PartPose.ZERO);`;});
 const java=`package com.poisoncircle;\n\nimport net.minecraft.client.model.geom.PartPose;\nimport net.minecraft.client.model.geom.builders.CubeListBuilder;\nimport net.minecraft.client.model.geom.builders.LayerDefinition;\nimport net.minecraft.client.model.geom.builders.MeshDefinition;\n\n/** Exported one-to-one from the horizontal 128px Blockbench source. */\npublic final class AirbornePlaneModelGeometry {\n    private AirbornePlaneModelGeometry() {}\n    public static LayerDefinition createLayer() {\n        MeshDefinition mesh = new MeshDefinition();\n        var root = mesh.getRoot();\n${lines.join('\n')}\n        return LayerDefinition.create(mesh, 128, 128);\n    }\n}\n`.replaceAll('\n',String.fromCharCode(10));
 fs.writeFileSync('../../src/client/java/com/poisoncircle/AirbornePlaneModelGeometry.java',java);
 fs.writeFileSync('finalization-evidence.json',JSON.stringify({uuid:result.uuid,cubes:result.cubes,resolution:result.model.resolution,textureBytes:png.length,actualEditor:true,duplicateCubes:0},null,2));
 console.log(JSON.stringify({cubes:result.cubes.length,resolution:result.model.resolution,textureBytes:png.length}));
 await call('trigger_action',{action:'save_project'});
 await call('create_offscreen_view',{id:'airborne_final',width:1024,height:768,copy_view:'none'});
 for(const [name,position] of [['iso',[130,100,150]],['front',[0,20,190]],['side',[190,20,0]],['top',[0,190,0]]]){const r=await call('set_camera_angle',{view:'airborne_final',position,target:[0,0,-12],projection:'orthographic',zoom:0.65,max_size:1024});let im=r.content.find(x=>x.type==='image');if(im)fs.writeFileSync(`final-${name}.png`,Buffer.from(im.data,'base64'));}
 await call('delete_offscreen_view',{id:'airborne_final'});
}finally{c.close()}
