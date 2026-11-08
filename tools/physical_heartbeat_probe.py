#!/usr/bin/env python3
"""Send the physical firmware contract without hardware. Stop the real sender first."""
import argparse
import json
import time
import urllib.error
import urllib.parse
import urllib.request


def main():
    parser = argparse.ArgumentParser(description='실제 기기 API로 1초마다 센서 입력을 보냅니다.')
    parser.add_argument('--base-url', default='http://localhost:8080')
    parser.add_argument('--code', default='NYANG-0001')
    parser.add_argument('--state', choices=['study', 'away', 'rest'], default='study')
    parser.add_argument('--seconds', type=int, default=15)
    args = parser.parse_args()
    base = args.base_url.rstrip('/')
    parsed = urllib.parse.urlparse(base)
    if parsed.scheme != 'http' or not parsed.hostname or any(ord(c) < 32 for c in base):
        parser.error('--base-url에 http://주소:8080 형식으로 입력하세요.')
    if not 1 <= args.seconds <= 3600:
        parser.error('--seconds는 1~3600 사이여야 합니다.')
    payloads = {
        'study': {'phone': True, 'seat': True, 'distanceCm': 42},
        'away': {'phone': True, 'seat': False, 'distanceCm': 999},
        'rest': {'phone': False, 'seat': True, 'distanceCm': 42},
    }
    url = base + '/api/devices/' + urllib.parse.quote(args.code, safe='') + '/heartbeat'
    body = json.dumps(payloads[args.state]).encode()
    print('기기 등록을 먼저 완료하세요. 같은 코드의 실제 기기/다른 테스트 송신기는 꺼두세요.')
    print(url)
    for number in range(args.seconds):
        start = time.monotonic()
        request = urllib.request.Request(url, data=body, method='POST',
                                         headers={'Content-Type': 'application/json'})
        try:
            with urllib.request.urlopen(request, timeout=1.5) as response:
                data = json.load(response)
            elapsed = (time.monotonic() - start) * 1000
            if set(data) != {'face', 'landPercent', 'remainMinutes', 'event'}:
                raise SystemExit('명세와 다른 응답 키: ' + str(list(data)))
            print(f'{number + 1:03d} {elapsed:.0f}ms {json.dumps(data, ensure_ascii=False)}', flush=True)
        except urllib.error.HTTPError as error:
            raise SystemExit(f'HTTP {error.code}: {error.read().decode(errors="replace")}') from None
        except (urllib.error.URLError, TimeoutError) as error:
            raise SystemExit(f'연결 실패 또는 1.5초 타임아웃: {error}') from None
        if number + 1 < args.seconds:
            time.sleep(max(0, 1 - (time.monotonic() - start)))
    print('송신 종료. 이후 입력이 없으면 마지막 입력 10초 뒤 OFFLINE으로 전환됩니다.')


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print('\n송신 중지')
