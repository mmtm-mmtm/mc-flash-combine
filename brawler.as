var mcEnabled=false;
var mcTravel=0;var SunChipsFrame=0;
var mcScene=getTimer()+new Date().getTime();
var mcBlocks={};var mcDamage={};var mcKids={};var mcNextKid=0;
var mcHitSeq=0;var mcHits=[];var mcAttackerX=0;var mcAttackerY=0;
var mcPlayerX=0;var mcPlayerY=0;var mcHeight=0;var mcFacing=1;
var mcLastX=0;var mcLastY=0;
var mcStreaming=false;var mcViewFrame=0;
var mcFeedback={hits:0,blood:0,objects:0,last:""};
function mcTarget(k,id,object) {
 var b=k.getBounds(this);var by=k.body==undefined?0:k.body._y;
 return {id:id,x:k.x,y:k.y,hp:object?3000:k.health,maxHP:object?3000:k.health_max,bodyY:by,name:k.name,
  nohit:object?false:(Boolean(k.nohit)||Boolean(pause)),frame:k._currentframe,object:object,grab:Boolean(k.grab),rider:k.rider==undefined?-1:k.rider,
  bounds:{x0:b.xMin,x1:b.xMax,y0:b.yMin,y1:b.yMax}};
}
function mcPosition(px,py,h,dir){mcStreaming=true;mcPlayerX=px;mcPlayerY=py;mcHeight=h;mcFacing=dir;}
function mcView(){return {scene:mcScene,frame:mcViewFrame,cameraX:game_x,cameraY:game_y,x:dude1.x,y:dude1.y,height:dude1.body==undefined?0:-dude1.body._y};}
function mcQueueHit(amount) {
 var nearest=null;var distance=10000000;var kid;var d;
 for(var j=1;j<=total_kids;j++) {
  kid=this["kid"+j];
  if(kid.alive && kid.health>0){d=Math.abs(kid.x-dude1.x)+Math.abs(kid.y-dude1.y);if(d<distance){distance=d;nearest=kid;}}
 }
 mcHitSeq++;mcHits.push({id:mcHitSeq,damage:amount/5,x:nearest==null?dude1.x+64:nearest.x,y:nearest==null?dude1.y:nearest.y});
}
function mcState() {
 if(dude1==undefined)return {ready:false};
 var enemies=[];var actors=[];var k;var id;
 for(var n=1;n<=total_kids;n++) {
  k=this["kid"+n];
  if(k.alive)actors.push({id:k.mcId==undefined?"slot"+n:k.mcId,hp:k.health,bodyY:k.body==undefined?0:k.body._y,vy:k.speed_toss_y==undefined?0:k.speed_toss_y,frame:k._currentframe,x:k.x,y:k.y});
  if(k.alive && k.health>0) {
   if(k.mcId==undefined){mcNextKid++;k.mcId=String(mcNextKid);}
   id=k.mcId;mcKids[id]=k;
   enemies.push(mcTarget(k,id,false));
  }
 }
 for(n=1;n<=total_objects;n++) {
  k=this["object_pt"+n];
  if(k!=undefined && k.punch && k.active) {
   if(k.mcId==undefined){mcNextKid++;k.mcId="object"+mcNextKid;}
   id=k.mcId;mcKids[id]=k;k.mcObject=true;enemies.push(mcTarget(k,id,true));
  }
 }
 var colliders=[];var o;
 for(var t=0;t<2;t++) {
  var count=t==0?total_statics:total_objects;
  for(var q=1;q<=count;q++) {
   o=this[(t==0?"static":"object_pt")+q];
   if(o!=undefined && o.zone!=undefined && (t==0 || o.active)) {
    var sx=Math.abs(o._xscale)/100;var sy=Math.abs(o._yscale)/100;
    var cx=o.x+o.zone._x;var cy=o.y+o.zone._y+2;
    var w=o.zone._width*sx/2;var h=o.zone._height*sy;
    if(w>1 && h>1 && Math.abs(cx-dude1.x)<1200)
     colliders.push({x0:cx-w,x1:cx+w,y0:cy-h,y1:cy,height:Math.min(120,Math.max(30,o._height*.6)),diagonal:Boolean(o.diagonal)});
   }
  }
 }
 return {ready:true,game:"dadnme",scene:mcScene,x:dude1.x,y:dude1.y,hp:dude1.health,
  travel:mcTravel,travelX:mcPlayerX,travelY:mcPlayerY,paused:Boolean(pause),cameraX:game_x,cameraY:game_y,minX:limit_topleft.x,maxX:limit_bottomright.x,
  minY:limit_topleft.y,maxY:limit_bottomright.y,checkpoint:checkpoint,enemies:enemies,colliders:colliders,hits:mcHits,
  feedback:mcFeedback,playerProxy:{bodyY:dude1.body._y,nohit:Boolean(dude1.nohit),alive:Boolean(dude1.alive),frame:dude1._currentframe,actors:actors}};
}
function mcApply(s) {
 if(s==undefined || s.scene!=mcScene || dude1==undefined)return false;
 mcEnabled=true;
 if(!mcStreaming){mcPlayerX=s.x;mcPlayerY=s.y;mcHeight=s.height;mcFacing=s.facing;}
 dude1.health=s.hp*5;dude1._visible=false;
 dude1.button_punch1=254;dude1.button_punch2=253;
 dude1.button_left=252;dude1.button_right=251;dude1.button_up=250;dude1.button_down=249;
 if(dude1.health<=0 && dude1.alive!=false)f_CheckDead(dude1);
 var d;var kid;var previous;var delta;
 for(var z=0;z<s.damage.length;z++) {
  d=s.damage[z];kid=mcKids[d.id];previous=mcDamage[d.id]==undefined?0:mcDamage[d.id];delta=d.amount-previous;
  if(delta>0 && kid!=undefined && kid.mcObject) {
   if(kid.punch && kid.active) {
    f_FlipInverse(kid,dude1);
    if(kid.grab){kid.speed_toss_y=-6;kid.speed_toss_x=random(6)+5;f_CallJuggle1(kid);f_PunchSound();}
    else if(kid.punch_function!=undefined)kid.punch_function(kid);
    mcFeedback.objects++;mcFeedback.last=d.id;
   }
   mcDamage[d.id]=d.amount;continue;
  }
  if((delta>0 || d.dead) && kid!=undefined && kid.alive && !kid.mcDying) {
   if(delta>0)f_Damage(kid,delta*3);
   var hitY=kid.y+(kid.body==undefined?0:kid.body._y)-35;
   f_FX(kid.x,hitY,int(kid.y)+7,"impact1");
   f_FX(kid.x,hitY,int(kid.y)+6,"blood1",kid.x>dude1.x?100:-100);
   f_PunchSound();mcFeedback.hits++;mcFeedback.blood++;mcFeedback.last=d.id;
   if(d.dead || kid.health<=0.001) {
    kid.health=0;kid.mcDying=true;
    kid.speed_toss_x=0;kid.speed_toss_y=0;kid.body._y=0;
    f_CheckHold(kid);f_KidSettings(kid);
   }
   else {f_CheckHold(kid);f_FlipInverse(kid,dude1);kid.gotoAndStop("hit"+(1+(mcFeedback.hits-1)%3));kid.body.gotoAndPlay(1);}
  }
  mcDamage[d.id]=d.amount;
 }
 while(mcHits.length>0 && mcHits[0].id<=s.hitAck)mcHits.shift();
 var keep={};var b;var key;
 for(z=0;z<s.blocks.length;z++) {
  b=s.blocks[z];key=b.key;keep[key]=true;mcBlocks[key]={x:b.x,y:b.y,height:b.height};
 }
 for(key in mcBlocks)if(!keep[key])delete mcBlocks[key];
 return true;
}
function mcFrame() {
 if(!mcEnabled || dude1==undefined)return;
 mcViewFrame++;
 // Scroll using the original camera function, with MC's authoritative street position.
 var dx=mcPlayerX-dude1.x;var dy=mcPlayerY-dude1.y;
 dude1.x=mcPlayerX;dude1.y=mcPlayerY;
 dude1._x=mcPlayerX;dude1._y=mcPlayerY;
 dude1._xscale=mcFacing*100;dude1._visible=false;
 // Native MC owns the player's knockback/jump. The Flash actor is only an AI target.
 if(dude1.health>0) {
  dude1.alive=true;dude1.nohit=false;
  if(dude1._currentframe!=1)dude1.gotoAndStop("stand");
  if(dude1.body==undefined)dude1.createEmptyMovieClip("body",65500);
  dude1.body._y=-mcHeight;
 }
 if(dude1.shadow_pt!=undefined){dude1.shadow_pt._x=mcPlayerX;dude1.shadow_pt._y=mcPlayerY;}
 if(dx!=0 && ((dx>0 && mcPlayerX+game_x>screen_width/2)||(dx<0 && mcPlayerX+game_x<screen_width/2)))f_Scroll(dx,false);
 if(dy!=0 && ((dy>0 && mcPlayerY+game_y>340)||(dy<0 && mcPlayerY+game_y<220)))f_Scroll(dy,true);
 f_Depth(dude1,mcPlayerY);f_Mask(dude1);
}
flash.external.ExternalInterface.addCallback("mcState",this,mcState);
flash.external.ExternalInterface.addCallback("mcPosition",this,mcPosition);
flash.external.ExternalInterface.addCallback("mcView",this,mcView);
flash.external.ExternalInterface.addCallback("mcApply",this,mcApply);
flash.external.ExternalInterface.addCallback("mcReset",this,function(){_root.gotoAndStop(2);_root.gotoAndStop(3);return true;});
function mcBossStage(){
 if(dude1==undefined)return false;
 // Use the original final arena, boss movie clip, introduction and combat AI.
 for(var i=1;i<=total_kids;i++){var kid=this["kid"+i];if(kid!=undefined){if(kid.shadow_pt!=undefined)f_RemoveChar(kid.shadow_pt);f_RemoveChar(kid);}}
 f_StopCarScene1();f_SwingsOff();bballhoop.gotoAndStop("off");
 pause=false;checkpoint=10;kills=0;kills_goal=3;
 limit_bottomright.x=12510;limit_bottomright._x=12510;
 limit_topleft.x=10710;limit_topleft._x=10710;new_limit_topleft=10710;checkpoint_x=12510;
 var newCamera=screen_width-limit_bottomright.x;var delta=game_x-newCamera;
 game_x=newCamera;_X=game_x;bg0._x-=delta*.25;bg1._x+=delta/2;bg2._x+=delta*.65;
 mcPlayerX=limit_bottomright.x-650;mcPlayerY=limit_topleft.y+200;mcHeight=0;mcStreaming=true;
 f_SetXY(dude1,mcPlayerX,mcPlayerY);f_StaticRange();
 mcTravel++;med1.gotoAndStop("on");f_SpawnSunChips();mcViewFrame++;
 return true;
}
flash.external.ExternalInterface.addCallback("mcBossStage",this,mcBossStage);
function mcNextStage(){
 if(dude1==undefined || checkpoint>=10)return false;
 for(var i=1;i<=total_kids;i++){var kid=this["kid"+i];if(kid!=undefined){if(kid.shadow_pt!=undefined)f_RemoveChar(kid.shadow_pt);f_RemoveChar(kid);}}
 pause=false;mcAdvanceCheckpoint();
 var newCamera=screen_width-limit_bottomright.x;var delta=game_x-newCamera;
 game_x=newCamera;_X=game_x;bg0._x-=delta*.25;bg1._x+=delta/2;bg2._x+=delta*.65;
 // Run the original right-edge encounter hooks exactly once for this checkpoint.
 limit_topleft.x=-game_x-1;f_Scroll(0,false);
 limit_topleft.x=new_limit_topleft;limit_topleft._x=limit_topleft.x;
 mcPlayerX=Math.max(limit_topleft.x+60,limit_bottomright.x-650);mcPlayerY=limit_topleft.y+200;mcHeight=0;mcStreaming=true;
 f_SetXY(dude1,mcPlayerX,mcPlayerY);f_StaticRange();
 var n=0;for(i=1;i<=total_kids;i++){kid=this["kid"+i];if(kid!=undefined && kid.alive && kid.health>0){
  if(kid.x<limit_topleft.x+30 || kid.x>limit_bottomright.x-30){f_SetXY(kid,Math.min(limit_bottomright.x-70,mcPlayerX+180+n*55),Math.max(limit_topleft.y+25,Math.min(limit_bottomright.y-25,kid.y)));n++;}
 }}
 mcTravel++;mcViewFrame++;return true;
}
flash.external.ExternalInterface.addCallback("mcNextStage",this,mcNextStage);
