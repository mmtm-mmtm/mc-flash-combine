from http.server import ThreadingHTTPServer,SimpleHTTPRequestHandler
from pathlib import Path
import os
os.chdir(Path(__file__).parent/'web')
ThreadingHTTPServer(('127.0.0.1',18125),SimpleHTTPRequestHandler).serve_forever()
