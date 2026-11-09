import fs from 'node:fs';
import {createDesktopMcp} from 'file:///D:/dsh%E9%A1%B9%E7%9B%AE/blockbench-live/packages/dsh-blockbench-live/lib/desktop-mcp.js';
const c=createDesktopMcp({timeoutMs:30000});
try {let result=process.argv[2]==='list'?await c.request('tools/list'):await c.tool(process.argv[2],JSON.parse(process.argv[3]||'{}')); if(process.argv[4])fs.writeFileSync(process.argv[4],JSON.stringify(result,null,2));else console.log(JSON.stringify(result));}finally{c.close()}
