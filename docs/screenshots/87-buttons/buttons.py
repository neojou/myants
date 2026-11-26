# 用法：python3 buttons.py IMG.png [--label NAME]
# 找工具列裡的按鈕（以填色比對）並量：文字對比、邊框對面板、邊框對自己的底；以及工具列外框 bbox。
import sys, numpy as np
from PIL import Image
from scipy import ndimage

def lin(c):
    c=np.asarray(c,float)/255.0
    return np.where(c<=0.03928,c/12.92,((c+0.055)/1.055)**2.4)
def lum(c): l=lin(c); return 0.2126*l[...,0]+0.7152*l[...,1]+0.0722*l[...,2]
def contrast(a,b):
    la,lb=lum(a),lum(b); hi,lo=max(la,lb),min(la,lb); return (hi+0.05)/(lo+0.05)

FILLS={'rest':(42,39,48),'selected':(103,80,164),'danger':(96,20,16)}
def toolbar_bbox(im):
    reg=im[0:260,1150:1600].astype(int)
    luma=(0.2126*reg[...,0]+0.7152*reg[...,1]+0.0722*reg[...,2])
    m=luma<75
    lab,n=ndimage.label(m)
    if n==0: return None
    sizes=ndimage.sum(m,lab,range(1,n+1)); k=1+int(np.argmax(sizes))
    ys,xs=np.where(lab==k); return (xs.min()+1150,ys.min(),xs.max()+1150,ys.max())

def find_buttons(im,name,tol=2):
    fill=np.array(FILLS[name]); reg=im[0:260,1150:1600].astype(int)
    m=(np.abs(reg-fill).max(axis=2)<=tol)
    m=ndimage.binary_opening(m,iterations=1)
    lab,n=ndimage.label(m)
    out=[]
    for i,sl in enumerate(ndimage.find_objects(lab),1):
        h=sl[0].stop-sl[0].start; w=sl[1].stop-sl[1].start
        if h>=14 and w>=24 and ndimage.sum(m[sl]==1)>0.4*h*w:
            out.append((sl[1].start+1150,sl[0].start,sl[1].stop+1150-1,sl[0].stop-1))
    return sorted(out,key=lambda b:(b[1]//10,b[0]))

def measure(im,box,name):
    x0,y0,x1,y1=box; fill=np.array(FILLS[name],float)
    patch=im[y0:y1+1,x0:x1+1].astype(float)
    L=lum(patch); thr=L.min()+0.8*(L.max()-L.min())
    txt=patch[L>=thr]
    tcol=np.median(txt,axis=0)
    peak=patch.reshape(-1,3)[np.argmax(L.reshape(-1))]
    cx=(x0+x1)//2
    border_px=im[y0-1,cx].astype(float); outside_px=im[y0-3,cx].astype(float)
    return dict(box=box,text_median=tcol.round(0).tolist(),text_px=int(len(txt)),
        text_contrast=round(float(contrast(tcol,fill)),2),text_peak=peak.tolist(),text_peak_contrast=round(float(contrast(peak,fill)),2),
        border_px=border_px.tolist(),panel_px=outside_px.tolist(),
        border_vs_panel=round(float(contrast(border_px,outside_px)),2),
        border_vs_fill=round(float(contrast(border_px,fill)),2))

if __name__=='__main__':
    im=np.array(Image.open(sys.argv[1]).convert('RGB'))
    print('toolbar bbox (x0,y0,x1,y1):',toolbar_bbox(im))
    for name in FILLS:
        for b in find_buttons(im,name):
            print(name,measure(im,b,name))
