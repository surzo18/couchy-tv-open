"""Emulator-only smoke test. Requires a booted TV AVD and installed debug APK.
Never changes a physical television. Restores emulator display overrides at the end.
"""
import argparse,subprocess,time,json,xml.etree.ElementTree as ET,re
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--adb',default='adb');p.add_argument('--serial',default='emulator-5556');a=p.parse_args()
assert a.serial.startswith('emulator-'),'This test is restricted to emulators'
root=Path(__file__).resolve().parents[1];out=root/'reports/ui-matrix';out.mkdir(parents=True,exist_ok=True)
def adb(*args):return subprocess.run([a.adb,'-s',a.serial,*args],capture_output=True,timeout=45,check=True).stdout.decode('utf-8',errors='replace')
pkg='io.github.surzo18.couchytv';component=pkg+'/com.conreo.couchytv.MainActivity'
results=[]
try:
 for w,h,density in [(1280,720,213),(1920,1080,320),(3840,2160,640),(1280,720,320)]:
  adb('shell','wm','size',f'{w}x{h}');adb('shell','wm','density',str(density))
  time.sleep(2)
  adb('shell','am','force-stop',pkg)
  for tab in ['home','Pozadia','Systém','Hudba','Návod']:
   command=['shell','am','start','-W','-n',component,'-f','0x10008000']
   if tab!='home':command+=['--es','studio_tab',tab]
   else:command+=['--ez','home_press','true']
   adb(*command);time.sleep(3)
   name=f'{w}x{h}-{density}-{tab}'
   for attempt in range(8):
    adb('shell','uiautomator','dump','/sdcard/package-ui.xml')
    xml=adb('shell','cat','/sdcard/package-ui.xml');tree=ET.fromstring(xml)
    first=next(tree.iter('node'))
    if first.get('bounds')==f'[0,0][{w},{h}]' and first.get('package')==pkg:break
    time.sleep(1)
   else:raise AssertionError(name+' window did not settle after display change')
   (out/(name+'.xml')).write_text(xml,encoding='utf-8')
   focused=[n for n in tree.iter('node') if n.get('focused')=='true']
   bounds=[]
   for n in focused:
    b=list(map(int,re.findall(r'\d+',n.get('bounds',''))));bounds.append(b)
    assert len(b)==4 and 0<=b[0]<b[2]<=w and 0<=b[1]<b[3]<=h,(name,b)
   assert any(n.get('package')==pkg for n in tree.iter('node')),name+' not displaying app'
   adb('shell','screencap','-p','/sdcard/package-ui.png');adb('pull','/sdcard/package-ui.png',str(out/(name+'.png')))
   # Exercise DPAD focus in scrollable settings/help, not only the initial screen.
   for _ in range(8):adb('shell','input','keyevent','20')
   adb('shell','input','keyevent','4')
   results.append({'resolution':[w,h],'density':density,'screen':tab,'initialFocus':bounds,'opened':True})
   print(name,'OK',flush=True)
finally:
 adb('shell','wm','size','reset');adb('shell','wm','density','reset')
 (out/'results.json').write_text(json.dumps(results,indent=2,ensure_ascii=False),encoding='utf-8')
assert len(results)==20
