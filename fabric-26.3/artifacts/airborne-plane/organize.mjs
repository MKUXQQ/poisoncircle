import {createDesktopMcp} from 'file:///D:/dsh%E9%A1%B9%E7%9B%AE/blockbench-live/packages/dsh-blockbench-live/lib/desktop-mcp.js';
const c=createDesktopMcp({timeoutMs:30000});
const mapping={'Nose':'Fuselage','Cabin core':'Fuselage','Rear fuselage':'Fuselage','Port wing root':'LeftWing','Port wing tip':'LeftWing','Port aileron':'LeftWing','Starboard wing root':'RightWing','Starboard wing tip':'RightWing','Starboard aileron':'RightWing','Tailplane root':'Tail','Port tailplane':'Tail','Starboard tailplane':'Tail','Fin':'Tail','Cockpit canopy':'Cockpit','Pilot seat':'Cockpit','Port engine nacelle':'Engines','Starboard engine nacelle':'Engines','Left wheel':'LandingGear','Right wheel':'LandingGear','Tail wheel':'LandingGear'};
try{const code=`Cube.all.forEach(c=>{let g=Group.all.find(x=>x.name===${JSON.stringify(mapping)}[c.name]);c.addTo(g);c.init()});Outliner.root.forEach(g=>g.sortContent());({groups:Group.all.map(g=>({name:g.name,count:g.children.length})),cubes:Cube.all.length,savePath:Project.save_path})`;
const r=await c.tool('risky_eval',{code});console.log(JSON.stringify(r));
}catch(e){console.error(e)}finally{c.close()}
