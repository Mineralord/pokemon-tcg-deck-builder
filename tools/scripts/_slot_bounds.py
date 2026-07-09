import cv2, numpy as np
mat = cv2.imread("../feature/game/src/main/res/drawable-nodpi/board_mat.png")
H, W = mat.shape[:2]
# Detect the blue border outline of the right deck/discard slot.
# Blue border ~ (light blue). Search region x 0.85..1.0, y 0.58..0.82
x0,x1 = int(W*0.83), W
y0,y1 = int(H*0.57), int(H*0.83)
reg = mat[y0:y1, x0:x1]
b,g,r = reg[:,:,0].astype(int), reg[:,:,1].astype(int), reg[:,:,2].astype(int)
blue = ((b>110)&(b-r>30)&(g>60)&(b>g)).astype(np.uint8)*255
blue = cv2.morphologyEx(blue, cv2.MORPH_CLOSE, np.ones((5,5),np.uint8))
ys,xs = np.where(blue>0)
if len(xs):
    print(f"slot border bbox norm: x={(x0+xs.min())/W:.3f} y={(y0+ys.min())/H:.3f} xr={(x0+xs.max())/W:.3f} yb={(y0+ys.max())/H:.3f}")
    print(f"  -> w={(xs.max()-xs.min())/W:.3f} h={(ys.max()-ys.min())/H:.3f}")
# also visualize
vis = reg.copy(); vis[blue>0]=(0,0,255)
cv2.imwrite("out/slot_vis.png", cv2.resize(vis,None,fx=2.2,fy=2.2,interpolation=cv2.INTER_NEAREST))
print("saved out/slot_vis.png")
