"""Destructive only to the test app's data on an explicitly selected emulator."""
import argparse,subprocess,time,xml.etree.ElementTree as ET,re,json
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--adb',default='adb');p.add_argument('--serial',default='emulator-5556');a=p.parse_args()
assert a.serial.startswith('emulator-'),'Never clear a real TV with this script'
root=Path(__file__).resolve().parents[1];out=root/'reports/onboarding';out.mkdir(parents=True,exist_ok=True)
pkg='io.github.surzo18.couchytv';component=pkg+'/com.conreo.couchytv.MainActivity'
def adb(*args):return subprocess.check_output([a.adb,'-s',a.serial,*args],timeout=40).decode('utf-8',errors='replace')
def start():adb('shell','am','start','-n',component);time.sleep(3)
def tree():
 adb('shell','uiautomator','dump','/sdcard/onboarding-test.xml')
 return ET.fromstring(adb('shell','cat','/sdcard/onboarding-test.xml'))
def has(t,text):return any(n.get('text')==text for n in t.iter('node'))
def tap(text):
 # TV Compose surfaces are remote-first; use DPAD rather than touch injection.
 for _ in range(16):
  t=tree();node=next(n for n in t.iter('node') if n.get('text')==text)
  x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')));x=(x1+x2)//2;y=(y1+y2)//2
  focus=next(n for n in t.iter('node') if n.get('focused')=='true')
  a1,b1,a2,b2=map(int,re.findall(r'\d+',focus.get('bounds')))
  if a1<=x<=a2 and b1<=y<=b2:
   adb('shell','input','keyevent','23');time.sleep(1.5);return
  key=19 if y<b1 else 20 if y>b2 else 21 if x<a1 else 22
  adb('shell','input','keyevent',str(key));time.sleep(.2)
 raise AssertionError('Could not focus '+text)
def shot(name):adb('shell','screencap','-p','/sdcard/onboarding-test.png');adb('pull','/sdcard/onboarding-test.png',str(out/(name+'.png')))
adb('shell','wm','size','1280x720');adb('shell','wm','density','213')
adb('shell','pm','clear',pkg);start()
assert has(tree(),'Vitaj v Couchy');shot('welcome-sk')
tap('English');assert has(tree(),'Welcome to Couchy');shot('welcome-en')
titles=['TV controls','Wallpapers and music','Your news','Weather','Jellyfin','Photos and memories','Ready to start']
for i,title in enumerate(titles):
 tap('Continue');t=tree();assert has(t,title),(i,title)
 if i==2:
  adb('shell','am','force-stop',pkg);start();assert has(tree(),title),'Progress not resumed'
 if i==6:shot('summary-en')
tap('Open home');time.sleep(2);assert not has(tree(),'Welcome to Couchy')
adb('shell','am','force-stop',pkg);start();t=tree();assert not has(t,'Welcome to Couchy') and not has(t,'Ready to start')
shot('finished-home-en')
(out/'results.json').write_text(json.dumps({'freshInstall':True,'languageSwitch':True,'resumeAfterProcessDeath':True,'optionalStepsSkipped':True,'completedPersists':True,'pages':8},indent=2),encoding='utf-8')
print('PASS: 8-page wizard, SK/EN, resumed progress, optional skips and persisted completion')
