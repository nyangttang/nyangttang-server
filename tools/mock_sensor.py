import getpass
import json
import threading
import urllib.request
import urllib.error


def main():
    base = input('서버 주소 [http://localhost:8080]: ').strip() or 'http://localhost:8080'
    room = input('참여한 방 번호: ').strip()
    if not room.isdigit():
        raise SystemExit('방 번호는 숫자로 입력해주세요.')
    token = getpass.getpass('이 방 참가자의 토큰 (입력 내용은 숨김): ').strip()
    endpoint = base.rstrip('/') + '/api/v1/dev/rooms/' + room + '/mock-device'

    def post(path, body=None):
        request = urllib.request.Request(endpoint + path,
            data=json.dumps(body or {}).encode(), method='POST',
            headers={'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json'})
        try:
            with urllib.request.urlopen(request, timeout=5) as response:
                return json.load(response)['data']
        except urllib.error.HTTPError as e:
            raise RuntimeError(f'HTTP {e.code}: 서버 로그/프로필/토큰/참가 여부를 확인해주세요.') from None
        except urllib.error.URLError:
            raise RuntimeError('서버에 연결할 수 없습니다.') from None

    connected = post('/connect')
    link = connected['linkId']
    print('연결 완료. 유예 시간:', connected['awayGraceMs'] / 1000, '초')
    print('study=폰·사람 있음 / away=폰만 있음 / rest=폰 없음 / stop=입력 중단 / quit=종료')
    print('study·away·rest 상태에서는 1초마다 전송합니다. 시작은 입력 중단 상태입니다.')
    mode = ['stop']
    lock = threading.Lock()
    done = threading.Event()

    def worker():
        seq = -1
        previous = None
        while not done.is_set():
            with lock:
                current = mode[0]
            if current != 'stop':
                phone, person = {'study': (True, True), 'away': (True, False), 'rest': (False, True)}[current]
                seq += 1
                try:
                    result = post('/telemetry', {'linkId': link, 'seq': seq,
                        'phonePresent': phone, 'personPresent': person})
                    state = result['state']
                    if state != previous:
                        print('\n상태:', state, ' / seq:', seq, flush=True)
                        previous = state
                except RuntimeError as e:
                    print('\n전송 중단:', e, '서버 재시작 후에는 도구도 다시 실행해주세요.', flush=True)
                    with lock:
                        mode[0] = 'stop'
            done.wait(1)

    thread = threading.Thread(target=worker, daemon=True)
    thread.start()
    try:
        while True:
            command = input('명령> ').strip().lower()
            if command == 'quit':
                break
            if command in ('study', 'away', 'rest', 'stop'):
                with lock:
                    mode[0] = command
                if command == 'stop':
                    print('입력 중단. 약 10~11초 후 snapshot에서 OFFLINE을 확인하세요.')
            else:
                print('study / away / rest / stop / quit 중 입력해주세요.')
    except (KeyboardInterrupt, EOFError):
        pass
    finally:
        done.set()
        thread.join(timeout=6)


if __name__ == '__main__':
    try:
        main()
    except RuntimeError as error:
        raise SystemExit(str(error))
