import numpy as np,colorsys,sys
from PIL import Image
from skimage.color import rgb2lab,deltaE_ciede2000
T=np.array([233,196,106.])
def lum(c):
    c=np.asarray(c,float)/255; c=np.where(c<=0.04045,c/12.92,((c+0.055)/1.055)**2.4); return c@[0.2126,0.7152,0.0722]
def cr(a,b): a,b=lum(a),lum(b); return (max(a,b)+0.05)/(min(a,b)+0.05)
def de(a,b): return deltaE_ciede2000(rgb2lab(np.atleast_2d(a)[None]/255)[0],rgb2lab(np.broadcast_to(b,np.atleast_2d(a).shape)[None]/255)[0])
SB=(0,540,300,900); DB=(20,640,210,870)   # locked camera: same box for main and PR
Ys={}
for tag in ['main','pr']:
  for s in ['day','night']:
    try: im=np.asarray(Image.open(f'{tag}_{s}.png').convert('RGB')).astype(float)
    except: continue
    W=im[:, :1180]; m=np.ones(W.shape[:2],bool); m[:130,:330]=False
    Y=(W[...,0]*.2126+W[...,1]*.7152+W[...,2]*.0722)[m].mean(); Ys[(tag,s)]=Y
    if s!='night': print(f"{tag} {s} Y'={Y:.1f}"); continue
    gm=m.copy(); gm[SB[1]:SB[3],SB[0]:SB[2]]=False
    ground=np.median(W[gm],0)
    S=im[SB[1]:SB[3],SB[0]:SB[2]].reshape(-1,3); sm=np.median(S,0)
    D=im[DB[1]:DB[3],DB[0]:DB[2]].reshape(-1,3)
    dig_m=de(D,sm)>12; dig=np.median(D[dig_m],0)
    st=S[de(S,sm)<=12]; L=lum(st); lit=np.median(st[L>=np.percentile(L,75)],0)
    h,sa,v=colorsys.rgb_to_hsv(*(dig/255))
    print(f"{tag} night Y'={Y:.1f} ground={ground.astype(int)} B-R={ground[2]-ground[0]:+.0f} stone={sm.astype(int)} stoneLit={lit.astype(int)} R-B={lit[0]-lit[2]:+.0f} stone:ground={cr(sm,ground):.2f} digit={dig.astype(int)} dE={float(de(dig,T)[0]):.1f} hue={h*360:.0f} sat={sa:.2f} digit:stone={cr(dig,sm):.2f}")
for t in ['main','pr']:
  if (t,'day') in Ys and (t,'night') in Ys: print(t,'night/day',f"{Ys[(t,'night')]/Ys[(t,'day')]*100:.0f}%")
