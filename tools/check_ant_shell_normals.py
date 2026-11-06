# 用法：python3 gltf_normals.py FILE.glb ...
# 照 GltfAnt.kt 的 loader 路徑：node TRS 累乘成 world，位置乘 world，法線乘 inverse(world)^T 後 normalize。
# 每個 primitive 輸出：world det、原始法線 vs 變換後法線、winding 面法線（右手、CCW）跟頂點法線一致比例、
# 法線/winding 朝外比例（相對 primitive 質心），以及上半部（y>質心）法線 y>0 的比例；doubleSided 由 material 讀。
import sys,json,struct,numpy as np
def load(f):
    b=open(f,'rb').read();jl=struct.unpack('<I',b[12:16])[0];j=json.loads(b[20:20+jl]);bin_=b[20+jl+8:];return j,bin_
def acc(j,bin_,i,comp):
    a=j['accessors'][i];v=j['bufferViews'][a['bufferView']];o=v.get('byteOffset',0)+a.get('byteOffset',0)
    dt={5126:'<f4',5123:'<u2',5125:'<u4',5121:'u1'}[a['componentType']];n=a['count']*comp
    return np.frombuffer(bin_,dtype=dt,count=n,offset=o).reshape(-1,comp) if comp>1 else np.frombuffer(bin_,dtype=dt,count=n,offset=o)
def trs(n):
    if 'matrix' in n:return np.array(n['matrix'],float).reshape(4,4).T
    t=n.get('translation',[0,0,0]);x,y,z,w=n.get('rotation',[0,0,0,1]);s=n.get('scale',[1,1,1])
    R=np.array([[1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w)],[2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w)],[2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y)]])
    M=np.eye(4);M[:3,:3]=R@np.diag(s);M[:3,3]=t;return M

# 用法：python3 tools/check_ant_shell_normals.py
# #94：失敗條件——worker_black／soldier 的 13 個甲殼 mesh 任一 rawN·wind 或 wind_out 不是 100%。
SHELL={"HeadShell","Neck","Clypeus","EyeL","EyeR","ProthoraxShell","MesothoraxShell","MetathoraxShell","PetioleShell","PostpetioleShell","GasterRear","GasterFront","GasterTip"}
ROOT=Path=None
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/"koolApp"/"assets"/"models"/"ants"
fail=0
for name in ("worker_black.glb","soldier.glb"):
  f=ROOT/name; j,bin_=load(str(f)); nodes=j["nodes"]; seen=0
  for i,n in enumerate(nodes):
    if n.get("name") not in SHELL or "mesh" not in n: continue
    for p in j["meshes"][n["mesh"]]["primitives"]:
      P=acc(j,bin_,p["attributes"]["POSITION"],3).astype(float); N=acc(j,bin_,p["attributes"]["NORMAL"],3).astype(float)
      I=acc(j,bin_,p["indices"],1).astype(int).reshape(-1,3); c=P.mean(0)
      a,b,cc=P[I[:,0]],P[I[:,1]],P[I[:,2]]; fn=np.cross(b-a,cc-a)
      agree=((fn*N[I].mean(1)).sum(1)>0).mean(); wout=((fn*((a+b+cc)/3-c)).sum(1)>0).mean()
      if agree!=1.0 or wout!=1.0:
        print(f"FAIL {name} {n['name']}: winding·N={agree:.3f} wind_out={wout:.3f}"); fail=1
      seen+=1
  if seen!=13: print(f"FAIL {name}: expected 13 shells, got {seen}"); fail=1
  else: print(f"ok {name}: 13 shells winding·N and outward 100%")
sys.exit(fail)
