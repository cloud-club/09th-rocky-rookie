# Week 6. 셸 스크립트 및 자동화 기초

## 1. 이번 주 학습 주제

- Bash 셸의 기본 구조 이해
- 변수와 환경 변수 활용
- 표준 입력·출력·리다이렉션과 파이프 처리
- 조건문과 반복문을 활용한 기초 스크립트 작성
- 간단한 관리 작업 자동화 실습

## 2. 실습 환경

- Host OS: Windows 11
- Virtualization: Hyper-V
- Guest OS: RHEL 8 (Red Hat Enterprise Linux 8) GUI 설치
- 사용자 계정: `ooo426`

---

# Part 1. Bash 셸의 기본 구조 + 변수와 환경 변수

## 3. 핵심 개념 정리

### 3-1. 셸(Shell)이란?

**셸(Shell)**은 사용자가 입력한 명령을 해석해 커널에 전달하는 **명령 해석기(Command Interpreter)**이다. 사용자와 운영체제(커널) 사이의 "통역사" 역할을 한다.

```
사용자 입력 (ls -l)
    ↓
 셸 (Bash)  ← 명령을 해석하고 실행 준비
    ↓
 커널       ← 실제 파일 시스템 접근, 결과 반환
    ↓
 출력
```

**대표적인 셸 종류**:

| 셸 | 경로 | 특징 |
|----|------|------|
| **bash** | `/bin/bash` | RHEL/CentOS 기본 셸, 가장 널리 사용 |
| **sh** | `/bin/sh` | POSIX 표준 셸 (호환성 목적) |
| **zsh** | `/bin/zsh` | 자동완성/테마 강화, macOS 기본 |
| **dash** | `/bin/dash` | 경량 POSIX 셸 (Ubuntu의 `/bin/sh`) |

```bash
# 현재 로그인 셸 확인
echo $SHELL

# 시스템에 설치된 셸 목록
cat /etc/shells
```

### 3-2. 셸 스크립트란?

**셸 스크립트(Shell Script)**는 여러 셸 명령어를 파일에 모아 순차 실행하는 **텍스트 파일**이다. 확장자는 보통 `.sh`를 사용한다.

```bash
#!/bin/bash             # Shebang: 이 스크립트를 bash로 실행하라는 지시
echo "Hello, Linux!"
```

> **Shebang(`#!`)**: 스크립트 **첫 줄**에 오는 특수 선언으로, 어떤 인터프리터로 실행할지 지정한다. `#!/bin/bash`는 "이 파일을 `/bin/bash`로 실행해 달라"는 의미이다.

### 3-3. 변수(Variable)

셸 변수는 값을 담는 **이름 있는 저장소**이다. 대소문자를 구분하며, `=` 주변에 공백을 두면 안 된다.

```bash
name="ilwoo"        # 변수 선언 (공백 없음!)
echo $name          # 변수 참조: $변수명
echo "${name}님"     # 중괄호로 경계 명확히
```

| 표기 | 의미 |
|------|------|
| `$name` | 변수 값 치환 |
| `"${name}"` | 경계를 명확히 해 주변 문자와 구분 |
| `$(명령)` | 명령 실행 결과를 값으로 치환 (명령 치환) |

```bash
today=$(date +%F)
echo "오늘은 ${today}"
```

### 3-4. 환경 변수(Environment Variable)

**환경 변수**는 셸과 자식 프로세스에까지 **상속되는 전역 변수**이다. 일반 변수는 현재 셸에서만 유효하지만, 환경 변수는 스크립트/서브 셸에서도 읽을 수 있다.

```bash
# 일반 변수 → 환경 변수로 승격
export MY_APP_HOME=/opt/myapp

# 현재 환경 변수 전체 보기
env

# 특정 환경 변수 확인
echo $PATH
echo $HOME
```

**주요 환경 변수**:

| 변수 | 의미 |
|------|------|
| `$HOME` | 사용자 홈 디렉터리 (`/home/ooo426`) |
| `$USER` | 로그인 사용자 이름 |
| `$PATH` | 명령어 검색 경로 (`:`로 구분) |
| `$PWD` | 현재 작업 디렉터리 |
| `$SHELL` | 로그인 셸 경로 |
| `$LANG` | 언어/로캘 설정 |

> **`$PATH`의 역할**: `ls`를 입력했을 때 셸은 `$PATH`에 나열된 디렉터리(`/usr/bin`, `/usr/local/bin` 등)를 **순서대로** 검색해서 실행 파일을 찾는다. 찾지 못하면 `command not found`가 출력된다.

### 3-5. 셸 초기화 파일

로그인 시 자동으로 실행되는 파일로, 환경 변수·alias·함수 등을 정의한다.

| 파일 | 적용 범위 | 실행 시점 |
|------|-----------|-----------|
| `/etc/profile` | 전체 사용자 | 로그인 셸 |
| `~/.bash_profile` | 해당 사용자 | 로그인 셸 |
| `~/.bashrc` | 해당 사용자 | 대화형 비로그인 셸(새 터미널 등) |

```bash
# .bashrc에 환경 변수와 alias 추가 예시
echo 'export MY_APP_HOME=/opt/myapp' >> ~/.bashrc
echo 'alias ll="ls -alF"' >> ~/.bashrc

# 수정 내용을 현재 셸에 즉시 반영
source ~/.bashrc
```

## 4. 실습: 변수와 환경 변수

```bash
# 1) 변수 선언과 사용
greeting="Hello"
name=$(whoami)
echo "${greeting}, ${name}!"

# 2) 환경 변수 확인
echo "HOME=$HOME"
echo "PATH=$PATH"

# 3) 환경 변수 일시 추가
export MY_VAR="rocky-rookie"
bash -c 'echo "자식 셸에서: $MY_VAR"'   # 환경 변수라 자식 셸에서 보임

# 4) 일반 변수는 자식 셸에 상속되지 않음
local_var="local"
bash -c 'echo "자식 셸에서: $local_var"'  # 빈 값 출력
```

---

# Part 2. 표준 입출력·리다이렉션과 파이프

## 5. 핵심 개념 정리

### 5-1. 표준 스트림(Standard Stream)

리눅스의 모든 프로세스는 기본으로 **3개의 I/O 통로**를 가진다. 각각 번호(파일 디스크립터)가 붙어 있다.

| 이름 | 번호(FD) | 기본 연결 | 설명 |
|------|----------|----------|------|
| **stdin** (표준 입력) | 0 | 키보드 | 프로세스에 들어오는 입력 |
| **stdout** (표준 출력) | 1 | 화면(터미널) | 정상 결과 출력 |
| **stderr** (표준 에러) | 2 | 화면(터미널) | 에러 메시지 출력 |

```
       ┌─────────┐
stdin  │         │  stdout
 ────▶ │ Process │ ─────▶
       │         │  stderr
       │         │ ─────▶
       └─────────┘
```

### 5-2. 리다이렉션(Redirection)

표준 스트림의 연결 대상을 **파일이나 다른 통로로 변경**하는 것이다.

| 기호 | 의미 | 예시 |
|------|------|------|
| `>` | stdout을 파일로 (덮어쓰기) | `ls > out.txt` |
| `>>` | stdout을 파일에 이어쓰기 | `date >> log.txt` |
| `<` | 파일을 stdin으로 | `wc -l < data.txt` |
| `2>` | stderr만 파일로 | `cmd 2> err.txt` |
| `2>&1` | stderr를 stdout과 합침 | `cmd > all.txt 2>&1` |
| `&>` | stdout+stderr를 함께 파일로 | `cmd &> all.txt` |

```bash
# 정상 결과와 에러를 분리해서 저장
find /etc -name "*.conf" > found.txt 2> error.txt

# 모두 하나의 로그로
find /etc -name "*.conf" &> all.log

# 에러를 버리고 싶을 때
find /etc -name "*.conf" 2> /dev/null
```

> **`/dev/null`**: "쓰레기통"으로 동작하는 특수 파일. 이곳으로 보낸 출력은 그대로 버려진다. 불필요한 에러 메시지를 숨길 때 자주 사용.

### 5-3. 파이프(Pipe, `|`)

앞 명령의 **stdout을 다음 명령의 stdin**으로 연결한다. 여러 명령을 조합해 강력한 처리를 만들 수 있다.

```bash
# 프로세스 중 sshd 관련만 보기
ps aux | grep sshd

# 파일 개수 세기
ls /etc | wc -l

# 상위 10개 줄만 보기
dmesg | head -10

# 디스크 사용량 상위 5개
du -sh /var/* 2>/dev/null | sort -h | tail -5
```

### 5-4. tee - 화면과 파일 동시 출력

`tee`는 파이프의 내용을 **화면에도 표시하고 파일에도 저장**한다. 결과를 보면서 로그로 남기고 싶을 때 유용하다.

```bash
# 화면 출력 + 파일 저장 동시
ls -l /etc | tee etc-list.txt

# 이어쓰기 모드
date | tee -a log.txt
```

## 6. 실습: 리다이렉션과 파이프

```bash
# 1) stdout/stderr 분리
ls /etc /없는경로 > out.txt 2> err.txt
cat out.txt        # 정상 결과
cat err.txt        # 에러 메시지

# 2) 파이프 체이닝: 현재 접속 사용자 중 유니크하게
who | awk '{print $1}' | sort -u

# 3) 로그에서 ERROR만 골라 개수 세기
journalctl -b | grep -i error | wc -l

# 4) tee로 실시간 확인 + 저장
df -h | tee disk.log
```

---

# Part 3. 조건문·반복문 + 자동화 스크립트

## 7. 핵심 개념 정리

### 7-1. 종료 상태(Exit Status)

모든 명령은 실행 후 **0~255의 종료 코드**를 반환한다. `0`은 성공, 그 외는 실패이다. 이 값으로 조건 분기를 한다.

```bash
ls /etc
echo $?           # 직전 명령의 종료 코드 (0이면 성공)

ls /없는경로
echo $?           # 0이 아닌 값 (실패)
```

### 7-2. 조건문 - `if`와 `test`

```bash
if [ 조건 ]; then
    명령
elif [ 조건 ]; then
    명령
else
    명령
fi
```

**자주 쓰는 조건식**:

| 형식 | 의미 |
|------|------|
| `[ -f 파일 ]` | 파일이 존재(regular file) |
| `[ -d 경로 ]` | 디렉터리가 존재 |
| `[ -z "$var" ]` | 문자열이 비어있음 |
| `[ -n "$var" ]` | 문자열이 비어있지 않음 |
| `[ "$a" = "$b" ]` | 문자열이 같음 |
| `[ "$a" != "$b" ]` | 문자열이 다름 |
| `[ "$n" -eq 10 ]` | 숫자가 같음 (`-ne`, `-lt`, `-gt`, `-le`, `-ge`) |

```bash
# 예: 파일 존재 여부 확인
if [ -f /etc/passwd ]; then
    echo "passwd 파일이 있습니다."
else
    echo "없습니다."
fi
```

### 7-3. case 문 - 다중 분기

```bash
read -p "명령을 입력하세요 (start/stop/status): " cmd

case "$cmd" in
    start)   echo "서비스 시작" ;;
    stop)    echo "서비스 중지" ;;
    status)  echo "서비스 상태 조회" ;;
    *)       echo "알 수 없는 명령" ;;
esac
```

### 7-4. 반복문 - `for`와 `while`

```bash
# for: 목록 순회
for name in alice bob carol; do
    echo "Hello, $name"
done

# for: 숫자 범위
for i in {1..5}; do
    echo "count $i"
done

# for: 파일 목록 순회
for f in /etc/*.conf; do
    echo "설정 파일: $f"
done

# while: 조건이 참인 동안 반복
count=1
while [ $count -le 3 ]; do
    echo "try $count"
    ((count++))
done
```

### 7-5. 사용자 입력과 함수

```bash
# 사용자 입력 받기
read -p "이름을 입력하세요: " user_name
echo "안녕하세요, ${user_name}님"

# 함수 정의와 사용
greet() {
    local name=$1           # 첫 번째 인자
    echo "Hello, ${name}!"
}

greet "ilwoo"
```

> **스크립트 인자**: 스크립트 실행 시 전달한 값은 `$1`, `$2`, ... 로 참조한다. `$0`은 스크립트 이름, `$#`은 인자 개수, `$@`은 전체 인자 목록.

## 8. 실습: 자동화 스크립트 작성

### 8-1. 디스크 사용량 점검 스크립트

루트 파티션 사용량이 일정 비율을 넘으면 경고 메시지를 출력한다.

```bash
#!/bin/bash
# 파일: ~/scripts/disk-check.sh

THRESHOLD=80      # 경고 기준 (%)
USAGE=$(df / | awk 'NR==2 {print $5}' | tr -d '%')

if [ "$USAGE" -ge "$THRESHOLD" ]; then
    echo "[WARN] / 사용량 ${USAGE}% (기준 ${THRESHOLD}%)"
else
    echo "[OK]   / 사용량 ${USAGE}%"
fi
```

```bash
# 실행 권한 부여 후 실행
chmod +x ~/scripts/disk-check.sh
~/scripts/disk-check.sh
```

### 8-2. 오래된 로그 정리 스크립트

지정한 디렉터리에서 N일 이상 지난 `.log` 파일을 삭제한다.

```bash
#!/bin/bash
# 파일: ~/scripts/cleanup-logs.sh
# 사용: ./cleanup-logs.sh <디렉터리> <보관일수>

TARGET_DIR=${1:-/tmp}
KEEP_DAYS=${2:-7}

if [ ! -d "$TARGET_DIR" ]; then
    echo "디렉터리가 없습니다: $TARGET_DIR"
    exit 1
fi

echo "[$(date +%F\ %T)] ${TARGET_DIR} 내 ${KEEP_DAYS}일 이상 .log 정리 시작"
find "$TARGET_DIR" -type f -name "*.log" -mtime +"$KEEP_DAYS" -print -delete
echo "[$(date +%F\ %T)] 정리 완료"
```

```bash
# 테스트용 오래된 파일 생성 후 실행
mkdir -p /tmp/logtest
touch -d "10 days ago" /tmp/logtest/old.log
touch /tmp/logtest/new.log

~/scripts/cleanup-logs.sh /tmp/logtest 7
ls /tmp/logtest   # old.log만 삭제됨
```

### 8-3. 서비스 상태 체크 스크립트

여러 서비스의 상태를 한 번에 확인한다.

```bash
#!/bin/bash
# 파일: ~/scripts/service-check.sh

SERVICES=("sshd" "firewalld" "crond")

for svc in "${SERVICES[@]}"; do
    if systemctl is-active --quiet "$svc"; then
        echo "[OK]   ${svc} 실행 중"
    else
        echo "[FAIL] ${svc} 중지됨"
    fi
done
```

### 8-4. cron으로 자동화 등록

위 스크립트를 주기적으로 실행하려면 week4에서 배운 `cron`과 결합한다.

```bash
crontab -e
```

```cron
# 매일 07:00 디스크 점검 결과를 로그에 기록
0 7 * * * /home/ooo426/scripts/disk-check.sh >> /home/ooo426/disk.log 2>&1

# 매주 일요일 03:00 /var/log/myapp 의 7일 초과 로그 정리
0 3 * * 0 /home/ooo426/scripts/cleanup-logs.sh /var/log/myapp 7 >> /home/ooo426/cleanup.log 2>&1
```

> **포인트**: 스크립트 + `cron` + 리다이렉션을 결합하면 "주기적으로 실행되고 결과가 로그로 남는 자동화"가 완성된다. 실무 운영 자동화의 가장 기본적인 패턴이다.

---

## 9. 전체 구조 요약

```
셸 스크립트 자동화 흐름
├─ Bash 셸
│   ├─ 변수 / 환경 변수 ($PATH, $HOME, export)
│   └─ 초기화 파일 (~/.bashrc, /etc/profile)
│
├─ 입출력 처리
│   ├─ 표준 스트림 (stdin/stdout/stderr)
│   ├─ 리다이렉션 (>, >>, <, 2>, &>)
│   ├─ 파이프 (|) → 명령 조합
│   └─ tee → 화면 + 파일 동시 출력
│
├─ 제어 구조
│   ├─ 조건문 (if / case)
│   ├─ 반복문 (for / while)
│   └─ 종료 상태 ($?, exit)
│
└─ 자동화
    ├─ 스크립트(.sh) + 실행 권한(chmod +x)
    ├─ 함수 / 인자($1, $@) / 사용자 입력(read)
    └─ cron 결합 → 주기 실행 + 로그 리다이렉션
```

---

## 10. 배운 점 / 느낀 점

- (실습 후 작성)

---

## 11. 참고 자료

- [Red Hat - Configuring Basic System Settings: Using Shell](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/8/html/configuring_basic_system_settings/)
- [GNU Bash Reference Manual](https://www.gnu.org/software/bash/manual/bash.html)
- [Red Hat - Automating System Tasks](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/8/html/configuring_basic_system_settings/assembly_automating-system-tasks_configuring-basic-system-settings)
