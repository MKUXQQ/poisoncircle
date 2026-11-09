import {createDesktopMcp} from 'file:///D:/dsh%E9%A1%B9%E7%9B%AE/blockbench-live/packages/dsh-blockbench-live/lib/desktop-mcp.js';
const c=createDesktopMcp({timeoutMs:30000});
try {
 const code=`(()=>{let t=Texture.all.find(x=>x.name==='airbornetransport'),x=t.canvas.getContext('2d');x.clearRect(0,0,128,128);x.fillStyle='#e8e5da';x.fillRect(0,0,128,128);x.fillStyle='#285b78';x.fillRect(0,0,128,16);x.fillRect(0,112,128,16);x.fillStyle='#b53632';x.fillRect(8,8,24,8);x.fillRect(96,8,24,8);x.fillStyle='#172b38';x.fillRect(32,8,32,8);t.source=t.canvas.toDataURL();t.img.src=t.source;t.saved=false;let d=x.getImageData(0,0,128,128).data,colors=new Set;for(let i=0;i<d.length;i+=4)colors.add(d[i]+','+d[i+1]+','+d[i+2]+','+d[i+3]);return {colors:[...colors],source:t.source.length,canvas:[t.canvas.width,t.canvas.height]}})()`;
 console.log(JSON.stringify(await c.tool('risky_eval',{code})));
} catch(e) { console.error(e); process.exitCode=1; } finally { c.close(); }
