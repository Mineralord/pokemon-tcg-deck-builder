import cv2, numpy as np
ref = cv2.imread("../referencias_live/combate1/ref_0040.png")
H, W = ref.shape[:2]
# tight crop around player deck "46": x 0.85..1.0, y 0.60..0.73
c = ref[int(H*0.595):int(H*0.735), int(W*0.84):W]
c = cv2.resize(c, None, fx=3.0, fy=3.0, interpolation=cv2.INTER_NEAREST)
cv2.imwrite("out/ref_deck_zoom.png", c)
# measure red card precisely in that band
band = ref[int(H*0.59):int(H*0.74), int(W*0.84):W]
b,g,r = band[:,:,0].astype(int), band[:,:,1].astype(int), band[:,:,2].astype(int)
red = ((r>100)&(r-g>40)&(r-b>40)).astype(np.uint8)*255
ys, xs = np.where(red>0)
if len(xs):
    x0,x1,y0,y1 = xs.min(),xs.max(),ys.min(),ys.max()
    bw, bh = (x1-x0+1), (y1-y0+1)
    print(f"red card px in crop: w={bw} h={bh} aspect_w/h={bw/bh:.2f}")
    # convert to normalized full-image
    fx0 = (int(W*0.84)+x0)/W; fy0 = (int(H*0.59)+y0)/H
    print(f"norm x={fx0:.3f} y={fy0:.3f} w={bw/W:.3f} h={bh/H:.3f}")
    print(f"px on 1080x2400: w={bw} h={bh}")
print("saved out/ref_deck_zoom.png", c.shape)
