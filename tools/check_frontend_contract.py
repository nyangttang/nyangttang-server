"""Run only against a disposable local DB. Creates users/rooms; does not delete data.
API_BASE_URL defaults to http://127.0.0.1:18081. Set TEST_DEVICE_KEY for NYANG-TEST.
"""
import json, os, urllib.request, urllib.error, urllib.parse
BASE = os.getenv('API_BASE_URL', 'http://127.0.0.1:18081').rstrip('/')
assert urllib.parse.urlparse(BASE).hostname in ('localhost', '127.0.0.1'), 'Local test server only'

def call(path, body=None, token=None, key=None, expected=200):
    headers={'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    if key: headers['X-Device-Key']=key
    req=urllib.request.Request(BASE+path, data=json.dumps(body).encode() if body is not None else None, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=15) as response: status=response.status; result=json.load(response)
    except urllib.error.HTTPError as error: status=error.code; result=json.load(error)
    assert status==expected, f'{path}: expected {expected}, got {status}'
    return result.get('data', result)

def user(name): return call('/api/v1/users',{'nickname':name},expected=201)['accessToken']

def create(token, capacity=2, color='TRICOLOR'):
    return call('/api/v1/rooms',{'name':'연동테스트','mode':'NORMAL','catName':'삼냥','catColor':color,'dailyGoalSeconds':3600,'capacity':capacity},token,expected=201)

host=user('호스트'); room=create(host)
assert room['capacity']==2 and room['catColor']=='TRICOLOR'
assert call('/api/v1/users/me/room',token=host)['capacity']==2
print('PASS: TRICOLOR creation and capacity persistence')
friend=user('친구')
joined=call('/api/v1/rooms/join',{'inviteCode':room['inviteCode'],'catName':'친구','catColor':'WHITE','dailyGoalSeconds':3600},friend,expected=201)
assert joined['capacity']==2
extra=user('정원검증')
call('/api/v1/rooms/join',{'inviteCode':room['inviteCode'],'catName':'추가','catColor':'GRAY','dailyGoalSeconds':3600},extra,expected=409)
print('PASS: configured two-member capacity rejects a third member')
for capacity in (0,5):
    call('/api/v1/rooms',{'name':'잘못된정원','mode':'NORMAL','catName':'냥','catColor':'WHITE','dailyGoalSeconds':3600,'capacity':capacity},extra,expected=400)
print('PASS: invalid capacities rejected')
for path in ('snapshot','ranking','devices/me'):
    call(f"/api/v1/rooms/{room['roomId']}/{path}",token=extra,expected=403)
print('PASS: room reads retain membership authorization')
legacy=call('/api/v1/rooms',{'name':'기존요청','mode':'NORMAL','catName':'냥','catColor':'CHEESE','dailyGoalSeconds':3600},extra,expected=201)
assert legacy['capacity']==4
print('PASS: older request without capacity defaults to four')
key=os.getenv('TEST_DEVICE_KEY')
if key:
    path=f"/api/v1/dev/rooms/{room['roomId']}/physical-device"
    call(path,{'deviceCode':'NYANG-TEST'},host,expected=401)
    call(path,{'deviceCode':'NYANG-TEST'},host,key)
    heartbeat='/api/devices/NYANG-TEST/heartbeat'
    call(heartbeat,{'phone':True,'seat':True,'distanceCm':42},expected=401)
    call(heartbeat,{'phone':True,'seat':True,'distanceCm':42},key=key)
    device=call(f"/api/v1/rooms/{room['roomId']}/devices/me",token=host)
    assert device['phonePresent'] is True and device['personPresent'] is True and device['connected'] is True
    call(heartbeat,{'phone':False,'seat':False,'distanceCm':999},key=key)
    device=call(f"/api/v1/rooms/{room['roomId']}/devices/me",token=host)
    assert device['phonePresent'] is False and device['personPresent'] is False
    print('PASS: device authentication retained; sensor true/false values reflect heartbeat')
print('All requested integration checks passed.')
