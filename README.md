# Backend
CEOS 23기 프론트엔드-백엔드 7주차 세션 과제 백엔드 repository

## Deployment configuration

배포 환경에서는 `.env.example`을 참고해 EC2의 `~/.env` 또는 배포 플랫폼 환경변수에 값을 설정해야 합니다.

- `JWT_SECRET`은 필수입니다.
- `CORS_ALLOWED_ORIGINS`에는 배포된 프론트엔드 origin을 포함해야 합니다.
- 운영 데이터 보존이 필요하면 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`로 영속 DB를 설정해야 합니다.
- 관리자 초기 계정은 `ADMIN_SEED_ENABLED=true`일 때만 생성됩니다.
