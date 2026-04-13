# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 1. 프로젝트 개요

**DevLog** — 미니 블로그와 개발 일기를 겸하는 개인용 풀스택 웹 애플리케이션.

- 공개 블로그 영역: 포스트 작성·조회·태그·검색 등 일반 블로그 기능
- 개발 일기 영역: 비공개/내부용 TIL·회고·작업 로그
- 학습 목적: Next.js + Spring Boot 풀스택 구성을 직접 스캐폴딩하고 운영하면서 실제 서비스 흐름을 익힌다.

## 2. 기술 스택

| 영역 | 기술 |
|------|------|
| Frontend | Next.js (App Router 전제) + TypeScript + Tailwind CSS |
| Backend | Spring Boot + Spring Data JPA + MySQL |
| Auth | Spring Security + JWT (access/refresh 분리 권장) |
| Build | Frontend는 `npm`/`pnpm`, Backend는 Gradle (`./gradlew`) |

## 3. 디렉토리 구조

루트에 프론트/백엔드를 서브 디렉토리로 분리한다.

```
devlog/
├── frontend/          # Next.js + TS + Tailwind
│   ├── app/           # App Router 라우트
│   ├── components/    # 재사용 UI 컴포넌트
│   ├── lib/           # API 클라이언트, 유틸
│   ├── styles/        # Tailwind 글로벌 스타일
│   └── package.json
├── backend/           # Spring Boot + JPA + MySQL
│   ├── src/main/java/com/devlog/
│   │   ├── domain/       # JPA 엔티티, 값객체
│   │   ├── repository/   # Spring Data JPA 리포지토리
│   │   ├── service/      # 비즈니스 로직
│   │   ├── controller/   # REST 컨트롤러
│   │   ├── security/     # Spring Security + JWT
│   │   └── config/       # 설정 클래스
│   ├── src/main/resources/
│   ├── src/test/java/    # 단위/통합 테스트
│   └── build.gradle
├── CLAUDE.md
└── README.md
```

신규 코드 추가 시 항상 위 경계를 먼저 확인하고, 교차 경계 변경은 사용자와 합의한 뒤 진행한다.

## 4. 커밋 컨벤션

[Conventional Commits](https://www.conventionalcommits.org/) 형식을 사용한다. 허용되는 타입:

| 타입 | 용도 |
|------|------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `test` | 테스트 코드 추가/수정 |
| `refactor` | 동작 변경 없는 코드 구조 개선 |
| `docs` | 문서 (README, CLAUDE.md 등) |
| `chore` | 빌드/설정/도구 등 기타 잡무 |

**형식**: `<type>(<scope>): <subject>`
- `scope`는 선택 (예: `feat(auth): JWT 리프레시 토큰 추가`).
- subject는 한국어/영어 모두 가능하나, 프로젝트 내에서 일관성 유지.
- 본문(body)이 필요하면 한 줄 띄우고 "왜" 중심으로 작성.

## 5. 브랜치 전략

| 브랜치 | 용도 |
|--------|------|
| `main` | 배포용. **직접 push 금지.** PR을 통해서만 머지. |
| `feature/<기능명>` | 새 기능 개발 (예: `feature/post-write`, `feature/jwt-auth`) |
| `fix/<버그명>` | 버그 수정 (예: `fix/login-redirect`) |

브랜치명은 kebab-case. 작업 시작 전 `main` 최신화 후 분기한다:

```bash
git switch main && git pull
git switch -c feature/<기능명>
```

## 6. 코딩 규칙

### 네이밍 컨벤션

| 대상 | 규칙 | 예시 |
|------|------|------|
| TypeScript 변수/함수 | `camelCase` | `getUserPosts` |
| TypeScript 타입/컴포넌트 | `PascalCase` | `PostCard`, `UserDto` |
| TypeScript 상수 | `UPPER_SNAKE_CASE` | `MAX_POST_LENGTH` |
| Next.js 파일 | App Router 규약 (`page.tsx`, `layout.tsx`, `route.ts`) | — |
| Java 클래스 | `PascalCase` | `PostService` |
| Java 메서드/필드 | `camelCase` | `findByAuthorId` |
| Java 상수 | `UPPER_SNAKE_CASE` | `DEFAULT_PAGE_SIZE` |
| JPA 엔티티 | 단수형 명사 | `Post`, `User`, `Tag` |
| REST 경로 | kebab-case + 복수 리소스 | `/api/posts`, `/api/dev-notes` |
| DB 테이블/컬럼 | `snake_case` | `post_id`, `created_at` |

### 패키지 구조 (backend)

계층형 패키지를 기본으로 하되, 도메인이 커지면 **도메인별 패키지**로 재구성한다.

```
com.devlog
├── domain        # 엔티티, 값객체 (프레임워크 의존 최소화)
├── repository    # Spring Data JPA 인터페이스
├── service       # 트랜잭션 경계, 유스케이스
├── controller    # REST 컨트롤러, 요청/응답 DTO
├── security      # SecurityConfig, JwtFilter, JwtProvider
├── config        # 기타 @Configuration
└── common        # 공통 예외, 응답 래퍼, 상수
```

- Controller는 도메인 로직을 직접 호출하지 않고 Service를 경유한다.
- Repository는 Service에서만 호출한다 (Controller에서 직접 호출 금지).
- DTO는 Controller 계층의 요청/응답에 국한하고, 엔티티를 그대로 반환하지 않는다.

### 프론트엔드 규칙

- 서버 컴포넌트 우선. 상호작용이 필요한 부분만 `"use client"`.
- API 호출은 `frontend/lib/api/` 하위 모듈에 모아두고 컴포넌트에서 직접 `fetch` 하지 않는다.
- Tailwind는 유틸리티 우선. 반복되는 조합은 컴포넌트로 추출하거나 `cn()` 유틸을 사용한다.

## 7. TDD 규칙

1. **Red** — 실패하는 테스트를 먼저 작성한다.
2. **Green** — 테스트를 통과하는 최소 구현을 작성한다.
3. **Refactor** — 테스트가 통과하는 상태에서 구조를 다듬는다.
4. **Commit** — 테스트가 모두 통과한 상태에서만 커밋한다.

- Backend 테스트는 `./gradlew test`로 실행. 단일 테스트는 `./gradlew test --tests <FQCN>`.
- Frontend 테스트는 `npm test` (러너는 스캐폴딩 시 확정). 단일 테스트는 러너별 패턴 옵션 사용.
- 본 프로젝트의 Stop 훅이 `./gradlew test`를 실행하므로, 백엔드 테스트가 깨진 상태에서는 자동 커밋이 차단된다.

## 8. 금지 사항

- ❌ **main 브랜치 직접 push 금지** — 반드시 feature/fix 브랜치에서 PR을 통해 머지한다.
- ❌ **테스트 실패 상태로 커밋 금지** — red 상태의 커밋은 이력을 어지럽히고 이분 탐색을 망가뜨린다.
- ❌ **엔티티 직접 반환 금지** — 컨트롤러 응답에는 DTO를 사용한다 (순환 참조/민감 필드 노출 방지).
- ❌ **시크릿 커밋 금지** — `application.yml`의 DB 비밀번호·JWT 시크릿은 환경 변수나 `application-local.yml`(gitignore) 사용.
- ❌ **`any` 남용 금지** — TypeScript에서 `any`는 정당한 사유가 없으면 사용하지 않는다.

## 9. 개발 순서

각 단계는 별도의 `feature/<기능명>` 브랜치에서 작업하고, 완료 시 PR을 통해 `main`으로 머지한다. 단계 번호는 권장 진행 순서이며, 백엔드 1~3단계가 끝난 뒤 프론트엔드 4~6단계로 넘어간다.

### 백엔드 개발 순서

#### 1단계 — `feature/user-auth`
User 엔티티 → UserRepository → JWT 설정 → Spring Security → 회원가입 API → 로그인 API → `@test-writer` 테스트 → `@backend-reviewer` 리뷰 → PR

#### 2단계 — `feature/post-crud`
Post/Tag 엔티티 → Repository → Service → Controller → `@test-writer` 테스트 → `@backend-reviewer` 리뷰 → PR

#### 3단계 — `feature/devnote-crud`
DevNote 엔티티 → CRUD API → `@test-writer` 테스트 → `@backend-reviewer` 리뷰 → PR

### 프론트엔드 개발 순서

#### 4단계 — `feature/frontend-auth`
로그인/회원가입 페이지 → API 연동 → `@frontend-reviewer` 리뷰 → `/web-design-reviewer` → PR

#### 5단계 — `feature/frontend-post`
포스트 목록/상세/작성 페이지 → API 연동 → 리뷰 → PR

#### 6단계 — `feature/frontend-devnote`
TIL 페이지 → API 연동 → 리뷰 → PR

### 개발 사이클 (매 기능마다 반복)

```
코드 작성
  → @backend-reviewer 리뷰
  → 수정
  → @test-writer 테스트 작성
  → 테스트 통과
  → 자동 commit + push
  → PR 생성
```

> 새 기능 코드 작성이 완료되면 별도 지시 없이 자동으로
> `@backend-reviewer` 리뷰 → 🔴🟡 항목 수정 → `@test-writer` 테스트 작성
> 순서로 진행해줘. 명시적으로 중단 요청이 없으면 끝까지 진행할 것.

---

## 자동화 메모 (`.claude/settings.json`)

이 레포에는 프로젝트 전용 훅이 설치되어 있다. 작업 중 다음 동작이 자동 발생한다:

- **PreToolUse (Bash)**: `rm -rf` 등 위험 삭제 패턴 차단.
- **PostToolUse (Edit|Write|MultiEdit)**: 파일 수정 후 자동 `git add -A`.
- **Stop**:
  1. `./gradlew test` 실행 (gradlew 존재 시). 실패 시 `"테스트 실패 - 커밋이 차단되었습니다"` 출력 후 커밋 건너뜀.
  2. 현재 브랜치가 `main`이면 커밋·푸시 건너뛰고 경고 출력.
  3. 위 게이트 통과 시 파일명 기반 `chore:` 메시지로 자동 커밋 + push + 비프음.

자동 커밋은 `chore:` 고정이므로, `feat`/`fix`/`test`/`refactor` 등의 의미 있는 커밋이 필요하면 대화 종료 **전에** 직접 `git commit -m`으로 먼저 커밋해 둘 것.

### 기능 개발 완료 시 종료 절차

기능 개발이 완료되면 대화 종료 **전에** 반드시 아래 순서로 진행한다:

1. **테스트 모두 통과 확인** (`./gradlew test`)
2. **의미 있는 커밋 메시지로 직접 커밋**
   ```bash
   git commit -m "feat(auth): add User entity with BCrypt and Bean Validation"
   ```
3. **현재 브랜치로 push**
   ```bash
   git push origin <현재브랜치명>
   ```
4. 그다음 대화 종료 (Stop 훅은 커밋할 것이 없으므로 자동으로 스킵됨)
