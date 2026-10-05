import sys,time,ctypes,os
ctypes.windll.user32.SetProcessDPIAware()
from PIL import ImageGrab, Image
start=float(sys.argv[1]); n=int(sys.argv[2]); dt=float(sys.argv[3])
time.sleep(start)
os.makedirs('gen/seq',exist_ok=True)
fr=[]
for i in range(n):
    im=ImageGrab.grab().crop((320,250,2240,1340)).resize((480,272))
    fr.append(im); time.sleep(dt)
cols=int(sys.argv[4]) if len(sys.argv)>4 else 4
rows=(n+cols-1)//cols
sheet=Image.new('RGB',(480*cols,272*rows))
for i,f in enumerate(fr): sheet.paste(f,((i%cols)*480,(i//cols)*272))
sheet.save('gen/seq/sheet.png'); print('ok')
