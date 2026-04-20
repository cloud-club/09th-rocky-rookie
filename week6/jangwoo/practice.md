# Week 6 — 셸 스크립트 및 자동화 실습

이 문서는 `week6.md`에 정리된 학습 내용을 바탕으로 직접 실습한 내용을 정리한 것이다.

---

## 실습 1: 시스템 상태 리포트 스크립트

여러 시스템 정보(호스트, CPU, 메모리, 디스크, 서비스 상태, 로그인 이력)를 한 번에 수집하여
텍스트 파일로 저장하는 스크립트이다.
변수, 명령어 치환(`$()`), 그룹 명령(`{ }`)과 출력 리다이렉션(`>`)의 조합을 실습한다.

### 스크립트 (`system_report.sh`)

```bash
#!/bin/bash
# system_report.sh — 시스템 상태 요약 리포트
# 용도 : 호스트·CPU·메모리·디스크·서비스·로그인 정보를 한 파일로 수집

# ── 변수 설정 ──────────────────────────────────────────────
# 리포트 파일 경로를 날짜+시각 기반으로 생성 (실행마다 고유 파일 생성)
REPORT_FILE="/tmp/system_report_$(date +%Y%m%d_%H%M%S).txt"

# ── 리포트 생성 ────────────────────────────────────────────
# { } 그룹 명령 : 중괄호 안의 모든 stdout을 > 로 한꺼번에 파일에 기록
{
    echo "========================================="
    echo "  시스템 상태 리포트"
    echo "  생성 시각: $(date)"           # $(date) : 현재 날짜·시각 삽입
    echo "========================================="
    echo ""

    # ▸ 호스트 정보
    echo "--- 호스트 정보 ---"
    echo "호스트명: $(hostname)"          # hostname  : 시스템 호스트명
    echo "커널 버전: $(uname -r)"         # uname -r  : 커널 릴리스 버전
    echo "가동 시간: $(uptime -p)"        # uptime -p : 가동 시간 (사람 친화적 형태)
    echo ""

    # ▸ CPU 로드
    echo "--- CPU 로드 ---"
    # /proc/loadavg : 1분·5분·15분 평균 부하값이 담긴 가상 파일
    # awk '{print $1, $2, $3}' : 처음 3개 필드만 출력
    echo "Load Average: $(cat /proc/loadavg | awk '{print $1, $2, $3}')"
    echo "CPU 코어 수: $(nproc)"          # nproc : 사용 가능한 CPU 코어 수
    echo ""

    # ▸ 메모리 사용
    echo "--- 메모리 사용 ---"
    free -h                               # free -h : 메모리 사용량 (G, M 단위)
    echo ""

    # ▸ 디스크 사용
    echo "--- 디스크 사용 ---"
    df -h /                               # df -h / : 루트(/) 파티션의 디스크 사용량
    echo ""

    # ▸ 실패한 서비스
    echo "--- 실패한 서비스 ---"
    systemctl --failed --no-pager         # 실패 상태인 systemd 유닛 목록
    echo ""

    # ▸ 최근 로그인 사용자
    echo "--- 최근 로그인 사용자 ---"
    last -5                               # last -5 : 최근 5건의 로그인 이력
} > "$REPORT_FILE"                        # 그룹 명령 전체 출력을 파일로 리다이렉션

echo "리포트가 생성되었습니다: $REPORT_FILE"
```

### 실행

```bash
jangwoojung@localhost:~/Documents/week6_practice$ vim system_report.sh

# 실행 권한 부여
jangwoojung@localhost:~/Documents/week6_practice$ chmod +x system_report.sh

# bash -n : 실행 없이 구문(문법) 오류만 검사 — 오류 없으면 출력 없음
jangwoojung@localhost:~/Documents/week6_practice$ bash -n system_report.sh

# 스크립트 실행
jangwoojung@localhost:~/Documents/week6_practice$ ./system_report.sh
리포트가 생성되었습니다: /tmp/system_report_20260412_121341.txt
```

### 출력 확인

```text
=========================================
  시스템 상태 리포트
  생성 시각: Sun Apr 12 12:13:41 PM KST 2026
=========================================

--- 호스트 정보 ---
호스트명: localhost.localdomain
커널 버전: 6.12.0-124.40.1.el10_1.x86_64
가동 시간: up 4 weeks, 5 days, 11 hours, 47 minutes

--- CPU 로드 ---
Load Average: 1.22 0.83 0.66
CPU 코어 수: 8

--- 메모리 사용 ---
               total        used        free      shared  buff/cache   available
Mem:            15Gi       4.6Gi       8.7Gi       715Mi       2.7Gi        10Gi
Swap:          7.7Gi          0B       7.7Gi

--- 디스크 사용 ---
Filesystem           Size  Used Avail Use% Mounted on
/dev/mapper/rl-root   70G  5.9G   65G   9% /

--- 실패한 서비스 ---
  UNIT                  LOAD   ACTIVE SUB    DESCRIPTION
● dnf-makecache.service loaded failed failed dnf makecache

--- 최근 로그인 사용자 ---
jangwooj tty2         tty2             Tue Mar 10 00:26   still logged in
jangwooj seat0        login screen     Tue Mar 10 00:26   still logged in
reboot   system boot  6.12.0-124.40.1. Tue Mar 10 00:25   still running
reboot   system boot  6.12.0-124.8.1.e Tue Mar 10 00:23 - 00:25  (00:02)
jangwooj tty2         tty2             Tue Mar 10 00:19 - down   (00:03)

wtmp begins Mon Mar  9 15:13:07 2026
```

---

## 실습 2: 로그 파일 자동 백업 스크립트

`/var/log`의 주요 로그 파일(`messages`, `secure`, `cron`)을 날짜별 `tar.gz`로 압축 백업하고,
30일 이상 지난 오래된 백업은 자동으로 삭제하는 스크립트이다.
조건문(`if`), 종료 코드 검사(`$?`), `find`의 `-mtime`/`-delete` 옵션을 활용한다.

### 스크립트 (`log_backup.sh`)

```bash
#!/bin/bash
# log_backup.sh — 로그 파일 자동 백업
# 용도 : /var/log 하위 주요 로그를 날짜별 tar.gz 로 백업, 오래된 백업 자동 정리

# ── 변수 설정 ──────────────────────────────────────────────
LOG_DIR="/var/log"                                              # 백업 대상 디렉터리
BACKUP_DIR="/home/jangwoojung/Documents/week6_practice/backup"  # 백업 저장 디렉터리
DATE=$(date +%Y%m%d)                                            # 오늘 날짜 (YYYYMMDD)
RETENTION_DAYS=30                                               # 백업 보관 기간 (일)

# ── 백업 디렉터리 확인 ─────────────────────────────────────
# -d : 디렉터리 존재 여부 확인 / -p : 상위 디렉터리까지 재귀 생성
if [ ! -d "$BACKUP_DIR" ]; then
    mkdir -p "$BACKUP_DIR"
    echo "백업 디렉터리를 생성했습니다: $BACKUP_DIR"
fi

# ── 로그 파일 압축 백업 ────────────────────────────────────
# -c : 아카이브 생성  -z : gzip 압축  -f : 출력 파일 지정
# -C "$LOG_DIR" : 해당 디렉터리로 이동 후 상대 경로로 파일 추가
# 2>/dev/null : 존재하지 않는 파일에 대한 에러 메시지 숨김
ARCHIVE="${BACKUP_DIR}/logs_${DATE}.tar.gz"
tar -czf "$ARCHIVE" -C "$LOG_DIR" messages secure cron 2>/dev/null

# $? : 직전 명령어(tar)의 종료 코드 — 0 이면 성공
if [ $? -eq 0 ]; then
    echo "[성공] 백업 완료: $ARCHIVE"
else
    echo "[실패] 백업 중 오류 발생" >&2   # >&2 : stderr 로 출력
    exit 1                                # 비정상 종료 (종료 코드 1)
fi

# ── 오래된 백업 정리 ───────────────────────────────────────
# -name  : 백업 파일 패턴 매칭
# -mtime +30 : 수정 시간이 30일 이전인 파일
# -delete : 조건에 맞는 파일 삭제
# -print  : 삭제된 파일 경로 출력 → wc -l 로 개수 카운트
DELETED=$(find "$BACKUP_DIR" -name "logs_*.tar.gz" -mtime +${RETENTION_DAYS} -delete -print | wc -l)
echo "${DELETED}개의 오래된 백업을 삭제했습니다."
```

### 실행

```bash
jangwoojung@localhost:~/Documents/week6_practice$ vim log_backup.sh
jangwoojung@localhost:~/Documents/week6_practice$ chmod +x log_backup.sh

# /var/log 의 로그 파일은 root 소유이므로 sudo 로 실행
jangwoojung@localhost:~/Documents/week6_practice$ sudo bash ~/Documents/week6_practice/log_backup.sh
백업 디렉터리를 생성했습니다: /home/jangwoojung/Documents/week6_practice/backup
[성공] 백업 완료: /home/jangwoojung/Documents/week6_practice/backup/logs_20260412.tar.gz
0개의 오래된 백업을 삭제했습니다.
```

### 출력 확인

```bash
# 백업 디렉터리에 tar.gz 파일 생성 확인
jangwoojung@localhost:~/Documents/week6_practice/backup$ ls -la
total 220
drwxr-xr-x. 2 root        root            34 Apr 12 14:43 .
drwxr-xr-x. 3 jangwoojung jangwoojung     65 Apr 12 14:43 ..
-rw-r--r--. 1 root        root        223953 Apr 12 14:43 logs_20260412.tar.gz

# 아카이브 내용 확인 — messages, secure, cron 세 파일이 포함됨
jangwoojung@localhost:~/Documents/week6_practice/backup$ tar -tvf logs_20260412.tar.gz
-rw------- root/root   1809694 2026-04-12 14:43 messages
-rw------- root/root     85615 2026-04-12 14:43 secure
-rw------- root/root      6874 2026-04-06 03:01 cron
```

---

## 실습 3: 다중 서비스 상태 점검 스크립트

핵심 서비스(`sshd`, `crond`, `firewalld`, `rsyslog`)의 동작 상태를 점검하고,
비활성 서비스가 발견되면 자동으로 재시작을 시도하는 스크립트이다.
배열, 함수, `for` 반복문, `systemctl`, `tee`를 활용한 로그 기록을 실습한다.

### 스크립트 (`service_check.sh`)

```bash
#!/bin/bash
# service_check.sh — 핵심 서비스 점검 및 자동 복구
# 용도 : 지정한 서비스 목록을 순회하며 상태 점검 → 비활성이면 재시작

# ── 변수 설정 ──────────────────────────────────────────────
SERVICES=("sshd" "crond" "firewalld" "rsyslog")   # 점검 대상 (Bash 배열)
LOG="/var/log/service_check.log"                   # 점검 로그 파일 경로

# ── 로깅 함수 ──────────────────────────────────────────────
# tee -a "$LOG" : stdout 에 출력 + 파일에 이어쓰기(append) 동시 수행
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG"
}

# ── 서비스 점검 루프 ──────────────────────────────────────
log "===== 서비스 점검 시작 ====="

# "${SERVICES[@]}" : 배열의 모든 요소를 개별 문자열로 반복
for svc in "${SERVICES[@]}"; do

    # systemctl is-active --quiet : 출력 없이 종료 코드로만 상태 반환
    if systemctl is-active --quiet "$svc"; then
        log "[OK] $svc: 정상 동작 중"
    else
        log "[WARN] $svc: 비활성 상태 → 재시작 시도"
        systemctl start "$svc"

        # 재시작 후 다시 상태 확인
        if systemctl is-active --quiet "$svc"; then
            log "[OK] $svc: 재시작 성공"
        else
            log "[FAIL] $svc: 재시작 실패 — 수동 점검 필요"
        fi
    fi
done

log "===== 서비스 점검 완료 ====="
```

### 실행 — 1차 (모든 서비스 정상)

```bash
jangwoojung@localhost:~/Documents/week6_practice$ chmod +x service_check.sh
jangwoojung@localhost:~/Documents/week6_practice$ sudo bash service_check.sh
[2026-04-12 15:01:35] ===== 서비스 점검 시작 =====
[2026-04-12 15:01:35] [OK] sshd: 정상 동작 중
[2026-04-12 15:01:35] [OK] crond: 정상 동작 중
[2026-04-12 15:01:35] [OK] firewalld: 정상 동작 중
[2026-04-12 15:01:35] [OK] rsyslog: 정상 동작 중
[2026-04-12 15:01:35] ===== 서비스 점검 완료 =====
```

### 실행 — 장애 시뮬레이션 후 2차

```bash
# rsyslog, crond 를 의도적으로 중지하여 장애 상황 재현
jangwoojung@localhost:~/Documents/week6_practice$ sudo systemctl stop rsyslog
jangwoojung@localhost:~/Documents/week6_practice$ sudo systemctl stop crond

# 스크립트 재실행 → 비활성 서비스 자동 복구 확인
jangwoojung@localhost:~/Documents/week6_practice$ sudo bash service_check.sh
[2026-04-12 15:04:11] ===== 서비스 점검 시작 =====
[2026-04-12 15:04:11] [OK] sshd: 정상 동작 중
[2026-04-12 15:04:11] [WARN] crond: 비활성 상태 → 재시작 시도
[2026-04-12 15:04:11] [OK] crond: 재시작 성공
[2026-04-12 15:04:11] [OK] firewalld: 정상 동작 중
[2026-04-12 15:04:11] [WARN] rsyslog: 비활성 상태 → 재시작 시도
[2026-04-12 15:04:11] [OK] rsyslog: 재시작 성공
[2026-04-12 15:04:11] ===== 서비스 점검 완료 =====
```

### cron 자동화 등록

서비스 점검 스크립트를 cron에 등록하여 주기적으로 자동 실행되도록 설정하였다.

```bash
jangwoojung@localhost:~/Documents/week6_practice$ sudo crontab -e
crontab: installing new crontab
```

등록한 크론잡:

```bash
# 매 1분마다 서비스 점검 스크립트 실행
# 필드 순서 : 분  시  일  월  요일  명령어
# */1 : 매 1분마다 (테스트 목적 — 실무에서는 0 * * * * 등으로 주기를 넓힘)
*/1 * * * * /bin/bash /home/jangwoojung/Documents/week6_practice/service_check.sh
```

### 출력 확인 — cron 자동 실행 로그

```text
[2026-04-12 15:16:58] ===== 서비스 점검 시작 =====
[2026-04-12 15:16:58] [OK] sshd: 정상 동작 중
[2026-04-12 15:16:58] [WARN] crond: 비활성 상태 → 재시작 시도
[2026-04-12 15:16:58] [OK] crond: 재시작 성공
[2026-04-12 15:16:58] [OK] firewalld: 정상 동작 중
[2026-04-12 15:16:58] [OK] rsyslog: 정상 동작 중
[2026-04-12 15:16:58] ===== 서비스 점검 완료 =====
[2026-04-12 15:17:01] ===== 서비스 점검 시작 =====
[2026-04-12 15:17:01] [OK] sshd: 정상 동작 중
[2026-04-12 15:17:01] [OK] crond: 정상 동작 중
[2026-04-12 15:17:02] [OK] firewalld: 정상 동작 중
[2026-04-12 15:17:02] [OK] rsyslog: 정상 동작 중
[2026-04-12 15:17:02] ===== 서비스 점검 완료 =====
```

---

## 실습 4: Podman 컨테이너 점검 및 자동 복구

Podman으로 관리되는 컨테이너의 상태를 점검하고, 중지(`exited`)된 컨테이너는 자동 재시작,
`unhealthy` 상태의 컨테이너는 `restart`하는 스크립트이다.
Podman CLI, `--filter`, Go 템플릿 `--format`, `grep`·`awk` 파이프 조합을 실습한다.

### 1) Podman 버전 확인

```bash
jangwoojung@localhost:~/Documents/week6_practice$ podman --version
podman version 5.6.0
```

### 2) 테스트용 nginx 컨테이너 실행

```bash
# -d          : detached 모드 (백그라운드 실행)
# --name      : 컨테이너에 이름 부여
# -p 8080:80  : 호스트 8080 → 컨테이너 80 포트 매핑
jangwoojung@localhost:~/Documents/week6_practice$ podman run -d --name test-nginx -p 8080:80 docker.io/library/nginx
06eafaa90a06621b236ef6e74683e736f2b9cf6d1d1267d8cdd5f09bf9449f00

jangwoojung@localhost:~/Documents/week6_practice$ podman ps
CONTAINER ID  IMAGE                           COMMAND               CREATED         STATUS         PORTS                 NAMES
06eafaa90a06  docker.io/library/nginx:latest  nginx -g daemon o...  23 seconds ago  Up 23 seconds  0.0.0.0:8080->80/tcp  test-nginx
```

### 3) 의도적으로 중지 (복구 스크립트 검증용)

```bash
jangwoojung@localhost:~/Documents/week6_practice$ podman stop test-nginx
test-nginx

# 실행 중인 컨테이너 목록이 비어 있음
jangwoojung@localhost:~/Documents/week6_practice$ podman ps
CONTAINER ID  IMAGE       COMMAND     CREATED     STATUS      PORTS       NAMES
```

### 4) 스크립트 (`podman_health_check.sh`)

```bash
#!/bin/bash
# podman_health_check.sh — Podman 컨테이너 상태 점검 및 자동 복구
# 용도 : 중지(exited) 컨테이너 재시작 + unhealthy 컨테이너 재시작

# ── 변수 설정 ──────────────────────────────────────────────
LOG="/home/jangwoojung/Documents/week6_practice/podman_health.log"

# ── 로깅 함수 ──────────────────────────────────────────────
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG"
}

log "===== Podman 상태 점검 시작 ====="

# ── 중지된 컨테이너 재시작 ─────────────────────────────────
# podman ps -a                   : 모든 컨테이너(실행 + 중지) 표시
# --filter "status=exited"       : 종료(중지)된 컨테이너만 필터링
# --format "{{.Names}}"          : Go 템플릿으로 컨테이너 이름만 추출
STOPPED=$(podman ps -a --filter "status=exited" --format "{{.Names}}")

for c in $STOPPED; do
    log "[WARN] 컨테이너 중지됨: $c → 재시작 시도"
    podman start "$c"

    # $? : 직전 명령(podman start)의 종료 코드 확인
    if [ $? -eq 0 ]; then
        log "[OK] $c 재시작 성공"
    else
        log "[FAIL] $c 재시작 실패"
    fi
done

# ── unhealthy 컨테이너 재시작 ──────────────────────────────
# grep -i unhealthy : 상태에 'unhealthy' 가 포함된 행만 필터링
# awk '{print $1}'  : 컨테이너 이름만 추출
# || true           : grep 매칭 없을 때 종료 코드 1 → set -e 환경 대비
UNHEALTHY=$(podman ps --format "{{.Names}} {{.Status}}" | grep -i unhealthy | awk '{print $1}' || true)

for c in $UNHEALTHY; do
    [ -z "$c" ] && continue       # 빈 문자열이면 건너뛰기
    log "[WARN] $c unhealthy → 재시작"
    podman restart "$c"            # restart : 중지 + 시작을 한 번에 수행
done

# (옵션) 정리는 나중에 따로 실행 추천
# log "[INFO] Podman 정리 작업 시작"
# podman container prune -f       # 중지된 컨테이너만 일괄 삭제

log "===== Podman 상태 점검 완료 ====="
```

> **`podman system prune -f`** 는 미사용 이미지·네트워크·캐시까지 삭제할 수 있으므로,
> 학습 단계에서는 `podman container prune -f` 처럼 범위를 좁히는 편이 안전하다.

### 5) 실행 및 출력 확인

```bash
jangwoojung@localhost:~/Documents/week6_practice$ chmod +x podman_health_check.sh

# 스크립트 실행 — 중지된 test-nginx 컨테이너가 자동으로 재시작됨
jangwoojung@localhost:~/Documents/week6_practice$ bash podman_health_check.sh
[2026-04-13 04:09:44] ===== Podman 상태 점검 시작 =====
[2026-04-13 04:09:44] [WARN] 컨테이너 중지됨: test-nginx → 재시작 시도
test-nginx
[2026-04-13 04:09:44] [OK] test-nginx 재시작 성공
[2026-04-13 04:09:44] ===== Podman 상태 점검 완료 =====

# 복구 확인 — test-nginx 가 다시 실행 중
jangwoojung@localhost:~/Documents/week6_practice$ podman ps
CONTAINER ID  IMAGE                           COMMAND               CREATED         STATUS         PORTS                 NAMES
06eafaa90a06  docker.io/library/nginx:latest  nginx -g daemon o...  18 minutes ago  Up 55 seconds  0.0.0.0:8080->80/tcp  test-nginx
```