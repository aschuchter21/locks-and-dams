"""Offline geometry audit. Does not launch Minecraft or load any world."""
import math
from pathlib import Path
count=0
for width in range(4,35):
 a,b=width//2,width-width//2
 for upstream in [(0,-1),(1,0),(0,1),(-1,0)]:
  right=(-upstream[1],upstream[0]);tips=[]
  for leaf,sign in [(a,1),(b,-1)]:
   u=(right[0]*sign,right[1]*sign);span=leaf-.5;length=math.hypot(span,1)
   rotated=(u[1],-u[0]);angle=math.copysign(math.atan2(1,span),rotated[0]*upstream[0]+rotated[1]*upstream[1]);c,s=math.cos(angle),math.sin(angle)
   v=(u[0]*c+u[1]*s,u[1]*c-u[0]*s)
   base=(0,0) if sign==1 else (right[0]*(width-1),right[1]*(width-1))
   tip=(base[0]+v[0]*length,base[1]+v[1]*length);tips.append(tip)
   assert abs(tip[0]*upstream[0]+tip[1]*upstream[1]-1)<1e-9
   for step in range(101):
    travel=abs(angle)+(math.pi/2-abs(angle))*step/100
    # Leaves retreat from the meeting plane as they open; no crossing through one another.
    assert length*math.cos(travel)<=span+1e-9
   assert 0<(.5*length+.48)/span+.02<length
   count+=1
  assert math.dist(*tips)<1e-9,(width,upstream,tips)
print(f'PASS: {count} leaf configurations; widths 4-34; all four headings; tips meet upstream; opening clearance; catwalk bounds.')
