import fs from 'node:fs';
import {createDesktopMcp} from 'file:///D:/dsh%E9%A1%B9%E7%9B%AE/blockbench-live/packages/dsh-blockbench-live/lib/desktop-mcp.js';
const c=createDesktopMcp({timeoutMs:30000});
const call=async(n,a)=>{const r=await c.tool(n,a); if(r.isError)throw Error(JSON.stringify(r));return r.structuredContent??r};
try {
const groups=[['Fuselage',[0,0,0] ],['LeftWing',[-1,4,0]],['RightWing',[1,4,0]],['Tail',[0,-4,0]],['Cockpit',[0,1,0]],['Engines',[0,4,0]],['LandingGear',[0,0,0]]];
for(const [name,origin] of groups) await call('add_group',{name,origin});
const tex='63df8a27-5975-8668-74b6-11a0fa1a3f9e';
const cubes=[
['Nose',[-1,-5,-1],[1,-1,1],'Fuselage'],['Cabin core',[-1,-1,-1],[1,4,1],'Fuselage'],['Rear fuselage',[-1,4,-1],[1,8,1],'Fuselage'],
['Port wing root',[-8,0,-0.5],[-1,4,0.5],'LeftWing'],['Port wing tip',[-10,3,-0.4],[-8,6,0.4],'LeftWing'],['Starboard wing root',[1,0,-0.5],[8,4,0.5],'RightWing'],['Starboard wing tip',[8,3,-0.4],[10,6,0.4],'RightWing'],
['Port aileron',[-9,4,-0.65],[-7,6,-0.4],'LeftWing'],['Starboard aileron',[7,4,-0.65],[9,6,-0.4],'RightWing'],
['Tailplane root',[-1,7,-0.25],[1,9,0.25],'Tail'],['Port tailplane',[-4,7,-0.2],[-1,9,0.2],'Tail'],['Starboard tailplane',[1,7,-0.2],[4,9,0.2],'Tail'],['Fin',[ -0.5,7,-0.2],[0.5,9,2.2],'Tail'],
['Cockpit canopy',[-0.85,-1,1],[0.85,1.4,2],'Cockpit'],['Pilot seat',[-0.5,-0.2,0],[0.5,0.4,1],'Cockpit'],
['Port engine nacelle',[-1.1,2,-1.7],[-0.2,5,-0.7],'Engines'],['Starboard engine nacelle',[0.2,2,-1.7],[1.1,5,-0.7],'Engines'],
['Left wheel',[ -2,0, -2.2],[-1,0.8,-1.5],'LandingGear'],['Right wheel',[1,0,-2.2],[2,0.8,-1.5],'LandingGear'],['Tail wheel',[-0.4,7,-1.7],[0.4,7.6,-1.1],'LandingGear']];
await call('place_cube',{texture:tex,elements:cubes.map(([name,from,to])=>({name,from,to,origin:[0,0,0]}))});
await call('trigger_action',{action:'save_project'});
const r=await call('risky_eval',{code:"({path:Project.save_path,uuid:Project.uuid,format:Project.format.id,groups:Outliner.root.map(g=>({name:g.name,cubes:g.children.filter(x=>x.type==='cube').length})),cubeCount:Cube.all.length,texture:Texture.all.map(t=>({name:t.name,width:t.width,height:t.height})),file:Project.save_path})"});
fs.writeFileSync('live-inspection.json',JSON.stringify(r,null,2)); console.log(JSON.stringify(r));
}finally{c.close()}
