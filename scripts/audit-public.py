"""Fail closed on undeclared media, private files, secrets and stale legal copies.
This check supplements human provenance review; it is not a legal opinion.
"""
from pathlib import Path
import hashlib,json,re,subprocess,sys,zipfile,argparse
p=argparse.ArgumentParser();p.add_argument('--apk',type=Path);a=p.parse_args()
r=Path(__file__).resolve().parents[1];errors=[]
manifest={x['path']:x for x in json.loads((r/'media-manifest.json').read_text())}
gallery={x['path']:x for x in json.loads((r/'docs/screenshots/manifest.json').read_text())}
for name,item in gallery.items():
 f=r/name
 if not name.startswith('docs/screenshots/') or not f.is_file() or hashlib.sha256(f.read_bytes()).hexdigest()!=item['sha256']:errors.append('Unreviewed screenshot: '+name)
for name,item in manifest.items():
 f=r/name
 if not f.is_file() or hashlib.sha256(f.read_bytes()).hexdigest()!=item['sha256']:errors.append('Changed/unavailable declared asset: '+name)
 if item['license'] not in {'CC0-1.0','OFL-1.1'}:errors.append('Asset license needs review: '+name)
try:files=[r/x for x in subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard'],cwd=r,stderr=subprocess.DEVNULL).decode().splitlines()]
except subprocess.CalledProcessError:files=[x for x in r.rglob('*') if x.is_file() and not any(y in {'.git','.gradle','.kotlin','build','reports','dist','__pycache__'} for y in x.relative_to(r).parts)]
secret=re.compile(rb'(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|AKIA[0-9A-Z]{16}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----)')
for f in files:
 name=f.relative_to(r).as_posix()
 if f.suffix.lower() in {'.apk','.jks','.keystore','.pem','.key'}:errors.append('Private/build artifact tracked: '+name)
 if f.suffix.lower() in {'.mp4','.m4a','.mp3','.wav','.jpg','.png','.ttf'} and name not in manifest:
  if name not in gallery:errors.append('Undeclared binary: '+name)
 if f.suffix.lower() in {'.kt','.kts','.xml','.json','.py','.md','.yml','.properties'}:
  data=f.read_bytes()
  if secret.search(data):errors.append('Potential secret: '+name)
  if re.search(rb'[A-Za-z]:\\{1,2}Users\\{1,2}[^\\/\s]+',data):errors.append('Absolute user path: '+name)
 if f.suffix.lower()=='.md':
  for target in re.findall(r'\]\(([^\s)]+)\)',f.read_text(encoding='utf-8')):
   if target.startswith(('http:','https:','mailto:','#')):continue
   if not (f.parent/target.split('#')[0]).exists():errors.append('Broken relative link in '+name+': '+target)
legal=r/'android/app/src/main/assets/legal'
for name in ['THIRD-PARTY-NOTICES.md','PRIVACY.md','SERVICES.md','MEDIA-LICENSE.md']:
 if (r/name).read_bytes()!=(legal/name).read_bytes():errors.append('Stale APK legal notice: '+name)
if json.loads((r/'android/app/src/main/assets/photo-notes.json').read_text())!={}:errors.append('Photo notes must be empty')
if json.loads((r/'android/app/src/main/assets/aerials.json').read_text())!=[]:errors.append('Remote aerial catalog must be empty')
edition=json.loads((r/'android/app/src/main/assets/edition.json').read_text())
if edition.get('newsSources') or edition.get('premiereSources'):errors.append('Public source presets must be empty')
if {x['file'] for x in edition['wallpapers']}!={'Aurora.mp4','Ocean.mp4','Ember.mp4'}:errors.append('Unexpected public wallpaper profile')
if a.apk:
 with zipfile.ZipFile(a.apk) as z:
  expected={n.removeprefix('android/app/src/main/') for n in manifest if '/assets/' in n}
  actual={n for n in z.namelist() if n.startswith('assets/') and n.endswith(('.mp4','.wav','.jpg','.png','.m4a','.mp3'))}
  if actual!=expected:errors.append('APK media differs from manifest')
  for n in expected:
   key='android/app/src/main/'+n
   if n not in z.namelist() or hashlib.sha256(z.read(n)).hexdigest()!=manifest[key]['sha256']:errors.append('APK asset checksum: '+n)
  for n in legal.iterdir():
   if z.read('assets/legal/'+n.name)!=n.read_bytes():errors.append('APK license mismatch: '+n.name)
print(json.dumps({'declared_assets':len(manifest),'checked_files':len(files),'errors':errors},indent=2))
sys.exit(bool(errors))
