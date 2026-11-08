import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const source = await readFile(new URL('../examples/roomSocket.js', import.meta.url), 'utf8');
const { connectRoomSocket } = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'));
const token = 'a'.repeat(43);
function fixture(run) {
  const old = { WebSocket: globalThis.WebSocket, setTimeout, clearTimeout, setInterval, clearInterval, now: Date.now, random: Math.random };
  let clock = 0, nextId = 0;
  const timers = new Map(), sockets = [], statuses = [], errors = [], snapshots = [];
  class FakeSocket {
    static OPEN = 1; static CONNECTING = 0;
    constructor(url) { this.url = url; this.readyState = 0; this.sent = []; sockets.push(this); }
    open() { this.readyState = 1; this.onopen?.(); }
    send(text) { this.sent.push(JSON.parse(text)); }
    receive(frame) { this.onmessage?.({ data: JSON.stringify(frame) }); }
    close(code = 1000) { this.readyState = 3; this.onclose?.({ code }); }
  }
  globalThis.WebSocket = FakeSocket;
  globalThis.setTimeout = (fn, ms) => { const id = ++nextId; timers.set(id, { fn, ms, interval: false }); return id; };
  globalThis.setInterval = (fn, ms) => { const id = ++nextId; timers.set(id, { fn, ms, interval: true }); return id; };
  globalThis.clearTimeout = globalThis.clearInterval = id => timers.delete(id);
  Date.now = () => clock; Math.random = () => 0;
  const options = { serverUrl: 'http://localhost:8080', roomId: '1', getAccessToken: () => token,
    onSnapshot: d => snapshots.push(d), onStatus: d => statuses.push(d), onError: e => errors.push(e.message) };
  const emit = (socket, seq, version = 1) => socket.receive({ type: 'SNAPSHOT', streamSeq: seq, data: { roomId: '1', myMemberId: '7', snapshotVersion: version } });
  const fire = ms => { const entry = [...timers.entries()].find(([, t]) => t.ms === ms); assert.ok(entry, `timer ${ms}`);
    const [id, t] = entry; if (!t.interval) timers.delete(id); clock += ms; t.fn(); };
  try { run({ options, sockets, statuses, errors, snapshots, timers, emit, fire }); }
  finally { Object.assign(globalThis, { WebSocket: old.WebSocket, setTimeout: old.setTimeout, clearTimeout: old.clearTimeout, setInterval: old.setInterval, clearInterval: old.clearInterval }); Date.now = old.now; Math.random = old.random; }
}

test('auth uses first message and no token is placed in URL', () => fixture(f => {
  const stop = connectRoomSocket(f.options); const ws = f.sockets[0];
  assert.equal(ws.url, 'ws://localhost:8080/ws/rooms/1'); assert.equal(ws.sent.length, 0); ws.open();
  assert.deepEqual(ws.sent[0], { type: 'AUTH', accessToken: token });
  ws.receive({ type: 'AUTH_REQUIRED', authTimeoutSeconds: 5 }); assert.equal(f.snapshots.length, 0);
  f.emit(ws, 1); assert.equal(f.statuses.at(-1).state, 'connected'); stop(); assert.equal(f.timers.size, 0);
}));
test('reconnect resets stream sequence and accepts full latest snapshot', () => fixture(f => {
  const stop = connectRoomSocket(f.options); let ws = f.sockets[0]; ws.open(); f.emit(ws, 3, 5); ws.close(1006);
  assert.equal(f.statuses.at(-1).state, 'reconnecting'); f.fire(1000); ws = f.sockets[1]; ws.open(); f.emit(ws, 1, 6);
  assert.equal(f.snapshots.at(-1).snapshotVersion, 6); stop();
}));
test('expired auth and denied rooms stop rather than retry', () => fixture(f => {
  for (const code of [4401, 4403, 4404]) {
    const stop = connectRoomSocket(f.options); f.sockets.at(-1).close(code);
    assert.equal(f.statuses.at(-1).state, 'error'); assert.equal(f.timers.size, 0); stop();
  }
}));
test('stop cancels reconnect and ignores stale socket messages', () => fixture(f => {
  const stop = connectRoomSocket(f.options); const ws = f.sockets[0]; ws.open(); ws.close(1006); stop();
  assert.equal(f.timers.size, 0); f.emit(ws, 1); assert.equal(f.snapshots.length, 0);
}));
test('ping is periodic and a silent server triggers reconnect', () => fixture(f => {
  const stop = connectRoomSocket(f.options); const ws = f.sockets[0]; ws.open(); f.emit(ws, 1);
  f.fire(15000); assert.deepEqual(ws.sent.at(-1), { type: 'PING' });
  f.fire(15000); f.fire(15000); assert.equal(f.statuses.at(-1).state, 'reconnecting'); stop();
}));
test('malformed or wrong-room data is never passed to the map', () => fixture(f => {
  const stop = connectRoomSocket(f.options); const ws = f.sockets[0]; ws.open();
  ws.receive({ type: 'SNAPSHOT', streamSeq: 1, data: { roomId: '2', myMemberId: '7' } });
  assert.equal(f.snapshots.length, 0); assert.equal(f.statuses.at(-1).state, 'error'); assert.equal(f.timers.size, 0); stop();
}));
test('missing token does not connect and https becomes wss', () => fixture(f => {
  connectRoomSocket({ ...f.options, getAccessToken: () => null }); assert.equal(f.sockets.length, 0);
  const stop = connectRoomSocket({ ...f.options, serverUrl: 'https://example.com' });
  assert.equal(f.sockets[0].url, 'wss://example.com/ws/rooms/1'); stop();
}));
test('retry budget is bounded and duplicate sequence is ignored', () => fixture(f => {
  const stop = connectRoomSocket(f.options); const first = f.sockets[0]; first.open(); f.emit(first, 1); f.emit(first, 1);
  assert.equal(f.snapshots.length, 1);
  for (const delay of [1000, 2000, 4000, 8000, 16000, 30000]) { f.sockets.at(-1).close(1006); f.fire(delay); }
  f.sockets.at(-1).close(1006); assert.equal(f.statuses.at(-1).state, 'error'); assert.equal(f.timers.size, 0); stop();
}));
