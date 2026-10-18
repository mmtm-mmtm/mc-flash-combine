"""Portable source-example helper. No desktop automation and no automatic browser launch."""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess, sys

ROOT = Path(__file__).resolve().parent
SWF_SHA256 = 'ced3849a90a97368279dec93cc06745a8484cb26b881753b90f25a08275e6c6e'

def call(args, **kwargs):
    subprocess.run([str(a) for a in args], check=True, **kwargs)

def java():
    home = os.environ.get('JAVA_HOME')
    return str(Path(home) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')) if home else 'java'

def prepare(args):
    original = args.swf.resolve()
    if hashlib.sha256(original.read_bytes()).hexdigest() != SWF_SHA256:
        raise RuntimeError('SWF revision mismatch. See original/README.md for the required SHA-256.')
    if not args.ffdec_jar.is_file():
        raise FileNotFoundError(args.ffdec_jar)
    ruffle = args.ruffle_dir.resolve()
    if not (ruffle / 'ruffle.js').is_file() or not list(ruffle.glob('*.wasm')):
        raise RuntimeError('Pass an unpacked Ruffle self-hosted directory containing ruffle.js and *.wasm.')
    target = ROOT / 'original' / '狂扁小朋友.swf'
    if original != target.resolve():
        shutil.copy2(original, target)
    call([java(), '-jar', args.ffdec_jar, '-export', 'script', ROOT / 'decompiled', target])
    call([sys.executable, ROOT / 'patch_swf.py'])
    call([java(), '-jar', args.ffdec_jar, '-importScript', target, ROOT / 'web/bridge.swf', ROOT / 'patched-scripts'])
    bridge = ROOT / 'web/bridge.swf'
    raw = bytearray(bridge.read_bytes()); raw[3] = 8; bridge.write_bytes(raw)
    call([sys.executable, ROOT / 'widen_swf.py'])
    shutil.copytree(ruffle, ROOT / 'web/ruffle', dirs_exist_ok=True)
    print('Local Flash resources prepared. Next: python manage.py check')

def runtime(args):
    base = Path(args.runtime).expanduser().resolve()
    if base == ROOT or ROOT in base.parents:
        raise RuntimeError('Use a runtime outside the source folder, preferably a short local ASCII path.')
    project = base / 'fabric-mod'
    project.mkdir(parents=True, exist_ok=True)
    shutil.copytree(ROOT / 'fabric-mod/src', project / 'src', dirs_exist_ok=True)
    shutil.copytree(ROOT / 'fabric-mod/gradle', project / 'gradle', dirs_exist_ok=True)
    for name in ('build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat'):
        shutil.copy2(ROOT / 'fabric-mod' / name, project / name)
    call([sys.executable, ROOT / 'prepare_world.py', base])
    print('Local Minecraft project:', project, flush=True)
    if args.prepare_only:
        return
    if os.name == 'nt':
        call([os.environ.get('COMSPEC', 'cmd.exe'), '/d', '/c', 'gradlew.bat', '--no-daemon', 'runClient'], cwd=project)
    else:
        call(['sh', 'gradlew', '--no-daemon', 'runClient'], cwd=project)

def check(args):
    names = ['fabric-mod/build.gradle', 'fabric-mod/gradle/wrapper/gradle-wrapper.jar',
             'fabric-mod/src/client/java/com/example/client/ExampleModClient.java',
             'brawler.as', 'patch_swf.py', 'widen_swf.py', 'prepare_world.py',
             'web/index.html', 'web/inventory.js', 'web/audio.js', 'web/motion.js']
    missing = [name for name in names if not (ROOT / name).is_file()]
    if missing:
        raise RuntimeError('Missing source files: ' + ', '.join(missing))
    assets = ['original/狂扁小朋友.swf', 'web/bridge.swf', 'web/ruffle/ruffle.js']
    absent = [name for name in assets if not (ROOT / name).is_file()]
    print(json.dumps({'sourceComplete': True, 'localAssetsReady': not absent,
                      'missingLocalAssets': absent}, ensure_ascii=False, indent=2))
    if args.require_assets and absent:
        raise RuntimeError('Import local resources with the prepare command first.')

parser = argparse.ArgumentParser(description=__doc__)
sub = parser.add_subparsers(dest='command', required=True)
p = sub.add_parser('prepare', help='Import user-supplied Flash and Ruffle resources; rebuild bridge.swf')
p.add_argument('--swf', type=Path, required=True)
p.add_argument('--ffdec-jar', type=Path, required=True)
p.add_argument('--ruffle-dir', type=Path, required=True)
p.set_defaults(func=prepare)
p = sub.add_parser('mc', help='Copy source to a local runtime, prepare an isolated world, run Minecraft')
default_runtime = str(Path(os.environ.get('LOCALAPPDATA', str(Path.home()))) / 'DadNMeBridgeExample')
p.add_argument('--runtime', default=default_runtime)
p.add_argument('--prepare-only', action='store_true')
p.set_defaults(func=runtime)
p = sub.add_parser('web', help='Serve on 127.0.0.1:18125 in this terminal')
p.set_defaults(func=lambda args: call([sys.executable, ROOT / 'serve.py']))
p = sub.add_parser('check', help='Check sources without launching services')
p.add_argument('--require-assets', action='store_true')
p.set_defaults(func=check)
if __name__ == '__main__':
    try:
        args = parser.parse_args(); args.func(args)
    except (OSError, RuntimeError, subprocess.CalledProcessError) as exc:
        parser.exit(1, f'ERROR: {exc}\n')
