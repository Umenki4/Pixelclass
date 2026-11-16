"""Writes the built-in class + skill JSON files (src/main/resources/data/pixelclasses/pixelclasses/...).
The JSON files are the real source of truth - you can edit them directly (or override them with a datapack).
This script only exists to keep the long VFX lists short to write."""
import json, os
HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "..", "src", "main", "resources", "data", "pixelclasses", "pixelclasses")
EX = os.path.join(HERE, "..", "examples", "example_datapack")
for sub in ("classes", "skills"): os.makedirs(os.path.join(DATA, sub), exist_ok=True)

def snd(i, v=1.0, p=1.0): return {"id": "pixelclasses:" + i if ":" not in i else i, "volume": v, "pitch": p}
def dust(c, size=1.2, shape="point", **kw): d = {"particle": "dust", "color": c, "size": size, "shape": shape}; d.update(kw); return d
def dtr(c1, c2, size=1.4, shape="point", **kw): d = {"particle": "dust_color_transition", "color": c1, "color_to": c2, "size": size, "shape": shape}; d.update(kw); return d
def pt(name, shape="point", **kw): d = {"particle": "minecraft:" + name, "shape": shape}; d.update(kw); return d
def eff(e, sec, amp=0): return {"effect": "minecraft:" + e, "duration": sec, "amplifier": amp}
def skill(cls, en, th, den, dth, cd, mana, stat, actions):
    return {"class": "pixelclasses:" + cls, "name": {"en": en, "th": th}, "description": {"en": den, "th": dth},
            "cooldown": cd, "mana_cost": mana, "scaling": {"stat": stat, "per_point": 0.01}, "actions": actions}

S = {}
# ------------------------------------------------------------------ WIZARD (scales with INT)
S["fireball"] = skill("wizard", "Fireball", "ลูกไฟ", "Hurl a blazing fireball that explodes on impact and sets enemies on fire.",
  "ปาลูกไฟที่ระเบิดเมื่อชน เผาศัตรูในรัศมี", 3.0, 12, "INT", [
  {"type": "vfx", "at": "caster", "sound": snd("cast_fire", 0.9), "vfx": [dust("#FF7A1A", 1.3, "ring", radius=1.3, count=24), pt("flame", "spiral", radius=0.8, height=1.6, turns=2, count=16, speed=0.01)]},
  {"type": "projectile", "at": "eye", "speed": 1.4, "range": 42, "hit_radius": 0.9,
   "trail": [pt("flame", spread=0.12, count=5, speed=0.01), dust("#FFB020", 1.4, count=3, spread=0.15), pt("smoke", count=1, spread=0.1)],
   "on_end": [{"type": "area", "radius": 3.6, "damage": 9, "damage_type": "fire", "knockback": 0.9, "ignite": 4,
     "vfx": [pt("explosion", count=2, spread=0.6), pt("flame", "burst", count=48, speed=0.35), dtr("#FFD040", "#C02010", 1.6, "sphere", radius=2.4, count=70, speed=0.08),
             pt("lava", "burst", count=14, speed=0.25), dust("#FF6A00", 1.8, "ring", radius=3.4, count=48, speed=0.12)],
     "sound": snd("boom", 1.2)}]}])
S["frost_nova"] = skill("wizard", "Frost Nova", "คลื่นน้ำแข็ง", "Release an icy shockwave that damages and heavily slows everything around you.",
  "ปล่อยคลื่นน้ำแข็งรอบตัว ทำดาเมจและทำให้ศัตรูเดินช้าลงมาก", 9.0, 25, "INT", [
  {"type": "vfx", "at": "caster", "sound": snd("cast_ice", 1.0), "vfx": [pt("snowflake", "spiral", radius=1.2, height=2.2, turns=3, count=30, speed=0.01)]},
  {"type": "vfx", "at": "caster", "vfx": [pt("snowflake", "ring", radius=2, count=30, speed=0.12), dust("#9FE8FF", 1.3, "ring", radius=2, count=30)]},
  {"type": "vfx", "at": "caster", "delay": 2, "vfx": [pt("snowflake", "ring", radius=4, count=48, speed=0.14), dust("#6FC8FF", 1.4, "ring", radius=4, count=48)]},
  {"type": "vfx", "at": "caster", "delay": 4, "vfx": [pt("snowflake", "ring", radius=6, count=70, speed=0.16), dust("#FFFFFF", 1.5, "ring", radius=6, count=70), pt("end_rod", "burst", count=26, speed=0.3)]},
  {"type": "area", "delay": 3, "radius": 6.5, "damage": 6, "damage_type": "magic", "knockback": 0.7, "effects": [eff("slowness", 4.0, 2)],
   "vfx": [dtr("#FFFFFF", "#5AB4FF", 1.6, "sphere", radius=3, count=60, speed=0.1)], "sound": snd("ice_break", 1.1)}])
S["arcane_beam"] = skill("wizard", "Arcane Beam", "ลำแสงอาคม", "Channel a piercing beam of raw magic that shreds everything in a line.",
  "ร่ายลำแสงเวททะลุทะลวง ฟันทุกอย่างในแนวตรง", 7.0, 28, "INT", [
  {"type": "projectile", "at": "eye", "repeat": {"count": 8, "interval": 2}, "speed": 7, "range": 30, "hit_radius": 0.8, "pierce": 99,
   "damage": 2.6, "damage_type": "magic", "knockback": 0.1, "sound": snd("zap", 0.6),
   "vfx": [dust("#B070FF", 1.3, "line", length=30, count=60, spread=0.04), pt("end_rod", "line", length=30, count=22, spread=0.06), dust("#FFFFFF", 0.8, "line", length=30, count=24)]}])
S["meteor"] = skill("wizard", "Meteor", "อุกกาบาต", "Mark the ground, then call down a huge meteor that devastates the area.",
  "ทำเครื่องหมายบนพื้น แล้วเรียกอุกกาบาตตกลงมาทำลายล้างพื้นที่", 14.0, 45, "INT", [
  {"type": "vfx", "at": "aim", "aim_range": 32, "repeat": {"count": 4, "interval": 4}, "vfx": [dust("#FF3A20", 1.4, "ring", radius=5, count=44), dust("#FF9A30", 1.2, "ring", radius=2.4, count=24)]},
  {"type": "vfx", "at": "caster", "sound": snd("cast_arcane", 1.0), "vfx": [dust("#C090FF", 1.3, "spiral", radius=1, height=2.4, turns=3, count=30), pt("enchant", "burst", count=30, speed=0.5)]},
  {"type": "projectile", "delay": 16, "at": "aim", "aim_range": 32, "y_offset": 22, "direction": "down", "speed": 1.8, "range": 40, "hit_radius": 1.3,
   "trail": [pt("flame", spread=0.3, count=10, speed=0.02), pt("large_smoke", spread=0.3, count=3), dtr("#FFD040", "#FF2000", 2.2, count=6, spread=0.3)],
   "on_end": [{"type": "area", "radius": 5.5, "damage": 20, "damage_type": "fire", "knockback": 1.5, "ignite": 6,
     "vfx": [pt("explosion", count=6, spread=2.0), pt("flame", "burst", count=90, speed=0.55), dtr("#FFE060", "#B01800", 2.0, "sphere", radius=4, count=120, speed=0.12),
             pt("lava", "burst", count=24, speed=0.35), dust("#FF5A10", 2.0, "ring", radius=5.2, count=80, speed=0.2), pt("large_smoke", "column", height=5, count=14, radius=1.2)],
     "sound": snd("boom", 1.7)}]}])
S["blink"] = skill("wizard", "Blink", "วาร์ป", "Teleport a short distance in the direction you are looking, then gain a burst of speed.",
  "วาร์ปไปข้างหน้าในทิศที่มอง แล้วได้ความเร็วชั่วขณะ", 7.0, 18, "INT", [
  {"type": "blink", "distance": 13, "sound": snd("blink", 1.0), "arrival_sound": snd("blink", 0.8, 1.35),
   "vfx": [pt("portal", "burst", count=40, speed=0.6), dust("#B070FF", 1.4, "ring", radius=1.2, count=26), pt("end_rod", "column", height=2, count=10)],
   "arrival_vfx": [pt("portal", "burst", count=40, speed=0.6), dust("#80E8FF", 1.4, "ring", radius=1.2, count=26), pt("end_rod", "burst", count=14, speed=0.25)]},
  {"type": "area", "affect": "self", "radius": 0, "effects": [eff("speed", 2.5, 1)]}])
S["arcane_barrier"] = skill("wizard", "Arcane Barrier", "เกราะอาคม", "Wrap yourself in a barrier of magic: gain absorption and blast nearby enemies away.",
  "ห่อหุ้มตัวด้วยเกราะเวท ได้พลังป้องกันเสริม และผลักศัตรูรอบตัวออกไป", 20.0, 35, "INT", [
  {"type": "vfx", "at": "caster", "sound": snd("shield", 1.0), "vfx": [dust("#A070FF", 1.4, "spiral", radius=1.1, height=2.2, turns=3, count=44), dust("#80E0FF", 1.2, "spiral", radius=0.8, height=2.2, turns=-3, count=36), pt("end_rod", "sphere", radius=1.4, count=26, speed=0.02)]},
  {"type": "area", "affect": "self", "radius": 0, "effects": [eff("absorption", 10.0, 2), eff("resistance", 4.0, 0)]},
  {"type": "area", "delay": 2, "radius": 4.5, "damage": 3, "damage_type": "magic", "knockback": 1.4,
   "vfx": [dust("#C090FF", 1.5, "ring", radius=4.2, count=56, speed=0.1), pt("enchanted_hit", "burst", count=30, speed=0.4)]}])

# ------------------------------------------------------------------ ARCHER (scales with DEX)
S["rapid_shot"] = skill("archer", "Rapid Shot", "ยิงรัว", "Unleash a fast volley of six arrows in quick succession.",
  "ยิงลูกธนู 6 ดอกติดๆ กันอย่างรวดเร็ว", 5.0, 15, "DEX", [
  {"type": "projectile", "at": "eye", "repeat": {"count": 6, "interval": 2}, "speed": 3.4, "range": 48, "hit_radius": 0.6, "spread": 2.2,
   "damage": 4.2, "damage_type": "projectile", "knockback": 0.15, "sound": snd("arrow_shot", 0.7),
   "trail": [pt("crit", spread=0.04, count=2), dust("#7CFF6A", 0.9, count=2, spread=0.05)],
   "on_end": [{"type": "vfx", "vfx": [pt("crit", "burst", count=6, speed=0.2)]}]}])
S["piercing_arrow"] = skill("archer", "Piercing Arrow", "ธนูทะลวง", "Fire a powerful arrow that pierces through every enemy in its path.",
  "ยิงธนูทรงพลังที่ทะลุศัตรูทุกตัวในเส้นทาง", 6.0, 20, "DEX", [
  {"type": "projectile", "at": "eye", "speed": 4.5, "range": 64, "hit_radius": 0.8, "pierce": 99,
   "damage": 11, "damage_type": "projectile", "knockback": 0.4, "sound": snd("arrow_shot", 1.0, 0.85),
   "vfx": [dust("#FFFFFF", 1.2, "ring", radius=0.8, count=14, speed=0.05)],
   "trail": [pt("end_rod", spread=0.02, count=3, speed=0.0), dust("#6BFF9A", 1.3, count=3, spread=0.05)],
   "on_hit": [{"type": "vfx", "sound": snd("arrow_hit", 0.9), "vfx": [pt("crit", "burst", count=18, speed=0.35), pt("enchanted_hit", "burst", count=10, speed=0.3)]}]}])
S["arrow_rain"] = skill("archer", "Arrow Rain", "ห่าฝนธนู", "Mark an area and rain a barrage of arrows from the sky.",
  "เล็งพื้นที่แล้วกระหน่ำห่าฝนลูกธนูลงมาจากฟ้า", 12.0, 35, "DEX", [
  {"type": "vfx", "at": "aim", "aim_range": 28, "repeat": {"count": 12, "interval": 4}, "vfx": [dust("#5CFF7A", 1.3, "ring", radius=4.8, count=44), dust("#FFFFFF", 0.9, "ring", radius=2.4, count=20)]},
  {"type": "vfx", "at": "caster", "sound": snd("arrow_shot", 1.0, 0.7), "vfx": [dust("#7CFF6A", 1.2, "spiral", radius=0.9, height=2, turns=2, count=20)]},
  {"type": "projectile", "delay": 6, "at": "aim", "aim_range": 28, "repeat": {"count": 26, "interval": 2}, "scatter": 4.8, "y_offset": 18, "direction": "down",
   "speed": 2.4, "range": 26, "hit_radius": 0.7, "damage": 3.2, "damage_type": "projectile",
   "trail": [dust("#8CFF70", 1.0, count=2, spread=0.04), pt("crit", count=1, spread=0.03)],
   "on_end": [{"type": "vfx", "vfx": [pt("crit", "burst", count=5, speed=0.18), pt("poof", count=1, spread=0.2)]}]}])
S["hunters_focus"] = skill("archer", "Hunter's Focus", "สมาธิพราน", "Sharpen your senses: run faster, jump higher and reveal nearby enemies.",
  "เพ่งสมาธิ วิ่งเร็วขึ้น กระโดดสูงขึ้น และทำให้ศัตรูใกล้ๆ เรืองแสง", 18.0, 25, "DEX", [
  {"type": "vfx", "at": "caster", "sound": snd("buff", 1.0), "vfx": [dust("#6BFF8A", 1.4, "spiral", radius=1, height=2.2, turns=3, count=40), dust("#FFFFFF", 1.0, "ring", radius=2.5, count=30, speed=0.1), pt("happy_villager", "burst", count=18, speed=0.3)]},
  {"type": "area", "affect": "self", "radius": 0, "effects": [eff("speed", 10.0, 1), eff("jump_boost", 10.0, 1)]},
  {"type": "area", "affect": "enemies", "radius": 24, "damage": 0, "max_targets": 40, "effects": [eff("glowing", 8.0, 0)],
   "hit_vfx": [pt("crit", "burst", count=8, speed=0.2)]}])
S["poison_trap"] = skill("archer", "Poison Trap", "กับดักพิษ", "Lay a toxic field on the ground that poisons and slows anything inside.",
  "วางพื้นที่พิษบนพื้น ศัตรูที่เหยียบจะโดนพิษและเดินช้าลง", 14.0, 25, "DEX", [
  {"type": "vfx", "at": "caster", "sound": snd("cast_arcane", 0.7, 1.4), "vfx": [dust("#58C850", 1.2, "spiral", radius=0.8, height=1.6, turns=2, count=16)]},
  {"type": "vfx", "at": "aim", "aim_range": 22, "repeat": {"count": 6, "interval": 10}, "vfx": [dust("#4CC040", 1.3, "ring", radius=3.8, count=40), pt("witch", "disc", radius=3.6, count=14), pt("happy_villager", "disc", radius=3.4, count=8)]},
  {"type": "area", "at": "aim", "aim_range": 22, "delay": 10, "repeat": {"count": 6, "interval": 10}, "radius": 3.8, "damage": 2, "damage_type": "magic",
   "effects": [eff("poison", 3.0, 1), eff("slowness", 2.0, 1)], "hit_vfx": [pt("witch", "burst", count=8, speed=0.2)]}])
S["evasive_roll"] = skill("archer", "Evasive Roll", "กลิ้งหลบ", "Dive backwards out of danger, then snap off a quick counter-shot.",
  "พุ่งถอยหลังหนีอันตราย แล้วยิงสวนทันที", 6.0, 14, "DEX", [
  {"type": "dash", "strength": 1.5, "vertical": 0.28, "backward": True, "sound": snd("whoosh", 0.9),
   "vfx": [pt("cloud", "ring", radius=1, count=16, speed=0.1), pt("poof", "burst", count=10, speed=0.15)]},
  {"type": "area", "affect": "self", "radius": 0, "effects": [eff("speed", 3.0, 1)]},
  {"type": "projectile", "delay": 3, "at": "eye", "speed": 3.5, "range": 40, "hit_radius": 0.7, "spread": 0.6, "damage": 6, "damage_type": "projectile", "knockback": 0.3,
   "sound": snd("arrow_shot", 0.9), "trail": [pt("crit", spread=0.04, count=2), dust("#9CFF80", 1.0, count=2, spread=0.05)]}])

# ------------------------------------------------------------------ WARRIOR (scales with STR)
S["ground_slam"] = skill("warrior", "Ground Slam", "กระแทกพื้น", "Leap into the air and smash the ground, knocking back and slowing nearby enemies.",
  "กระโดดขึ้นแล้วทุบพื้นอย่างรุนแรง ผลักและทำให้ศัตรูรอบๆ เดินช้าลง", 8.0, 20, "STR", [
  {"type": "dash", "strength": 0.15, "vertical": 0.85, "sound": snd("whoosh", 0.8, 0.8), "vfx": [pt("cloud", "ring", radius=1.2, count=14, speed=0.1)]},
  {"type": "area", "at": "caster", "delay": 9, "radius": 5.5, "damage": 11, "damage_type": "physical", "knockback": 1.4, "effects": [eff("slowness", 2.0, 1)],
   "vfx": [pt("explosion", count=3, spread=1.2), dust("#FF8A30", 1.8, "ring", radius=5, count=64, speed=0.22), pt("crit", "burst", count=40, speed=0.5), pt("cloud", "ring", radius=3, count=30, speed=0.2), pt("large_smoke", "disc", radius=4, count=14)],
   "sound": snd("slam", 1.4)},
  {"type": "vfx", "at": "caster", "delay": 11, "vfx": [dust("#FFD070", 1.4, "ring", radius=3, count=40, speed=0.12)]}])
S["whirlwind"] = skill("warrior", "Whirlwind", "พายุหมุน", "Spin your blade in a deadly whirl, striking everything around you several times.",
  "หมุนตัวฟาดฟันศัตรูรอบตัวหลายครั้งติดต่อกัน", 10.0, 30, "STR", [
  {"type": "area", "at": "caster", "repeat": {"count": 6, "interval": 3}, "radius": 4.2, "damage": 4.2, "damage_type": "physical", "knockback": 0.45,
   "vfx": [pt("sweep_attack", "ring", radius=3.2, count=10, speed=0.0), dust("#FFFFFF", 1.3, "ring", radius=4, count=30, speed=0.15), pt("crit", "ring", radius=3.4, count=10, speed=0.3)],
   "sound": snd("whoosh", 0.7, 1.1)}])
S["battle_cry"] = skill("warrior", "Battle Cry", "คำรามศึก", "Roar to empower yourself and nearby allies and force enemies to focus you.",
  "คำรามปลุกพลังตัวเองและพันธมิตร และบังคับให้ศัตรูหันมาโจมตีคุณ", 20.0, 25, "STR", [
  {"type": "vfx", "at": "caster", "sound": snd("warcry", 1.2), "vfx": [dust("#FF4A30", 1.6, "spiral", radius=1.2, height=2.4, turns=3, count=50), dust("#FFC040", 1.4, "ring", radius=4, count=40, speed=0.15), pt("flame", "burst", count=24, speed=0.25)]},
  {"type": "vfx", "at": "caster", "delay": 3, "vfx": [dust("#FF7A40", 1.5, "ring", radius=7, count=70, speed=0.2)]},
  {"type": "area", "affect": "allies", "radius": 9, "damage": 0, "effects": [eff("strength", 12.0, 1), eff("regeneration", 6.0, 0)], "hit_vfx": [pt("flame", "burst", count=10, speed=0.15)]},
  {"type": "taunt", "radius": 10}])
S["shield_wall"] = skill("warrior", "Shield Wall", "กำแพงโล่", "Brace behind your shield: take far less damage and gain absorption, but move slower.",
  "ตั้งโล่รับ ลดความเสียหายที่ได้รับมหาศาลและได้พลังป้องกันเสริม แต่เคลื่อนที่ช้าลง", 22.0, 30, "STR", [
  {"type": "vfx", "at": "caster", "sound": snd("shield", 1.1, 0.85), "vfx": [dust("#7AB4FF", 1.5, "sphere", radius=1.5, count=60, speed=0.0), dust("#FFFFFF", 1.0, "ring", radius=1.6, count=24, speed=0.08), pt("enchanted_hit", "burst", count=24, speed=0.3)]},
  {"type": "area", "affect": "self", "radius": 0, "effects": [eff("resistance", 7.0, 2), eff("absorption", 8.0, 3), eff("slowness", 7.0, 0)]}])
S["charge"] = skill("warrior", "Charge", "พุ่งชน", "Rush forward at high speed, battering and knocking aside anything in your way.",
  "พุ่งไปข้างหน้าด้วยความเร็วสูง ชนศัตรูที่ขวางทางกระเด็น", 9.0, 20, "STR", [
  {"type": "dash", "strength": 2.3, "vertical": 0.1, "sound": snd("charge", 1.0), "vfx": [pt("cloud", "burst", count=18, speed=0.2)]},
  {"type": "area", "at": "caster", "repeat": {"count": 7, "interval": 2}, "radius": 2.4, "damage": 5, "damage_type": "physical", "knockback": 1.5,
   "vfx": [pt("cloud", count=6, spread=0.4), pt("crit", count=5, spread=0.5, speed=0.2), dust("#FF8A30", 1.4, count=6, spread=0.5)], "hit_vfx": [pt("explosion", count=1), pt("crit", "burst", count=12, speed=0.35)]}])
S["cleave"] = skill("warrior", "Cleave", "ฟันกวาด", "A quick wide slash that hits every enemy in front of you.",
  "ฟันกวาดกว้างอย่างรวดเร็ว โดนศัตรูทุกตัวด้านหน้า", 3.5, 10, "STR", [
  {"type": "area", "at": "caster", "radius": 4.6, "cone": 150, "damage": 8, "damage_type": "physical", "knockback": 0.9,
   "vfx": [pt("sweep_attack", "arc", radius=3.0, angle=150, count=8), dust("#FFFFFF", 1.3, "arc", radius=4, angle=150, count=34, speed=0.1), pt("enchanted_hit", "arc", radius=3.5, angle=150, count=12, speed=0.2)],
   "sound": snd("slash", 1.0)}])

# ------------------------------------------------------------------ ASSASSIN (scales with DEX) - purple / black / white, custom sprite particles
PUR, DPUR, LAV, WHT = "#B070FF", "#6A3CFF", "#D8C0FF", "#F4EEFF"
def cp(name, shape, **kw):
    """A custom sprite particle (see client/fx): slash, swirl, sparkle, shard, glow."""
    d = {"particle": "pixelclasses:" + name, "shape": shape}; d.update(kw); return d
def fx_slash(roll, size=1.5, y=1.1, sparks=5):
    """One crescent slash streak (sprite cut from the concept art) + sparkles + shards + a thin crisp arc underneath."""
    return [cp("slash", "stamp", count=1, size=size, roll=roll, length=1.4, y_offset=y),
            cp("sparkle", "stamp", count=sparks, length=1.4, y_offset=y, spread=0.9, speed=0.05),
            cp("shard", "stamp", count=2, length=1.4, y_offset=y, spread=0.5, speed=0.12),
            dust(PUR, 0.6, "slash", radius=1.9 * size / 1.5, angle=140, roll=roll, length=1.3, y_offset=y, count=16)]
def fx_cross(size=2.0, y=1.1):
    return fx_slash(45, size, y, 6) + fx_slash(-45, size, y, 0) + [cp("swirl", "stamp", count=1, size=1.1, length=0.0, y_offset=y)]
def fx_vanish(y=1.0):
    return [dtr(PUR, WHT, 1.2, "sphere", radius=1.0, count=34, speed=0.04, y_offset=y), cp("swirl", "stamp", count=1, size=1.3, length=0.0, y_offset=y),
            cp("sparkle", "burst", count=16, speed=0.3, y_offset=y), cp("shard", "burst", count=10, speed=0.25, y_offset=y)]
def fx_arrive(y=1.0):
    return [cp("swirl", "stamp", count=1, size=1.1, length=0.0, y_offset=y), cp("sparkle", "burst", count=14, speed=0.25, y_offset=y),
            cp("shard", "burst", count=8, speed=0.2, y_offset=y), cp("glow", "sphere", radius=0.9, count=18, speed=0.0, size=2.0, y_offset=y)]

acts = [
  {"type": "vfx", "at": "caster", "sound": snd("vanish", 1.0), "vfx": fx_vanish()},
  {"type": "area", "affect": "self", "radius": 0, "effects": [eff("invisibility", 1.6, 0), eff("speed", 1.6, 1)]},
  {"type": "shadow_step", "delay": 3, "range": 14, "behind": 1.7, "fallback": 7, "sound": snd("shadow_step", 1.0), "arrival_sound": snd("shadow_step", 0.8, 1.3), "arrival_vfx": fx_arrive()}]
for i, r in enumerate([20, -35, 70, -60, 105, -15]):
    acts.append({"type": "area", "at": "caster", "direction": "caster", "delay": 7 + 2 * i, "radius": 3.3, "damage": 2.7, "damage_type": "physical", "knockback": 0.1,
                 "vfx": fx_slash(r, 1.5), "sound": snd("dagger_slash", 0.7, 1.0 + 0.05 * i)})
acts.append({"type": "area", "at": "caster", "direction": "caster", "delay": 21, "radius": 3.6, "damage": 8, "damage_type": "physical", "knockback": 0.7,
             "vfx": fx_cross(2.1) + [cp("sparkle", "burst", count=18, speed=0.4, y_offset=1.1), dust(WHT, 1.1, "ring", radius=3.2, count=44, speed=0.3)],
             "sound": snd("dagger_crit", 1.2)})
S["shadow_strike"] = skill("assassin", "Shadow Strike", "หายตัวฟันรัว", "Vanish, reappear behind the enemy you are looking at, and carve them with a flurry of slashes ending in a heavy finisher.",
  "หายตัวไปปรากฏด้านหลังศัตรูที่เล็งอยู่ แล้วฟันรัวต่อเนื่องปิดท้ายด้วยท่าฟันหนัก", 10.0, 30, "DEX", acts)

acts = [{"type": "vfx", "at": "caster", "sound": snd("shadow_step", 0.8, 1.2), "vfx": [dust(PUR, 1.1, "spiral", radius=1.2, height=2.0, turns=3, count=34), cp("sparkle", "ring", radius=2, count=16, speed=0.08, y_offset=1.0), cp("swirl", "stamp", count=1, size=1.4, length=0.0, y_offset=0.3)]},
        {"type": "area", "affect": "self", "radius": 0, "effects": [eff("speed", 2.5, 1)]}]
for i, r in enumerate([0, 60, -60, 120, -120, 30, -30, 90, -90, 150]):
    acts.append({"type": "area", "at": "caster", "direction": "caster", "delay": 2 + 2 * i, "radius": 3.4, "damage": 2.3, "damage_type": "physical", "knockback": 0.1,
                 "vfx": fx_slash(r, 1.6, 1.1, 3), "sound": snd("dagger_slash", 0.6, 0.95 + 0.04 * (i % 5))})
S["blade_flurry"] = skill("assassin", "Blade Flurry", "พายุใบมีด", "Spin in a storm of twin blades, slashing every enemy around you ten times in two seconds.",
  "หมุนตัวเป็นพายุมีดคู่ ฟันศัตรูรอบตัว 10 ครั้งใน 2 วินาที", 9.0, 28, "DEX", acts)

S["phantom_lunge"] = skill("assassin", "Phantom Lunge", "พุ่งเงา", "Dash forward as a streak of shadow, cutting through anything in the way and ending with a double slash.",
  "พุ่งไปข้างหน้าเป็นเงาวูบ ตัดทุกอย่างที่ขวางทาง ปิดท้ายด้วยฟันไขว้", 6.0, 18, "DEX", [
  {"type": "dash", "strength": 2.6, "vertical": 0.05, "sound": snd("whoosh", 0.9, 1.3), "vfx": fx_vanish(0.9)},
  {"type": "vfx", "at": "caster", "repeat": {"count": 6, "interval": 2}, "vfx": [cp("glow", "column", radius=0.3, height=1.9, count=10, size=1.6), dust(PUR, 0.9, "column", radius=0.45, height=1.9, count=10),
      cp("sparkle", "column", radius=0.5, height=1.9, count=4), cp("shard", "stamp", count=2, length=0.0, y_offset=1.0, spread=0.4, speed=0.05)]},
  {"type": "area", "at": "caster", "repeat": {"count": 6, "interval": 2}, "radius": 2.2, "damage": 3.4, "damage_type": "physical", "knockback": 0.25},
  {"type": "area", "at": "caster", "direction": "caster", "delay": 12, "radius": 3.2, "damage": 5, "damage_type": "physical", "knockback": 0.6,
   "vfx": fx_slash(30, 1.7) + fx_slash(-30, 1.7, 1.1, 0), "sound": snd("dagger_crit", 1.0)}])

S["smoke_veil"] = skill("assassin", "Smoke Veil", "ม่านควัน", "Throw down a smoke bomb: blind and slow nearby enemies while you turn invisible and speed away.",
  "ปาระเบิดควัน ทำให้ศัตรูรอบๆ ตาบอดและช้าลง ขณะที่คุณล่องหนและวิ่งเร็วขึ้น", 16.0, 25, "DEX", [
  {"type": "vfx", "at": "caster", "sound": snd("vanish", 1.1, 0.9), "vfx": [pt("cloud", "sphere", radius=2.2, count=60, speed=0.02, y_offset=1.0), dust("#8A78B8", 1.5, "ring", radius=4.5, count=50, speed=0.12, y_offset=0.3),
      cp("swirl", "stamp", count=1, size=2.2, length=0.0, y_offset=0.4), cp("sparkle", "burst", count=22, speed=0.35, y_offset=1.0), cp("shard", "burst", count=12, speed=0.3, y_offset=1.0)]},
  {"type": "area", "affect": "self", "radius": 0, "effects": [eff("invisibility", 5.0, 0), eff("speed", 5.0, 1)]},
  {"type": "area", "affect": "enemies", "radius": 5, "damage": 0, "effects": [eff("blindness", 2.5, 0), eff("slowness", 2.0, 0)], "hit_vfx": [pt("cloud", "burst", count=8, speed=0.1)]}])

S["throwing_knives"] = skill("assassin", "Throwing Knives", "ปามีดบิน", "Fling a fan of seven throwing knives at your target.",
  "ปามีดเงา 7 เล่มเป็นรูปพัดใส่เป้าหมาย", 5.0, 15, "DEX", [
  {"type": "projectile", "at": "eye", "repeat": {"count": 7, "interval": 1}, "speed": 2.8, "range": 32, "hit_radius": 0.6, "spread": 7, "damage": 3.6, "damage_type": "projectile", "knockback": 0.1,
   "sound": snd("knife_throw", 0.7), "trail": [cp("glow", "point", count=2, spread=0.03, size=0.9), dust(WHT, 0.7, count=2, spread=0.02), cp("sparkle", "point", count=1, spread=0.08, speed=0.02)],
   "on_end": [{"type": "vfx", "vfx": [cp("sparkle", "burst", count=6, speed=0.15), cp("shard", "burst", count=4, speed=0.15), cp("swirl", "stamp", count=1, size=0.35, length=0.0)]}]}])

S["execution"] = skill("assassin", "Execution", "สังหาร", "Mark your prey, vanish, and strike from behind with a devastating cross-slash.",
  "ล็อกเหยื่อ หายตัว แล้วฟันไขว้ทรงพลังจากด้านหลัง", 14.0, 35, "DEX", [
  {"type": "vfx", "at": "caster", "sound": snd("vanish", 1.0, 1.1), "vfx": fx_vanish()},
  {"type": "area", "affect": "self", "radius": 0, "effects": [eff("invisibility", 1.2, 0)]},
  {"type": "shadow_step", "delay": 3, "range": 12, "behind": 1.5, "fallback": 0, "sound": snd("shadow_step", 1.0, 0.9), "arrival_sound": snd("shadow_step", 0.8, 1.2), "arrival_vfx": fx_arrive()},
  {"type": "area", "at": "caster", "direction": "caster", "delay": 9, "radius": 3.0, "damage": 20, "damage_type": "physical", "knockback": 0.4,
   "vfx": fx_cross(2.4) + [dust(WHT, 1.2, "ring", radius=3.0, count=44, speed=0.3), cp("sparkle", "burst", count=26, speed=0.45, y_offset=1.1), cp("shard", "burst", count=14, speed=0.4, y_offset=1.1)],
   "sound": snd("dagger_crit", 1.4)}])

CLASSES = {
 "wizard": {"name": {"en": "Wizard", "th": "นักเวท"}, "color": "#9B6BFF",
   "description": {"en": "A fragile master of the arcane. Devastating spells from a distance, but very little protection.",
                   "th": "จอมเวทร่างบาง ร่ายเวทรุนแรงจากระยะไกล แต่ป้องกันตัวได้น้อย"},
   "mana": {"name": {"en": "Mana", "th": "มานา"}, "color": "#5A8CFF", "max": 140, "regen": 5.0},
   "attributes": [{"attribute": "minecraft:generic.max_health", "amount": -4.0, "operation": "addition"}],
   "skills": ["fireball", "frost_nova", "arcane_beam", "meteor", "blink", "arcane_barrier"]},
 "archer": {"name": {"en": "Archer", "th": "นักธนู"}, "color": "#6BE36B",
   "description": {"en": "A nimble marksman who keeps pressure on with a constant stream of arrows and traps.",
                   "th": "พรานคล่องแคล่ว กดดันศัตรูต่อเนื่องด้วยห่าธนูและกับดัก"},
   "mana": {"name": {"en": "Focus", "th": "สมาธิ"}, "color": "#6BE36B", "max": 100, "regen": 6.0},
   "attributes": [{"attribute": "minecraft:generic.movement_speed", "amount": 0.08, "operation": "multiply_base"}],
   "skills": ["rapid_shot", "piercing_arrow", "arrow_rain", "hunters_focus", "poison_trap", "evasive_roll"]},
 "warrior": {"name": {"en": "Warrior", "th": "นักรบ"}, "color": "#FF6B5B",
   "description": {"en": "A sturdy front-liner who holds the line, soaks up damage and cuts down everything in reach.",
                   "th": "นักรบแนวหน้าสุดอึด ยืนสู้ยื้อไฟต์ รับดาเมจและฟันทุกอย่างที่อยู่ในระยะ"},
   "mana": {"name": {"en": "Rage", "th": "พลังโกรธ"}, "color": "#FF6B5B", "max": 100, "regen": 4.0},
   "attributes": [{"attribute": "minecraft:generic.max_health", "amount": 8.0, "operation": "addition"},
                  {"attribute": "minecraft:generic.armor", "amount": 2.0, "operation": "addition"},
                  {"attribute": "minecraft:generic.knockback_resistance", "amount": 0.2, "operation": "addition"}],
   "skills": ["ground_slam", "whirlwind", "battle_cry", "shield_wall", "charge", "cleave"]},
 "assassin": {"name": {"en": "Assassin", "th": "นักฆ่า"}, "color": "#B070FF",
   "description": {"en": "A fast, fragile dual-dagger killer. Vanishes, strikes from behind and cuts targets down in a blur of slashes.",
                   "th": "นักฆ่ามีดคู่ว่องไว ตัวบาง หายตัวไปฟันจากด้านหลังและรัวใบมีดจนศัตรูล้มในพริบตา"},
   "mana": {"name": {"en": "Shadow", "th": "พลังเงา"}, "color": "#7A4CFF", "max": 100, "regen": 7.0},
   "attributes": [{"attribute": "minecraft:generic.movement_speed", "amount": 0.12, "operation": "multiply_base"},
                  {"attribute": "minecraft:generic.attack_speed", "amount": 0.2, "operation": "multiply_base"},
                  {"attribute": "minecraft:generic.max_health", "amount": -2.0, "operation": "addition"}],
   "skills": ["shadow_strike", "blade_flurry", "phantom_lunge", "throwing_knives", "smoke_veil", "execution"]},
}
def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f: json.dump(obj, f, indent=2, ensure_ascii=False)
for k, v in S.items(): dump(os.path.join(DATA, "skills", k + ".json"), v)
for k, v in CLASSES.items():
    v = dict(v); v["icon"] = f"pixelclasses:textures/class/{k}.png"; v["skills"] = ["pixelclasses:" + s for s in v["skills"]]
    dump(os.path.join(DATA, "classes", k + ".json"), v)

# ------------------------------------------------------------ example datapack (copy into <world>/datapacks/)
dump(os.path.join(EX, "pack.mcmeta"), {"pack": {"description": "Pixel Classes example addon", "pack_format": 15}})
star = skill("wizard", "Starfall (example)", "ดาวตก (ตัวอย่าง)", "EXAMPLE addon skill: a shower of falling stars at the target area.",
  "สกิลตัวอย่างจาก addon: ฝนดาวตกลงบริเวณเป้าหมาย", 10.0, 30, "INT", [
  {"type": "vfx", "at": "aim", "aim_range": 30, "repeat": {"count": 5, "interval": 4}, "vfx": [dust("#FFE060", 1.3, "ring", radius=4, count=36)]},
  {"type": "projectile", "delay": 8, "at": "aim", "aim_range": 30, "repeat": {"count": 14, "interval": 3}, "scatter": 4, "y_offset": 16, "direction": "down", "speed": 1.6, "range": 22, "hit_radius": 0.8,
   "sound": snd("cast_arcane", 0.5, 1.6), "trail": [pt("end_rod", count=3, spread=0.05), dust("#FFE060", 1.2, count=3, spread=0.08)],
   "on_end": [{"type": "area", "radius": 2.2, "damage": 5, "damage_type": "magic", "knockback": 0.5, "vfx": [pt("firework", "burst", count=14, speed=0.25), pt("end_rod", "burst", count=10, speed=0.3)], "sound": snd("zap", 0.5)}]}])
star["icon"] = "pixelclasses:textures/skill/meteor.png"
dump(os.path.join(EX, "data", "example", "pixelclasses", "skills", "starfall.json"), star)
fb = json.loads(json.dumps(S["fireball"])); fb["actions"][1]["on_end"][0]["damage"] = 30; fb["cooldown"] = 1.0
dump(os.path.join(EX, "data", "pixelclasses", "pixelclasses", "skills", "fireball.json"), fb)
print("skills:", len(S), "classes:", len(CLASSES))
