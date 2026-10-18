const inventoryStage=document.querySelector('#stage'),inventoryImage=document.querySelector('#inventory');
inventoryImage.draggable=false;
let inventoryButton=-1,inventoryPoint={x:.5,y:.5},pendingInventoryMove=null,inventoryFramePending=false,inventoryBlob=null,wasInventoryOpen=false;
const mods=e=>({shift:e.shiftKey,ctrl:e.ctrlKey,alt:e.altKey});
function inventoryPosition(e){const r=inventoryStage.getBoundingClientRect(),w=inventoryImage.naturalWidth||640,h=inventoryImage.naturalHeight||480,scale=Math.min(r.width/w,r.height/h);return {x:(e.clientX-r.left-(r.width-w*scale)/2)/(w*scale),y:(e.clientY-r.top-(r.height-h*scale)/2)/(h*scale),...mods(e)};}
function inventoryInput(phase,data={}){return sendAction({type:'inventoryInput',phase,...data});}
function flushInventoryMove(){if(pendingInventoryMove){inventoryInput('move',pendingInventoryMove);pendingInventoryMove=null;}}
inventoryStage.addEventListener('pointerdown',e=>{
 if(!enabled||!mcState?.inventoryOpen)return;
 e.preventDefault();e.stopPropagation();inventoryStage.setPointerCapture(e.pointerId);
 flushInventoryMove();inventoryButton=e.button;inventoryPoint=inventoryPosition(e);
 inventoryInput('down',{...inventoryPoint,button:e.button});
},true);
inventoryStage.addEventListener('pointermove',e=>{
 if(!enabled||!mcState?.inventoryOpen)return;
 inventoryPoint=inventoryPosition(e);pendingInventoryMove=inventoryPoint;
},true);
window.addEventListener('pointerup',e=>{
 if(inventoryButton<0)return;
 flushInventoryMove();inventoryInput('up',{...inventoryPosition(e),button:inventoryButton});inventoryButton=-1;
 if(inventoryStage.hasPointerCapture(e.pointerId))inventoryStage.releasePointerCapture(e.pointerId);
},true);
inventoryStage.addEventListener('pointercancel',()=>{inventoryButton=-1;pendingInventoryMove=null;inventoryInput('cancel');},true);
inventoryStage.addEventListener('wheel',e=>{
 if(!mcState?.inventoryOpen)return;e.preventDefault();inventoryInput('wheel',{...inventoryPosition(e),delta:-Math.sign(e.deltaY)});
},{capture:true,passive:false});
window.addEventListener('blur',()=>{inventoryButton=-1;pendingInventoryMove=null;inventoryInput('cancel');});
const inventoryKeys={Escape:256,Enter:257,Backspace:259,Delete:261,Tab:258,ArrowRight:262,ArrowLeft:263,ArrowDown:264,ArrowUp:265,ShiftLeft:340,ShiftRight:344,ControlLeft:341,ControlRight:345,AltLeft:342,AltRight:346,Space:32,Semicolon:59,Equal:61,Comma:44,Minus:45,Period:46,Slash:47,BracketLeft:91,Backslash:92,BracketRight:93,Backquote:96};
function inventoryKeyboard(e,down){
 if(!enabled||!mcState?.inventoryOpen||e.target.closest?.('input,textarea,[contenteditable=true]'))return;
 e.preventDefault();e.stopImmediatePropagation();
 const code=inventoryKeys[e.code]??(/^Key[A-Z]$/.test(e.code)?e.code.charCodeAt(3):/^Digit[0-9]$/.test(e.code)?e.code.charCodeAt(5):e.key.length===1?-1:null);
 if(code!=null)inventoryInput('key',{keyCode:code,down,text:down&&e.key.length===1?e.key:'',...mods(e)});
}
window.addEventListener('keydown',e=>inventoryKeyboard(e,true),true);window.addEventListener('keyup',e=>inventoryKeyboard(e,false),true);
function inventoryPump(){flushInventoryMove();requestAnimationFrame(inventoryPump);}requestAnimationFrame(inventoryPump);
function updateInventory(state){
 const open=state.inventoryOpen;inventoryImage.style.display=open?'block':'none';
 if(open&&!wasInventoryOpen){partner.left=partner.right=partner.jump=false;sendAction({type:'release'});}
 wasInventoryOpen=open;
 if(open&&!inventoryFramePending){
  inventoryFramePending=true;
  fetch('http://127.0.0.1:18126/frame?t='+Date.now()).then(r=>{if(!r.ok||r.status===204)throw Error();return r.blob();}).then(blob=>{
   const url=URL.createObjectURL(blob),old=inventoryBlob;inventoryBlob=url;
   inventoryImage.onload=()=>{if(old)URL.revokeObjectURL(old);inventoryFramePending=false;};
   inventoryImage.onerror=()=>{URL.revokeObjectURL(url);inventoryFramePending=false;};inventoryImage.src=url;
  }).catch(()=>inventoryFramePending=false);
 }
}

