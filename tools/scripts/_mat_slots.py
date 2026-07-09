import cv2, numpy as np
mat = cv2.imread("../feature/game/src/main/res/drawable-nodpi/board_mat.png")
H, W = mat.shape[:2]
print("mat size", W, H)
# crop bottom-right quadrant where deck/discard baked slots live
c = mat[int(H*0.55):int(H*0.82), int(W*0.66):W]
c2 = cv2.resize(c, None, fx=2.4, fy=2.4, interpolation=cv2.INTER_NEAREST)
cv2.imwrite("out/mat_br.png", c2)
# find bright (white-ish) baked number glyphs in bottom-right
band = mat[int(H*0.50):int(H*0.85), int(W*0.66):W]
grey = cv2.cvtColor(band, cv2.COLOR_BGR2GRAY)
_, th = cv2.threshold(grey, 200, 255, cv2.THRESH_BINARY)
n,lbl,stats,cent = cv2.connectedComponentsWithStats(th)
ox, oy = int(W*0.66), int(H*0.50)
print("bright glyphs (baked numbers) bottom-right:")
for i in range(1,n):
    x,y,w,h,a = stats[i]
    if a<40 or a>4000: continue
    print(f"  norm cx={(ox+cent[i][0])/W:.3f} cy={(oy+cent[i][1])/H:.3f} x={(ox+x)/W:.3f} y={(oy+y)/H:.3f} w={w/W:.3f} h={h/H:.3f} area={a}")
print("saved out/mat_br.png", c2.shape)
