import numpy as np,colorsys,sys
from PIL import Image
from skimage.color import rgb2lab,deltaE_ciede2000
def de(a,b): return float(deltaE_ciede2000(rgb2lab(np.array([[a]])/255),rgb2lab(np.array([[b]])/255))[0,0])
for tag in sys.argv[1:]:
  im=np.asarray(Image.open(tag).convert('RGB')).astype(float)[130:, :1180]
  p=im.reshape(-1,3)/255
  mx=p.max(1);mn=p.min(1);d=mx-mn;s=np.where(mx>0,d/np.maximum(mx,1e-6),0)
  r,g,b=p.T
  h=np.where(d==0,0,np.where(mx==r,(60*((g-b)/np.maximum(d,1e-6)))%360,np.where(mx==g,60*((b-r)/np.maximum(d,1e-6))+120,60*((r-g)/np.maximum(d,1e-6))+240)))
  cls={'rose':((h>=330)|(h<10))&(s>0.45)&(mx>0.35),'gold':(h>=35)&(h<60)&(s>0.6)&(mx>0.55),'lilac':(h>=215)&(h<250)&(s>0.25)&(mx>0.35),'leaf':(h>=110)&(h<165)&(s>0.3)}
  ground=np.median(p[(h>=15)&(h<60)&(s<0.45)&(mx<0.75)],0)*255
  med={k:np.median(p[m],0)*255 for k,m in cls.items()}
  print(tag,{k:(int(m.sum()),med[k].astype(int).tolist()) for k,m in cls.items()},'ground',ground.astype(int).tolist(),'B/R %.2f'%(ground[2]/ground[0]))
  for a,b in [('rose','gold'),('rose','lilac'),('gold','lilac'),('rose','leaf'),('gold','leaf'),('lilac','leaf')]:
    print('  %s-%s %.1f'%(a,b,de(med[a],med[b])))
