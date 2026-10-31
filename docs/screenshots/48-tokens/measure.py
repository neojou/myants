import sys,numpy as np
from PIL import Image
def L(c):
  c=np.asarray(c)/255.;c=np.where(c<=.04045,c/12.92,((c+.055)/1.055)**2.4);return c@[.2126,.7152,.0722]
def cr(a,b):a,b=max(a,b),min(a,b);return (a+.05)/(b+.05)
for f in sys.argv[1:]:
  im=np.asarray(Image.open(f).convert('RGB')).astype(float)
  a=im[420:470,505:600].reshape(-1,3);la=L(a)
  ant=np.median(a[la<=np.percentile(la,10)],0);gnd=np.median(a[la>=np.percentile(la,60)],0)
  b=im[395:480,590:680].reshape(-1,3);red=(b[:,0]>1.8*b[:,1])&(b[:,0]>120)
  sh=np.median(b[red],0);nr=b[~red];ln=L(nr);sp=np.median(nr[ln<=np.percentile(ln,10)],0)
  print(f,'ant',ant.astype(int),'gnd',gnd.astype(int),'D1=%.2f'%cr(L(ant),L(gnd)),'shell',sh.astype(int),'spot',sp.astype(int),'D2=%.2f'%cr(L(sh),L(sp)))
