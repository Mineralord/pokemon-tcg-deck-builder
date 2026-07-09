import cv2, numpy as np
mine = cv2.imread("out/deckcol_now.png")
ref  = cv2.imread("../referencias_live/combate1/ref_0040.png")
def crop(img):
    H, W = img.shape[:2]
    return img[int(H*0.55):int(H*0.75), int(W*0.60):W]
a = crop(mine); b = crop(ref)
h = max(a.shape[0], b.shape[0])
def pad(x):
    return cv2.copyMakeBorder(x, 0, h - x.shape[0], 0, 0, cv2.BORDER_CONSTANT, value=(0, 0, 0))
sep = np.full((h, 8, 3), 255, np.uint8)
sbs = cv2.hconcat([pad(a), sep, pad(b)])
sbs = cv2.resize(sbs, None, fx=1.7, fy=1.7, interpolation=cv2.INTER_NEAREST)
cv2.imwrite("out/deckdiscard_sbs.png", sbs)
print("saved", sbs.shape, "(left=MINE, right=REF)")
