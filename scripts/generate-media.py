"""Generate original abstract motion and synthesized effects, without external samples.
SPDX-License-Identifier: GPL-3.0-only
Generated media are dedicated under CC0-1.0; see MEDIA-LICENSE.md.
Requires FFmpeg with libx264 on PATH (or --ffmpeg PATH).
"""
from pathlib import Path
import argparse, math, random, struct, subprocess, wave

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'android/app/src/main/assets'
def main():
 p=argparse.ArgumentParser();p.add_argument('--ffmpeg',default='ffmpeg');args=p.parse_args()
 for folder in ['wallpapers','thumbnails','game-audio']:(ASSETS/folder).mkdir(parents=True,exist_ok=True)
 for name,cb,cr in [('Aurora',150,110),('Ocean',165,118),('Ember',110,155)]:
  target=ASSETS/'wallpapers'/f'{name}.mp4'
  # Every time-dependent component completes an integer cycle in six seconds.
  filt=f"nullsrc=s=3840x2160:r=24:d=6,geq=lum='40+18*sin(X/W*5+Y/H*3+T*2*PI/6)+16*sin(Y/H*4-X/W*2-T*2*PI/6)':cb='{cb}+10*sin(X/W*3+T*2*PI/6)':cr='{cr}+8*cos(Y/H*4-T*2*PI/6)'"
  subprocess.run([args.ffmpeg,'-hide_banner','-loglevel','error','-y','-f','lavfi','-i',filt,'-an','-c:v','libx264','-preset','veryfast','-crf','24','-pix_fmt','yuv420p','-movflags','+faststart',str(target)],check=True)
  subprocess.run([args.ffmpeg,'-hide_banner','-loglevel','error','-y','-i',str(target),'-frames:v','1','-vf','scale=480:270',str(ASSETS/'thumbnails'/f'{name}.jpg')],check=True)
  print(name,flush=True)
 names=['brake','coin','confirm','gold','hit','jump','land','rail_hit','reward','roll','splash','step','ui','unlock','warning','water','whoosh','wood','welcome']
 for index,name in enumerate(names):
  rate=24000;duration=1.1 if name=='welcome' else .22 if name in ['coin','ui','step','land'] else .45
  rng=random.Random(index);samples=[]
  for i in range(int(rate*duration)):
   t=i/rate;x=t/duration;env=min(1,t/.015)*(1-x)**2
   freq=240+index*27
   if name in ['coin','reward','gold','unlock','welcome']:freq*=1+int(x*3)*.25
   signal=math.sin(2*math.pi*freq*t)+.25*math.sin(2*math.pi*freq*1.5*t)
   if name in ['brake','hit','land','rail_hit','roll','splash','step','water','whoosh','wood']:signal=.35*signal+.65*rng.uniform(-1,1)
   samples.append(struct.pack('<h',int(11000*env*signal)))
  with wave.open(str(ASSETS/'game-audio'/f'{name}.wav'),'wb') as out:
   out.setnchannels(1);out.setsampwidth(2);out.setframerate(rate);out.writeframes(b''.join(samples))
 print('Generated 3 original 4K loops, thumbnails and 19 synthetic sounds.')
if __name__=='__main__':main()
