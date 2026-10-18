from pathlib import Path
r=Path(__file__).parent
p=r/'patched-scripts';p.mkdir(exist_ok=True)
src=r/'decompiled/scripts/DefineSprite_1745/frame_1/DoAction.as'
s=src.read_text(encoding='utf-8-sig')
# SWF6 ignored identifier case; SWF8 ExternalInterface does not.
s=s.replace('f_SpawnSunchips','f_SpawnSunChips').replace('s_SunChipsMusic','s_SunchipsMusic')
s=s.replace('screen_width = 640;','screen_width = 960;').replace('screen_height = 480;','screen_height = 540;')
# Viewport expansion must not move the original first encounter's boundary.
s=s.replace('limit_bottomright.x = screen_width - game_x + 1800;','limit_bottomright.x = 640 - game_x + 1800;')
bridge=r/'brawler.as'
# Share the original checkpoint transition between normal kills and F7 skips.
trigger=s.index('if(kills >= kills_goal)')
opening=s.index('{',trigger);depth=1;closing=opening+1
while depth:
    if s[closing]=='{':depth+=1
    elif s[closing]=='}':depth-=1
    closing+=1
transition=s[opening+1:closing-1]
s=s[:opening+1]+'\n      mcAdvanceCheckpoint();\n   '+s[closing-1:]
s+='\nfunction mcAdvanceCheckpoint(){'+transition+'\n}\n'
s=s.replace('function main()\n{','function main()\n{\n   mcFrame();',1)
# The original fighter remains a sensor/AI target, never a second movement authority.
s=s.replace('function f_Walk(zone)\n{','function f_Walk(zone)\n{\n   if(mcEnabled && zone==dude1)return;',1)
s=s.replace('function f_MoveChar(zone, vert, speed)\n{','function f_MoveChar(zone, vert, speed)\n{\n   if(mcEnabled && zone==dude1)return;',1)
s=s.replace('function f_CallJuggle1(u_kid)\n{','function f_CallJuggle1(u_kid)\n{\n   if(mcEnabled && u_kid==dude1)return;',1)
# SWF6 treated an absent numeric property as zero. ExternalInterface needs SWF8;
# preserve the original gravity expression's default rather than rewriting its AI.
s=s.replace('_loc1_.speed_toss_y += gravity + _loc1_.gravity;','_loc1_.speed_toss_y += gravity + (_loc1_.gravity==undefined?0:_loc1_.gravity);')
s=s.replace('   _loc1_.health -= damage_pow;','''   if(mcEnabled && _loc1_==dude1) {
      mcQueueHit(damage_pow);return;
   }
   _loc1_.health -= damage_pow;''',1)
# Report the direction of original enemy contact for native shield checks.
s=s.replace('function f_PunchHit(zone)\n{','function f_PunchHit(zone)\n{\n   mcAttackerX=zone.x;mcAttackerY=zone.y;',1)
# New block collision participates in the original enemies' locomotion.
s=s.replace('function f_HitWall(zone, vert, speed)\n{','''function f_HitWall(zone, vert, speed)
{
   if(mcEnabled && zone!=dude1) {
      var bx=zone.x+(vert?0:speed);var by=zone.y+(vert?speed:0);
      for(var bk in mcBlocks) {
         var bb=mcBlocks[bk];
         if(Math.abs(bx-bb.x)<23 && Math.abs(by-bb.y)<19)return true;
      }
   }''',1)
out=p/'DefineSprite_1745/frame_1/DoAction.as';out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(s+'\n'+bridge.read_text(encoding='utf-8'),encoding='utf-8')
root=p/'frame_1/DoAction.as';root.parent.mkdir(parents=True,exist_ok=True)
root.write_text((r/'decompiled/scripts/frame_1/DoAction.as').read_text(encoding='utf-8-sig')+'''
function mcStart(){if(_root._framesloaded<3)return false;_root.gotoAndStop(3);return true;}
flash.external.ExternalInterface.addCallback("mcStart",this,mcStart);
''',encoding='utf-8')
print('Patched original AS2 engine')
music_src=r/'decompiled/scripts/DefineSprite_1862/frame_71/DoAction.as'
music_dst=p/'DefineSprite_1862/frame_71/DoAction.as'
music_dst.parent.mkdir(parents=True,exist_ok=True)
music_dst.write_text(music_src.read_text(encoding='utf-8-sig').replace('s_SunChipsMusic','s_SunchipsMusic'),encoding='utf-8')
