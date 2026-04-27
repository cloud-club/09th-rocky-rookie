# 셸 스크립트 및 자동화 기초

## 셸 (Shell)

`셸(Shell)`은 사용자와 운영체제 커널 사이를 연결하는 명령어 해석기입니다. 사용자가 입력한 명령을 해석해 커널에 전달하고, 그 결과를 사용자에게 돌려줍니다.

> 이름처럼 커널을 감싸는 "껍데기(Shell)"의 역할

- **bash**, zsh, sh, fish 등...

### 계층 구조

```
사용자 (User) --- 셸 (Shell) --- 커널 (Kernel) --- 하드웨어 (Hardware)
```

## 변수와 환경 변수

- 변수: 현재 실행 중인 셸이나 스크립트 내부에서만 유효한 변수
- 환경 변수: 현재 셸뿐만 아니라 이 셸에서 파생되는 자식 프로세스에도 전달되는 변수

```bash
#!/bin/bash
# = Shebang(셔뱅): Sharp(#) + Bang(!), 스크립트 파일 최상단에 위치하여 해당 파일을 실행할 인터프리터의 경로를 지정

# 변수 선언
APP_NAME="bk_app"
PORT=8080

# 환경 변수 선언
export APP_ENV="production"
export JAVA_HOME="/usr/lib/openjdk-11"
export PATH="$PATH:$JAVA_HOME/bin"

# 변수 참조
echo "이름은 $APP_NAME."
echo "포트 번호는 $PORT입니다."
echo $JAVA_HOME
```

## 표준 입출력 스트림

- 표준 입력 (stdin, 0): 사용자 또는 파일로부터 데이터를 읽어들이는 스트림
- 표준 출력 (stdout, 1): 프로세스가 처리한 데이터를 화면에 출력하는 스트림
- 표준 오류 출력 (stderr, 2): 오류 메시지를 출력하는 스트림

## 리다이렉션 (Redirection)

`리다이렉션(Redirection)`은 표준 입출력 스트림을 다른 스트림으로 전환하는 기능입니다.

- `>` (= 1>): 파일이 없으면 생성하고 있으면 기존 내용을 모두 지우고 새로 작성  
  e.g. `'ls -l > list.txt'`
- `2>`: 오류 메시지만 따로 분리하여 파일에 저장  
  e.g. `find / -name "test" 2> error.log`
- `&>` (= 2>&1): 정상 출력과 오류 메시지를 파일에 모두 저장  
  e.g. `dnf update &> update_result.log`
- `>>`: 파일이 없으면 생성하고 있으면 기존 내용의 맨 끝에 새로운 결과를 추가  
  e.g. `echo "completed" >> log.txt`
- `<`: 파일의 내용을 명령어의 입력 데이터로 사용  
  e.g. `sort < data.txt`

## 파이프 (Pipe)

`파이프(Pipe)`는 한 명령어의 표준 출력을 다음 명령어의 표준 입력으로 직접 연결하는 기능입니다.

```bash
#!/bin/bash

# messages 파일의 전체 내용을 출력하고 "error"라는 단어가 포함된 줄만 화면에 필터링하여 출력
cat /var/log/messages | grep "error"

# 현재 실행 중인 전체 프로세스 목록을 출력하고, "httpd" 프로세스만 찾아 해당 줄의 개수를 출력
ps -ef | grep "httpd" | wc -l
```

## 조건문과 반복문

```bash
#!/bin/bash

[ 5 -eq 5 ]
echo $?
# 0 (True)
(( 5 == 5 ))
echo $?
# 0 (True)
test 5 -eq 5
echo $?
# 0 (True)


# --- 조건문 ---

if [ condition ]; then
  # 조건이 참일 때 실행
elif [ other condition ]; then
  # 다른 조건이 참일 때 실행
else
  # 모든 조건이 거짓일 때 실행
fi

NUM=10
if [ "$NUM" -gt 5 ]; then
  echo "bigger than 5"
elif [ "$NUM" -eq 5 ]; then
  echo "equal to 5"
else
  echo "smaller than 5"
fi

if (( NUM > 5 )); then
  echo "bigger than 5"
elif (( NUM == 5 )); then
  echo "equal to 5"
else
  echo "smaller than 5"
fi

---

# 논리 연산자 - tmp.txt 파일이 존재하면 삭제
# [ 조건문 ] && 성공인 경우 실행 || 실패인 경우 실행
[ -f "tmp.txt" ] && rm "tmp.txt"


# --- 반복문 ---

for i in {1..5}; do
  echo "count: $i"
done

---

for SERVER in "web01" "web02" "db01"; do
  echo "server: $SERVER"
done

---

while [ condition ]; do
  # 참일 동안 반복 실행
done

COUNT=1
while [ "$COUNT" -le 3 ]; do
  echo "count: $COUNT"
  COUNT=$((COUNT + 1))
done

---

until [ 조건 ]; do
  # 거짓일 동안 반복 실행
done

COUNT=1
until [ "$COUNT" -gt 3 ]; do
  echo "count: $COUNT"
  COUNT=$((COUNT + 1))
done

```

## 작업 자동화 예시

```bash
#!/usr/bin/ksh

# HMC-FSP 연결 확인

INTERVAL=5
while true; do
  echo "=== [ $(date +"%Y-%m-%d %H:%M:%S") ] ==="
  lssysconn -r all
  sleep $INTERVAL
done

---

# 모니터링...
```
