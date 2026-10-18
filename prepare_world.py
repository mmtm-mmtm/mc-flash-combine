from pathlib import Path
import struct,gzip,time,sys
r=Path(sys.argv[1]) if len(sys.argv)>1 else Path(__file__).resolve().parent;world=r/'fabric-mod/run/saves/MC Dad n Me Bridge';world.mkdir(parents=True,exist_ok=True)
# Minimal vanilla 1.20.1 level.dat, in a new isolated development save.
def string(s):
 b=s.encode();return struct.pack('>H',len(b))+b
def compound(values):return b''.join(bytes([t])+string(k)+payload(t,v) for k,(t,v) in values.items())+b'\0'
def payload(t,v):
 if t==1:return struct.pack('>b',v)
 if t==3:return struct.pack('>i',v)
 if t==4:return struct.pack('>q',v)
 if t==5:return struct.pack('>f',v)
 if t==6:return struct.pack('>d',v)
 if t==8:return string(v)
 if t==9:
  typ,items=v;return bytes([typ])+struct.pack('>i',len(items))+b''.join(payload(typ,x) for x in items)
 if t==10:return compound(v)
 raise ValueError(t)
flat={'type':(8,'minecraft:flat'),'settings':(10,{'biome':(8,'minecraft:plains'),'features':(1,0),'lakes':(1,0),'layers':(9,(10,[{'height':(3,1),'block':(8,'minecraft:air')}])), 'structure_overrides':(9,(8,[]))})}
dims={'minecraft:overworld':(10,{'type':(8,'minecraft:overworld'),'generator':(10,flat)})}
for key,preset,biome in [('minecraft:the_nether','minecraft:nether','minecraft:nether'),('minecraft:the_end','minecraft:end','minecraft:the_end')]:
 dims[key]=(10,{'type':(8,key),'generator':(10,{'type':(8,'minecraft:noise'),'settings':(8,preset),'biome_source':(10,{'type':(8,'minecraft:multi_noise' if 'nether' in key else 'minecraft:the_end'),**({'preset':(8,biome)} if 'nether' in key else {})})})})
data={'DataVersion':(3,3465),'version':(3,19133),'Version':(10,{'Id':(3,3465),'Name':(8,'1.20.1'),'Snapshot':(1,0),'Series':(8,'main')}),'LevelName':(8,'MC Dad n Me Bridge'),'GameType':(3,0),'Difficulty':(1,1),'DifficultyLocked':(1,0),'allowCommands':(1,1),'hardcore':(1,0),'initialized':(1,1),'SpawnX':(3,2),'SpawnY':(3,74),'SpawnZ':(3,0),'Time':(4,6000),'DayTime':(4,6000),'LastPlayed':(4,int(time.time()*1000)),'DataPacks':(10,{'Enabled':(9,(8,['vanilla'])),'Disabled':(9,(8,[]))}),'WorldGenSettings':(10,{'seed':(4,0),'generate_features':(1,0),'bonus_chest':(1,0),'dimensions':(10,dims)}),'GameRules':(10,{key:(8,val) for key,val in {'doMobSpawning':'false','doWeatherCycle':'false','doDaylightCycle':'false','keepInventory':'true','randomTickSpeed':'0'}.items()})}
p=world/'level.dat'
if not p.exists():p.write_bytes(gzip.compress(b'\x0a\x00\x00'+compound({'Data':(10,data)})))
run=r/'fabric-mod/run';(run/'options.txt').write_text('pauseOnLostFocus:false\nrenderDistance:4\nsimulationDistance:4\nmaxFps:30\nguiScale:2\nautoJump:false\n','utf-8')
