# 셸 스크립트 및 자동화 기초

## 공부 흐름

이 문서는 다음 순서로 구성되어 있다. 각 단계는 이전 단계의 지식을 기반으로 쌓아올리는 구조이다.

```
1. 사전 지식          셸, Bash, 표준 스트림 등 핵심 용어 정리
       ↓
2. Bash 셸 기본 구조   셸의 종류 → 작동 원리 → 로그인/비로그인 셸 → 스크립트 파일 구조
       ↓
3. 변수               셸 변수 → 환경 변수 → 특수 변수 → 따옴표 규칙
       ↓              "스크립트 안에서 데이터를 저장하고 꺼내 쓰는 방법"
       ↓
4. 입출력·파이프       표준 스트림 → 리다이렉션 → 파이프 → 필터 조합
       ↓              "데이터를 파일로 보내고, 명령어끼리 연결하는 방법"
       ↓
5. 조건문·반복문       if/case → for/while/until → 함수
       ↓              "변수 + 입출력 위에 논리적 흐름을 얹는 방법"
       ↓
6. 자동화 실습         리포트 → 백업 → 서비스 점검 → Podman 점검·복구
       ↓              "1~5단계를 실무 시나리오에 종합 적용"
       ↓
7. 디버깅             Bash Strict Mode, 실행 추적, 구문 검사
                      "작성한 스크립트의 오류를 찾고 수정하는 방법"
```

---

## 사전 지식

**셸(Shell):** 사용자가 입력한 명령어를 해석하여 커널(Kernel)에 전달하고, 커널의 실행 결과를 다시 사용자에게 돌려주는 명령줄 인터페이스(CLI)이다. 커널과 사용자 사이에서 통역관 역할을 수행한다.

**Bash(Bourne Again Shell):** 대부분의 리눅스 배포판에서 기본으로 채택된 셸이다. GNU 프로젝트의 일환으로 개발되었으며, 이전의 Bourne Shell(`sh`)을 계승·확장한 것이다. RHEL/Rocky Linux 환경에서도 기본 로그인 셸로 사용된다.

**인터프리터(Interpreter):** 소스 코드를 한 줄씩 읽어가며 즉시 실행하는 방식의 프로그램. 셸 스크립트는 컴파일 과정 없이 Bash 인터프리터가 스크립트 파일을 위에서 아래로 한 줄씩 해석하며 실행한다.

**표준 스트림(Standard Stream):** 리눅스에서 모든 프로세스가 생성될 때 커널이 자동으로 열어주는 세 개의 데이터 통로를 의미한다.

- **stdin (Standard Input, fd 0):** 키보드 등으로부터 데이터가 들어오는 입력 통로
- **stdout (Standard Output, fd 1):** 정상 실행 결과가 나가는 출력 통로
- **stderr (Standard Error, fd 2):** 오류 메시지가 나가는 출력 통로

**종료 코드(Exit Code):** 명령어가 실행을 마친 뒤 셸에 반환하는 정수값. `0`은 성공, `0`이 아닌 값(1~255)은 실패를 의미한다. `$?` 변수를 통해 직전 명령어의 종료 코드를 확인할 수 있다.

---

## Bash 셸의 기본 구조 이해

### 셸의 종류와 계보

리눅스에서 사용할 수 있는 셸은 여러 종류가 있으며, 각각 기능과 문법에 차이가 있다. 현재 시스템에서 사용 가능한 셸 목록은 `/etc/shells` 파일에서 확인할 수 있다.

```bash
cat /etc/shells
# /bin/sh
# /bin/bash
# /usr/bin/sh
# /usr/bin/bash
```

```
                         ┌──────────────────┐
                         │  Bourne Shell(sh) │  1977, AT&T
                         └────────┬─────────┘
              ┌───────────────────┼───────────────────┐
              ▼                   ▼                    ▼
     ┌────────────────┐  ┌───────────────┐  ┌─────────────────┐
     │   csh (1978)    │  │  ksh (1983)   │  │   bash (1989)   │
     │   C Shell       │  │  Korn Shell   │  │  Bourne Again   │
     └───────┬────────┘  └───────────────┘  └─────────────────┘
             ▼                                       ▲
     ┌────────────────┐                              │
     │  tcsh (1983)   │                    RHEL / Rocky Linux
     │  Enhanced C    │                       기본 셸
     └────────────────┘
```


| **셸**                   | **등장** | **특징**                                                                                                                                                                              |
| ----------------------- | ------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Bourne Shell (`sh`)** | 1977   | AT&T 벨 연구소의 Stephen Bourne이 개발. 최초의 표준 유닉스 셸로, 변수·리다이렉션·파이프·조건문 등 셸 스크립트의 기본 문법을 확립했다. 현대 셸들의 공통 조상이자 POSIX 셸 표준의 기반이다.                                                             |
| **C Shell (`csh`)**     | 1978   | UC Berkeley의 Bill Joy가 개발. C 언어와 유사한 문법(`if-then` 대신 `if ( ) then`)을 채택하여 프로그래머에게 친숙했다. 히스토리 기능과 별칭(alias)을 최초로 도입했으나, 스크립트 작성 시 파이프라인 및 리다이렉션 처리가 `sh`보다 불안정하여 스크립팅 용도로는 권장되지 않았다. |
| **Korn Shell (`ksh`)**  | 1983   | AT&T 벨 연구소의 David Korn이 개발. Bourne Shell의 완전한 하위 호환성을 유지하면서 C Shell의 장점(히스토리, 별칭)과 배열·산술 연산 등의 고급 기능을 결합한 셸이다. 상용 유닉스(AIX, HP-UX 등)에서 기본 셸로 널리 채택되었다.                               |
| **tcsh**                | 1983   | C Shell을 확장한 버전으로, 명령어 자동 완성(Tab Completion)과 명령줄 편집 기능을 추가했다. FreeBSD 등 일부 BSD 계열에서 기본 셸로 사용되었다.                                                                                   |
| **Bash (`bash`)**       | 1989   | GNU 프로젝트의 Brian Fox가 개발. Bourne Shell을 완전히 대체하면서 `ksh`와 `csh`의 유용한 기능(히스토리, 배열, 산술 확장, Tab 완성 등)을 모두 흡수·통합했다. 오픈소스이며, 현재 대부분의 리눅스 배포판(RHEL, Rocky Linux, Ubuntu 등)의 기본 셸로 자리잡았다.    |


 

### 셸의 작동 원리

사용자가 터미널에 명령어를 입력하면, Bash 셸은 다음과 같은 순서로 처리한다.

```
사용자 입력          Bash 셸                     커널
───────────    ───────────────────    ───────────────────
               ┌─ 1. 토큰 분리        
  "ls -la"  →  ├─ 2. 확장(Expansion)  
               ├─ 3. 명령어 탐색       →  시스템 콜 실행
               └─ 4. 프로세스 생성           (fork + exec)
                                              │
                  결과 수신 ←──────────────────┘
                  화면 출력
```

1. **토큰 분리:** 입력된 문자열을 공백 기준으로 명령어, 옵션, 인자로 분리한다.
2. **확장(Expansion):** 변수(`$HOME`), 와일드카드(`*.txt`), 명령어 치환(`date`) 등을 실제 값으로 치환한다.
3. **명령어 탐색:** 내장 명령어(builtin)인지 확인하고, 아니면 `$PATH` 경로에서 실행 파일을 검색한다.
4. **프로세스 생성:** `fork()` 시스템 콜로 자식 프로세스를 만들고, `exec()`으로 해당 프로그램을 적재하여 실행한다.

**예시: `echo "Hello $USER, you have $(ls *.txt | wc -l) files"` 입력 시**


| 단계            | 처리 내용                                                                  | 결과                                                               |
| ------------- | ---------------------------------------------------------------------- | ---------------------------------------------------------------- |
| **1. 토큰 분리**  | 공백 기준으로 명령어와 인자를 분리                                                    | 토큰 1(명령어): `echo` / 토큰 2(인자): `"Hello $USER, you have $(ls *.txt |
| **2. 확장**     | `$USER` → `jangwoo` (변수 확장)                                            | `echo "Hello jangwoo, you have 2 files"`                         |
|               | `$(ls *.txt)` → `memo.txt note.txt` (명령어 치환)                           |                                                                  |
|               | `$(...                                                                 | wc -l)`→`2` (파이프 + 명령어 치환)                                       |
| **3. 명령어 탐색** | `echo`가 내장 명령어(builtin)인지 확인 → **Yes**                                 | builtin으로 확인됨                                                    |
|               | (No일 경우 `$PATH` 순서대로 탐색: `/usr/local/bin` → `/usr/bin` → `/bin` → ...) |                                                                  |
| **4. 실행**     | builtin이므로 `fork()` 없이 현재 셸에서 바로 실행                                    | 화면 출력: `Hello jangwoo, you have 2 files`                         |


```
[1. 토큰 분리]          [2. 확장]                [3. 탐색]        [4. 실행]

echo "Hello $USER..."   $USER     -> jangwoo     builtin? -> Yes   현재 셸에서 실행
                         $(ls...) -> 2 files      $PATH 탐색 불필요
     |                       |                        |                 |
     v                       v                        v                 v
  명령어 + 인자 분리    변수/명령어를 값으로 치환   실행 파일 위치 결정   프로세스 생성 or 직접 실행
```

> **내장 명령어(builtin) vs 외부 명령어:** `echo`, `cd`, `export` 같은 내장 명령어는 자식 프로세스를 생성하지 않고 Bash 자체가 즉시 실행한다. 반면 `ls`, `grep`, `find` 같은 외부 명령어는 `fork()` + `exec()`를 거쳐 별도의 프로세스로 실행된다. `type` 명령어로 구분할 수 있다.

```bash
type echo    # echo is a shell builtin
type ls      # ls is /usr/bin/ls

# 외부 명령어의 실제 경로 확인
which grep   # /usr/bin/grep
```

 

**예시: `ls /etc/*.conf | head -5` 실행 시 — 외부 명령어 + 파이프의 프로세스 흐름**

```
  Bash (PID 1234, 부모 셸)
    │
    ├─ fork() ──→ 자식 프로세스 A
    │              exec("/usr/bin/ls", "/etc/*.conf")
    │              stdout ──┐
    │                       │ 파이프 (커널이 관리하는 버퍼)
    │                       │
    ├─ fork() ──→ 자식 프로세스 B
    │              stdin  ←─┘
    │              exec("/usr/bin/head", "-5")
    │              stdout → 터미널 화면
    │
    ├─ waitpid() ── 두 자식이 모두 종료될 때까지 대기
    │
    └─ 프롬프트 복귀 ($?)
```

- 파이프(`|`)를 사용하면 Bash는 **양쪽 명령어를 동시에** `fork()`하여 두 개의 자식 프로세스를 생성한다.
- 커널은 두 프로세스 사이에 **파이프 버퍼**(기본 64KB)를 만들어 `ls`의 stdout과 `head`의 stdin을 연결한다.
- `ls`가 출력을 쓰는 동시에 `head`가 읽기 시작하므로 **스트리밍 방식**으로 처리되어, 대용량 데이터도 메모리를 과도하게 소모하지 않는다.
- 두 자식 프로세스가 모두 종료되면 Bash는 `waitpid()`로 결과를 수거하고 프롬프트를 다시 표시한다.

 

### 로그인 셸 vs 비로그인 셸

셸이 시작되는 방식에 따라 읽어 들이는 초기화 파일이 달라진다. 이 구분은 환경 변수나 별칭(alias)이 적용되지 않는 문제를 해결할 때 핵심적인 단서가 된다.

**로그인 셸 (Login Shell)이란?**

사용자가 시스템에 **인증(ID/비밀번호)을 거쳐 처음 진입**할 때 실행되는 셸이다. "지금부터 이 사용자가 시스템을 쓰기 시작한다"는 의미이므로, 시스템 전역 환경(`/etc/profile`)부터 사용자 개인 환경(`~/.bash_profile`)까지 **모든 초기화 파일을 순서대로 읽어** 완전한 환경을 구성한다.

- SSH로 원격 접속: `ssh user@server`
- 콘솔에서 직접 로그인 (TTY)
- `su - username` (대시 `-`가 핵심 — 로그인 셸로 전환)
- `sudo -i` (root 로그인 셸)

**비로그인 셸 (Non-login Shell)이란?**

이미 로그인된 상태에서 **추가로 열리는 셸**이다. 사용자가 이미 인증을 마친 상태이므로 전역 환경을 다시 읽을 필요가 없고, 사용자 개인 설정인 `~/.bashrc`만 읽어 빠르게 시작한다.

- GNOME 터미널, ptyxis 등 터미널 에뮬레이터에서 새 탭/창 열기
- 셸 스크립트 내부에서 `bash`를 실행하여 서브셸 생성
- `su username` (대시 없이 — 비로그인 셸)

```bash
# 현재 셸이 로그인 셸인지 확인하는 방법
echo $0
# -bash  → 앞에 대시(-)가 붙으면 로그인 셸
# bash   → 대시가 없으면 비로그인 셸

# 또 다른 확인 방법
shopt login_shell
# login_shell    on  → 로그인 셸
# login_shell    off → 비로그인 셸
```


| **구분**     | **로그인 셸 (Login Shell)**                          | **비로그인 셸 (Non-login Shell)** |
| ---------- | ------------------------------------------------ | ---------------------------- |
| **의미**     | 시스템에 인증을 거쳐 처음 진입                                | 이미 로그인된 상태에서 추가로 열리는 셸       |
| **실행 시점**  | SSH 접속, `su -`, 콘솔 로그인                           | 터미널 에뮬레이터 실행, 스크립트 내부        |
| **초기화 파일** | `/etc/profile` → `~/.bash_profile` → `~/.bashrc` | `~/.bashrc` 만 읽음             |
| **확인 방법**  | `echo $0` → `-bash`                              | `echo $0` → `bash`           |


 

**초기화 파일 로딩 순서**

```
[로그인 셸]

  /etc/profile                 (1) 시스템 전역 환경 — 모든 사용자 공통
       |
       v
  /etc/profile.d/*.sh          (2) 패키지별 추가 설정 (lang.sh, colorls.sh 등)
       |
       v
  ~/.bash_profile              (3) 사용자 개인 로그인 설정
       |
       +-- source ~/.bashrc    (4) 내부에서 .bashrc를 호출하는 것이 관례
              |
              v
           alias, 함수, PS1 등 적용


[비로그인 셸]

  ~/.bashrc                    (1) 사용자 개인 설정만 읽음 — 빠르게 시작
       |
       v
    alias, 함수, PS1 등 적용
```


| **파일**                | **역할**                                        |
| --------------------- | --------------------------------------------- |
| `/etc/profile`        | 시스템 전역 환경 변수, `umask`, PATH 등 설정. 모든 사용자에게 적용 |
| `/etc/profile.d/*.sh` | 패키지별·기능별 추가 환경 설정 스크립트                        |
| `~/.bash_profile`     | 개별 사용자 로그인 시 1회 실행. 보통 내부에서 `~/.bashrc`를 호출   |
| `~/.bashrc`           | 셸이 열릴 때마다 실행. alias, 함수, 프롬프트 설정 등            |
| `~/.bash_logout`      | 로그인 셸 종료 시 실행되는 정리 스크립트                       |


 

> **실무 트러블슈팅:** `~/.bashrc`에 alias를 추가했는데 SSH 접속 시 적용이 안 된다면, `~/.bash_profile` 안에 `source ~/.bashrc` 호출이 빠져 있을 가능성이 높다. 반대로 cron이나 스크립트에서 `$PATH`가 달라 명령어를 못 찾는 경우는, cron 환경이 로그인/비로그인 셸 어디에도 해당하지 않아 초기화 파일이 전혀 읽히지 않기 때문이다. 이 경우 스크립트 상단에서 직접 `source /etc/profile` 또는 필요한 `PATH`를 명시해야 한다.

 

### 셸 스크립트 파일의 기본 구조

셸 스크립트란, 터미널에서 하나씩 직접 입력하던 명령어들을 **하나의 텍스트 파일에 순서대로 모아 놓은 것**이다. 이 파일을 실행하면 Bash가 위에서 아래로 한 줄씩 읽으며 명령어를 자동으로 수행한다. 즉, 반복적인 수작업을 파일 하나로 묶어 **한 번에 실행할 수 있게 만든 자동화 도구**라고 할 수 있다.

셸 스크립트는 확장자 `.sh`를 관례적으로 사용하며, 반드시 첫 줄에 **셔뱅(Shebang)** 라인을 포함해야 한다.

```bash
#!/bin/bash
# ↑ 셔뱅(Shebang): 이 스크립트를 어떤 인터프리터로 실행할지 커널에 알려주는 지시자

# 주석은 # 으로 시작한다
echo "Hello, Rocky Linux!"
```

**스크립트 실행 방법**

```bash
# 방법 1: 실행 권한 부여 후 직접 실행
chmod +x script.sh
./script.sh

# 방법 2: bash 인터프리터를 명시적으로 호출 (실행 권한 불필요)
bash script.sh

# 방법 3: source 명령으로 현재 셸에서 실행 (변수가 현재 셸에 남음)
source script.sh
# 또는
. script.sh
```

> `**./script.sh`와 `source script.sh`의 차이:** `./`로 실행하면 자식 셸(서브셸)이 생성되어 스크립트가 종료되면 내부에서 선언한 변수가 사라진다. 반면 `source`는 현재 셸에서 그대로 실행하므로 변수가 유지된다.

```bash
# 테스트용 스크립트 작성
cat > test_var.sh <<'EOF'
#!/bin/bash
MY_COLOR="blue"
echo "스크립트 내부: MY_COLOR=$MY_COLOR"
EOF
chmod +x test_var.sh
```

```bash
# ./로 실행 — 서브셸에서 실행되므로 변수가 사라짐
$ ./test_var.sh
스크립트 내부: MY_COLOR=blue

$ echo $MY_COLOR
                           ← 비어 있음 (서브셸이 종료되면서 변수도 소멸)
```

```bash
# source로 실행 — 현재 셸에서 실행되므로 변수가 남아 있음
$ source test_var.sh
스크립트 내부: MY_COLOR=blue

$ echo $MY_COLOR
blue                       ← 현재 셸에 변수가 그대로 유지됨
```

```
[./test_var.sh]                    [source test_var.sh]

현재 셸 (PID 1234)                 현재 셸 (PID 1234)
  |                                  |
  +-- fork() -> 서브셸 (PID 5678)   MY_COLOR="blue" 직접 실행
  |              MY_COLOR="blue"     |
  |              echo 실행           echo 실행
  |              종료 (변수 소멸)     |
  |                                  MY_COLOR 유지됨
  v
  MY_COLOR 없음
```

> 이 차이 때문에 `~/.bashrc`를 수정한 뒤 `source ~/.bashrc`로 적용하는 것이다. `./~/.bashrc`로 실행하면 서브셸에서 실행되어 현재 셸에는 아무 변화가 없다.

---

## 변수와 환경 변수 활용

앞서 Bash 셸의 구조와 스크립트 파일을 만들고 실행하는 방법까지 익혔다. 하지만 스크립트가 유용해지려면 **데이터를 임시로 저장해두고, 필요할 때 꺼내 쓰는** 수단이 필요하다. 그것이 바로 변수다. 프로그래밍에서 변수가 "값을 담는 상자"인 것처럼, 셸에서도 명령어 결과나 설정값을 변수에 담아두고 스크립트 전체에서 재사용할 수 있다.

### 셸 변수 (지역 변수)

셸 변수는 현재 셸 세션 내부에서만 유효하며, 자식 프로세스에게 상속되지 않는다.

```bash
# 변수 선언 — = 좌우에 공백이 있으면 오류 발생
MY_NAME="Rocky"

# 변수 참조 — $ 기호를 앞에 붙인다
echo $MY_NAME        # Rocky
echo "${MY_NAME}"    # Rocky (중괄호로 변수 경계를 명확히)

# 잘못된 선언 (공백 주의)
MY_NAME = "Rocky"    # bash: MY_NAME: command not found
```

**변수 명명 규칙**


| 규칙                              | 예시                              |
| ------------------------------- | ------------------------------- |
| 영문 대소문자, 숫자, 밑줄(`_`) 사용 가능      | `LOG_DIR`, `count`, `file_name` |
| 숫자로 시작할 수 없음                    | `1st_var` (불가), `var1` (가능)     |
| 대소문자를 구분함                       | `Name`과 `name`은 다른 변수           |
| 관례적으로 환경 변수는 대문자, 지역 변수는 소문자 사용 | `PATH` (환경), `result` (지역)      |


 

### 환경 변수 (전역 변수)

환경 변수는 `export` 키워드로 선언하여 자식 프로세스에게도 상속되는 변수다. 시스템 전체 동작에 영향을 미치는 핵심 설정값들이 환경 변수로 관리된다.

```bash
# 셸 변수를 환경 변수로 승격
export MY_VAR="hello"

# 선언과 동시에 환경 변수로 설정
export LOG_LEVEL="debug"

# 현재 설정된 모든 환경 변수 확인
env
# 또는
printenv
```

```
┌──────────────── 부모 셸 ────────────────────────┐
│                                               │
│  LOCAL_VAR="local"    ← 셸 변수 (여기만)         │
│  export ENV_VAR="env" ← 환경 변수 (상속됨)        │
│                                               │
│  ┌──────────── 자식 프로세스 ──────────────────┐ │
│  │                                         │  │
│  │  echo $LOCAL_VAR  → (비어 있음)           │  │
│  │  echo $ENV_VAR    → "env" (상속됨)        │  │
│  │                                         │  │
│  └─────────────────────────────────────────┘  │
└───────────────────────────────────────────────┘
```

 

### 주요 시스템 환경 변수


| **변수**      | **설명**                        | **확인 명령어**       |
| ----------- | ----------------------------- | ---------------- |
| `$PATH`     | 실행 파일을 탐색하는 디렉터리 목록 (콜론으로 구분) | `echo $PATH`     |
| `$HOME`     | 현재 사용자의 홈 디렉터리 경로             | `echo $HOME`     |
| `$USER`     | 현재 로그인한 사용자 이름                | `echo $USER`     |
| `$SHELL`    | 현재 사용자의 기본 로그인 셸 경로           | `echo $SHELL`    |
| `$PWD`      | 현재 작업 디렉터리 경로                 | `echo $PWD`      |
| `$HOSTNAME` | 시스템의 호스트 이름                   | `echo $HOSTNAME` |
| `$LANG`     | 시스템 로케일(언어) 설정                | `echo $LANG`     |
| `$PS1`      | 명령 프롬프트 형식 정의                 | `echo $PS1`      |
| `$?`        | 직전 명령어의 종료 코드 (0=성공)          | `echo $?`        |
| `$$`        | 현재 셸의 PID                     | `echo $$`        |
| `$!`        | 마지막으로 백그라운드에 보낸 프로세스의 PID     | `echo $!`        |


 

### 특수 변수 (스크립트 매개변수)

셸 스크립트 실행 시 전달되는 인자(Argument)를 참조하는 특수 변수들이다. 인자란 스크립트를 실행할 때 **스크립트 이름 뒤에 공백으로 구분하여 넘겨주는 값**들을 말한다. 프로그래밍 언어에서 함수에 파라미터를 넘기는 것과 같은 원리이며, 스크립트 내부에서 `$1`, `$2` 등의 번호로 순서대로 꺼내 쓸 수 있다.

```
./args_demo.sh  apple   banana   cherry
       |          |        |        |
      $0         $1       $2       $3
  (스크립트명)  (1번째)  (2번째)  (3번째)

                  $# = 3  (인자 개수)
                  $@ = apple banana cherry (전체)
```

```bash
#!/bin/bash
# 파일명: args_demo.sh
# 실행: ./args_demo.sh apple banana cherry

echo "스크립트 이름: $0"      # ./args_demo.sh
echo "첫 번째 인자: $1"       # apple
echo "두 번째 인자: $2"       # banana
echo "세 번째 인자: $3"       # cherry
echo "전체 인자 개수: $#"     # 3
echo "모든 인자 (문자열): $*"  # apple banana cherry
echo "모든 인자 (배열): $@"   # apple banana cherry
```

```bash
$ ./args_demo.sh apple banana cherry
스크립트 이름: ./args_demo.sh
첫 번째 인자: apple
두 번째 인자: banana
세 번째 인자: cherry
전체 인자 개수: 3
모든 인자 (문자열): apple banana cherry
모든 인자 (배열): apple banana cherry
```


| **변수**    | **의미**                       |
| --------- | ---------------------------- |
| `$0`      | 실행된 스크립트의 이름(경로)             |
| `$1 ~ $9` | 1번째 ~ 9번째 위치 매개변수            |
| `${10}`   | 10번째 이후는 중괄호 필수              |
| `$#`      | 전달된 인자의 총 개수                 |
| `$`*      | 모든 인자를 하나의 문자열로 취급           |
| `$@`      | 모든 인자를 개별 문자열로 취급 (반복문에서 권장) |
| `$?`      | 직전 명령어의 종료 코드 (0=성공, 그 외=실패) |
| `$$`      | 현재 셸(스크립트)의 PID              |
| `$!`      | 마지막으로 백그라운드에 보낸 프로세스의 PID    |


> `**$`* vs `$@`의 실질적 차이:** 평소에는 동일하게 동작하지만, **큰따옴표로 감쌌을 때** 차이가 드러난다. `"$*"`는 모든 인자를 `"apple banana cherry"` 하나의 덩어리로 합치고, `"$@"`는 `"apple" "banana" "cherry"` 각각 개별 문자열로 유지한다. 인자에 공백이 포함될 수 있는 상황(파일명 등)에서는 반드시 `"$@"`를 써야 안전하다.

```bash
# $* vs $@ 차이 확인
#!/bin/bash
# 실행: ./test.sh "hello world" foo

echo '--- "$*" ---'
for arg in "$*"; do
    echo "  -> $arg"
done
# 출력:
#   -> hello world foo     ← 전부 하나로 합쳐짐

echo '--- "$@" ---'
for arg in "$@"; do
    echo "  -> $arg"
done
# 출력:
#   -> hello world         ← 개별 인자 유지
#   -> foo
```

 

### 변수 확장과 따옴표 규칙

따옴표 종류에 따라 변수 확장(치환) 동작이 달라지므로 정확히 구분해야 한다.


| **표기**         | **변수 확장**       | **예시**                    | **결과**                   |
| -------------- | --------------- | ------------------------- | ------------------------ |
| 큰따옴표 `" "`     | 수행함             | `echo "Hello $USER"`      | `Hello jangwoo`          |
| 작은따옴표 `' '`    | 수행 안 함 (문자 그대로) | `echo 'Hello $USER'`      | `Hello $USER`            |
| 백틱 `` 또는 `$()` | 명령어 치환          | `echo "Today is $(date)"` | `Today is Thu Apr 9 ...` |


```bash
NAME="World"

echo "Hello, $NAME"      # Hello, World    (변수 치환됨)
echo 'Hello, $NAME'      # Hello, $NAME    (문자 그대로)
echo "Files: $(ls | wc -l)"  # Files: 42  (명령어 치환)
```

---

## 표준 입력·출력·리다이렉션과 파이프 처리

앞서 변수를 통해 스크립트 내부에 **데이터를 저장하고 꺼내 쓰는 방법**을 익혔다. 하지만 실제 시스템 관리에서는 명령어의 실행 결과를 파일에 기록하거나, 파일의 내용을 명령어에 넣어주거나, 여러 명령어의 출력을 체인처럼 연결하는 작업이 끊임없이 발생한다. 이러한 **데이터의 흐름을 자유자재로 제어하는 기술**이 리다이렉션과 파이프다.

### 표준 스트림의 구조

리눅스의 모든 프로세스는 생성과 동시에 커널로부터 세 개의 파일 디스크립터(File Descriptor, fd)를 부여받는다. 이 세 개의 데이터 통로를 통해 입력을 받고, 결과와 오류를 출력한다.

```
                    ┌─────────────────────────┐
  키보드/파일          │                         │       터미널/파일
 ──────────────→    │     프로세스 (ls 등)       │  ──────────────→  정상 출력
   stdin (fd 0)     │                         │    stdout (fd 1)
                    │                         │
                    │                         │  ──────────────→  오류 출력
                    └─────────────────────────┘    stderr (fd 2)
```


| **스트림** | **파일 디스크립터** | **기본 연결** | **용도**        |
| ------- | ------------ | --------- | ------------- |
| stdin   | 0            | 키보드       | 프로세스에 데이터를 입력 |
| stdout  | 1            | 터미널 화면    | 정상 실행 결과 출력   |
| stderr  | 2            | 터미널 화면    | 오류 메시지 출력     |


 

### 리다이렉션 (Redirection)

리다이렉션은 표준 스트림의 방향을 변경하여, 화면 대신 파일로 출력하거나 파일에서 입력을 읽어오는 기법이다.

**출력 리다이렉션**


| **연산자** | **동작**                    | **예시**                                |
| ------- | ------------------------- | ------------------------------------- |
| `>`     | stdout을 파일로 보냄 (덮어쓰기)     | `ls -la > filelist.txt`               |
| `>>`    | stdout을 파일에 추가 (이어쓰기)     | `echo "log" >> app.log`               |
| `2>`    | stderr을 파일로 보냄 (덮어쓰기)     | `find / -name "*.conf" 2> errors.log` |
| `2>>`   | stderr을 파일에 추가 (이어쓰기)     | `command 2>> errors.log`              |
| `&>`    | stdout과 stderr 모두 파일로 보냄  | `command &> all_output.log`           |
| `2>&1`  | stderr를 stdout과 같은 곳으로 보냄 | `command > output.log 2>&1`           |


```bash
# stdout만 파일로 저장, 오류는 화면에 출력
ls /etc /nonexistent > result.txt
# result.txt ← /etc 디렉터리 내용
# 화면 ← "ls: cannot access '/nonexistent': No such file or directory"

# stdout과 stderr를 각각 다른 파일로 분리
ls /etc /nonexistent > success.txt 2> error.txt

# stdout과 stderr를 모두 하나의 파일로 통합
ls /etc /nonexistent &> all.txt

# 출력을 완전히 버리기 (블랙홀)
command > /dev/null 2>&1
```

**입력 리다이렉션**


| **연산자** | **동작**                                  | **예시**                         |
| ------- | --------------------------------------- | ------------------------------ |
| `<`     | 파일에서 stdin으로 읽어옴                        | `wc -l < access.log`           |
| `<<`    | Here Document: 지정한 종료 문자열까지를 stdin으로 전달 | 아래 예시 참조                       |
| `<<<`   | Here String: 문자열을 stdin으로 전달            | `grep "error" <<< "$LOG_TEXT"` |


```bash
# 입력 리다이렉션: 파일을 stdin으로
wc -l < /etc/passwd

# Here Document: 여러 줄의 텍스트를 stdin으로 전달
cat <<EOF
서버 이름: $(hostname)
날짜: $(date)
사용자: $USER
EOF

# Here String: 한 줄의 문자열을 stdin으로
grep "root" <<< "root:x:0:0:root:/root:/bin/bash"
```

 

### 파이프 (Pipe)

파이프(`|`)는 앞 명령어의 stdout을 뒤 명령어의 stdin으로 연결하여, 여러 명령어를 체인처럼 이어 붙이는 메커니즘이다. 리눅스의 "작은 도구를 조합하여 큰 일을 수행한다"는 철학의 핵심이다.

```
┌─────────┐  stdout   ┌─────────┐  stdout   ┌─────────┐
│ 명령어 1  │ ────────→ │ 명령어 2 │ ────────→ │ 명령어 3  │ → 최종 출력
│  (ls)   │   stdin   │  (grep) │   stdin   │  (wc)   │
└─────────┘           └─────────┘           └─────────┘
```

```bash
# /etc 아래 .conf 파일의 개수를 세기
ls /etc | grep "\.conf$" | wc -l

# 현재 로그인한 사용자 목록에서 중복 제거 후 정렬
who | awk '{print $1}' | sort | uniq

# 프로세스 목록에서 특정 데몬 찾기
ps -ef | grep sshd | grep -v grep

# 디스크 사용량 상위 5개 디렉터리
du -sh /var/* 2>/dev/null | sort -rh | head -5
```

 

### 자주 사용하는 파이프 조합 필터


| **명령어**         | **역할**                       | **파이프 활용 예시**          |
| --------------- | ---------------------------- | ---------------------- |
| `grep`          | 패턴과 일치하는 줄 필터링               | `cat /var/log/messages |
| `sort`          | 줄 단위 정렬                      | `du -sh *              |
| `uniq`          | 연속된 중복 줄 제거 (정렬 후 사용)        | `sort access.log       |
| `wc`            | 줄(-l), 단어(-w), 바이트(-c) 수 카운트 | `cat file.txt          |
| `head` / `tail` | 앞/뒤 N줄 출력                    | `journalctl            |
| `awk`           | 필드(열) 기반 텍스트 처리              | `df -h                 |
| `sed`           | 스트림 편집기 (치환, 삭제)             | `echo "hello"          |
| `cut`           | 특정 필드나 문자 범위 추출              | `echo "a:b:c"          |
| `tee`           | stdout을 화면과 파일에 동시 출력        | `ls                    |
| `xargs`         | stdin을 명령어의 인자로 변환           | `find . -name "*.log"  |


---

## 조건문과 반복문을 활용한 기초 스크립트 작성

여기까지 **변수**(데이터 저장)와 **입출력/파이프**(데이터 이동)를 다뤘다. 이제 이 재료들 위에 **"만약 ~이면 ~하고, 아니면 ~한다"** 또는 **"목록의 각 항목마다 ~를 반복한다"** 같은 논리적 흐름을 얹는 방법을 배운다. 조건문과 반복문이 더해지면 비로소 사람의 판단이 필요하던 작업을 스크립트가 스스로 처리할 수 있게 된다.

### 조건문 (if / elif / else)

셸 스크립트에서 조건에 따라 분기 처리를 수행하는 핵심 구문이다.

**기본 구조**

```bash
if [ 조건식 ]; then
    # 조건이 참일 때 실행
elif [ 다른 조건 ]; then
    # 위 조건이 거짓이고 이 조건이 참일 때 실행
else
    # 모든 조건이 거짓일 때 실행
fi
```

> `**[ ]`와 `[[ ]]`의 차이:** `[ ]`는 POSIX 호환 test 명령의 별칭이며, `[[ ]]`는 Bash 확장 구문으로 패턴 매칭(`=~`), 논리 연산자(`&&`, `||`)를 직접 사용할 수 있다. Bash 스크립트에서는 `[[ ]]` 사용이 권장된다.

 

**파일 검사 연산자**


| **연산자**   | **의미**                   |
| --------- | ------------------------ |
| `-e FILE` | 파일이 존재하는가                |
| `-f FILE` | 일반 파일인가 (디렉터리 제외)        |
| `-d FILE` | 디렉터리인가                   |
| `-r FILE` | 읽기 권한이 있는가               |
| `-w FILE` | 쓰기 권한이 있는가               |
| `-x FILE` | 실행 권한이 있는가               |
| `-s FILE` | 파일 크기가 0보다 큰가 (비어있지 않은가) |


**문자열 비교 연산자**


| **연산자**     | **의미**               |
| ----------- | -------------------- |
| `=` 또는 `==` | 두 문자열이 같은가           |
| `!=`        | 두 문자열이 다른가           |
| `-z STRING` | 문자열의 길이가 0인가 (비어있는가) |
| `-n STRING` | 문자열의 길이가 0이 아닌가      |


**정수 비교 연산자**


| **연산자** | **의미**                    | **기호 대응** |
| ------- | ------------------------- | --------- |
| `-eq`   | 같다 (equal)                | `==`      |
| `-ne`   | 다르다 (not equal)           | `!=`      |
| `-gt`   | 크다 (greater than)         | `>`       |
| `-ge`   | 크거나 같다 (greater or equal) | `>=`      |
| `-lt`   | 작다 (less than)            | `<`       |
| `-le`   | 작거나 같다 (less or equal)    | `<=`      |


 

**예시: 디스크 사용량 경고 스크립트**

```bash
#!/bin/bash

USAGE=$(df / | awk 'NR==2 {print $5}' | tr -d '%')
THRESHOLD=80

if [ "$USAGE" -ge "$THRESHOLD" ]; then
    echo "[경고] 루트 파티션 사용량이 ${USAGE}%로 임계값(${THRESHOLD}%)을 초과했습니다."
else
    echo "[정상] 루트 파티션 사용량: ${USAGE}%"
fi
```

 

### case 문

여러 값에 대한 분기 처리를 깔끔하게 표현할 때 사용한다. `if-elif` 체인보다 가독성이 우수하다.

```bash
#!/bin/bash

case "$1" in
    start)
        echo "서비스를 시작합니다."
        ;;
    stop)
        echo "서비스를 중지합니다."
        ;;
    restart)
        echo "서비스를 재시작합니다."
        ;;
    status)
        echo "서비스 상태를 확인합니다."
        ;;
    *)
        echo "사용법: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
```

 

### 반복문 — for

지정된 목록의 각 항목에 대해 동일한 작업을 반복 수행한다.

```bash
# 기본 형태: 목록 기반 반복
for item in apple banana cherry; do
    echo "과일: $item"
done

# 파일 목록 반복
for file in /etc/*.conf; do
    echo "설정 파일: $file"
done

# 숫자 범위 반복 (Bash 확장)
for i in {1..5}; do
    echo "번호: $i"
done

# C 스타일 반복
for ((i=0; i<10; i++)); do
    echo "인덱스: $i"
done

# 명령어 결과 반복
for user in $(cut -d: -f1 /etc/passwd); do
    echo "사용자: $user"
done
```

 

### 반복문 — while

조건이 참인 동안 계속 반복한다. 파일을 한 줄씩 읽는 패턴에서 매우 자주 활용된다.

```bash
# 기본 카운터 반복
count=1
while [ $count -le 5 ]; do
    echo "카운트: $count"
    count=$((count + 1))
done

# 파일을 한 줄씩 읽기 (가장 실무적인 패턴)
while IFS= read -r line; do
    echo "줄 내용: $line"
done < /etc/hostname

# 무한 루프 (서비스 감시 등)
while true; do
    if ! systemctl is-active --quiet httpd; then
        systemctl start httpd
        echo "$(date): httpd 재시작" >> /var/log/httpd_watch.log
    fi
    sleep 60
done
```

 

### 반복문 — until

`while`과 반대로, 조건이 거짓인 동안 반복하다가 참이 되면 종료한다.

```bash
# 특정 서비스가 시작될 때까지 대기
until systemctl is-active --quiet sshd; do
    echo "sshd 시작 대기 중..."
    sleep 2
done
echo "sshd가 활성화되었습니다."
```

 

### 함수 (Function)

반복적으로 사용하는 코드 블록을 재사용 가능한 단위로 묶어 정의한다. 스크립트의 가독성과 유지보수성을 크게 향상시킨다.

```bash
#!/bin/bash

# 함수 정의
log_message() {
    local level="$1"
    local message="$2"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$level] $message"
}

check_service() {
    local service_name="$1"

    if systemctl is-active --quiet "$service_name"; then
        log_message "INFO" "${service_name} 정상 동작 중"
        return 0
    else
        log_message "WARN" "${service_name} 비활성 상태"
        return 1
    fi
}

# 함수 호출
log_message "INFO" "시스템 점검을 시작합니다."
check_service "sshd"
check_service "crond"
```

> `**local` 키워드:** 함수 내부에서 선언한 변수를 해당 함수 스코프로 제한한다. `local`을 쓰지 않으면 변수가 전역으로 노출되어 의도치 않은 충돌이 발생할 수 있다.

---

## 간단한 관리 작업 자동화 실습

지금까지 익힌 변수(3단계), 입출력/파이프(4단계), 조건문/반복문(5단계)을 **실제 시스템 관리 시나리오에 종합 적용**하는 단계다. 각 실습은 실무에서 자주 마주치는 작업을 스크립트로 자동화하는 예시이며, 앞에서 배운 문법 요소들이 어떻게 조합되는지를 직접 확인할 수 있다.

### 실습 1: 시스템 상태 리포트 스크립트

여러 시스템 정보를 한 번에 수집하여 보고하는 자동화 스크립트다.

```bash
#!/bin/bash
# system_report.sh — 시스템 상태 요약 리포트

REPORT_FILE="/tmp/system_report_$(date +%Y%m%d_%H%M%S).txt"

{
    echo "========================================="
    echo "  시스템 상태 리포트"
    echo "  생성 시각: $(date)"
    echo "========================================="
    echo ""

    echo "--- 호스트 정보 ---"
    echo "호스트명: $(hostname)"
    echo "커널 버전: $(uname -r)"
    echo "가동 시간: $(uptime -p)"
    echo ""

    echo "--- CPU 로드 ---"
    echo "Load Average: $(cat /proc/loadavg | awk '{print $1, $2, $3}')"
    echo "CPU 코어 수: $(nproc)"
    echo ""

    echo "--- 메모리 사용 ---"
    free -h
    echo ""

    echo "--- 디스크 사용 ---"
    df -h /
    echo ""

    echo "--- 실패한 서비스 ---"
    systemctl --failed --no-pager
    echo ""

    echo "--- 최근 로그인 사용자 ---"
    last -5
} > "$REPORT_FILE"

echo "리포트가 생성되었습니다: $REPORT_FILE"
```

 

### 실습 2: 로그 파일 자동 백업 스크립트

지정된 디렉터리의 로그 파일을 날짜별로 압축하여 백업하는 스크립트다.

```bash
#!/bin/bash
# log_backup.sh — 로그 파일 자동 백업

LOG_DIR="/var/log"
BACKUP_DIR="/backup/logs"
DATE=$(date +%Y%m%d)
RETENTION_DAYS=30

# 백업 디렉터리 확인 및 생성
if [ ! -d "$BACKUP_DIR" ]; then
    mkdir -p "$BACKUP_DIR"
    echo "백업 디렉터리를 생성했습니다: $BACKUP_DIR"
fi

# 로그 파일 압축 백업
ARCHIVE="${BACKUP_DIR}/logs_${DATE}.tar.gz"
tar -czf "$ARCHIVE" -C "$LOG_DIR" messages secure cron 2>/dev/null

if [ $? -eq 0 ]; then
    echo "[성공] 백업 완료: $ARCHIVE"
else
    echo "[실패] 백업 중 오류 발생" >&2
    exit 1
fi

# 오래된 백업 정리
DELETED=$(find "$BACKUP_DIR" -name "logs_*.tar.gz" -mtime +${RETENTION_DAYS} -delete -print | wc -l)
echo "${DELETED}개의 오래된 백업을 삭제했습니다."
```

 

### 실습 3: 다중 서비스 상태 점검 스크립트

여러 서비스를 한꺼번에 점검하고, 비활성 서비스는 자동 재시작을 시도하는 스크립트다.

```bash
#!/bin/bash
# service_check.sh — 핵심 서비스 점검 및 자동 복구

SERVICES=("sshd" "crond" "firewalld" "rsyslog")
LOG="/var/log/service_check.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG"
}

log "===== 서비스 점검 시작 ====="

for svc in "${SERVICES[@]}"; do
    if systemctl is-active --quiet "$svc"; then
        log "[OK] $svc: 정상 동작 중"
    else
        log "[WARN] $svc: 비활성 상태 → 재시작 시도"
        systemctl start "$svc"

        if systemctl is-active --quiet "$svc"; then
            log "[OK] $svc: 재시작 성공"
        else
            log "[FAIL] $svc: 재시작 실패 — 수동 점검 필요"
        fi
    fi
done

log "===== 서비스 점검 완료 ====="
```

**cron을 활용한 자동화 등록**

```bash
# 매시 정각마다 서비스 점검 스크립트 실행
crontab -e

# 아래 줄 추가
0 * * * * /usr/local/bin/service_check.sh
```

 

### 실습 4: Podman 컨테이너 점검 및 자동 복구

**Podman**으로 같은 자동화 구조를 구성하는 방식은 RHEL·Rocky Linux 계열에서도 실무에서 많이 쓰인다. 루트 데몬 없이 컨테이너를 다루고, `systemd`와 연동하기도 수월하다. 자동화 스크립트는 **점검할 대상(실행 중인 컨테이너)**이 있어야 의미가 있으므로, 먼저 테스트용 컨테이너를 띄운 뒤 스크립트를 검증한다.

#### 1) Podman 설치 및 버전 확인

```bash
podman --version
```

Rocky Linux / RHEL 10에서는 패키지 매니저로 설치한다.

```bash
sudo dnf install -y podman
```

> Debian·Ubuntu 등에서는 `sudo apt install -y podman` 과 동일한 목적의 설치 명령을 사용한다.

#### 2) 테스트용 컨테이너 실행

자동화의 대상으로 **nginx** 컨테이너를 백그라운드로 실행한다.

```bash
podman run -d --name test-nginx -p 8080:80 docker.io/library/nginx
podman ps
```

호스트에서 `curl -sI http://127.0.0.1:8080` 등으로 응답을 확인할 수 있다.

#### 3) 의도적으로 중지해 두기 (복구 스크립트 검증용)

```bash
podman stop test-nginx
podman ps -a --filter "name=test-nginx"
```

이 상태에서 아래 `podman_health_check.sh`를 실행하면 **중지된 컨테이너를 다시 시작**하는 흐름을 확인할 수 있다.

#### 4) `podman_health_check.sh` — 상태 점검 + 자동 복구

```bash
#!/bin/bash
# podman_health_check.sh — Podman 컨테이너 상태 점검 및 자동 복구

LOG="/home/jangwoojung/Documents/week6_practice/podman_health.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG"
}

log "===== Podman 상태 점검 시작 ====="

# 중지된 컨테이너 재시작
STOPPED=$(podman ps -a --filter "status=exited" --format "{{.Names}}")

for c in $STOPPED; do
    log "[WARN] 컨테이너 중지됨: $c → 재시작 시도"
    podman start "$c"

    if [ $? -eq 0 ]; then
        log "[OK] $c 재시작 성공"
    else
        log "[FAIL] $c 재시작 실패"
    fi
done

# unhealthy 컨테이너 재시작
UNHEALTHY=$(podman ps --format "{{.Names}} {{.Status}}" | grep -i unhealthy | awk '{print $1}' || true)

for c in $UNHEALTHY; do
    [ -z "$c" ] && continue
    log "[WARN] $c unhealthy → 재시작"
    podman restart "$c"
done

# (옵션) 정리는 나중에 따로 실행 추천
# log "[INFO] Podman 정리 작업 시작"
# podman container prune -f

log "===== Podman 상태 점검 완료 ====="
```

> `**podman container prune -f`:** 중지(exited) 상태인 컨테이너만 삭제한다. 다른 작업이 나중에 쓸 **이미지**는 그대로 둔다.  
> `**podman system prune -f`:** 범위가 넓어 미사용 이미지·네트워크·캐시 등까지 지울 수 있어, 스크립트에서는 위처럼 **주석으로 두고 필요할 때만** 수동 실행하는 편이 안전하다.

#### 5) 스크립트 배치 및 실행

로그를 `/var/log`에 쓰려면 root 권한이 필요하다.

```bash
sudo install -m 755 podman_health_check.sh /usr/local/bin/podman_health_check.sh
sudo /usr/local/bin/podman_health_check.sh
sudo tail -30 /var/log/podman_health.log
podman ps
```

로그 경로를 홈 디렉터리 등으로 바꾸면 `sudo` 없이도 동작 흐름만 연습할 수 있다.

 

### ** systemd 타이머를 활용한 스크립트 자동화

> **RHEL 10에서의 변화:** RHEL 10 / Rocky Linux 10 환경에서는 `cronie` 패키지가 더 이상 기본 설치에 포함되지 않으며, 시스템 유지보수 작업들이 `systemd` 타이머(`.timer` 유닛)로 이관되는 추세가 가속화되고 있다. 새로운 자동화 작업을 설정할 때는 cron 대신 systemd 타이머 사용이 권장된다.

**서비스 유닛 파일 작성**

```bash
sudo tee /etc/systemd/system/podman-health-check.service >/dev/null <<'EOF'
[Unit]
Description=Podman 컨테이너 상태 점검 및 자동 복구

[Service]
Type=oneshot
ExecStart=/usr/local/bin/podman_health_check.sh
EOF
```

**타이머 유닛 파일 작성**

```bash
sudo tee /etc/systemd/system/podman-health-check.timer >/dev/null <<'EOF'
[Unit]
Description=Podman 점검 주기 실행 타이머

[Timer]
OnCalendar=*-*-* 06:00:00
Persistent=true

[Install]
WantedBy=timers.target
EOF
```

**타이머 활성화**

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now podman-health-check.timer

# 등록된 타이머 확인
systemctl list-timers --all | grep podman-health

# 즉시 한 번 실행해 보기 (타이머 대기 없이)
sudo systemctl start podman-health-check.service
sudo journalctl -u podman-health-check.service -n 30 --no-pager
```


| **Timer 지시어**      | **의미**                                                       |
| ------------------ | ------------------------------------------------------------ |
| `OnCalendar=`      | 달력 기반 스케줄 (cron과 유사). 예: `daily`, `weekly`, `*-*-* 06:00:00` |
| `OnBootSec=`       | 부팅 후 지정 시간이 지나면 실행. 예: `OnBootSec=5min`                      |
| `OnUnitActiveSec=` | 유닛이 마지막 활성화된 후 지정 시간마다 반복. 예: `OnUnitActiveSec=1h`           |
| `Persistent=true`  | 시스템이 꺼져 있어 놓친 스케줄을 부팅 후 즉시 보상 실행                             |


 

**cron vs systemd 타이머 비교**


| **항목**   | **cron** | **systemd 타이머**                         |
| -------- | -------- | --------------------------------------- |
| 로그 확인    | 별도 설정 필요 | `journalctl -u <서비스명>` 통합 로그            |
| 의존성 관리   | 불가       | `After=`, `Wants=` 등 유닛 의존성 활용 가능       |
| 자원 제어    | 불가       | cgroup 기반 `MemoryMax`, `CPUQuota` 적용 가능 |
| 놓친 작업 처리 | 지원 안 함   | `Persistent=true`로 보상 실행                |
| 초 단위 스케줄 | 지원 안 함   | `OnCalendar`로 초 단위 지정 가능                |


---

## 스크립트 디버깅 기법

스크립트를 작성하고 자동화까지 구현했다면, 마지막으로 필요한 것은 **"잘못된 부분을 빠르게 찾아내는 능력"**이다. 스크립트가 길어지거나 복잡해질수록 예상과 다르게 동작하는 상황이 반드시 발생한다. 아래 기법들을 활용하면 오류 원인을 체계적으로 추적할 수 있다.


| **기법**    | **명령어/설정**          | **설명**                           |
| --------- | ------------------- | -------------------------------- |
| 구문 검사     | `bash -n script.sh` | 스크립트를 실행하지 않고 문법 오류만 검사          |
| 실행 추적     | `bash -x script.sh` | 각 줄이 실행되기 전에 명령어를 `+` 접두사와 함께 출력 |
| 스크립트 내부   | `set -x` / `set +x` | 특정 구간만 추적 활성화/비활성화               |
| 미정의 변수 감지 | `set -u`            | 정의되지 않은 변수를 사용하면 오류로 처리          |
| 오류 즉시 중단  | `set -e`            | 명령어가 실패(종료 코드 ≠ 0)하면 스크립트 즉시 중단  |
| 파이프 오류 감지 | `set -o pipefail`   | 파이프라인 중 하나라도 실패하면 전체를 실패로 처리     |


 

**권장 디버깅 순서**

1. `**bash -n`** — 실행 없이 문법만 검사. 괄호·`fi` 누락 등 구조 오류를 먼저 잡는다.
2. `**bash -x`** — 실행하면서 각 줄이 어떻게 확장·실행되는지 추적. "어디까지 실행되고 멈췄는지"를 본다.
3. `**echo` / 임시 로그** — 변수 값이 예상과 다른지 중간에 `echo "DEBUG: var=$var"`로 확인.
4. `**set -euo pipefail`** — 수정이 끝난 뒤 상단에 두어 재발 방지(실패·오타·파이프 오류를 조기에 드러냄).

**예시 1: 구문 오류 — `bash -n`으로 잡기**

`if` 블록을 닫는 `fi`를 빠뜨린 스크립트를 가정한다.

```bash
#!/bin/bash
if [ -f /etc/hostname ]; then
    echo "파일 있음"
# fi 누락 — 의도적 오류
```

```bash
$ bash -n broken_if.sh
broken_if.sh: line 6: syntax error: unexpected end of file
```

실행(`./broken_if.sh`) 전에 `bash -n`만으로 오류 위치를 알 수 있어, 운영 환경에서 위험한 "반쪽 실행"을 줄일 수 있다.

 

**예시 2: 실행 추적 — `bash -x`로 흐름 확인**

아래는 변수 이름에 오타가 있는 스크립트다. `VERSOIN`은 정의되지 않았으므로 빈 문자열로 확장된다.

```bash
#!/bin/bash
# debug_trace.sh
NAME="Rocky"
echo "Hello, $NAME"
echo "Version: $VERSOIN"   # 오타: VERSION 이어야 함
echo "끝"
```

```bash
$ bash -x debug_trace.sh
+ NAME=Rocky
+ echo 'Hello, Rocky'
Hello, Rocky
+ echo 'Version: '
Version:
+ echo 끝
끝
```

`bash -x` 출력에서 `+` 줄은 **셸이 실제로 실행하려고 확장한 뒤의 명령**이다. `+ echo 'Version: '`처럼 인자가 비어 있으면, 바로 위 스크립트에서 변수명 오타를 의심하면 된다.

 

**예시 3: 미정의 변수 — `set -u`로 즉시 중단**

```bash
#!/bin/bash
set -u
BACKUP_ROOT="/backup"
# 오타: BACKUP_ROOT vs BACKUP_RROT
tar -czf "$BACKUP_RROT/daily.tgz" /etc
echo "백업 완료"   # set -u 없으면 이 줄까지 실행될 수 있음
```

```bash
$ bash typo_backup.sh
typo_backup.sh: line 5: BACKUP_RROT: unbound variable
```

`set -u`가 없으면 빈 문자열로 치환되어 `/daily.tgz` 같은 엉뚱한 경로에 쓰거나, 조용히 잘못된 동작을 할 수 있다. Strict Mode는 이런 오타를 **조용한 데이터 손실 전에** 끊어 준다.

 

**예시 4: Bash Strict Mode 템플릿 + 구간만 추적**

```bash
#!/bin/bash
set -euo pipefail
# -e: 오류 시 즉시 중단
# -u: 미정의 변수 사용 시 오류
# -o pipefail: 파이프라인 오류 감지

prepare_data() {
    # 이 함수만 상세 추적하고 싶을 때
    set -x
    mkdir -p "$WORKDIR"
    cp -r "$SOURCE" "$WORKDIR/"
    set +x
}

WORKDIR="/tmp/work"
SOURCE="/etc/hostname"
prepare_data
echo "준비 완료"
```

> 실무에서는 스크립트 상단에 `set -euo pipefail`을 관례적으로 추가하여 안전망을 확보하는 것이 권장된다. 이를 **"Bash Strict Mode"**라고 부른다.

> `**set -e` 주의:** 일부 명령어는 실패가 "정상"인 경우도 있다(예: `grep`이 패턴을 못 찾으면 종료 코드 1). 이런 줄은 `grep ... || true`처럼 의도적으로 무시하거나, `if grep ...; then` 형태로 감싸야 스크립트가 중간에 끊기지 않는다.

---

## Ref.


| 주제                   | 문서                                                                                                                                                                                               |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Bash 셸 스크립트 작성 기초    | [Getting started with Bash scripting — RHEL 10](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/getting_started_with_bash_scripting/index)                             |
| 셸 환경 및 변수 설정         | [Configuring basic system settings — RHEL 10](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/configuring_basic_system_settings/index)                                 |
| systemd 타이머를 활용한 자동화 | [systemd 장치 파일을 사용하여 시스템 사용자 지정 및 최적화 — RHEL 10](https://docs.redhat.com/ko/documentation/red_hat_enterprise_linux/10/html/using_systemd_unit_files_to_customize_and_optimize_your_system/index) |
| Podman 컨테이너 구축·관리    | [컨테이너 빌드, 실행 및 관리 — RHEL 10](https://docs.redhat.com/ko/documentation/red_hat_enterprise_linux/10/html/building_running_and_managing_containers/index)                                           |
| 시스템 관리 및 자동화         | [System administration and management — RHEL 10](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/system_administration_and_management/index)                           |
| GNU Bash 공식 매뉴얼      | [Bash Reference Manual — GNU](https://www.gnu.org/software/bash/manual/bash.html)                                                                                                                |


