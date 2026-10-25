import numpy as np,colorsys
from PIL import Image
from skimage.color import rgb2lab,deltaE_ciede2000
T=np.array([233,196,106.])
def lum(c):
    c=np.asarray(c,float)/255; c=np.where(c<=0.04045,c/12.92,((c+0.055)/1.055)**2.4); return c@[0.2126,0.7152,0.0722]
def cr(a,b): a,b=lum(a),lum(b); return (max(a,b)+0.05)/(min(a,b)+0.05)
def de(a,b): return float(deltaE_ciede2000(rgb2lab(np.array([[a]])/255),rgb2lab(np.array([[b]])/255))[0,0])
# stone bbox, digit band (x0,y0,x1,y1)
B={('main','day'):((720,150,1180,500),(740,210,1180,330)),('main','night'):((160,250,760,780),(160,360,720,520)),
   ('pr','day'):((500,0,1100,400),(520,60,1080,180)),('pr','night'):((0,760,560,900),(0,840,560,900))}
rows=[]
for (tag,s),(sb,db) in B.items():
    im=np.asarray(Image.open(f'{tag}_{s}.png').convert('RGB')).astype(float)
    W=im[:, :1180].copy(); m=np.ones(W.shape[:2],bool); m[:130,:330]=False
    Y=(W[...,0]*.2126+W[...,1]*.7152+W[...,2]*.0722)[m].mean()
    gm=np.ones_like(m); gm[sb[1]:sb[3],sb[0]:sb[2]]=False; gm&=m
    ground=np.median(W[gm],0)
    S=im[sb[1]:sb[3],sb[0]:sb[2]].reshape(-1,3); D=im[db[1]:db[3],db[0]:db[2]].reshape(-1,3)
    mx=D.max(1);mn=D.min(1);sat=(mx-mn)/np.maximum(mx,1)
    if tag=='pr': dm=(sat>0.3)&(D[:,0]>D[:,2]+40)&(mx>120)
    else: dm=(mn>np.percentile(mn,85))&(sat<0.12)
    dig=np.median(D[dm],0)
    smx=S.max(1);smn=S.min(1);ss=(smx-smn)/np.maximum(smx,1)
    if tag=='pr': st=(ss<0.2)
    else: st=(S[:,2]>S[:,0]+10)
    st&=np.array([de(p,dig)>15 for p in S[::1][:0]]).all() if False else st
    SS=S[st]; L=lum(SS); lit=np.median(SS[L>=np.percentile(L,75)],0); dark=np.median(SS[L<=np.percentile(L,25)],0); sm=np.median(SS,0)
    h,sa,v=colorsys.rgb_to_hsv(*(dig/255))
    print(f"{tag:4} {s:5} Y'={Y:5.1f} ground={ground.astype(int)} stoneLit={lit.astype(int)} R-B={lit[0]-lit[2]:+.0f} stoneDark={dark.astype(int)} R-B={dark[0]-dark[2]:+.0f} digit={dig.astype(int)} dE={de(dig,T):.1f} hue={h*360:.0f} sat={sa:.2f} digit:stone={cr(dig,sm):.2f} stone:ground={cr(sm,ground):.2f}")
