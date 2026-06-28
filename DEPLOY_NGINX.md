# Nginx + Docker Compose 배포 메모

## 1. 애플리케이션 빌드

```bash
./gradlew build
```

## 2. HTTP 상태로 우선 기동

```bash
docker compose up -d --build
```

브라우저에서 `http://ceos-jobdri.store`가 열리면 프록시 연결은 정상입니다.

## 3. Certbot으로 인증서 발급

DNS가 서버를 바라보고 있고 80 포트가 열려 있어야 합니다.

```bash
mkdir -p certbot/www certbot/conf
docker run --rm \
  -v "$(pwd)/certbot/www:/var/www/certbot" \
  -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
  certbot/certbot certonly --webroot \
  -w /var/www/certbot \
  -d ceos-jobdri.store \
  -d www.ceos-jobdri.store
```

## 4. HTTPS 설정 적용

`nginx/conf.d/app-ssl.conf.example` 내용을 `nginx/conf.d/app.conf`에 반영한 뒤 재기동합니다.

```bash
docker compose restart nginx
```

이후 `https://ceos-jobdri.store`로 접속할 수 있습니다.
