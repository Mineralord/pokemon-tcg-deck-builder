import cv2, numpy as np
mine = cv2.imread("out/deckcol_now.png")
H, W = mine.shape[:2]
# tight crop mine deck+discard: x 0.64..1.0, y 0.55..0.75
c = mine[int(H*0.55):int(H*0.75), int(W*0.64):W]
c = cv2.resize(c, None, fx=2.6, fy=2.6, interpolation=cv2.INTER_NEAREST)
cv2.imwrite("out/mine_deck_zoom.png", c)
# detect the grey 3D edge (light grey) in bottom-right region to locate deck footprint
band = mine[int(H*0.55):int(H*0.75), int(W*0.60):W]
b,g,r = band[:,:,0].astype(int), band[:,:,1].astype(int), band[:,:,2].astype(int)
grey = ((abs(r-g)<18)&(abs(g-b)<18)&(r>150)&(r<235)).astype(np.uint8)*255
grey = cv2.morphologyEx(grey, cv2.MORPH_CLOSE, np.ones((9,9),np.uint8))
n,lbl,stats,cent = cv2.connectedComponentsWithStats(grey)
ox, oy = int(W*0.60), int(H*0.55)
print("grey blobs (deck edge candidates):")
for i in range(1,n):
    x,y,w,h,a = stats[i]
    if a<1500: continue
    print(f"  norm x={(ox+x)/W:.3f} y={(oy+y)/H:.3f} w={w/W:.3f} h={h/H:.3f} xr={(ox+x+w)/W:.3f} area={a}")
print("saved out/mine_deck_zoom.png", c.shape)
