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

리눅스에서는 "프로그램의 입력/출력도 파일처럼 다룬다"는 철학이 있다. 프로세스가 실행되면 커널이 기본으로 **3개의 I/O 통로를 파일로 열어** 준다. 이 통로를 **표준 스트림(Standard Stream)**이라고 부르고, 각각 **파일 디스크립터(FD)** 번호가 붙어 있다.

| 이름 | FD | 기본 연결 | 역할 | 예시 |
|------|----|----------|------|------|
| **stdin** (표준 입력) | 0 | 키보드 | 프로세스가 **읽는** 통로 | `read` 명령, `wc < file.txt` |
| **stdout** (표준 출력) | 1 | 터미널 화면 | **정상** 결과가 나가는 통로 | `ls` 결과, `echo` 결과 |
| **stderr** (표준 에러) | 2 | 터미널 화면 | **에러 메시지**가 나가는 통로 | `ls: 없는파일: 그런 파일 없음` |

```
           키보드 입력
               │
               ▼ (FD 0)
         ┌──────────┐
         │          │ ─(FD 1)─▶  정상 결과  → 화면
         │ Process  │
         │          │ ─(FD 2)─▶  에러 메시지 → 화면
         └──────────┘
```

> **왜 stdout과 stderr를 나눴나?** 둘 다 화면으로 나가기 때문에 눈으로 보면 섞여 보이지만, 사실 **다른 통로**이다. 이렇게 분리되어 있기 때문에 "정상 결과만 파일로 저장하고, 에러는 화면에만 보여주기" 같은 처리가 가능해진다.

**직접 확인해보기**:

```bash
# stdout과 stderr가 같이 화면에 섞여 보이지만...
ls /etc /없는경로

# stdout만 버리면 에러만 남고,
ls /etc /없는경로 > /dev/null
# → 에러만 화면에 보인다

# stderr만 버리면 정상 결과만 남는다
ls /etc /없는경로 2> /dev/null
# → 정상 결과만 화면에 보인다
```

#### 종료 코드(Exit Status)와는 다르다

초보자들이 헷갈리는 부분인데, **stderr에 메시지가 나간다 ≠ 명령이 실패했다**는 뜻이 아니다. 명령의 성공/실패는 **종료 코드(`$?`)**로 판단한다. stderr는 단순히 "화면에 띄우고 싶은 메시지 중 에러 성격인 것"을 보내는 통로일 뿐이다.

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

#### 주의: "허가 거부"가 뜨는 경우

`>` 리다이렉션은 **셸이 현재 작업 디렉터리에 파일을 생성**한다. 쓰기 권한이 없는 경로(예: `/etc`)에서 실행하면 실패한다.

```bash
[ooo426@linux etc]$ ls /etc /fff > out.txt 2> err.txt
bash: out.txt: 허가 거부
```

또한 **`sudo`도 해결책이 아니다**. 리다이렉션은 `sudo`가 실행되기 전에 셸이 먼저 파일을 여는 동작이므로, `sudo`는 `ls`에만 적용되고 파일 생성은 여전히 일반 사용자 권한으로 시도된다.

```bash
# ❌ 여전히 실패 — 리다이렉션은 sudo 밖에서 동작
sudo ls /etc /fff > /etc/out.txt

# ✅ 해결책 1: 쓰기 가능한 위치로 이동/지정
cd ~
ls /etc /fff > out.txt 2> err.txt

# ✅ 해결책 2: 반드시 보호된 경로에 써야 한다면 셸 전체를 sudo로 실행
sudo sh -c 'ls /etc /fff > /etc/out.txt 2> /etc/err.txt'
```

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

## 8. 실무 사례: Spring에서 JVM 분리 실행용 sh 스크립트

현재 회사에서는 문서 변환(썸네일/PDF 생성)에 **Hyland Document Filters**라는 네이티브 라이브러리를 Java SDK 형태로 사용하고 있다. 이 모듈을 Spring 애플리케이션에서 호출할 때, **Spring과 같은 JVM에 올리지 않고 별도의 자식 프로세스(JVM)로 분리**해서 실행한다. 그 실행 진입점이 바로 아래의 sh 스크립트이다.

### 8-1. 왜 별도의 JVM으로 띄우는가? (JNI 크래시 격리)

> "JAVA SDK면 스프링에 바로 dependency로 넣고 같은 JVM에서 호출하면 되지 않나?"

Hyland Document Filters는 순수 자바가 아니라 **JNI(Java Native Interface)** 기반 모듈이다. 실제 변환 로직은 `.so`(리눅스 네이티브 라이브러리) 안에서 돌아가고, Java는 단지 JNI로 그 네이티브 코드를 호출하는 얇은 래퍼일 뿐이다.

```
[문제 상황: 같은 JVM에 올렸을 때]

Spring Boot JVM (PID 1001)
  ├─ Tomcat 요청 스레드
  ├─ 비즈니스 로직
  └─ Hyland JNI 호출  ──▶ libHyland.so  ◀── 여기서 SEGFAULT 발생하면
                                              JVM 전체가 다운 → Spring도 같이 죽음
```

JNI 모듈은 네이티브 C/C++ 코드이므로 **자바의 예외 처리로 막을 수 없는 크래시**(SEGV, abort 등)가 발생할 수 있다. 특정 손상된 입력 파일 하나 때문에 Spring 전체 서비스가 내려가는 사고로 이어질 수 있다.

그래서 실무에서는 다음 구조로 격리한다:

```
[실무 구조: sh 스크립트로 JVM 분리]

Spring Boot JVM (PID 1001)
    │ ProcessBuilder("make_thumb.sh", 파일, ...)
    │
    ▼
  sh 프로세스 (PID 1002)
    │ exec java -cp ... MakeImage
    │
    ▼
  Hyland JVM (PID 1003)
    └─ libHyland.so ──▶ 여기서 크래시 나도
                          이 JVM만 죽음. Spring은 영향 없음.
                          Spring은 exit code로 실패만 감지하고
                          그 요청만 에러 응답 처리.
```

**정리하자면**:

| 같은 JVM | 별도 JVM (실무 방식) |
|---------|---------------------|
| 네이티브 크래시 → Spring 전체 다운 | 크래시 나도 자식 프로세스만 죽음 |
| GC/메모리를 서비스와 공유 | 변환 전용 메모리 한도 별도 설정 가능 |
| 하나의 파일 처리가 전체 지연 유발 | 프로세스 단위로 타임아웃/격리 가능 |
| JNI 라이브러리 버전 강결합 | 독립 JDK(Zulu 8)로 별도 구동 가능 |

### 8-2. 실제 스크립트와 문법 해설

```sh
#!/bin/sh

pushd () {
   SAVE=`pwd`
   if [ "$1" = "" ]
   then   if [ "$DSTACK" = "" ]
      then   echo "pushd: directory stack empty."
         return 1
      fi
      set $DSTACK
      cd $1 || return
      shift 1
      DSTACK="$*"
   else   cd $1 > /dev/null || return
   fi
   DSTACK="$SAVE $DSTACK"
}

popd () {
   if [ "$DSTACK" = "" ]
   then   echo "popd: directory stack empty."
      return 1
   fi
   set $DSTACK
   cd $1
   shift
   DSTACK=$*
}

##################################################
# Set SERVICE_HOME Path
##################################################
SERVICE_HOME=/Fasoo_product/Wrapsody_eCo/app/make_thumb

if [ "$SERVICE_HOME" = "" ]
then
   SERVICE_HOME=..
fi

##################################################
# Set FB_JAVA_HOME Path
##################################################
FB_JAVA_EXCUTE=/Fasoo_product/Wrapsody_eCo/JVM/LINUX/64/zulu8.58.0.13-ca-jdk8.0.312-linux_x64/bin/java

LD_LIBRARY_PATH=.:$SERVICE_HOME/jni
export LD_LIBRARY_PATH

##################################################
# Parameters
##################################################
INPUT_FILE=$1
OUTPUT_DIR=$2
RESULT_FILE=$3
DPI=$4
TIMEOUT=$5
EXTENSION=$6
PREFIX=$7
OBFUSCATE=$8
GRAPHIC_WIDTH=$9

# 10번째 파라미터: OUTPUT_FORMAT (png 또는 pdf)
OUTPUT_FORMAT=${10:-png}

if [ "$TIMEOUT" = "" ]
then
   TIMEOUT=60
fi

# graphicWidth가 비어있으면 0으로 설정
if [ "$GRAPHIC_WIDTH" = "" ]; then
   GRAPHIC_WIDTH=0
fi

##################################################
# Run MakeImage
##################################################
pushd $SERVICE_HOME

SERVICE_HOME=`pwd -P`

$FB_JAVA_EXCUTE -Djava.library.path="$LD_LIBRARY_PATH" -classpath "$SERVICE_HOME/bin/*:$SERVICE_HOME/lib/*" com.fasoo.eco.utils.hyland.MakeImage "$INPUT_FILE" "$OUTPUT_DIR" "$RESULT_FILE" "$DPI" "$TIMEOUT" "$EXTENSION" "$PREFIX" "$OBFUSCATE" "$GRAPHIC_WIDTH" "$OUTPUT_FORMAT"

JAVA_RET=$?

popd

if [ $JAVA_RET -ne 0 ]; then
   echo "Processing failed with exit code $JAVA_RET"
   exit 1
fi

exit 0
```

#### (1) Shebang과 셸 선택

```sh
#!/bin/sh
```

`#!/bin/bash`가 아닌 `#!/bin/sh`로 되어 있다. Bash 전용 문법(배열, `[[ ]]`, `{1..5}` 등)을 사용하지 않고 **POSIX sh 호환 문법**만 사용해 어떤 리눅스 배포판에서도 동일하게 동작하도록 보장한다. 실제로 아래에서 사용하는 문법(`` ` ` ``, `[ "$x" = "" ]`, `$*` 등)이 모두 POSIX 스타일이다.

#### (2) pushd/popd 직접 구현

`/bin/sh`에는 bash의 내장 `pushd`/`popd`가 없다. 그래서 **환경 변수 `DSTACK`을 스택처럼 사용**해 직접 구현했다.

```sh
pushd () {
   SAVE=`pwd`                      # 현재 디렉터리를 백업
   ...
   cd $1 > /dev/null || return     # 전달된 디렉터리로 이동, 실패 시 함수 종료
   ...
   DSTACK="$SAVE $DSTACK"          # 스택 맨 앞에 이전 디렉터리를 push
}
```

| 문법 | 의미 |
|------|------|
| `` SAVE=`pwd` `` | **명령 치환**. `pwd` 실행 결과를 `SAVE`에 저장. (`$(pwd)`와 동일, 구버전 표기) |
| `cd $1 \|\| return` | `cd`가 실패하면(0 이외 종료 코드) 함수를 바로 빠져나옴 |
| `> /dev/null` | `cd -`가 이동 경로를 출력할 때가 있어 그 출력을 버림 |
| `set $DSTACK` | `DSTACK`의 값을 공백 기준으로 나눠 **위치 매개변수 `$1, $2, ...`에 재할당** |
| `shift 1` | `$1`을 버리고 `$2→$1`, `$3→$2`로 한 칸씩 당김 (스택 pop 효과) |
| `"$*"` | 남은 모든 위치 매개변수를 공백으로 이어 붙인 문자열 |

> Spring에서 실행한 sh는 자식 프로세스라 `cd`로 작업 디렉터리를 바꿔도 부모(Spring)에 영향이 없다. 다만 스크립트 내부에서 "실행 디렉터리 → 원래 위치로 복귀"를 깔끔히 처리하려고 pushd/popd 패턴을 쓴 것이다.

#### (3) SERVICE_HOME과 기본값 처리

```sh
SERVICE_HOME=/Fasoo_product/Wrapsody_eCo/app/make_thumb

if [ "$SERVICE_HOME" = "" ]
then
   SERVICE_HOME=..
fi
```

| 문법 | 의미 |
|------|------|
| `[ "$SERVICE_HOME" = "" ]` | 변수가 빈 문자열인지 검사. `-z "$SERVICE_HOME"`와 같음. **양쪽 따옴표는 필수** — 빈 값일 때 `[ = "" ]`로 깨지지 않도록 |
| `then` 다음 줄 들여쓰기 | 가독성용. sh 문법상은 `;`로 구분되거나 줄바꿈이면 됨 |

#### (4) 네이티브 라이브러리 경로 지정 — `LD_LIBRARY_PATH`

```sh
LD_LIBRARY_PATH=.:$SERVICE_HOME/jni
export LD_LIBRARY_PATH
```

이 부분이 **JNI 실행의 핵심**이다.

- `LD_LIBRARY_PATH`는 리눅스 동적 로더(`ld.so`)가 **`.so` 파일을 찾을 때 참고하는 경로 목록**이다.
- Hyland JNI 래퍼는 `System.loadLibrary("HylandXxx")`로 `.so`를 로드하는데, JVM은 내부적으로 `java.library.path`와 `LD_LIBRARY_PATH`를 함께 참조한다.
- `.:$SERVICE_HOME/jni` → 현재 디렉터리(`.`)와 `.../make_thumb/jni` 두 곳에서 `.so` 파일을 찾는다.
- `export`로 환경 변수로 승격해야 **자식 프로세스(=곧 실행할 java)가 상속**받는다. (Part 1에서 본 개념)

> 같은 JVM에 올리지 않은 또 다른 이유: JNI는 한 번 `.so`를 로드하면 **언로드가 사실상 불가능**하다. 같은 JVM에 붙이면 서비스가 돌아가는 내내 네이티브 메모리를 잡고 있게 된다. 자식 프로세스로 분리하면 변환이 끝나는 순간 프로세스가 내려가면서 네이티브 메모리까지 깔끔히 회수된다.

#### (5) 위치 매개변수와 기본값 `${VAR:-default}`

```sh
INPUT_FILE=$1
OUTPUT_DIR=$2
...
OUTPUT_FORMAT=${10:-png}

if [ "$TIMEOUT" = "" ]
then
   TIMEOUT=60
fi
```

| 문법 | 의미 |
|------|------|
| `$1 ~ $9` | 호출 시 전달된 **첫 번째 ~ 아홉 번째 인자** |
| `${10}` | 10번째부터는 **중괄호 필수**. `$10`은 `$1` + 리터럴 `0`으로 해석됨 |
| `${10:-png}` | 10번째 인자가 **없거나 빈 값이면** 기본값 `png` 사용. **파라미터 확장 문법** |
| 별도 if문으로 기본값 | `TIMEOUT`은 옛날 스타일로 `[ "$X" = "" ]`로 체크. `${5:-60}`과 기능적으로 동일 |

> `${VAR:-default}` vs `${VAR-default}`: 전자는 "빈 문자열도 기본값 적용", 후자는 "변수가 아예 unset일 때만 적용". 실무에서는 거의 `:-`를 쓴다.

#### (6) 실제 자바 실행

```sh
pushd $SERVICE_HOME
SERVICE_HOME=`pwd -P`

$FB_JAVA_EXCUTE \
   -Djava.library.path="$LD_LIBRARY_PATH" \
   -classpath "$SERVICE_HOME/bin/*:$SERVICE_HOME/lib/*" \
   com.fasoo.eco.utils.hyland.MakeImage \
   "$INPUT_FILE" "$OUTPUT_DIR" ... "$OUTPUT_FORMAT"

JAVA_RET=$?
popd
```

| 문법 | 의미 |
|------|------|
| `pwd -P` | **심볼릭 링크를 해석한 실제 물리 경로**를 반환. classpath를 상대경로에 의존하지 않게 만듦 |
| `$FB_JAVA_EXCUTE` | 번들된 **전용 JDK**(Zulu 8)의 `java` 바이너리. 시스템 JDK와 버전/설정이 섞이지 않게 하려는 의도 |
| `-Djava.library.path=...` | JVM에게 JNI `.so` 검색 경로를 알려줌 (`LD_LIBRARY_PATH`와 이중 방어) |
| `-classpath "bin/*:lib/*"` | `bin`과 `lib` 아래 **모든 jar**를 classpath에 포함 (`:`는 리눅스 구분자, 윈도우는 `;`) |
| `com.fasoo.eco.utils.hyland.MakeImage` | 실행할 **main 클래스**의 FQCN |
| `"$INPUT_FILE"` | **반드시 따옴표로 감쌈**. 경로에 공백/한글이 있을 때 인자가 쪼개지지 않도록 |
| `JAVA_RET=$?` | **직전 명령(java)의 종료 코드**를 저장. `popd` 이후에는 `$?`가 popd 결과로 덮여 버리므로 **즉시** 저장해둬야 함 |

#### (7) 종료 코드 전파

```sh
if [ $JAVA_RET -ne 0 ]; then
   echo "Processing failed with exit code $JAVA_RET"
   exit 1
fi

exit 0
```

- Java 프로세스가 0이 아닌 값으로 죽으면 sh도 `exit 1`로 실패를 명확히 표시.
- 성공 시 `exit 0`.
- Spring 쪽에서는 `Process#waitFor()`가 반환하는 이 종료 코드로 "변환 성공/실패"를 판단하고, 실패하면 특정 요청만 에러 응답 + 재시도 로직으로 흘려보낸다.

> **이 한 줄이 왜 중요한가**: JNI 크래시로 자식 JVM이 죽으면 Java는 비정상 종료(`137`, `139` 등)가 된다. sh가 그 코드를 **Spring까지 전달**해주기 때문에 Spring이 "이 요청은 실패했으니 다른 파일은 계속 처리"라는 판단을 할 수 있다. 만약 같은 JVM이었다면 Spring 자체가 죽어서 **이런 판단 자체가 불가능**했을 것이다.

### 8-3. 이 스크립트에 녹아있는 이번 주 개념

| 이번 주 학습 포인트 | 스크립트에서의 사용처 |
|---------------------|----------------------|
| 변수 / 환경 변수 / `export` | `SERVICE_HOME`, `LD_LIBRARY_PATH`(export로 자식 JVM에 상속) |
| 명령 치환 `` ` ` ``, `$()` | `` SAVE=`pwd` ``, `` SERVICE_HOME=`pwd -P` `` |
| 위치 매개변수 `$1`~`${10}` | Spring이 ProcessBuilder로 넘긴 입력 파일, 출력 경로, DPI 등 |
| 파라미터 확장 `${V:-기본값}` | `OUTPUT_FORMAT=${10:-png}` |
| 조건문 `if [ ... ]` | 빈 값 방어, 종료 코드 판별 |
| 함수 정의 | `pushd`/`popd` 수동 구현 |
| 리다이렉션 `> /dev/null` | `cd` 출력 숨기기 |
| 종료 코드 `$?` / `exit N` | `JAVA_RET=$?` → Spring에 전파 |

결과적으로 이 sh 스크립트는 **Spring과 JNI 모듈 사이의 "안전 격리막"** 역할을 한다. 이번 주에 배운 문법이 실제로 장애 격리 같은 실무 아키텍처 결정을 떠받치는 데 쓰이고 있다는 점이 가장 흥미로운 부분이었다.

---

## 9. 전체 구조 요약

```
셸 스크립트 자동화 흐름
├─ Bash/sh 셸
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
└─ 실무 활용: JNI 격리 실행
    Spring JVM ──exec──▶ sh ──java──▶ Hyland JVM ──▶ libHyland.so
                                            ▲
                                    여기서 크래시가 나도
                                    Spring은 exit code만 받고 계속 동작
```

---

## 10. 참고 자료

- [Red Hat - Configuring Basic System Settings: Using Shell](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/8/html/configuring_basic_system_settings/)
- [GNU Bash Reference Manual](https://www.gnu.org/software/bash/manual/bash.html)
- [Red Hat - Automating System Tasks](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/8/html/configuring_basic_system_settings/assembly_automating-system-tasks_configuring-basic-system-settings)
