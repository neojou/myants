# 用法：python3 tools/fix_ant_shell_normals.py IN.glb OUT.glb
# #94：只翻正「NORMAL 與 winding 全部相反且 winding 朝外」的 primitive。
# 只把「頂點法線與 winding 面法線全部反向（rawN·wind>0 = 0%）且 winding 100% 朝外」的 primitive 的 NORMAL 取負號，
# 原地改寫 BIN 中該 accessor 的 float；JSON、位置、索引、材質、其他 byte 一律不動。
import sys,os,struct,json,numpy as np
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

src,dst=sys.argv[1],sys.argv[2];raw=bytearray(open(src,'rb').read())
j,b=load(src);jl=struct.unpack('<I',raw[12:16])[0];binstart=20+jl+8;fixed=[]
for m in j['meshes']:
  for p in m['primitives']:
    P=acc(j,b,p['attributes']['POSITION'],3).astype(float);N=acc(j,b,p['attributes']['NORMAL'],3).astype(float)
    I=acc(j,b,p['indices'],1).astype(int).reshape(-1,3);c=P.mean(0);a,bb,cc=P[I[:,0]],P[I[:,1]],P[I[:,2]];fn=np.cross(bb-a,cc-a)
    agree=((fn*N[I].mean(1)).sum(1)>0).mean();wout=((fn*((a+bb+cc)/3-c)).sum(1)>0).mean()
    if agree==0.0 and wout==1.0:
      ai=p['attributes']['NORMAL'];A=j['accessors'][ai];v=j['bufferViews'][A['bufferView']]
      assert A['componentType']==5126 and v.get('byteStride',12) in (12,)
      o=binstart+v.get('byteOffset',0)+A.get('byteOffset',0);n=A['count']*3
      arr=np.frombuffer(bytes(raw[o:o+4*n]),'<f4').copy();raw[o:o+4*n]=(-arr).astype('<f4').tobytes();fixed.append(m.get('name'))
open(dst,'wb').write(raw);print(len(fixed),'fixed:',fixed)
