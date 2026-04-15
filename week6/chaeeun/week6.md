## Bash 셸

- 셸(Shell) : 사용자와 커널 사이의 인터페이스
- Bash(Bourne Again SHell) : 리눅스에서 가장 널리 쓰이는 셸
- 커널은 직접 명령어를 못 알아듣기 때문에 유저가 원하는 내용을 해석하고 실행하는 기능
- 스크립트 = 여러 명령어를 파일에 저장해 순서대로 실행하는 텍스트 파일
- 컴파일이 아니라 해석(interpreting) 되어 실행

## 스크립트 기본 구조

```bash
#!/bin/bash

echo "Hello, Bash!"
echo "현재 디렉터리: $(pwd)"
echo "오늘 날짜: $(date +%Y-%m-%d)"
```

- 첫 줄 `#!/bin/bash` = shebang, 실행할 인터프리터를 선언
    - bash = 배시 셸 (Bash Shell)
    - sh = 본 셸 (Bourne Shell)
    - csh = 씨 셸 (C Shell)
    - ksh =  콘 셸 (Korn Shell)
    - perl = 펄
    - python = 파이썬
- `#`으로 시작하는 줄은 주석
- bash가 한줄씩 해석

### 실행 권한 부여 및 실행

```bash
chmod +x script.sh
./script.sh
```

### 특수 변수

| 변수 | 설명 |
| --- | --- |
| $0 | 현재 실행한 스크립트 이름 |
| $1 ~ $9 | 스크립트에 전달된 위치 인자 |
| $@ | 전달된 모든 인수 |
| $# | 전달된 인수의 개수 |
| $? | 이전 명령의 종료 코드 (0=성공) |
| $$ | 현재 셸의 PID |
| $! | 마지막 백그라운드 프로세스 PID |

## 일반 변수

- 현재 Bash 내부에서만 쓰는 값
- 선언 : `=` 양쪽에 공백 없이 작성
- 참조 : `$변수명` 또는 `${변수명}`
    - 중괄호 사용을 권장
- 삭제 : `unset 변수명`

### 선언 및 참조

```bash
name="홍길동"
age=30
greeting="안녕하세요, ${name}님!"

echo $greeting
echo "나이: ${age}세"
echo "내년 나이: $((age + 1))세"   # 산술 연산
```

## 환경 변수

- 셸이 실행하는 자식 프로세스에도 전달되는 값
- `export`로 선언
- 주요 환경 변수
    - $HOME : 현재 사용자의 홈 디렉터리
    - $USER : 현재 로그인된 사용자 이름
    - $PATH : 실행 파일 검색 경로 목록
    - $SHELL : 현재 사용 중인 셸 경로
    - $PWD : 현재 작업 디렉터리

### export

```bash
export MY_VAR="value"     # 환경 변수로 선언
export -p | grep MY_VAR   # export된 변수 확인
env                        # 전체 환경 변수 목록
```

### 기본값 설정 패턴

```bash
ENV=${ENV:-production}        # 미설정 시 기본값 사용 (변수에 대입 안 됨)
LOG_DIR=${LOG_DIR:=/var/log}  # 미설정 시 기본값 대입 후 사용
echo "환경: $ENV, 로그: $LOG_DIR"
```

## 표준 입출력

- 프로세스의 입출력을 다른 곳으로 연결

| 스트림 | 번호 | 설명 |
| --- | --- | --- |
| stdin | 0 | 표준 입력 |
| stdout | 1 | 표준 출력 |
| stderr | 2 | 표준 에러 |

### 리다이렉션

- `>` : 표준 출력 덮어쓰기
- `>>` : 표준 출력 이어쓰기
- `<` : 파일을 표준 입력으로 사용
- `2>` : 표준 에러를 파일로 보냄

```bash
echo "내용" > file.txt        # stdout → 파일 (덮어쓰기)
echo "추가" >> file.txt       # stdout → 파일 (이어쓰기)
wc -l < file.txt              # 파일 → stdin
ls /없는경로 2> error.log     # stderr → 파일
ls /없는경로 &> all.log       # stdout + stderr → 파일
ls /없는경로 2>/dev/null      # 에러 무시
```

### Here-document

- 여러 줄의 텍스트를 명령의 stdin으로 전달

```bash
cat <<EOF > config.txt
host=localhost
port=8080
debug=true
EOF
```

## 파이프

- `|` 기호로 앞 명령의 stdout을 다음 명령의 stdin으로 전달
- 작은 프로그램 여러 개를 이어 붙임

### 파이프 활용

```bash
# 에러 로그 유형별 집계
cat app.log | grep ERROR | awk '{print $4}' | sort | uniq -c | sort -rn | head -5

# 현재 디렉터리 파일 크기 상위 10개
du -sh * | sort -rh | head -10

# 특정 포트 사용 중인 프로세스
ss -tulnp | grep :8080
```

### tee

- 파이프 중간에서 파일에도 동시 저장

```bash
grep ERROR app.log | tee errors.txt | wc -l
```

## 조건문

### if / elif / else

```bash
score=85

if [[ $score -ge 90 ]]; then
  echo "A학점"
elif [[ $score -ge 80 ]]; then
  echo "B학점"
elif [[ $score -ge 70 ]]; then
  echo "C학점"
else
  echo "재시험"
fi
```

- `[[ ]]` 이중 대괄호 사용 권장
    - `&&`, `||` 논리 연산자 사용 가능
    - 패턴 매칭, 정규식(`=~`) 지원

### 자주 쓰는 조건

```bash
[ -f file ]   # 일반 파일 존재
[ -d dir ]    # 디렉터리 존재
[ -z "$var" ] # 문자열 길이 0
[ -n "$var" ] # 문자열 비어있지 않음
[ "$a" = "$b" ]
[ "$num" -gt 10 ]
```

| 연산자 | 설명 |
| --- | --- |
| -f | 파일 존재 |
| -d | 디렉터리 존재 |
| -z | 빈 문자열 |
| -n | 비어있지 않은 문자열 |
| -r | 읽기 가능 |
| -w | 쓰기 가능 |
- 대괄호 내부 공백 필수 중요!

### case

```bash
case "$1" in
  start)  echo "서비스 시작" ;;
  stop)   echo "서비스 중지" ;;
  status) echo "상태 확인" ;;
  *)      echo "사용법: $0 {start|stop|status}" ;;
esac
```

## 반복문

- 자동화는 주로 반복 처리에 사용

### for

```bash
# 목록 순회
for fruit in 사과 배 딸기; do
  echo "과일: $fruit"
done

# 범위
for i in {1..5}; do
  echo "번호: $i"
done

# 파일 글로브
for f in *.log; do
  echo "처리: $f"
done
```

### while

```bash
count=1
while [[ $count -le 3 ]]; do
  echo "카운트: $count"
  ((count++))
done
```

- `break` : 반복문 즉시 탈출
- `continue` : 현재 반복 건너뛰고 다음으로

### until

```bash
# 조건이 거짓인 동안 반복 (while의 반대)
until [[ $count -gt 5 ]]; do
  echo "카운트: $count"
  ((count++))
done
```
