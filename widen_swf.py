"""Set the actual SWF stage rectangle, preserving original tags and assets."""
from pathlib import Path
import struct,zlib
p=Path(__file__).parent/'web'/'bridge.swf'
raw=p.read_bytes();body=zlib.decompress(raw[8:]) if raw[:3]==b'CWS' else raw[8:]
bits=''.join(f'{b:08b}' for b in body);old_n=int(bits[:5],2);old_size=(5+4*old_n+7)//8
values=(0,960*20,0,540*20);n=max(v.bit_length()+1 for v in values)
rect=f'{n:05b}'+''.join(f'{v:0{n}b}' for v in values);rect+='0'*((-len(rect))%8)
body=int(rect,2).to_bytes(len(rect)//8,'big')+body[old_size:]
p.write_bytes(b'FWS'+bytes([8])+struct.pack('<I',len(body)+8)+body)
print('Native SWF stage: 960 x 540')
