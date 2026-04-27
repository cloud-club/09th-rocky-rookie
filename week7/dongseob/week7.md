# 1. 로그(Log)

로그는 시스템, 커널, 서비스, 애플리케이션이 동작하면서 남기는 이벤트 기록이다.
-> 아래에 대한 것들을 알 수 있음
* 무슨 일이 일어났는지
* 언제 일어났는지
* 누가 실행했는지
* 성공했는지 실패했는지
* 왜 실패했는지



# 2. RHEL 계열의 로그 구조

현대 RHEL 계열에서는 로그가 보통 아래 2개가 주요 축으로 관리됨

1. systemd-journald
2. rsyslog
// 보조적으로 커널 ring buffer, auditd 존재


## 2-1. 전체 구조

```text
프로그램 / 서비스 / 커널
        │
        ▼
 systemd-journald  <- systemd가 수집하는 중앙 로그
        │
        ├── journalctl 로 조회
        │
        └── rsyslog 로 전달 가능
                 │
                 ▼
             /var/log/*
```


* journald = 로그를 중앙 수집
* journalctl = journald 조회 도구
* rsyslog = 텍스트 로그 파일로 저장/분배
* /var/log/ = 전통적인 로그 파일 위치

로그는 계속 누적되므로, 운영 환경에서는 logrotate를 통해 로그 파일을 회전(rotating)하고 압축/삭제하여 디스크 사용량을 관리해야 함!!!


## 2-2. journald

systemd-journald : systemd 기반 로그 수집 데몬 + “로그 수집 허브” 같은 역할

특징:
* 커널 메시지 수집
* 서비스 stdout/stderr 수집
* syslog 메시지 수집
* 바이너리 형식으로 저장
* journalctl로 조회


기본 저장 위치:
- /run/log/journal/      : 휘발성 (재부팅 시 사라질 수 있음)
- /var/log/journal/      : 영구 저장 (설정 시 사용)

-> journald 로그는 기본적으로 휘발적일 수 있으므로,
운영 환경에서는 영구 저장 설정 여부를 확인하는 것이 중요!!!


## 2-3. rsyslog

rsyslog : 전통적인 텍스트 기반 로그 저장/전송 데몬

특징:
* /var/log/messages, /var/log/secure 같은 파일 관리
* facility / severity 기반 분류
* 원격 로그 서버 전송 가능
* 사람이 cat, tail, grep로 바로 볼 수 있음

-> rsyslog : **“텍스트 로그 관리자”**에 가까움


## 2-4. journald와 rsyslog 관계

둘은 경쟁 관계라기보다 서로 역할이 다른 협력 관계(운영에서 보통 함께 쓰임)

journald
* 중앙 수집
* systemd 친화적
* 서비스 단위 로그 조회 강함
* 부팅 단위 조회 강함

rsyslog
* 텍스트 파일 저장
* facility/severity 기반 분류
* grep/tail 친화적
* 외부 전송 강함



# 3. 주요 로그 저장 위치

전통적인 로그 파일은 보통 /var/log/ 아래에 있음

```bash
// 확인:
ls -lh /var/log/
```

```bash
// 로그 내용 확인 : 해당 경로 파일의 마지막 부분 숫자만큼(기본 10줄) 출력
tail -n 숫자 /var/log/messages
```


| 파일 경로 | 의미 |
|---|---|
| `/var/log/messages` | 일반 시스템 메시지. systemd 서비스 상태 변화, 네트워크 관련 이벤트, 데몬 동작 등의 종합 로그 |
| `/var/log/secure` | **인증 및 보안 관련** 로그. `sshd`, `sudo`, `su`, PAM 인증 메시지 등을 포함 |
| `/var/log/cron` | cron 작업 실행 로그 |
| `/var/log/maillog` | 메일 서비스 관련 로그 |
| `/var/log/boot.log` | 부팅 관련 로그. 부팅 중 서비스 시작/실패 메시지, 부팅 스크립트/서비스 상태 메시지 |
| `/var/log/dmesg` | 배포판/설정에 따라 저장되는 커널 메시지 |
| `/var/log/audit/audit.log` | **감사(audit) 및 SELinux 관련** 로그 |
| `/var/log/httpd/` | Apache 웹 서버 로그 |
| `/var/log/nginx/` | Nginx 웹 서버 로그 |
| `/var/log/wtmp` | 로그인/로그아웃 이력 저장용 바이너리 로그. 보통 `last` 명령으로 조회 |
| `/var/log/btmp` | 로그인 실패 이력 저장용 바이너리 로그. 보통 `lastb` 명령으로 조회 |
| `/var/log/lastlog` | 각 사용자별 마지막 로그인 정보 저장용 바이너리 로그. 보통 `lastlog` 명령으로 조회 |



# 4. 로그 메시지의 기본 구조

예)
`Apr 21 10:33:12 localhost sshd[1203]: Accepted password for dongseob from 192.168.0.10 port 50211 ssh2`

| 로그 구성 요소 | 설명 |
|---|---|
| `Apr 21 10:33:12` | 로그가 기록된 시각 |
| `localhost` | 로그를 남긴 시스템(=호스트) 이름 |
| `sshd[1203]` | 로그를 발생시킨 프로세스 이름과 PID |
| `Accepted password ...` | 실제 이벤트 내용 |

-> 로그 구조 : 시간 + 호스트 + 프로세스 + 메시지


# 5. 로그 레벨(Log Level)

로그는 심각도에 따라 레벨이 나뉘며 운영에서는 보통 **err**, **warning**, **crit** 이상의 로그를 우선적으로 확인
> 숫자가 낮을수록 더 심각한 로그 레벨

| 번호 | 레벨 | 의미 |
|---|---|---|
| 0 | `emerg` | 시스템 사용 불가 |
| 1 | `alert` | 즉시 조치 필요 |
| 2 | `crit` | 심각한 오류 |
| 3 | `err` | 오류 |
| 4 | `warning` | 경고 |
| 5 | `notice` | 주목할 이벤트 |
| 6 | `info` | 일반 정보 |
| 7 | `debug` | 디버깅용 상세 정보 |



# 6. journalctl

journalctl : systemd-journald가 수집한 로그를 조회하는 명령어

```bash
// 전체 로그 조회:
journalctl

// 최근 20줄:
journalctl -n 20

// 실시간 추적:
journalctl -f

// 현재 부팅 이후 로그:
journalctl -b

// 직전 부팅 로그:
journalctl -b -1

// 부팅 이력 목록 확인 - 어떤 부팅 번호가 현재/이전인지 확인 가능:
journalctl --list-boots

// 커널 로그:
journalctl -k

// 특정 서비스 로그:
journalctl -u <서비스명>

// 서비스 실시간 로그 추적
journalctl -u <서비스명> -f

// 특정 시간 이후:
journalctl --since "1 hour ago"
journalctl --since "today"

// 에러 이상만:
journalctl -p err
journalctl -b -p err

// 최근 중요 이벤트나 에러를 더 자세히 보고 싶을 때:
journalctl -xe
```


## 6-1. 자주 쓰는 명령어 조합 및 해석

```bash
// sshd 서비스 관련 로그 중 최근 20줄
journalctl -u sshd -n 20

아래 5가지 확인
* 설정 오류
* 포트 바인딩 오류
* permission denied
* invalid option
* failed / start request repeated too quickly
```

```bash
// 현재 부팅 이후 발생한 에러 레벨 이상만 보기
journalctl -b -p err
```

```bash
// 인증로그 확인 (sshd or sudo) - SSH 접속 및 sudo 사용 흔적
grep <서비스명> /var/log/secure | tail -20
```

```bash
// 부팅별 로그 보기
journalctl --list-boots
journalctl -b
journalctl -b -1
```



# 7. 장애 분석

* 서비스 장애가 났을 때의 권장 순서

```text
문제 인지
  ↓
systemctl status <service>
  ↓
journalctl -u <service> -b --no-pager
[less 같은 페이저를 거치지 않고 바로 출력 + 스크립트, 파이프, grep 조합 시 특히 유용!]
  ↓
오류 메시지 확인
  ↓
설정 / 권한 / 포트 / SELinux / 리소스 확인
  ↓
수정 후 재시작
```


## 7-1. 대표 원인


| 로그 메시지 | 가능한 원인 |
|---|---|
| `Permission denied` | 권한 문제 / SELinux |
| `Address already in use` | 포트 충돌 |
| `No such file or directory` | 설정 파일 경로 오류 |
| `Failed to start` | 의존성 문제 / 설정 문제 |
| `AVC denied` | SELinux 차단 |
| `syntax error` | 설정 문법 오류 |
| `Out of memory` / `OOM` | 메모리 부족 |


## 7-2. 분석 명령어 세트
```bash
// 특정 서비스의 현재 상태 확인
// - Loaded, Active, Main PID, 최근 로그 일부 등을 함께 보여줌
systemctl status <service>

// 현재 부팅 이후(-b) 특정 서비스(-u)의 전체 저널 로그를 페이저 없이 바로 출력
// - less 같은 화면 분할기 없이 그대로 출력하므로 grep, tail과 조합하기 좋음
journalctl -u <service> -b --no-pager

// 현재 부팅 이후(-b) 특정 서비스(-u)의 에러 레벨(err) 로그만 출력
// - 장애 원인 후보를 빠르게 좁힐 때 유용
journalctl -u <service> -b -p err --no-pager

// 현재 열려 있는 TCP 포트와 리슨 상태 확인
// -t : TCP만 표시
// -l : LISTEN 상태만 표시
// -n : 포트/주소를 숫자로 표시
// -p : 해당 포트를 사용 중인 프로세스 정보 표시
ss -tlnp

// 파일시스템 전체 사용량 확인
// -h : 사람이 읽기 쉬운 단위(K, M, G)로 출력
df -h

// 메모리 및 스왑 사용량 확인
free -h
```


## 7-3. 서비스 장애 분석

```bash
// 상태 확인
systemctl status <서비스명>

// 서비스 로그 확인
journalctl -u sshd -b --no-pager

// 설정 문법 확인
sudo <서비스명> -t

// 포트 충돌 확인
ss -tlnp | grep :포트번호

//SELinux 확인
getenforce
sudo ausearch -m avc -ts recent | tail -20

//방화벽 정책 확인
firewall-cmd --list-all
```


## 7-4. 장애 발생 시 관점 고려

**서비스 장애는 status만 보면 부족하다**
- 반드시 journalctl -u 서비스명까지 같이 봐야 한다.

**시간 필터링은 매우 중요하다**
- 문제가 난 시각 전후만 잘라서 보는 습관이 좋다.
```bash
journalctl --since "10 minutes ago"
journalctl -u httpd --since "2026-04-21 10:00:00"
```