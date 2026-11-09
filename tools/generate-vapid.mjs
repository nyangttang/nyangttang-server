// Run locally: node tools/generate-vapid.mjs --subject mailto:YOUR_REAL_EMAIL
import { generateKeyPairSync } from 'node:crypto';
import { writeFileSync } from 'node:fs';
const subject = process.argv[process.argv.indexOf('--subject') + 1];
if (!process.argv.includes('--subject') || !/^(mailto:[^\s@]+@[^\s@]+|https:\/\/[^\s]+)$/.test(subject || '')) {
  throw new Error('--subject 뒤에 실제 연락 가능한 mailto:이메일 또는 HTTPS 주소를 입력하세요.');
}
const { publicKey, privateKey } = generateKeyPairSync('ec', { namedCurve: 'prime256v1' });
const pub = publicKey.export({ format: 'jwk' });
const priv = privateKey.export({ format: 'jwk' });
const raw = Buffer.concat([Buffer.from([4]), Buffer.from(pub.x, 'base64url'), Buffer.from(pub.y, 'base64url')]).toString('base64url');
writeFileSync('.env.push.local', `PUSH_ENABLED=true\nVAPID_PUBLIC_KEY=${raw}\nVAPID_PRIVATE_KEY=${priv.d}\nVAPID_SUBJECT=${subject}\n`, { mode: 0o600, flag: 'wx' });
console.log('.env.push.local 생성 완료. 기존 파일은 덮어쓰지 않습니다. 이 파일은 Git에 올리지 마세요.');
console.log('Spring Boot는 이 파일을 자동 로드하지 않습니다. 실행 환경변수로 설정하세요.');
