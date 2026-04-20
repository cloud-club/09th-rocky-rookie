# RHEL 10 기반 Bash 셸 스크립트: 변수, 입출력 제어, 조건·반복문을 활용한 자동화 스크립트 작성 가이드

RHEL 10 환경에서 시스템 관리와 자동화 작업을 수행하기 위해서는 스크립트의 뼈대를 이루는 변수, 입출력 및 파이프 처리, 그리고 논리적 흐름을 제어하는 조건문과 반복문에 대한 깊이 있는 이해가 필수적입니다. 이 자료는 각 개념의 상세한 작동 원리와 이를 종합한 기초 스크립트 작성법을 다룹니다.

---

## 1. 변수와 환경 변수의 활용 (Variables & Environment Variables)

Bash 셸에서 변수는 데이터를 메모리에 임시로 저장하고 스크립트 실행 중 동적으로 값을 재사용하게 해주는 핵심 요소입니다. Bash의 변수는 기본적으로 데이터 타입(문자열, 정수 등)을 명시적으로 선언할 필요가 없는 유연성을 가집니다.

### 사용자 정의 변수 선언과 호출

변수를 선언할 때는 **공백 없이** `변수명=값`의 형태로 작성해야 합니다. 변수의 값을 호출할 때는 이름 앞에 `$` 기호를 붙입니다.

```bash
MY_VAR="hello"
echo $MY_VAR    # hello 출력
```

### 변수의 범위와 환경 변수

| 종류 | 특징 |
|---|---|
| **스크립트 전역 변수** | 스크립트 최상위에서 선언된 변수는 스크립트 전체 범위에서 유효한 **전역 변수**입니다. 단, `export`하지 않으면 자식 프로세스에는 전달되지 않습니다. |
| **지역 변수** | 함수 내부에서 `local` 키워드로 선언된 변수는 해당 함수 실행 중에만 존재합니다. |
| **환경 변수** | `export` 명령으로 승격된 변수는 자식 프로세스(서브셸 등)에서도 참조할 수 있습니다. |

```bash
MY_VAR="local scope"       # 스크립트 내 전역 변수
export MY_VAR              # 환경 변수로 승격 → 자식 프로세스에서도 접근 가능
```

RHEL 시스템이 기본으로 제공하는 주요 환경 변수:

| 변수 | 설명 |
|---|---|
| `$PATH` | 실행 파일 탐색 경로 |
| `$USER` | 현재 로그인한 사용자 이름 |
| `$HOME` | 현재 사용자의 홈 디렉터리 경로 |

### 특수 변수와 위치 매개변수 (Special & Positional Parameters)

스크립트를 실행할 때 명령줄에서 전달받는 인자(Argument)들은 특수 변수에 자동으로 할당되어 스크립트의 유연성을 극대화합니다.

| 변수 | 설명 |
|---|---|
| `$0` | 실행 중인 스크립트의 이름 |
| `$1` ~ `$9` | 첫 번째부터 아홉 번째까지 전달된 인수 (10 이상은 `${10}`) |
| `$#` | 스크립트에 전달된 총 인수의 개수 |
| `"$@"` | 전달된 모든 인수를 **각각 별개의 단어**로 확장. 인수 내 공백을 보존하므로 권장 |
| `"$*"` | 전달된 모든 인수를 **IFS(기본값: 공백)로 연결한 하나의 문자열**로 확장 |
| `$?` | 가장 마지막으로 실행된 명령어의 종료 상태 코드. 성공 시 `0`, 실패 시 `1`~`255` |
| `$$` | 현재 셸의 프로세스 ID(PID). 임시 파일 생성 시 고유한 이름을 부여할 때 유용 |

> **`"$@"` vs `"$*"` 핵심 차이:**
> `for arg in "$@"` 는 인수가 3개면 루프를 3번 돕니다.
> `for arg in "$*"` 는 전체를 하나의 문자열로 묶어 루프를 1번만 돕니다.
> 실무에서는 인수 내 공백을 안전하게 처리하는 **`"$@"`** 를 사용합니다.

---

## 2. 데이터 흐름 제어: 표준 입출력, 리다이렉션, 파이프

리눅스 시스템의 모든 프로세스는 데이터를 주고받기 위해 3개의 기본 스트림(파일 디스크립터)을 사용합니다.

### 표준 스트림

| 스트림 | FD | 설명 |
|---|:---:|---|
| 표준 입력 (stdin) | `0` | 키보드 등으로부터 프로그램에 전달되는 입력 |
| 표준 출력 (stdout) | `1` | 명령어가 정상 실행된 결과 출력 |
| 표준 에러 (stderr) | `2` | 명령어 실행 중 발생한 오류 및 진단 메시지 |

### 리다이렉션 (Redirection)

명령어의 결과를 화면(터미널)이 아닌 파일로 보내거나, 파일의 내용을 명령어의 입력으로 전달합니다.

| 연산자 | 동작 |
|---|---|
| `>` | 표준 출력을 파일로 저장. 기존 파일이 있다면 **덮어씀** |
| `>>` | 기존 파일 내용을 유지한 채 파일 끝에 **추가(Append)** |
| `<` | 명령어의 입력을 키보드가 아닌 **특정 파일**에서 읽어옴 |
| `2>` | **에러 메시지(stderr)만** 특정 파일로 저장 |
| `2>&1` 또는 `&>` | stderr를 stdout이 향하는 곳으로 **병합**하여 정상 출력과 에러를 하나의 파일에 기록 |

### `/dev/null`

리눅스의 '블랙홀'과 같은 특수 파일입니다. 출력을 완전히 버리고 싶을 때 사용합니다.

```bash
command > /dev/null 2>&1    # 정상 출력과 에러 메시지를 모두 버림
```

### 파이프 (`|`)

하나의 명령어에서 나온 표준 출력(stdout)을 다른 명령어의 표준 입력(stdin)으로 직접 연결합니다. 임시 파일 없이 명령어를 조합해 강력한 데이터 필터링 파이프라인을 구성할 수 있습니다.

```bash
ls -l | grep "txt"
```

---

## 3. 논리적 흐름 제어: 조건문 (If, Elif, Else, Case)

명령어의 성공 여부나 변수의 값을 평가하여 스크립트가 상황에 맞는 판단을 내리게 합니다.

### `if`, `elif`, `else` 구조

테스트 조건이 참(True)일 때 특정 명령을 실행합니다.

```bash
if [[ 조건 ]]; then
    # 참일 때 실행
elif [[ 다른_조건 ]]; then
    # elif 조건이 참일 때 실행
else
    # 모두 거짓일 때 실행
fi
```

### 주요 테스트 연산자

**파일 검사:**

| 연산자 | 설명 |
|---|---|
| `-f 파일명` | 일반 파일 존재 여부 |
| `-d 파일명` | 디렉터리 존재 여부 |
| `-e 파일명` | 파일·디렉터리 관계없이 존재 여부 |

**문자열 검사:**

| 연산자 | 설명 |
|---|---|
| `문자열1 == 문자열2` | 두 문자열이 같음 |
| `문자열1 != 문자열2` | 두 문자열이 다름 |
| `-z 문자열` | 문자열 길이가 0(비어 있음) |

**숫자 비교:**

| 연산자 | 설명 |
|---|---|
| `-eq` | 같음 (equal) |
| `-ne` | 다름 (not equal) |
| `-gt` | 초과 (greater than) |
| `-lt` | 미만 (less than) |
| `-ge` | 이상 (greater or equal) |
| `-le` | 이하 (less or equal) |

### 다중 패턴 매칭 (`case` 문)

하나의 변수를 여러 가지 특정 패턴과 비교할 때 사용합니다. 복잡한 `if-else` 체인을 간결하게 만들어 주며, 사용자 메뉴를 만들거나 입력된 옵션 인자를 처리할 때 효율적입니다.

```bash
case "$1" in
    start)  echo "서비스 시작" ;;
    stop)   echo "서비스 중지" ;;
    status) echo "상태 확인"  ;;
    *)      echo "사용법: $0 {start|stop|status}" ;;
esac
```

---

## 4. 반복 작업 자동화: 반복문 (For, While, Until)

동일한 작업을 조건이 만족될 때까지, 또는 지정된 목록만큼 반복 수행합니다.

### `for` 문

숫자 범위, 파일 목록, 텍스트 문자열 등 정해진 리스트의 항목을 하나씩 순회하며 작업을 수행합니다.

```bash
# 숫자 범위 순회
for i in {1..5}; do
    echo "반복 $i"
done

# 디렉터리 내 파일 순회
for FILE in /etc/*.conf; do
    echo "$FILE"
done
```

### `while` 문

주어진 조건이 **참(True)인 동안** 루프 안의 명령어를 계속해서 반복 실행합니다. 로그 파일을 읽거나, 특정 프로세스가 실행 중인 동안 대기하는 스크립트에 주로 사용됩니다.

```bash
COUNT=0
while [[ $COUNT -lt 5 ]]; do
    echo "count: $COUNT"
    ((COUNT++))
done
```

### `until` 문

`while`과 정반대로, 주어진 조건이 **거짓(False)인 동안** 실행되며 조건이 참(True)이 되면 반복문을 종료합니다.

```bash
COUNT=0
until [[ $COUNT -ge 5 ]]; do
    echo "count: $COUNT"
    ((COUNT++))
done
```

### 루프 제어 (`break`와 `continue`)

| 키워드 | 동작 |
|---|---|
| `break` | 어떤 조건이 충족되었을 때 전체 반복문을 **즉시 강제 종료** |
| `continue` | 현재 반복 주기를 즉시 건너뛰고, **다음 주기**로 바로 넘어감 |

---

## 5. 종합: 기초 자동화 스크립트 작성 실습

지금까지 배운 개념들을 모두 조합하여, 인자로 전달받은 디렉터리를 백업하고 작업 로그를 남기는 실전 기초 스크립트를 작성해 보겠습니다.

```bash
#!/bin/bash
set -uo pipefail

# 1. 스크립트 실행 시 디렉터리 경로를 인자로 받음 ($1 활용)
TARGET_DIR=$1
BACKUP_DIR="/var/backups"
LOG_FILE="/var/log/backup_job.log"

# 2. 인자 전달 여부 확인 및 조건문 활용 (-z 연산자)
if [[ -z "$TARGET_DIR" ]]; then
    echo "사용법: $0 [백업할_디렉터리_경로]"
    exit 1  # 스크립트 비정상 종료
fi

# 3. 대상이 디렉터리인지 확인 (-d 연산자)
if [[ ! -d "$TARGET_DIR" ]]; then
    echo "오류: '$TARGET_DIR'은(는) 유효한 디렉터리가 아닙니다."
    exit 2
fi

# 4. 백업 디렉터리 생성 (이미 존재해도 오류 없이 넘어감, 에러는 /dev/null로 버림)
mkdir -p "$BACKUP_DIR" 2>/dev/null

echo "===== 백업 작업 시작: $(date) =====" >> "$LOG_FILE"

# 5. for 루프를 사용해 대상 디렉터리 내의 파일 순회
for FILE in "$TARGET_DIR"/*; do
    # 일반 파일인 경우에만 백업 진행 (-f 연산자)
    if [[ -f "$FILE" ]]; then
        # cp는 성공 시 stdout에 아무것도 출력하지 않음
        # 2>>"$LOG_FILE"로 에러 발생 시에만 로그에 기록
        cp "$FILE" "$BACKUP_DIR/" 2>>"$LOG_FILE"

        # 6. 종료 상태 확인 ($? 활용)
        if [[ $? -eq 0 ]]; then
            echo "[성공] $FILE 백업 완료" >> "$LOG_FILE"
        else
            echo "[실패] $FILE 백업 중 오류 발생" >> "$LOG_FILE"
        fi
    fi
done

echo "===== 백업 작업 종료 =====" >> "$LOG_FILE"
echo "작업이 완료되었습니다. 로그는 $LOG_FILE 을 확인하세요."
exit 0  # 스크립트 정상 종료
```

### 학습 스크립트 분석 요약

| 개념 | 활용 내용 |
|---|---|
| **변수** | `TARGET_DIR`, `BACKUP_DIR`, `LOG_FILE`로 경로를 변수화하여 재사용성과 가독성 확보 |
| **조건문 & 특수 변수** | `$1`이 비어있는지(`-z`), 디렉터리가 존재하는지(`-d`) 검사하여 안전성 확보. `$?`로 복사 성공/실패 여부 판단 |
| **반복문** | `for` 루프로 디렉터리 내부를 순회하며 개별 파일 단위로 작업 제어 |
| **리다이렉션** | `cp`는 성공 시 stdout 출력이 없으므로 `2>>"$LOG_FILE"`로 에러만 로그에 기록. `>>`로 누적 Append하여 후속 감사 및 디버깅 가능 |

---

## 6. 심화 실습: Blue-Green 무중단 배포 스크립트 (`deploy.sh`)

기초 개념을 넘어, 실제 운영 환경에서 사용하는 **Blue-Green 무중단 배포(Zero-Downtime Deployment)** 자동화 스크립트입니다. 지금까지 배운 변수, 조건문, 반복문, 리다이렉션이 실전에서 어떻게 조합되는지 확인할 수 있습니다.

### Blue-Green 배포 전략이란?

두 개의 동일한 환경(Blue, Green)을 유지하고, 새 버전을 현재 트래픽을 받지 않는 환경에 먼저 배포한 뒤 헬스체크가 성공하면 nginx가 트래픽을 전환하는 방식입니다. 롤백이 즉각적이고 서비스 중단이 없다는 것이 핵심 장점입니다.

```
[사용자] → [Nginx] → [Blue 또는 Green 컨테이너]
                          ↕ 배포 시 전환
```

### 전체 스크립트

```bash
#!/bin/bash

# 환경 변수 설정
HOST_NGINX_CONF_DIR=/home/ubuntu/nginx/conf.d
NGINX_DEFAULT_CONF="${HOST_NGINX_CONF_DIR}/default.conf"
SERVER_PUBLIC_IP=138.2.121.208
DOCKER_CONTAINER_NAME_PREFIX=springboot
INTERNAL_PORT=8080
EXTERNAL_PORT_GREEN=8081
EXTERNAL_PORT_BLUE=8080
DOCKER_IMAGE_NAME=hanseunghyun0615/campus-be:latest
DOCKER_COMPOSE_FILE="docker-compose.yaml"

# 네트워크 확인 및 생성
if ! docker network ls | grep -q campus; then
  echo "Creating the campus-network Docker network..."
  docker network create campus
else
  echo "Docker network 'campus' already exists."
fi

# nginx 컨테이너가 정상 작동하는지 확인
IS_NGINX_RUNNING=$(docker compose -f $DOCKER_COMPOSE_FILE ps -q nginx)
if [ -z "$IS_NGINX_RUNNING" ]; then
  echo "Nginx container is not running. Starting nginx container..."
  docker compose -f $DOCKER_COMPOSE_FILE up -d nginx
  sleep 3
else
  echo "Nginx container is already running."
fi

# 현재 실행 중인 컨테이너 확인
IS_BLUE_RUNNING=$(docker compose -f $DOCKER_COMPOSE_FILE ps -q ${DOCKER_CONTAINER_NAME_PREFIX}-blue)

if [ -n "$IS_BLUE_RUNNING" ]; then
  echo "Switching to green environment..."

  docker compose -f $DOCKER_COMPOSE_FILE rm -f ${DOCKER_CONTAINER_NAME_PREFIX}-green
  docker compose -f $DOCKER_COMPOSE_FILE up -d ${DOCKER_CONTAINER_NAME_PREFIX}-green

  BEFORE_COLOR=blue
  AFTER_COLOR=green
  EXTERNAL_PORT=${EXTERNAL_PORT_GREEN}
else
  echo "Switching to blue environment..."

  docker compose -f $DOCKER_COMPOSE_FILE rm -f ${DOCKER_CONTAINER_NAME_PREFIX}-blue
  docker compose -f $DOCKER_COMPOSE_FILE up -d ${DOCKER_CONTAINER_NAME_PREFIX}-blue

  BEFORE_COLOR=green
  AFTER_COLOR=blue
  EXTERNAL_PORT=${EXTERNAL_PORT_BLUE}
fi

sleep 3

# Health Check
echo "Starting health check..."

TARGET="${DOCKER_CONTAINER_NAME_PREFIX}-${AFTER_COLOR}:${INTERNAL_PORT}"
MAX_TRIES=60   # 최대 ~3분 대기 (앱 기동 느릴 때 대비)
SLEEP_SECS=3

for i in $(seq 1 ${MAX_TRIES}); do
  # nginx 컨테이너에서 같은 네트워크로 직접 헬스체크
  OUT=$(docker compose -f "$DOCKER_COMPOSE_FILE" exec -T nginx sh -lc \
    "curl -m 3 -sS -w ' HTTP_CODE:%{http_code}' http://${TARGET}/actuator/health" 2>&1 || true)

  echo "[try ${i}] ${OUT}"

  # 1) HTTP 200 확인
  echo "${OUT}" | grep -q 'HTTP_CODE:200' || { sleep ${SLEEP_SECS}; continue; }

  # 2) 바디에 "status":"UP" 있는지 확인
  if echo "${OUT}" | grep -q '"status":"UP"'; then
    echo "> Health check successful!"
    sed -i -E "/upstream[[:space:]]+api[[:space:]]*\\{/,/\\}/ s|^[[:space:]]*server[[:space:]].*;|        server ${DOCKER_CONTAINER_NAME_PREFIX}-${AFTER_COLOR}:${INTERNAL_PORT};|" "$NGINX_DEFAULT_CONF"

    docker compose -f $DOCKER_COMPOSE_FILE exec -T nginx nginx -t
    docker compose -f $DOCKER_COMPOSE_FILE exec -T nginx nginx -s reload

    sleep 5

    docker compose -f $DOCKER_COMPOSE_FILE stop ${DOCKER_CONTAINER_NAME_PREFIX}-${BEFORE_COLOR}
    docker compose -f $DOCKER_COMPOSE_FILE rm -f ${DOCKER_CONTAINER_NAME_PREFIX}-${BEFORE_COLOR}
    echo "${BEFORE_COLOR} environment is down."

    docker image prune -a -f

    echo "Deployment successful!"
    exit 0
  fi
done

# 헬스체크 실패 시 롤백
echo "Health check failed. Rolling back..."
docker compose -f $DOCKER_COMPOSE_FILE stop ${DOCKER_CONTAINER_NAME_PREFIX}-${AFTER_COLOR}
docker compose -f $DOCKER_COMPOSE_FILE rm -f ${DOCKER_CONTAINER_NAME_PREFIX}-${AFTER_COLOR}
echo "The previous container remains active."
exit 1
```

---

### 스크립트 심화 개념 분석

#### ① 명령어 치환 `$()` — 컨테이너 실행 여부를 변수에 저장

```bash
IS_NGINX_RUNNING=$(docker compose -f $DOCKER_COMPOSE_FILE ps -q nginx)
IS_BLUE_RUNNING=$(docker compose -f $DOCKER_COMPOSE_FILE ps -q ${DOCKER_CONTAINER_NAME_PREFIX}-blue)
```

`$(명령어)` 형태의 **명령어 치환(Command Substitution)** 은 괄호 안 명령어를 실행하고 그 **표준 출력(stdout) 결과를 문자열로 변환**하여 변수에 저장합니다. `docker compose ps -q`는 실행 중인 컨테이너의 ID를 출력하고, 실행 중이지 않으면 아무것도 출력하지 않습니다. 이 특성을 활용해 컨테이너 실행 여부를 판별합니다.

#### ② `-z` / `-n` 문자열 조건 — 빈 문자열로 존재 여부 판별

```bash
if [ -z "$IS_NGINX_RUNNING" ]; then   # 비어 있으면 → 실행 중 아님
  ...
fi

if [ -n "$IS_BLUE_RUNNING" ]; then    # 값이 있으면 → Blue가 실행 중
  ...
fi
```

| 연산자 | 의미 | 이 스크립트에서의 의미 |
|---|---|---|
| `-z "$VAR"` | 문자열 길이가 **0**(비어 있음) | 컨테이너 ID가 없음 → **미실행** |
| `-n "$VAR"` | 문자열 길이가 **0이 아님** | 컨테이너 ID가 존재 → **실행 중** |

`$(docker ps -q ...)` 의 출력 유무를 `-z` / `-n` 으로 판별하는 것이 핵심 패턴입니다.

#### ③ 파이프 + `grep -q` — 종료 코드 기반 존재 확인

```bash
if ! docker network ls | grep -q campus; then
```

- `docker network ls` 의 stdout을 파이프(`|`)로 `grep`에 전달합니다.
- `grep -q` 는 **Quiet 모드**로, 화면에 아무것도 출력하지 않고 패턴 발견 시 종료 코드 `0`, 미발견 시 `1`을 반환합니다.
- `!` 는 종료 코드를 반전시켜, **네트워크가 없을 때만** `if` 블록이 실행됩니다.

출력 결과가 아니라 **종료 코드** 자체를 조건으로 활용하는 전형적인 패턴입니다.

#### ④ `for` + `$(seq ...)` — 헬스체크 최대 시도 횟수 제어

```bash
MAX_TRIES=60
SLEEP_SECS=3

for i in $(seq 1 ${MAX_TRIES}); do
  ...
  sleep ${SLEEP_SECS}
done
```

`seq 1 60` 은 `1 2 3 ... 60` 의 숫자 목록을 stdout으로 출력합니다. `$()` 로 이를 캡처하여 `for` 루프의 순회 목록으로 사용합니다. 최대 60회 × 3초 = **약 3분** 동안 헬스체크를 시도하며, 성공하면 루프 내부에서 `exit 0`으로 스크립트를 즉시 종료합니다.

> `{1..60}` 과 `$(seq 1 60)` 은 결과가 동일하지만, 변수를 범위에 사용할 때(`{1..$MAX}`)는 Bash가 중괄호 확장을 변수보다 먼저 처리하므로 동작하지 않습니다. 변수를 범위로 쓸 때는 반드시 `$(seq 1 $MAX_TRIES)` 를 사용해야 합니다.

#### ⑤ `2>&1 || true` — 헬스체크 실패 시 스크립트 중단 방지

```bash
OUT=$(docker compose ... exec -T nginx sh -lc \
  "curl -m 3 -sS -w ' HTTP_CODE:%{http_code}' http://${TARGET}/actuator/health" 2>&1 || true)
```

두 가지 테크닉이 조합되어 있습니다.

| 테크닉 | 역할 |
|---|---|
| `2>&1` | curl의 stderr(연결 오류 메시지 등)를 stdout으로 합쳐 `$()` 가 모두 캡처하게 함 |
| `\|\| true` | curl이 실패(타임아웃, 연결 거부 등)해서 종료 코드가 `0`이 아니더라도 `true`(종료 코드 `0`)로 대체하여 스크립트가 중단되지 않게 함 |

`set -e` 환경이나 파이프라인에서 중간 명령이 실패해도 루프를 계속 돌려야 할 때 `|| true` 패턴을 사용합니다.

#### ⑥ `sed -i -E` + 범위 주소 지정 — nginx 설정 파일 자동 수정

```bash
sed -i -E "/upstream[[:space:]]+api[[:space:]]*\\{/,/\\}/ \
  s|^[[:space:]]*server[[:space:]].*;|        server ${DOCKER_CONTAINER_NAME_PREFIX}-${AFTER_COLOR}:${INTERNAL_PORT};|" \
  "$NGINX_DEFAULT_CONF"
```

가장 복잡한 부분입니다. 각 요소를 분해하면:

| 요소 | 의미 |
|---|---|
| `-i` | 파일을 직접 수정(in-place). 별도 임시 파일 없이 원본을 덮어씀 |
| `-E` | 확장 정규식(ERE) 사용. `\+`, `\{` 대신 `+`, `{` 사용 가능 |
| `/패턴A/,/패턴B/` | **범위 주소 지정**: 패턴A가 있는 줄부터 패턴B가 있는 줄까지의 범위에만 `s` 명령 적용 |
| `s\|기존\|신규\|` | 구분자를 `/` 대신 `\|`로 사용 (치환 내용에 `/`가 포함될 때 충돌 방지) |
| `[[:space:]]` | POSIX 문자 클래스 — 공백, 탭 등 모든 공백 문자 매칭 |

결과적으로 nginx 설정 파일 안의 `upstream api { ... }` 블록 내부에서 `server ...;` 줄만 찾아 새 컨테이너 주소로 교체합니다.

#### ⑦ `exit 0` / `exit 1` — 종료 코드로 배포 결과 전달

```bash
echo "Deployment successful!"
exit 0   # CI/CD 파이프라인에게 성공을 알림

# ...

echo "Health check failed. Rolling back..."
exit 1   # CI/CD 파이프라인에게 실패를 알려 후속 알림·롤백 트리거
```

스크립트의 종료 코드는 이를 호출한 상위 프로세스(GitHub Actions, Jenkins 등 CI/CD 도구)가 `$?`로 수신합니다. `exit 0`이면 파이프라인이 다음 단계로 진행하고, `exit 1`이면 빌드를 실패 처리하여 Slack 알림 발송, 자동 롤백 등의 후속 처리를 트리거합니다.

---

### 전체 흐름 요약

```
스크립트 실행
    │
    ├─ Docker 네트워크 존재 확인 (파이프 + grep -q)
    │
    ├─ Nginx 컨테이너 실행 확인 ($() + -z)
    │
    ├─ Blue 실행 중? ($() + -n)
    │    ├─ Yes → Green 컨테이너 기동 (AFTER=green)
    │    └─ No  → Blue  컨테이너 기동 (AFTER=blue)
    │
    ├─ 헬스체크 루프 (for + seq, 최대 60회)
    │    ├─ curl 실행 (2>&1 || true)
    │    ├─ HTTP 200 확인 (grep -q)
    │    └─ "status":"UP" 확인 (grep -q)
    │         └─ 성공 시
    │              ├─ nginx 설정 수정 (sed -i -E)
    │              ├─ nginx reload
    │              ├─ 이전 컨테이너 종료
    │              └─ exit 0 ✅
    │
    └─ 60회 모두 실패 → 롤백 후 exit 1 ❌
```
