"""Load local Push keys without shell evaluation; preserve existing DB environment settings."""
from pathlib import Path
import os
import subprocess
import sys

environment = os.environ.copy()
allowed = {'PUSH_ENABLED', 'VAPID_PUBLIC_KEY', 'VAPID_PRIVATE_KEY', 'VAPID_SUBJECT'}
for line in Path('.env.push.local').read_text().splitlines():
    if not line.strip() or line.startswith('#'):
        continue
    key, value = line.split('=', 1)
    if key not in allowed:
        raise SystemExit('허용되지 않은 환경변수 항목입니다.')
    environment[key] = value
command = sys.argv[1:] or ['./gradlew', 'bootRun', '--args=--spring.profiles.active=dev']
raise SystemExit(subprocess.run(command, env=environment).returncode)
