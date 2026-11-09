import {createDesktopMcp} from 'file:///D:/dsh%E9%A1%B9%E7%9B%AE/blockbench-live/packages/dsh-blockbench-live/lib/desktop-mcp.js';
const c=createDesktopMcp({timeoutMs:15000});
try { const list=await c.request('tools/list'); console.log(JSON.stringify(list)); for (const name of ['get_capabilities','list_modes','get_project_info']) { try {console.log(name,JSON.stringify(await c.tool(name,name==='get_capabilities'?{include_tools:true}:{})));}catch(e){console.log(name,e.message)} } }finally{c.close()}
