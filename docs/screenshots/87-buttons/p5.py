# 用法：python3 p5.py PR.png NOPHERO.png [--box x0,y0,x1,y1]
# P5：3D 室外 home 軌跡像素（pr 對同圖 pheromones=false 的差異）的 Lab 色相與 ΔE2000。
# home-only 判定：pr−nophero 的 B 增量 > R 增量+8 且 B 增量>0（糖＝黃：R、G 增；danger＝洋紅：R、B 同增；螞蟻＝變暗）。
import sys, numpy as np
from PIL import Image
from skimage.color import rgb2lab, deltaE_ciede2000
pr=np.array(Image.open(sys.argv[1]).convert('RGB')).astype(int)
np_=np.array(Image.open(sys.argv[2]).convert('RGB')).astype(int)
x0,y0,x1,y1=0,112,1190,790
if '--box' in sys.argv: x0,y0,x1,y1=map(int,sys.argv[sys.argv.index('--box')+1].split(','))
a=pr[y0:y1,x0:x1]; b=np_[y0:y1,x0:x1]
d=a-b
home=(d[...,2]>d[...,0]+8)&(d[...,2]>0)&(d[...,1]>=0)
labA=rgb2lab(a/255.0); labB=rgb2lab(b/255.0)
de=deltaE_ciede2000(labA,labB)
hue=(np.degrees(np.arctan2(labA[...,2],labA[...,1]))+360)%360
sel=home&(de>=5)
print('pixels diff>0:',int((np.abs(d).max(axis=2)>0).sum()),' home-like with ΔE>=5:',int(sel.sum()))
if sel.sum()==0: sys.exit()
h=hue[sel]; e=de[sel]
print('Lab hue pr  : median %.1f  p10 %.1f  p90 %.1f'%(np.median(h),np.percentile(h,10),np.percentile(h,90)))
print('ΔE2000 vs nophero: median %.1f  p10 %.1f  p90 %.1f  max %.1f'%(np.median(e),np.percentile(e,10),np.percentile(e,90),e.max()))
top=sel&(de>=np.percentile(e,50))
print('upper half (ΔE>=median) hue median %.1f  ΔE median %.1f  n=%d'%(np.median(hue[top]),np.median(de[top]),top.sum()))
print('median RGB pr:',np.median(a[sel],axis=0),' nophero:',np.median(b[sel],axis=0))
