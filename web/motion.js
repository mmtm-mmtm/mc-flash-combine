// Display interpolation only: Minecraft still computes every movement and collision.
const motionSamples=[],motionTrace=[],nativeStreams=new Map();let motionScene=null,sceneImageCamera=null,viewState=null,lastPaint=-1;
let previousMotion=null;const movingMetrics={frames:0,reverseSteps:0,maxStep:0,maxGap:0};
let imageGeneration=0;
function resetMotion(){
 imageGeneration++;motionScene=null;motionSamples.length=0;motionTrace.length=0;viewState=null;sceneImageCamera=null;previousMotion=null;lastPaint=-1;
 for(const stream of nativeStreams.values())if(stream.url)URL.revokeObjectURL(stream.url);nativeStreams.clear();
 document.querySelector('#steve').style.display='none';for(const id of ['steve','scene','hud'])document.querySelector('#'+id).removeAttribute('src');
}
function sampleMotion(s){
 if(motionScene!==s.scene){motionScene=s.scene;motionSamples.length=0;lastPaint=-1;viewState=null;sceneImageCamera=null;}
 motionSamples.push({t:performance.now(),x:s.x,y:s.y,h:s.height,dir:s.facing});
 if(motionSamples.length>8)motionSamples.shift();
}
function motionAt(t){
 if(!motionSamples.length)return null;
 let a=motionSamples[0],b=a;
 for(const p of motionSamples){if(p.t<=t)a=p;else{b=p;break;}b=p;}
 const f=b.t>a.t?Math.max(0,Math.min(1,(t-a.t)/(b.t-a.t))):1;
 if(Math.hypot(b.x-a.x,b.y-a.y)>200)return b;
 return {x:a.x+(b.x-a.x)*f,y:a.y+(b.y-a.y)*f,h:a.h+(b.h-a.h)*f,dir:b.dir};
}
async function nativeImage(id,route,stamp,meta){
 let stream=nativeStreams.get(id);if(!stream){stream={busy:false,last:null,url:null};nativeStreams.set(id,stream);}
 if(stream.busy||stream.last===stamp)return;stream.busy=true;
 const generation=imageGeneration;
 try{
  const res=await fetch(base+'/'+route+'?t='+stamp);if(res.status!==200)return;
  const url=URL.createObjectURL(await res.blob()),img=new Image();img.src=url;await img.decode();if(generation!==imageGeneration){URL.revokeObjectURL(url);return;}
  document.querySelector('#'+id).src=url;const old=stream.url;stream.url=url;stream.last=stamp;
  if(old)URL.revokeObjectURL(old);
  if(id==='scene')sceneImageCamera=meta;
 }catch{}finally{stream.busy=false;}
}
function motionFrame(now){
 if(enabled&&mcState?.ready&&String(mcState.scene)===String(flashState?.scene)&&motionSamples.length){
  const p=motionAt(now-65);
  try{
   player.ruffle().callExternalInterface('mcPosition',p.x,p.y,p.h,p.dir);
   const v=player.ruffle().callExternalInterface('mcView');
   if(v&&String(v.scene)===String(motionScene)&&v.frame!==lastPaint){
    lastPaint=v.frame;viewState=v;
    const hero=document.querySelector('#steve');hero.style.display='block';hero.style.left=(v.x+v.cameraX-92.4)+'px';hero.style.top=(v.y+v.cameraY-v.height-165.9)+'px';
    if(sceneImageCamera)document.querySelector('#scene').style.transform=`translate(${v.cameraX-sceneImageCamera.x}px,${v.cameraY-sceneImageCamera.y}px)`;
    stage.dataset.viewFrame=String(v.frame);
    motionTrace.push({t:Math.round(now),frame:v.frame,x:v.x,screenX:v.x+v.cameraX,screenY:v.y+v.cameraY-v.height});
    const point=motionTrace[motionTrace.length-1];
    if(previousMotion){
     const wx=point.x-previousMotion.x,sx=point.screenX-previousMotion.screenX;
     if(Math.abs(wx)>.25){movingMetrics.frames++;movingMetrics.maxStep=Math.max(movingMetrics.maxStep,Math.abs(wx));movingMetrics.maxGap=Math.max(movingMetrics.maxGap,point.t-previousMotion.t);if(wx*sx<-.5)movingMetrics.reverseSteps++;stage.dataset.movingMetrics=JSON.stringify(movingMetrics);}
    }
    previousMotion=point;
    if(motionTrace.length>180)motionTrace.shift();
    stage.dataset.motionTrace=JSON.stringify(motionTrace);
   }
  }catch{}
 }
 requestAnimationFrame(motionFrame);
}
requestAnimationFrame(motionFrame);
