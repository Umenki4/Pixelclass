"""Generates every texture (PNG) and sound (OGG) of Pixel Classes. 100% procedural = original, no copyright issues.
Run:  python3 tools/generate_assets.py     (needs: pillow numpy soundfile)"""
import json, math, os
import numpy as np, soundfile as sf
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "pixelclasses")
GUI, SKILL, CLASS, SND = (os.path.join(ASSETS, p) for p in ("textures/gui", "textures/skill", "textures/class", "sounds"))
for d in (GUI, SKILL, CLASS, SND): os.makedirs(d, exist_ok=True)

# ============================================================ textures
def img(w=16, h=16): return Image.new("RGBA", (w, h), (0, 0, 0, 0))
def rgb(h): h = h.lstrip("#"); return tuple(int(h[i:i+2], 16) for i in (0, 2, 4)) + (255,)
def outline(im, col=(20, 14, 28, 255)):
    px = im.load(); w, h = im.size; add = []
    for y in range(h):
        for x in range(w):
            if px[x, y][3] == 0:
                for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
                    nx, ny = x+dx, y+dy
                    if 0 <= nx < w and 0 <= ny < h and px[nx, ny][3] > 0: add.append((x, y)); break
    for p in add: px[p] = col
    return im
def corners(im, n=1):
    w, h = im.size
    for (x, y) in ((0,0),(w-1,0),(0,h-1),(w-1,h-1)): im.putpixel((x, y), (0,0,0,0))

def panel():
    im = img(32, 32); d = ImageDraw.Draw(im)
    d.rectangle([0,0,31,31], fill=(10,8,18,255)); d.rectangle([1,1,30,30], fill=(226,182,74,255))
    d.rectangle([2,2,29,29], fill=(122,84,34,255)); d.rectangle([3,3,28,28], fill=(84,70,140,255))
    d.rectangle([4,4,27,27], fill=(16,14,34,255)); d.rectangle([5,5,26,26], fill=(28,24,52,255))
    for (cx, cy) in ((5,5),(26,5),(5,26),(26,26)):
        for dx, dy in ((0,0),(1,0),(-1,0),(0,1),(0,-1)): im.putpixel((cx+dx, cy+dy), (255,226,140,255))
    corners(im); return im
def row(hover=False):
    im = img(); d = ImageDraw.Draw(im)
    d.rectangle([0,0,15,15], fill=(236,196,96,255) if hover else (10,8,20,255))
    d.rectangle([1,1,14,14], fill=(20,18,44,255))
    d.line([(1,1),(14,1)], fill=(110,96,180,255)); d.line([(1,1),(1,14)], fill=(110,96,180,255))
    d.rectangle([2,2,13,13], fill=(62,54,112,255) if hover else (40,34,78,255))
    corners(im); return im
def banner():
    im = img(128, 24); d = ImageDraw.Draw(im)
    gold, dg, red, dr, lr = (236,196,96,255), (130,90,36,255), (96,52,170,255), (60,30,110,255), (150,110,230,255)
    d.polygon([(0,8),(14,8),(14,22),(0,22),(6,15)], fill=dr, outline=dg)
    d.polygon([(127,8),(113,8),(113,22),(127,22),(121,15)], fill=dr, outline=dg)
    d.rectangle([10,2,117,18], fill=gold); d.rectangle([11,3,116,17], fill=dg); d.rectangle([12,4,115,16], fill=red)
    d.line([(12,5),(115,5)], fill=lr); d.line([(12,16),(115,16)], fill=dr)
    for x in (16, 111): d.rectangle([x-1,9,x+1,11], fill=(120,220,255,255)); im.putpixel((x,10),(255,255,255,255))
    return outline(im, (14,10,18,255))
def slot():
    im = img(24, 24); d = ImageDraw.Draw(im)
    d.rectangle([0,0,23,23], fill=(10,8,18,255)); d.rectangle([1,1,22,22], fill=(226,182,74,255))
    d.rectangle([2,2,21,21], fill=(122,84,34,255)); d.rectangle([3,3,20,20], fill=(22,20,46,255))
    d.line([(3,3),(20,3)], fill=(60,54,110,255)); d.line([(3,3),(3,20)], fill=(60,54,110,255))
    corners(im); return im

P = dict(o=(255,140,30,255), y=(255,220,70,255), r=(230,60,40,255), w=(255,255,255,255), c=(130,220,255,255),
         b=(70,130,240,255), p=(160,100,255,255), dp=(100,60,190,255), g=(80,210,90,255), dg=(40,130,60,255),
         br=(150,100,56,255), gr=(150,156,170,255), dgr=(90,96,112,255), k=(30,24,40,255), sv=(214,222,236,255), gd=(236,190,70,255))

def ic_fireball():
    im = img(); d = ImageDraw.Draw(im)
    d.polygon([(1,1),(7,4),(4,7)], fill=P['r']); d.polygon([(3,3),(7,5),(5,7)], fill=P['o'])
    d.ellipse([4,4,14,14], fill=P['o']); d.ellipse([6,6,13,13], fill=P['y']); d.ellipse([8,8,12,12], fill=P['w'])
    return outline(im)
def ic_frost():
    im = img(); d = ImageDraw.Draw(im); c = P['c']
    d.line([(8,1),(8,14)], fill=c); d.line([(1,8),(14,8)], fill=c); d.line([(3,3),(13,13)], fill=c); d.line([(13,3),(3,13)], fill=c)
    for p in ((8,3),(8,13),(3,8),(13,8)): d.point(p, fill=P['w'])
    d.rectangle([7,7,9,9], fill=P['w']); d.point((6,4), fill=c); d.point((10,4), fill=c); d.point((6,12), fill=c); d.point((10,12), fill=c)
    return outline(im)
def ic_beam():
    im = img(); d = ImageDraw.Draw(im)
    d.line([(2,13),(13,2)], fill=P['dp'], width=5); d.line([(2,13),(13,2)], fill=P['p'], width=3); d.line([(2,13),(13,2)], fill=P['w'], width=1)
    for p in ((13,4),(3,5),(11,11)): d.point(p, fill=P['c'])
    return outline(im)
def ic_meteor():
    im = img(); d = ImageDraw.Draw(im)
    d.line([(1,1),(8,8)], fill=P['r'], width=4); d.line([(1,2),(8,9)], fill=P['o'], width=2); d.line([(2,1),(8,7)], fill=P['y'], width=1)
    d.ellipse([6,6,14,14], fill=P['dgr']); d.ellipse([7,7,12,12], fill=P['gr']); d.point((9,9), fill=P['sv'])
    return outline(im)
def ic_blink():
    im = img(); d = ImageDraw.Draw(im)
    d.line([(2,3),(7,8),(2,13)], fill=P['dp'], width=3); d.line([(7,3),(12,8),(7,13)], fill=P['p'], width=3)
    d.line([(7,3),(12,8),(7,13)], fill=P['c'], width=1); d.point((14,2), fill=P['w']); d.point((14,14), fill=P['w'])
    return outline(im)
def ic_barrier():
    im = img(); d = ImageDraw.Draw(im)
    d.ellipse([1,1,14,14], fill=P['dp']); d.ellipse([3,3,12,12], fill=(40,30,90,255)); d.ellipse([3,3,12,12], outline=P['c'])
    d.polygon([(8,4),(9,7),(12,8),(9,9),(8,12),(7,9),(4,8),(7,7)], fill=P['w'])
    return outline(im)
def ic_rapid():
    im = img(); d = ImageDraw.Draw(im)
    for dx, dy in ((0,0),(-3,3),(3,-3)):
        d.line([(2+dx,12+dy),(10+dx,4+dy)], fill=P['br']); d.polygon([(13+dx,1+dy),(8+dx,3+dy),(11+dx,6+dy)], fill=P['sv'])
        d.point((2+dx,12+dy), fill=P['g']); d.point((3+dx,13+dy), fill=P['dg'])
    return outline(im)
def ic_pierce():
    im = img(); d = ImageDraw.Draw(im)
    d.ellipse([5,3,12,12], outline=P['g'], width=2)
    d.line([(1,8),(13,8)], fill=P['br'], width=1); d.polygon([(15,8),(11,5),(11,11)], fill=P['sv'])
    d.line([(1,6),(3,8),(1,10)], fill=P['g']); d.point((7,8), fill=P['w'])
    return outline(im)
def ic_rain():
    im = img(); d = ImageDraw.Draw(im)
    for x, y in ((3,1),(8,3),(12,1)):
        d.line([(x,y),(x,y+8)], fill=P['br']); d.polygon([(x,y+12),(x-2,y+8),(x+2,y+8)], fill=P['sv']); d.point((x,y), fill=P['g'])
    d.line([(1,14),(14,14)], fill=P['dg'])
    return outline(im)
def ic_focus():
    im = img(); d = ImageDraw.Draw(im)
    d.ellipse([2,2,13,13], outline=P['g'], width=2)
    d.line([(8,0),(8,5)], fill=P['g']); d.line([(8,10),(8,15)], fill=P['g']); d.line([(0,8),(5,8)], fill=P['g']); d.line([(10,8),(15,8)], fill=P['g'])
    d.rectangle([7,7,8,8], fill=P['r'])
    return outline(im)
def ic_poison():
    im = img(); d = ImageDraw.Draw(im)
    d.ellipse([1,8,14,14], fill=P['dg']); d.ellipse([3,9,12,13], fill=P['g'])
    for x, y, r in ((5,6,2),(9,4,2),(7,2,1),(11,7,1)): d.ellipse([x-r+1,y-r+1,x+r-1+1,y+r-1+1], fill=(170,255,120,255))
    return outline(im)
def ic_roll():
    im = img(); d = ImageDraw.Draw(im)
    d.arc([2,2,13,13], 200, 100, fill=P['g'], width=3); d.arc([2,2,13,13], 200, 100, fill=P['w'], width=1)
    d.polygon([(13,9),(9,9),(12,13)], fill=P['w']); d.point((3,12), fill=P['c'])
    return outline(im)
def ic_slam():
    im = img(); d = ImageDraw.Draw(im)
    d.polygon([(8,13),(5,8),(7,9),(8,3),(9,9),(11,8)], fill=P['o'])
    d.line([(1,14),(14,14)], fill=P['dgr'], width=2); d.line([(3,12),(1,9)], fill=P['y']); d.line([(12,12),(14,9)], fill=P['y'])
    d.point((8,6), fill=P['w']); d.rectangle([6,10,9,12], fill=P['r'])
    return outline(im)
def ic_whirl():
    im = img(); d = ImageDraw.Draw(im)
    d.arc([1,1,14,14], 30, 300, fill=P['c'], width=2); d.arc([4,4,11,11], 120, 400, fill=P['w'], width=2)
    d.polygon([(14,6),(10,5),(13,9)], fill=P['c']); d.point((8,8), fill=P['w'])
    return outline(im)
def ic_cry():
    im = img(); d = ImageDraw.Draw(im)
    d.polygon([(1,6),(7,6),(11,2),(11,13),(7,9),(1,9)], fill=P['gd']); d.rectangle([2,7,6,8], fill=P['y'])
    d.arc([10,4,15,11], 300, 60, fill=P['r'], width=1); d.arc([12,2,17,13], 300, 60, fill=P['o'], width=1)
    return outline(im)
def ic_shieldwall():
    im = img(); d = ImageDraw.Draw(im)
    d.polygon([(2,2),(13,2),(13,8),(8,14),(7,14),(2,8)], fill=P['b']); d.polygon([(4,4),(11,4),(11,8),(8,12),(7,12),(4,8)], fill=P['c'])
    d.line([(7,4),(7,11)], fill=P['w']); d.line([(8,4),(8,11)], fill=P['w']); d.line([(4,7),(11,7)], fill=P['w'])
    return outline(im)
def ic_charge():
    im = img(); d = ImageDraw.Draw(im)
    for y, l in ((4,5),(8,8),(12,5)): d.line([(1,y),(1+l,y)], fill=P['o'])
    d.polygon([(15,8),(8,2),(8,6),(5,6),(5,10),(8,10),(8,14)], fill=P['r']); d.polygon([(13,8),(9,4),(9,12)], fill=P['y'])
    return outline(im)
def ic_cleave():
    im = img(); d = ImageDraw.Draw(im)
    d.arc([-4,3,15,22], 235, 335, fill=P['sv'], width=4); d.arc([-4,3,15,22], 240, 330, fill=P['w'], width=2)
    d.line([(12,2),(14,0)], fill=P['w']); d.line([(2,5),(4,3)], fill=P['c'])
    d.line([(5,13),(11,13)], fill=P['gd'], width=2); d.line([(8,13),(8,15)], fill=P['br'])
    return outline(im)

def cls_wizard():
    im = img(); d = ImageDraw.Draw(im)
    d.polygon([(8,0),(12,10),(4,10)], fill=P['p']); d.polygon([(8,2),(10,9),(6,9)], fill=P['dp'])
    d.rectangle([1,10,14,12], fill=P['p']); d.line([(4,9),(11,9)], fill=P['gd']); d.point((8,5), fill=P['y']); d.point((9,6), fill=P['y'])
    d.rectangle([13,3,14,4], fill=P['y']); d.point((2,2), fill=P['c'])
    return outline(im)
def cls_archer():
    im = img(); d = ImageDraw.Draw(im)
    d.arc([-4,1,9,14], 280, 80, fill=P['br'], width=2); d.line([(5,2),(5,13)], fill=P['w'])
    d.line([(1,7),(14,7)], fill=P['br']); d.polygon([(15,7),(11,5),(11,9)], fill=P['sv']); d.line([(1,7),(3,5)], fill=P['g']); d.line([(1,7),(3,9)], fill=P['g'])
    return outline(im)
def cls_warrior():
    im = img(); d = ImageDraw.Draw(im)
    d.line([(12,1),(5,8)], fill=P['sv'], width=3); d.line([(11,2),(5,9)], fill=P['w'], width=1)
    d.line([(3,7),(8,12)], fill=P['gd'], width=2); d.line([(5,9),(2,13)], fill=P['br'], width=2)
    d.polygon([(9,8),(14,8),(14,11),(11,14),(9,11)], fill=P['r']); d.rectangle([11,9,12,12], fill=P['y'])
    return outline(im)

def logo():
    im = Image.new("RGBA", (128,128), (28,24,52,255)); d = ImageDraw.Draw(im)
    d.rectangle([0,0,127,127], outline=(10,8,18,255)); d.rectangle([2,2,125,125], outline=(226,182,74,255), width=2); d.rectangle([5,5,122,122], outline=(122,84,34,255))
    for f, pos in ((cls_wizard,(10,34)),(cls_archer,(44,34)),(cls_warrior,(78,34))):
        im.alpha_composite(f().resize((40,40), Image.NEAREST), pos)
    im.alpha_composite(ic_fireball().resize((24,24), Image.NEAREST), (52,78)); im.alpha_composite(ic_pierce().resize((24,24), Image.NEAREST), (20,78)); im.alpha_composite(ic_slam().resize((24,24), Image.NEAREST), (84,78))
    return im

ui = {"panel": panel(), "row": row(), "row_hover": row(True), "banner": banner(), "slot": slot()}
for n, im in ui.items(): im.save(os.path.join(GUI, n + ".png"))
logo().save(os.path.join(ASSETS, "..", "..", "logo.png"))
for n, f in (("wizard", cls_wizard), ("archer", cls_archer), ("warrior", cls_warrior)): f().save(os.path.join(CLASS, n + ".png"))
skills = dict(fireball=ic_fireball, frost_nova=ic_frost, arcane_beam=ic_beam, meteor=ic_meteor, blink=ic_blink, arcane_barrier=ic_barrier,
              rapid_shot=ic_rapid, piercing_arrow=ic_pierce, arrow_rain=ic_rain, hunters_focus=ic_focus, poison_trap=ic_poison, evasive_roll=ic_roll,
              ground_slam=ic_slam, whirlwind=ic_whirl, battle_cry=ic_cry, shield_wall=ic_shieldwall, charge=ic_charge, cleave=ic_cleave)
for n, f in skills.items(): f().save(os.path.join(SKILL, n + ".png"))

# contact sheet for a quick visual check (not shipped)
sheet = Image.new("RGBA", (6*72, 4*72), (50,50,62,255)); i = 0
for n, f in list(skills.items()) + [("c1", cls_wizard), ("c2", cls_archer), ("c3", cls_warrior)]:
    sheet.alpha_composite(f().resize((64,64), Image.NEAREST), ((i%6)*72+4, (i//6)*72+4)); i += 1
sheet.save("/tmp/pc_icons.png")

# ============================================================ sounds
SR = 44100
rng = np.random.default_rng(11)
def t(dur): return np.linspace(0, dur, int(SR*dur), endpoint=False)
def sine(f, dur): return np.sin(2*math.pi*f*t(dur))
def square(f, dur, duty=0.5): return np.where((t(dur)*f) % 1.0 < duty, 1.0, -1.0)
def tri(f, dur): return 2*np.abs(2*((t(dur)*f) % 1.0) - 1) - 1
def sweep(f0, f1, dur, wave=np.sin):
    ft = np.linspace(f0, f1, int(SR*dur)); return wave(2*math.pi*np.cumsum(ft)/SR)
def noise(dur): return rng.standard_normal(int(SR*dur))
def lp(x, k):
    k = np.broadcast_to(np.asarray(k, dtype=float), x.shape); y = np.zeros_like(x); a = 0.0
    for i in range(len(x)): a += k[i]*(x[i]-a); y[i] = a
    return y
def hp(x, k): return x - lp(x, k)
def decay(x, rate=6.0, atk=0.004):
    n = len(x); a = max(1, int(atk*SR)); e = np.exp(-np.linspace(0, rate, n)); e[:a] *= np.linspace(0, 1, a); return x*e
def bell(x, p=1.0): return x*np.sin(math.pi*np.linspace(0, 1, len(x)))**p
def cat(*xs): return np.concatenate(xs)
def silence(d): return np.zeros(int(SR*d))
def mix(*xs):
    n = max(len(x) for x in xs); out = np.zeros(n)
    for x in xs: out[:len(x)] += x
    return out
def write(name, x, peak=0.85):
    x = np.asarray(x, dtype=np.float32); x = x/max(1e-6, float(np.max(np.abs(x))))*peak; x[-250:] *= np.linspace(1, 0, 250)
    sf.write(os.path.join(SND, name + ".ogg"), x, SR, format="OGG", subtype="VORBIS")
n_ = lambda s: 440.0*2**((s-9)/12)
C5, E5, G5, B5, C6, E6, G6, C7 = (n_(x) for x in (12, 16, 19, 23, 24, 28, 31, 36))
def arp(notes, d, wave=sine, vol=0.5, rate=6): return cat(*[decay(wave(f, d)*vol, rate) for f in notes])

SOUNDS = {}
SOUNDS["ui_open"]   = cat(arp([C5,E5,G5,C6], 0.055, tri, 0.6), decay(tri(C6*2, 0.16)*0.35, 5))
SOUNDS["ui_close"]  = arp([G5,E5,C5], 0.06, tri, 0.6)
SOUNDS["ui_hover"]  = decay(square(1500, 0.03, 0.125)*0.3, 8)
SOUNDS["ui_click"]  = cat(decay(square(660,0.035)*0.5, 8), decay(square(990,0.05)*0.5, 8))
SOUNDS["ui_select"] = cat(arp([E5,G5,C6], 0.07, square, 0.35, 5), decay(sine(E6,0.3)*0.5, 5))
SOUNDS["ui_equip"]  = cat(decay(square(520,0.04)*0.5, 8), decay(sine(C6,0.25)*0.5, 6), decay(sine(G6,0.25)*0.3, 6))
SOUNDS["ui_deny"]   = cat(decay(square(150,0.09)*0.6, 4), silence(0.02), decay(square(110,0.13)*0.6, 4))
SOUNDS["cast_fire"] = mix(bell(lp(noise(0.5), np.linspace(0.02, 0.4, int(SR*0.5))), 0.8)*1.2, bell(sweep(110, 280, 0.5), 0.8)*0.5)
SOUNDS["boom"]      = mix(decay(sweep(120, 32, 0.8)*1.0, 4), decay(lp(noise(0.8), 0.06)*1.3, 4.5), decay(hp(noise(0.12), 0.3), 14)*0.5)
SOUNDS["cast_ice"]  = mix(bell(sweep(1300, 2300, 0.5)*(1+0.2*sine(14,0.5)), 0.7)*0.5, *[np.concatenate([silence(o), decay(sine(f, 0.12)*0.35, 8)]) for o, f in ((0.1,3100),(0.2,3700),(0.3,4200))])
SOUNDS["ice_break"] = mix(*[np.concatenate([silence(o), decay(hp(noise(0.06), 0.4), 9)*0.9]) for o in (0.0, 0.05, 0.11, 0.2, 0.26)], decay(sine(2600, 0.3)*0.25, 7))
SOUNDS["cast_arcane"] = mix(bell(sweep(300, 900, 0.55)*(1+0.3*sine(26,0.55)), 0.8)*0.6, bell(sweep(600, 1800, 0.55), 0.8)*0.3)
SOUNDS["zap"]       = mix(decay(sweep(1100, 200, 0.2, lambda p: np.sign(np.sin(p))), 5)*0.5, decay(hp(noise(0.2), 0.3), 6)*0.4)
SOUNDS["blink"]     = mix(decay(sweep(1600, 180, 0.22)*0.8, 5), decay(hp(noise(0.12), 0.3), 10)*0.3, cat(silence(0.12), decay(sine(900, 0.2)*0.25, 7)))
SOUNDS["arrow_shot"]= mix(decay(hp(noise(0.08), 0.25), 12)*0.8, decay(sweep(420, 170, 0.18, lambda p: 2*np.abs(2*((p/(2*math.pi)) % 1)-1)-1), 7)*0.6)
SOUNDS["arrow_hit"] = mix(decay(lp(noise(0.09), 0.2), 12)*0.9, decay(sweep(180, 80, 0.12), 9)*0.8)
SOUNDS["whoosh"]    = bell(lp(noise(0.34), np.linspace(0.03, 0.35, int(SR*0.34))), 1.0)*1.3
SOUNDS["slam"]      = mix(decay(sweep(95, 28, 0.6), 4.5)*1.0, decay(lp(noise(0.6), 0.04), 5)*1.2, decay(hp(noise(0.08), 0.3), 14)*0.6)
_horn = sum(square(f, 0.7, 0.35)*(1+0.04*sine(6, 0.7)) for f in (196.0, 294.0, 392.0))
SOUNDS["warcry"]    = bell(lp(_horn, 0.12), 0.45)*0.5
SOUNDS["shield"]    = mix(*[decay(sine(f, 0.6)*a, 5) for f, a in ((880,0.5),(1320,0.4),(1960,0.3),(2570,0.2))], decay(hp(noise(0.05), 0.4), 14)*0.5)
SOUNDS["heal_chime"]= cat(arp([C6,E6,G6,C7], 0.07, sine, 0.5, 5), decay(sine(C7, 0.35)*0.4, 5))
SOUNDS["charge"]    = mix(bell(lp(noise(0.42), np.linspace(0.02, 0.5, int(SR*0.42))), 1.3)*1.2, bell(sweep(80, 240, 0.42, lambda p: 2*np.abs(2*((p/(2*math.pi)) % 1)-1)-1), 0.8)*0.5)
SOUNDS["slash"]     = mix(decay(hp(lp(noise(0.2), np.linspace(0.9, 0.25, int(SR*0.2))), 0.05), 9)*1.2, decay(sweep(2400, 700, 0.18), 8)*0.25)
SOUNDS["buff"]      = mix(cat(arp([C5,E5,G5], 0.08, tri, 0.5, 4), decay(tri(C6, 0.35)*0.5, 4)), cat(silence(0.2), decay(sine(G6, 0.3)*0.3, 5)))
for k, v in SOUNDS.items(): write(k, v)
with open(os.path.join(ASSETS, "sounds.json"), "w", encoding="utf-8") as f:
    json.dump({k: {"sounds": [{"name": "pixelclasses:" + k, "stream": False}]} for k in SOUNDS}, f, indent=2)
print("sounds:", ", ".join(SOUNDS)); print("textures + sounds generated")
