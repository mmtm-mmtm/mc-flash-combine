// Vanilla chooses sounds/variants and owns lifecycle. Web Audio plays the actual resolved OGG.
const mcAudioBase='http://127.0.0.1:18126',mcAudioClient=crypto.randomUUID();
const mcAudioButton=document.createElement('button');mcAudioButton.id='mcAudioButton';mcAudioButton.textContent='点击启用 MC 音效';
document.querySelector('#toolbar').append(mcAudioButton);
const mcAudioRoot=document.querySelector('#stage'),mcAudioVoices=new Map(),mcAudioBuffers=new Map();
const mcAudioRecent=[];
let mcAudioContext=null,mcAudioMaster=null,mcAudioAnalyser=null,mcAudioScene=null,mcAudioBoot=null,mcAudioCursor=-1;
let mcAudioWanted=true,mcAudioPaused=false,mcAudioPlayed=0,mcAudioDecoded=0,mcAudioPeak=0,mcAudioError='',mcAudioLastPoll=0,mcAudioGeneration=0;
function mcAudioMetrics(){
 Object.assign(mcAudioRoot.dataset,{audioState:mcAudioContext?.state??'locked',audioPlayed:String(mcAudioPlayed),audioDecoded:String(mcAudioDecoded),audioVoices:String(mcAudioVoices.size),audioPeak:String(mcAudioPeak),audioLast:mcAudioRoot.dataset.audioLast??'',audioError:mcAudioError,audioMuted:String(!mcAudioWanted),audioScene:mcAudioScene??''});
 mcAudioButton.textContent=!mcAudioContext||mcAudioContext.state!=='running'?'点击启用 MC 音效':mcAudioWanted?'MC 音效：开启':'MC 音效：静音';
 mcAudioButton.setAttribute('aria-pressed',String(mcAudioWanted&&mcAudioContext?.state==='running'));
 mcAudioButton.title=mcAudioError||`原生声音已播放 ${mcAudioPlayed} 次`;
}
function mcAudioGain(){
 if(!mcAudioMaster)return;
 mcAudioMaster.gain.setTargetAtTime(mcAudioWanted&&!mcAudioPaused&&!document.hidden?1:0,mcAudioContext.currentTime,.015);
 for(const voice of mcAudioVoices.values())if(voice.source)voice.source.playbackRate.setTargetAtTime(mcAudioPaused?0:voice.event.pitch,mcAudioContext.currentTime,.015);
}
async function mcAudioUnlock(){
 try{
  if(!mcAudioContext){
   mcAudioContext=new AudioContext({latencyHint:'interactive'});mcAudioMaster=mcAudioContext.createGain();mcAudioAnalyser=mcAudioContext.createAnalyser();mcAudioAnalyser.fftSize=512;
   mcAudioMaster.connect(mcAudioAnalyser);mcAudioAnalyser.connect(mcAudioContext.destination);
  }
  await mcAudioContext.resume();mcAudioGain();mcAudioMetrics();
 }catch(error){mcAudioError=String(error);mcAudioMetrics();}
}
function mcAudioStop(id){const voice=mcAudioVoices.get(id);if(!voice)return;mcAudioVoices.delete(id);voice.cancelled=true;if(voice.source){try{voice.source.stop();}catch{}voice.source.disconnect();voice.gain.disconnect();voice.pan.disconnect();}}
function mcAudioClear(){for(const id of [...mcAudioVoices.keys()])mcAudioStop(id);}
function updateNativeAudio(state){
 const next=String(state.scene);
 if(next!==mcAudioScene){mcAudioScene=next;mcAudioCursor=-1;mcAudioGeneration++;mcAudioClear();}
 mcAudioPaused=!!state.won;mcAudioGain();
}
async function mcAudioBuffer(clip){
 const key=mcAudioBoot+':'+clip;
 if(!mcAudioBuffers.has(key)){
  const promise=fetch(mcAudioBase+'/audio/clip?path='+encodeURIComponent(clip)).then(r=>{if(!r.ok)throw Error('原生音频资源读取失败 '+clip+' '+r.status);return r.arrayBuffer();}).then(data=>mcAudioContext.decodeAudioData(data)).then(buffer=>{mcAudioDecoded++;return buffer;});
  mcAudioBuffers.set(key,promise);promise.catch(()=>mcAudioBuffers.delete(key));
  while(mcAudioBuffers.size>96)mcAudioBuffers.delete(mcAudioBuffers.keys().next().value);
 }
 return mcAudioBuffers.get(key);
}
function mcAudioUpdateVoice(voice,event){
 voice.event=event;
 if(!voice.source)return;
 const now=mcAudioContext.currentTime;
 voice.gain.gain.setTargetAtTime(event.gain,now,.015);voice.pan.pan.setTargetAtTime(event.pan,now,.015);voice.source.playbackRate.setTargetAtTime(mcAudioPaused?0:event.pitch,now,.015);
}
async function mcAudioPlay(event){
 if(mcAudioVoices.has(event.id)){mcAudioUpdateVoice(mcAudioVoices.get(event.id),event);return;}
 const voice={event,cancelled:false,generation:mcAudioGeneration,source:null};mcAudioVoices.set(event.id,voice);
 try{
  const buffer=await mcAudioBuffer(event.clip);
  if(voice.cancelled||voice.generation!==mcAudioGeneration)return;
  // Native stop may arrive while decoding. Never resurrect a stopped voice.
  const current=voice.event;let offset=0;
  const elapsed=Math.max(0,(Date.now()-current.started)/1000)*current.pitch;
  if(elapsed>2){if(current.loop)offset=elapsed%buffer.duration;else if(elapsed<buffer.duration)offset=elapsed;else{mcAudioStop(event.id);return;}}
  const source=mcAudioContext.createBufferSource(),gain=mcAudioContext.createGain(),pan=mcAudioContext.createStereoPanner();
  Object.assign(voice,{source,gain,pan});source.buffer=buffer;source.loop=current.loop;source.playbackRate.value=mcAudioPaused?0:current.pitch;gain.gain.value=current.gain;pan.pan.value=current.pan;
  source.connect(gain);gain.connect(pan);pan.connect(mcAudioMaster);
  source.onended=()=>{if(mcAudioVoices.get(event.id)===voice){mcAudioVoices.delete(event.id);source.disconnect();gain.disconnect();pan.disconnect();}};
  source.start(0,offset);mcAudioPlayed++;mcAudioError='';mcAudioRoot.dataset.audioLast=current.event;
  mcAudioRecent.push({event:current.event,clip:current.clip,loop:current.loop,gain:current.gain,pitch:current.pitch});if(mcAudioRecent.length>32)mcAudioRecent.shift();mcAudioRoot.dataset.audioRecent=JSON.stringify(mcAudioRecent);mcAudioMetrics();
 }catch(error){mcAudioStop(event.id);mcAudioError=String(error);mcAudioMetrics();}
}
async function mcAudioPoll(){
 try{
  if(!mcAudioContext||mcAudioContext.state!=='running'||mcAudioScene===null)return;
  if(!enabled||!mcState?.ready||String(mcState.scene)!==mcAudioScene){mcAudioClear();return;}
  const scene=mcAudioScene,generation=mcAudioGeneration;
  const response=await fetch(mcAudioBase+'/audio/events?client='+mcAudioClient+'&scene='+encodeURIComponent(scene)+'&after='+mcAudioCursor,{cache:'no-store'});
  if(!response.ok)throw Error('MC 音频连接 '+response.status);
  const packet=await response.json();
  if(generation!==mcAudioGeneration||scene!==mcAudioScene)return;
  mcAudioLastPoll=performance.now();
  if(!packet.ready){mcAudioClear();return;}
  if(mcAudioBoot!==packet.boot){mcAudioBoot=packet.boot;mcAudioBuffers.clear();mcAudioClear();}
  mcAudioPaused=!!packet.paused||!!mcState.won;mcAudioGain();
  mcAudioRoot.dataset.audioNativeMutedWrites=String(packet.nativeMutedWrites??0);
  if(packet.reset)mcAudioClear();
  else for(const event of packet.events){if(event.op==='reset')mcAudioClear();else if(event.op==='stop')mcAudioStop(event.id);else if(event.op==='play')mcAudioPlay(event);}
  // Reconcile live voices, including loops rejoining after a missing packet. One-shots are not replayed.
  for(const event of packet.active){const voice=mcAudioVoices.get(event.id);if(voice)mcAudioUpdateVoice(voice,event);else if(event.loop)mcAudioPlay(event);}
  mcAudioCursor=packet.seq;
  const samples=new Float32Array(mcAudioAnalyser.fftSize);mcAudioAnalyser.getFloatTimeDomainData(samples);
  const level=Math.max(...samples.map(Math.abs));mcAudioRoot.dataset.audioLevel=String(level);
  mcAudioPeak=Math.max(mcAudioPeak,level);mcAudioMetrics();
 }catch(error){if(performance.now()-mcAudioLastPoll>1000){mcAudioClear();mcAudioError=String(error);mcAudioMetrics();}}
 finally{setTimeout(mcAudioPoll,40);}
}
mcAudioButton.addEventListener('click',()=>{if(!mcAudioContext||mcAudioContext.state!=='running'){mcAudioWanted=true;mcAudioUnlock();}else{mcAudioWanted=!mcAudioWanted;mcAudioGain();mcAudioMetrics();}});
for(const kind of ['pointerdown','keydown'])window.addEventListener(kind,event=>{if(event.isTrusted&&!event.target.closest?.('#mcAudioButton')&&(!mcAudioContext||mcAudioContext.state!=='running'))mcAudioUnlock();},true);
document.addEventListener('visibilitychange',mcAudioGain);
window.addEventListener('pagehide',()=>{mcAudioClear();mcAudioGeneration++;});
mcAudioMetrics();mcAudioPoll();
