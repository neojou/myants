# rim.py — #84/#91 光澤與夜間輪廓量測（Artist 定案版；#91 補 N4：遮罩內最亮 15% 的 RGB 中位數）
# 用法：python3 rim.py MAIN_DAY_BASE.png [--beetle] IMG.png ...
# 遮罩：base 圖固定框內 L < 0.5×base 地面 L（蟻 x505–600,y420–470；甲蟲 x615–695,y422–462）；
#       瓢蟲殼：base 圖 x590–680,y395–480 內 R>1.8G 且 R>120。
# 地面：固定框 x700–740,y470–500 中位數（日夜同框，也用於 #74 B/R）。
# core=遮罩最暗10%中位數；rim=最亮15%中位數；mid=10–50 百分位中位數；S=rim/core（亮度比）
# hot%=遮罩內 L≥地面 的比例；edge3%=遮罩內比地面亮且對比≥3:1 的比例
import sys,numpy as np
from PIL import Image
def L(c):
  c=np.asarray(c,float)/255.;c=np.where(c<=.04045,c/12.92,((c+.055)/1.055)**2.4);return c@[.2126,.7152,.0722]
def cr(a,b):a,b=max(a,b),min(a,b);return (a+.05)/(b+.05)
def ld(f):return np.asarray(Image.open(f).convert('RGB')).astype(float)
a=sys.argv[1:];beetle='--beetle' in a;a=[x for x in a if x!='--beetle']
B=ld(a[0]);G=(slice(470,500),slice(700,740))
box=(slice(422,462),slice(615,695)) if beetle else (slice(420,470),slice(505,600))
SB=(slice(395,480),slice(590,680))
mask=L(B[box])<0.5*np.median(L(B[G]))
sb=B[SB];red=(sb[...,0]>1.8*sb[...,1])&(sb[...,0]>120)
for f in a[1:]:
  im=ld(f);g=np.median(L(im[G]));gr=np.median(im[G].reshape(-1,3),0);m=L(im[box])[mask]
  p=lambda q:np.percentile(m,q)
  core=np.median(m[m<=p(10)]);rim=np.median(m[m>=p(85)]);mid=np.median(m[(m>=p(10))&(m<=p(50))])
  hot=(m>=g).mean()*100;e3=((m>=g)&((m+.05)/(g+.05)>=3)).mean()*100
  out=f'{f}: px={mask.sum()} gndRGB={gr.astype(int)} B/R={gr[2]/gr[0]:.3f} gndL={g:.4f} core={core:.4f} mid={mid:.4f} rim={rim:.4f} | core/gnd={cr(core,g):.2f} rim/gnd={cr(rim,g):.2f} S={rim/core:.2f} hot%={hot:.1f} edge3%={e3:.1f}'
  px=im[box][mask];top=px[m>=p(85)];t=np.median(top,0)
  out+=f' | top15RGB={t.round(1)} B>=R={t[2]>=t[0]}'
  if red.sum():
    s=L(im[SB])[red];out+=f' | shell/gnd={cr(np.median(s),g):.2f} shellrim/gnd={cr(np.median(s[s>=np.percentile(s,85)]),g):.2f}'
  print(out)
