# 시스템 로그 관리 및 성능 모니터링

## 공부 흐름

이 문서는 다음 순서로 구성되어 있다. 각 단계는 이전 단계의 지식을 기반으로 쌓아올리는 구조이다.

```
1. 사전 지식           로그, 저널, syslog, 리소스 모니터링 등 핵심 용어 정리
       ↓
2. 로그의 역할과 아키텍처  로그란 무엇인가 → systemd-journald와 rsyslog의 이중 구조
       ↓               "시스템에서 로그가 생성·수집·저장되는 전체 흐름"
       ↓
3. 주요 로그 파일 구조    /var/log 디렉터리 → 파일별 역할 → rsyslog 설정
       ↓               "어떤 로그가 어디에 저장되며, 어떻게 관리되는가"
       ↓
4. journalctl 활용      기본 조회 → 필터링(시간/유닛/우선순위) → 영속적 저널
       ↓               "systemd 저널을 자유자재로 조회하는 방법"
       ↓
5. 서비스 장애 분석       장애 시나리오 → journalctl + systemctl 연계 분석
       ↓               "서비스가 죽었을 때 로그로 원인을 추적하는 방법"
       ↓
6. CPU·메모리·디스크·프로세스  top/htop → free → df/du → ps/kill
       ↓               "시스템 리소스를 실시간으로 점검하는 기본 도구"
       ↓
7. 성능 측정 도구 활용    vmstat → iostat → sar → PCP
                       "시스템 성능을 정량적으로 측정하고 기록하는 도구"
```

---

## 사전 지식

**로그(Log):** 시스템, 커널, 서비스, 애플리케이션이 실행 중에 발생하는 이벤트를 시간순으로 기록한 데이터다. 장애 원인 추적, 보안 감사, 성능 분석 등 시스템 관리의 핵심 근거 자료로 활용된다.

**systemd-journald:** systemd에 내장된 로그 수집 데몬이다. 커널, 부팅 과정, 서비스(유닛), 애플리케이션의 로그를 바이너리 형식으로 수집하여 구조화된 저널(Journal)에 저장한다. `journalctl` 명령으로 조회한다.

**rsyslog:** 전통적인 syslog 프로토콜을 구현한 고성능 로그 처리 데몬이다. systemd-journald로부터 메시지를 전달받아 `/var/log` 아래의 텍스트 파일로 저장하거나 원격 서버로 전송하는 역할을 수행한다.

**syslog 우선순위(Priority):** 로그 메시지의 심각도를 나타내는 등급이다. 숫자가 낮을수록 심각하다. 가장 심각한 `emerg(0)`부터 디버깅용 `debug(7)`까지 8단계로 구분된다.

**파일 디스크립터(File Descriptor):** 리눅스에서 열린 파일이나 I/O 스트림을 가리키는 정수 식별자. 로그 시스템에서도 로그 파일을 fd로 관리하며, 로그 로테이션 시 fd 갱신이 중요하다.

**리소스(Resource):** 시스템이 작업을 수행하기 위해 사용하는 하드웨어 자원을 의미한다. CPU 연산 능력, 메모리(RAM) 용량, 디스크 I/O 대역폭, 네트워크 대역폭이 대표적이다.

**병목(Bottleneck):** 전체 시스템 성능을 제한하는 가장 느린 리소스 지점을 말한다. CPU 병목, 메모리 부족(OOM), 디스크 I/O 포화 등이 흔한 사례다.

---

## 로그의 역할과 아키텍처

### 왜 로그가 필요한가

시스템 관리자는 서버 앞에 항상 앉아 있을 수 없다. 로그는 관리자가 보지 못하는 사이에 **무슨 일이 일어났는지를 재구성**할 수 있게 해주는 유일한 수단이다.

```
로그의 4대 활용 목적

┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   장애 분석       │  │   보안 감사       │  │   성능 분석        │  │   규정 준수       │
│                 │  │                 │  │                 │  │                 │
│ 서비스 중단 원인    │  │ 비인가 접근 탐지    │  │ 리소스 사용 추이    │  │ 감사 추적 기록     │
│ 에러 발생 시점     │  │ SSH brute-force │  │ 디스크 풀 예측      │  │ 법적 증거 보전     │
│ 설정 오류 추적     │  │ sudo 남용 감시    │  │ 트래픽 패턴 파악     │  │ 운영 이력 관리     │
└─────────────────┘  └─────────────────┘  └─────────────────┘  └─────────────────┘
```

### RHEL / Rocky Linux의 로그 아키텍처

RHEL 10 / Rocky Linux 환경에서는 **systemd-journald**와 **rsyslog** 두 시스템이 상호 보완적으로 동작한다. RHEL 10에서 journald 설정 파일의 위치가 `/usr/lib/systemd/journald.conf`로 이동되었으며, 오버라이드 시 `/etc/systemd/journald.conf.d/` 디렉터리에 별도 파일을 생성하는 방식이 권장된다.

```
         ┌─────────────────────────────────────────────────────────┐
         │                    로그 소스                              │
         │  커널(kmsg)  서비스(stdout/stderr)  애플리케이션(syslog())  │
         └─────────────────┬───────────────────────────────────────┘
                           │
                           ▼
              ┌──────────────────────────┐
              │   systemd-journald       │
              │   (바이너리 저널 저장)        │
              │                          │
              │  /run/log/journal/   (휘발)│
              │  /var/log/journal/   (영속)│
              │                          │
              │  조회: journalctl         │
              └──────────┬───────────────┘
                         │  syslog 소켓으로 전달
                         ▼
              ┌──────────────────────────┐
              │   rsyslog                │
              │   (텍스트 파일 저장)         │
              │                          │
              │  /var/log/messages       │
              │  /var/log/secure         │
              │  /var/log/cron           │
              │  /var/log/maillog        │
              │  /var/log/boot.log       │
              │                          │
              │  설정: /etc/rsyslog.conf  │
              └──────────────────────────┘
```

**이중 구조의 장점:**


| **구분**      | **systemd-journald**               | **rsyslog**                    |
| ----------- | ---------------------------------- | ------------------------------ |
| **저장 형식**   | 바이너리 (구조화, 인덱싱)                    | 텍스트 (사람이 직접 읽기 가능)             |
| **조회 방식**   | `journalctl` 명령어                   | `cat`, `grep`, `tail` 등 텍스트 도구 |
| **검색 성능**   | 인덱스 기반 — 빠른 필터링                    | 정규식 기반 — 대용량 시 느림              |
| **원격 전송**   | 기본 미지원 (systemd-journal-remote 별도) | 내장 지원 (TCP/UDP/RELP)           |
| **기본 저장소**  | `/run/log/journal/` (휘발, 재부팅 시 소멸) | `/var/log/` (영속)               |
| **부팅 전 로그** | 커널 링버퍼부터 수집                        | journald로부터 전달받아 기록            |


> **journald 단독으로는 왜 부족한가?** 기본 설정에서 journald는 `/run/log/journal/`에 저장하므로 **재부팅 시 모든 로그가 소멸**한다. 또한 바이너리 형식이라 `grep`으로 직접 검색할 수 없고, 원격 전송 기능이 별도 패키지(`systemd-journal-remote`)를 요구한다. rsyslog는 이러한 한계를 보완하여 영속적 텍스트 로그와 원격 로그 전송을 담당한다.

### syslog 우선순위(Severity Level)

로그 메시지의 심각도를 분류하는 체계로, journald와 rsyslog 모두 동일한 기준을 사용한다. `journalctl -p` 옵션이나 rsyslog 설정에서 이 우선순위를 기준으로 필터링할 수 있다.


| **숫자** | **키워드**   | **의미**      | **실무 예시**                |
| ------ | --------- | ----------- | ------------------------ |
| 0      | `emerg`   | 시스템 사용 불가   | 커널 패닉, 파일시스템 손상          |
| 1      | `alert`   | 즉시 조치 필요    | RAID 디스크 장애, 데이터베이스 손상   |
| 2      | `crit`    | 심각한 오류      | 하드웨어 오류, OOM Kill 발생     |
| 3      | `err`     | 일반 오류       | 서비스 시작 실패, 설정 파일 오류      |
| 4      | `warning` | 경고          | 디스크 용량 80% 초과, 인증서 만료 임박 |
| 5      | `notice`  | 정상이지만 주의 필요 | 서비스 재시작 완료, 설정 변경 적용     |
| 6      | `info`    | 일반 정보       | 사용자 로그인, 서비스 정상 시작       |
| 7      | `debug`   | 디버깅용 상세 정보  | 개발 단계의 변수 값 추적           |


```
심각도 높음 ◄─────────────────────────────────────────► 심각도 낮음

 emerg(0) → alert(1) → crit(2) → err(3) → warning(4) → notice(5) → info(6) → debug(7)
   │          │          │         │          │            │           │          │
   │          │          │         │          │            │           │          └─ 개발 디버깅
   │          │          │         │          │            │           └─ 일반 운영 정보
   │          │          │         │          │            └─ 주목할 정상 이벤트
   │          │          │         │          └─ 곧 문제가 될 수 있는 상태
   │          │          │         └─ 서비스 레벨 오류
   │          │          └─ 하드웨어·심각한 소프트웨어 오류
   │          └─ 즉각 대응 필요
   └─ 시스템 전체 마비
```

> **실무 기준:** 운영 서버에서는 보통 `err(3)` 이상의 로그를 알림(Alert)으로 설정하고, `warning(4)` 이상을 정기 모니터링 대상으로 삼는다. `debug(7)` 로그를 운영 환경에서 상시 활성화하면 디스크를 빠르게 소진시키므로 장애 조사 시에만 일시적으로 켠다.

### syslog 시설(Facility)

**시설(Facility)**은 로그 메시지를 **발생 원천(카테고리)별로 분류**하는 체계다. rsyslog 설정에서 `시설.우선순위` 형식의 셀렉터(Selector)를 사용하여 어떤 소스의 어떤 심각도 메시지를 어디에 저장할지 결정한다.


| **시설(Facility)** | **설명**                            |
| ---------------- | --------------------------------- |
| `kern`           | 커널 메시지                            |
| `user`           | 사용자 프로세스                          |
| `mail`           | 메일 시스템 (`postfix`, `sendmail` 등)  |
| `daemon`         | 시스템 데몬 (`sshd`, `crond` 등)        |
| `auth`           | 인증·보안 관련 (로그인, `sudo`, PAM)       |
| `authpriv`       | 민감한 인증 정보 (`/var/log/secure`에 기록) |
| `syslog`         | syslog 데몬 자체의 내부 메시지              |
| `cron`           | 예약 작업 (`cron`, `at`)              |
| `local0~7`       | 사용자 정의 시설 (애플리케이션 분류용)            |


```
rsyslog 셀렉터 문법:   시설.우선순위   동작(Action)

예시:
  authpriv.*              /var/log/secure      ← auth 관련 모든 메시지 → secure 파일
  mail.*                  /var/log/maillog     ← 메일 관련 모든 메시지 → maillog 파일
  cron.*                  /var/log/cron        ← cron 관련 모든 메시지 → cron 파일
  *.info;mail.none        /var/log/messages    ← info 이상 모든 메시지 (단, mail 제외) → messages
  local7.*                /var/log/boot.log    ← 부팅 관련 → boot.log
```

---

## 주요 로그 파일 구조

### /var/log 디렉터리 구조

`/var/log`는 리눅스 시스템에서 **모든 텍스트 기반 로그 파일이 집중되는 디렉터리**다. rsyslog가 여기에 로그를 기록하며, 각 서비스는 자체 하위 디렉터리를 생성하기도 한다.

```
/var/log/
├── messages          ← 시스템 전반의 일반 로그 (가장 먼저 확인)
├── secure            ← 인증·보안 로그 (SSH, sudo, PAM)
├── cron              ← cron/at 예약 작업 실행 로그
├── maillog           ← 메일 서비스 로그
├── boot.log          ← 부팅 과정 서비스 시작/실패 로그
├── dmesg             ← 커널 링버퍼 메시지 (하드웨어 초기화)
├── dnf.log           ← 패키지 설치/업데이트 이력 (dnf)
├── lastlog           ← 전체 사용자의 마지막 로그인 (바이너리, lastlog 명령)
├── wtmp              ← 로그인/로그아웃 이력 (바이너리, last 명령)
├── btmp              ← 실패한 로그인 시도 (바이너리, lastb 명령)
├── journal/          ← systemd-journald 영속적 저널 (바이너리)
│   └── <machine-id>/
├── audit/            ← SELinux 감사 로그
│   └── audit.log
├── httpd/            ← Apache 웹서버 로그
│   ├── access_log
│   └── error_log
├── tuned/            ← TuneD 프로파일 적용 로그
│   └── tuned.log
├── firewalld         ← 방화벽 로그
└── ...
```

### 주요 로그 파일 상세


| **파일**                     | **내용**                              | **확인 명령어**                          |
| -------------------------- | ----------------------------------- | ----------------------------------- |
| `/var/log/messages`        | 시스템 전반의 info 이상 로그. 장애 분석의 첫 번째 출발점 | `tail -f /var/log/messages`         |
| `/var/log/secure`          | SSH 접속, sudo 사용, PAM 인증 등 보안 관련 이벤트 | `grep "Failed" /var/log/secure`     |
| `/var/log/cron`            | cron 및 anacron 작업 실행 기록             | `grep "CMD" /var/log/cron`          |
| `/var/log/boot.log`        | 부팅 시 각 서비스의 시작 성공/실패 메시지            | `cat /var/log/boot.log`             |
| `/var/log/dmesg`           | 커널 링버퍼 — 하드웨어 초기화, 드라이버 로드          | `dmesg                              |
| `/var/log/dnf.log`         | dnf 패키지 관리자의 설치/삭제/업데이트 이력          | `grep "Installed" /var/log/dnf.log` |
| `/var/log/wtmp`            | 사용자 로그인/로그아웃 이력 (바이너리)              | `last`                              |
| `/var/log/btmp`            | 로그인 실패 기록 (바이너리)                    | `lastb` (root 권한)                   |
| `/var/log/audit/audit.log` | SELinux 정책 위반, 시스템 호출 감사 로그         | `ausearch -m AVC`                   |


### rsyslog 설정 파일

rsyslog의 주 설정 파일은 `/etc/rsyslog.conf`이며, 확장 설정은 `/etc/rsyslog.d/*.conf`에 배치한다.

```bash
# 주 설정 파일 확인
cat /etc/rsyslog.conf
```

**핵심 설정 영역:**

```
# /etc/rsyslog.conf 주요 구조

#### MODULES ####
module(load="imuxsock")    # 로컬 시스템 로그 소켓 수신
module(load="imjournal")   # systemd 저널에서 로그 가져오기

#### RULES ####
# 셀렉터(시설.우선순위)          액션(저장 위치)
*.info;mail.none;authpriv.none;cron.none    /var/log/messages
authpriv.*                                   /var/log/secure
mail.*                                       -/var/log/maillog
cron.*                                       /var/log/cron
*.emerg                                      :omusrmsg:*
```

> `**-` 접두사의 의미:** `/var/log/maillog` 앞의 `-`는 **비동기 쓰기(async write)**를 의미한다. 매 로그마다 `fsync()`를 호출하지 않으므로 성능이 향상되지만, 갑작스러운 전원 차단 시 최근 로그 일부가 유실될 수 있다. 중요도가 상대적으로 낮은 mail 로그에 적용한 것이다.

### 로그 로테이션 (logrotate)

로그 파일은 끊임없이 커지므로, 오래된 로그를 압축하고 일정 기간이 지나면 삭제하는 **로테이션** 관리가 필수다. `logrotate` 유틸리티가 이 역할을 수행하며, systemd 타이머(`logrotate.timer`)에 의해 매일 자동 실행된다.

**설정 파일 위치:**

- 전역 설정: `/etc/logrotate.conf`
- 패키지별 설정: `/etc/logrotate.d/*.conf`

```bash
# logrotate 전역 설정 확인
cat /etc/logrotate.conf
```

```
# /etc/logrotate.conf 기본 구조 예시

weekly              # 주 1회 로테이션
rotate 4            # 4세대까지 보관 (4주치)
create              # 로테이션 후 새 빈 파일 생성
dateext             # 파일명에 날짜 접미사 추가 (messages-20260416)
compress            # gzip으로 압축
include /etc/logrotate.d   # 패키지별 개별 설정 포함
```

```
로테이션 흐름 (rotate 4 기준)

messages          ← 현재 활성 로그
messages-20260409.gz  ← 1주 전 (압축)
messages-20260402.gz  ← 2주 전
messages-20260326.gz  ← 3주 전
messages-20260319.gz  ← 4주 전 → 다음 로테이션 시 삭제
```

---

## journalctl을 활용한 시스템 로그 조회

### journalctl 기본 사용법

`journalctl`은 systemd-journald가 수집한 모든 로그를 조회하는 명령어다. 바이너리 저널에 대한 인덱스 검색을 수행하므로, 텍스트 파일을 `grep`하는 것보다 훨씬 빠르고 정확하게 필터링할 수 있다.

```bash
# 전체 저널 보기 (페이저로 열림, q로 종료)
journalctl

# 최신 로그부터 역순으로 보기 (가장 자주 사용)
journalctl -r

# 마지막 N줄만 보기
journalctl -n 20

# 실시간 로그 추적 (tail -f와 유사)
journalctl -f

# 페이저 없이 터미널에 바로 출력 (스크립트 활용 시)
journalctl --no-pager -n 50
```

### 시간 기반 필터링

```bash
# 오늘의 로그만 보기
journalctl --since today

# 어제 로그만 보기
journalctl --since yesterday --until today

# 특정 시각 범위 지정
journalctl --since "2026-04-16 09:00:00" --until "2026-04-16 12:00:00"

# 상대 시간 사용 (최근 1시간)
journalctl --since "1 hour ago"

# 상대 시간 (최근 30분)
journalctl --since "30 min ago"
```

### 유닛(서비스) 기반 필터링

```bash
# 특정 서비스의 로그만 보기
journalctl -u sshd

# 특정 서비스의 최근 로그 20줄
journalctl -u sshd -n 20

# 특정 서비스의 실시간 로그 추적
journalctl -u httpd -f

# 여러 서비스 동시 필터링
journalctl -u sshd -u crond

# 현재 부팅 이후의 특정 서비스 로그
journalctl -b -u firewalld
```

### 우선순위(심각도) 기반 필터링

```bash
# err 이상(emerg ~ err)의 위험한 로그만 보기
journalctl -p err

# warning 이상(emerg ~ warning)
journalctl -p warning

# 특정 범위의 우선순위만 보기 (err 부터 crit 까지)
journalctl -p crit..err

# 서비스 + 우선순위 조합
journalctl -u sshd -p err

# 시간 + 우선순위 조합
journalctl --since today -p warning
```

### 부팅별 로그 조회

systemd-journald는 부팅 세션별로 로그를 구분하여 저장한다.

재부팅할 때마다 새 세션이 되고, 각 로그에 그 부팅 정보가 붙는다. 그래서 이번 부팅만(`-b`), 직전 부팅만(`-b -1`)처럼 골라 볼 수 있다.

```bash
# 현재 부팅 세션의 로그
journalctl -b

# 이전 부팅 세션의 로그 (재부팅 전 장애 분석)
journalctl -b -1

# 2번 전 부팅 세션
journalctl -b -2

# 저장된 모든 부팅 세션 목록 확인
journalctl --list-boots
```

```
부팅 세션 인덱스
─────────────────────────────
 -2   3번째 전 부팅 (가장 오래됨)
 -1   이전 부팅
  0   현재 부팅 (기본값)
```

> **주의:** 부팅 간 로그 보존은 **영속적 저널이 설정되어 있어야** 가능하다. 기본 설정에서는 `/run/log/journal/`에 저장되어 재부팅 시 소멸하므로, `-b -1` 옵션이 동작하지 않는다.

### 출력 형식 지정

```bash
# JSON 형식으로 출력 (스크립트에서 파싱 용도)
journalctl -u sshd -o json-pretty -n 5

# 짧은 형식 (기본, syslog와 유사)
journalctl -o short

# 모든 필드 포함 상세 출력
journalctl -o verbose -n 1

# 짧은 형식 + ISO 타임스탬프
journalctl -o short-iso
```

### 커널 메시지 조회

```bash
# 커널 메시지만 보기 (dmesg와 유사하지만 시간 필터링 가능)
journalctl -k

# 현재 부팅의 커널 메시지 중 err 이상만
journalctl -k -b -p err
```

### 영속적 저널(Persistent Journal) 설정

기본 상태에서 journald 로그는 재부팅 시 사라진다. 영속적 저널을 활성화하면 `/var/log/journal/`에 저장되어 부팅 간 로그가 보존된다.

```bash
# 영속적 저널 디렉터리 생성
sudo mkdir -p /var/log/journal

# systemd-journald에 영속적 저장 설정 (RHEL 10 방식: drop-in 파일)
sudo mkdir -p /etc/systemd/journald.conf.d
sudo tee /etc/systemd/journald.conf.d/99-persistent.conf > /dev/null <<EOF
[Journal]
Storage=persistent
SystemMaxUse=500M
SystemKeepFree=1G
MaxRetentionSec=1month
EOF

# 저널 서비스 재시작 및 기존 로그 flush
sudo systemctl restart systemd-journald
sudo journalctl --flush

# 영속적 저널 확인
ls -la /var/log/journal/
```


| **설정 항목**                | **의미**                                   |
| ------------------------ | ---------------------------------------- |
| `Storage=persistent`     | `/var/log/journal/`에 영속 저장               |
| `Storage=volatile`       | `/run/log/journal/`에만 저장 (기본)            |
| `Storage=auto`           | `/var/log/journal/` 디렉토리가 있으면 영속, 없으면 휘발 |
| `SystemMaxUse=500M`      | 저널이 사용할 최대 디스크 용량                        |
| `SystemKeepFree=1G`      | 파일시스템에 최소한 남겨둘 여유 공간                     |
| `MaxRetentionSec=1month` | 보관 기간 (이후 자동 삭제)                         |


### journalctl 주요 옵션 정리


| **옵션**           | **설명**            | **예시**                            |
| ---------------- | ----------------- | --------------------------------- |
| `-r`             | 역순(최신 먼저) 출력      | `journalctl -r`                   |
| `-n N`           | 마지막 N개 항목만 출력     | `journalctl -n 50`                |
| `-f`             | 실시간 추적 (follow)   | `journalctl -f`                   |
| `-u UNIT`        | 특정 systemd 유닛 필터링 | `journalctl -u sshd`              |
| `-p PRIORITY`    | 우선순위 필터링          | `journalctl -p err`               |
| `-b [N]`         | 부팅 세션별 필터링        | `journalctl -b -1`                |
| `-k`             | 커널 메시지만           | `journalctl -k`                   |
| `--since`        | 시작 시각             | `journalctl --since "1 hour ago"` |
| `--until`        | 종료 시각             | `journalctl --until "12:00"`      |
| `-o FORMAT`      | 출력 형식             | `journalctl -o json-pretty`       |
| `--no-pager`     | 페이저 없이 출력         | `journalctl --no-pager`           |
| `--list-boots`   | 부팅 세션 목록          | `journalctl --list-boots`         |
| `--disk-usage`   | 저널이 사용 중인 디스크 확인  | `journalctl --disk-usage`         |
| `--vacuum-time=` | 지정 기간 이전 로그 삭제    | `journalctl --vacuum-time=7d`     |
| `--vacuum-size=` | 지정 크기 초과 로그 삭제    | `journalctl --vacuum-size=200M`   |


---

## 서비스 장애 분석을 위한 로그 확인

### 장애 분석 흐름

서비스에 문제가 발생했을 때 체계적으로 원인을 추적하는 순서다. 먼저 **유닛 상태**로 실패 여부를 확인하고, **해당 서비스 저널**에서 에러 문구·종료 코드를 본 뒤, **시간 범위**를 좁혀 장애 시점을 잡는다. 그다음 **같은 시각의 시스템 전체 로그**와 맞춰 OOM·디스크 풀 등 다른 계층의 원인이 있었는지 본다. 마지막으로 설정을 수정하고 재시작·실시간 추적(`-f`)으로 정상 동작을 확인한다.

아래 도식은 위 순서를 단계별로 나타낸 것이다.

```
[1단계] 장애 인지
  systemctl status <서비스>
  → Active: failed / inactive 확인
       │
       ▼
[2단계] 최근 로그 확인
  journalctl -u <서비스> -n 30 --no-pager
  → 에러 메시지, 종료 코드, 시그널 확인
       │
       ▼
[3단계] 시간 범위 좁히기
  journalctl -u <서비스> --since "10 min ago" -p err
  → 장애 발생 시점 전후의 관련 로그 집중 분석
       │
       ▼
[4단계] 관련 시스템 로그 교차 확인
  journalctl --since "10 min ago" -p err
  → 동일 시간대에 OOM, 디스크 풀, SELinux 등 다른 원인이 있었는지
       │
       ▼
[5단계] 설정 검증 및 재시작
  <서비스별 설정 검증 명령어>
  systemctl restart <서비스>
  journalctl -u <서비스> -f   ← 재시작 후 실시간 추적
```

### CPU·메모리·디스크·프로세스 사용량 점검

로그만 보지 말고, **호스트가 감당할 수 있는 상태인지**를 같이 본다. 장애 원인은 크게 **설정·권한·버그** 쪽과 **리소스 고갈** 쪽으로 갈리는데, 후자는 저널에 `Out of memory`, `No space left on device` 같은 흔적이 남기도 하지만, 그 전에 **현재 값**을 보면 범위를 빠르게 좁힐 수 있다.

점검할 때는 보통 아래 네 영역으로 나눈다.

- **CPU** — 평균 부하(load average)와 코어별 사용률. 스파이크·단일 코어 병목 여부. 예: `uptime`, `top` / `htop`.
- **메모리** — 물리 메모리 여유, **buff/cache**, **Swap 사용 여부**(Swap이 늘면 디스크 I/O 부담). 예: `free -h`.
- **디스크** — 파티션·볼륨 **용량**과 **inode** 부족(용량은 남았는데 파일 생성이 안 되는 경우). 예: `df -h`, `df -i`, 필요 시 `du`로 큰 디렉터리 추적.
- **프로세스** — 어떤 프로세스가 CPU·메모리를 쓰는지, 좀비/과다 생성 여부. 예: `top`, `ps aux --sort=-%mem | head`, `ps aux --sort=-%cpu | head`.

이 단계는 위 [4단계]에서 저널로 “다른 원인”을 찾을 때와 맞물려 보면 이해하기 쉽다.

### systemctl과 journalctl 연계

상태 한 줄과 최근 로그를 **한 번에** 보고 싶을 때 아래처럼 묶어 쓴다.

```bash
# 1. 서비스 상태 빠른 확인 — 최근 로그 10줄 포함
systemctl status sshd

# 2. 실패한 유닛 목록 한눈에 보기
systemctl --failed

# 3. 실패한 서비스의 상세 로그 확인
journalctl -u <실패한 서비스> -n 50 --no-pager

# 4. 서비스 재시작 후 실시간 추적
sudo systemctl restart <서비스>
journalctl -u <서비스> -f
```

### 실무 장애 시나리오 예시

**시나리오 1: sshd가 시작되지 않는 경우**

```bash
# 상태 확인
$ systemctl status sshd
● sshd.service - OpenSSH server daemon
     Loaded: loaded (/usr/lib/systemd/system/sshd.service; enabled)
     Active: failed (Result: exit-code) since ...
    Process: 12345 ExecStart=/usr/sbin/sshd -D $OPTIONS (code=exited, status=255/EXCEPTION)

# 상세 로그 확인
$ journalctl -u sshd -n 20 --no-pager
...
Apr 16 10:15:33 server sshd[12345]: /etc/ssh/sshd_config line 47: Bad configuration option: PermitLogin
Apr 16 10:15:33 server sshd[12345]: Terminating, 1 bad configuration options
```

위 로그에서 `/etc/ssh/sshd_config`의 47번째 줄에 `PermitLogin`이라는 잘못된 옵션이 있음을 알 수 있다. (`PermitRootLogin`의 오타)

```bash
# 설정 문법 검사
sudo sshd -t
# → 오류 있으면 줄 번호와 함께 출력

# 수정 후 재시작
sudo vim /etc/ssh/sshd_config   # 오타 수정
sudo systemctl restart sshd
journalctl -u sshd -f           # 실시간 추적으로 정상 시작 확인
```

**시나리오 2: 서비스가 반복적으로 죽는 경우 (CrashLoop)**

```bash
# 상태에서 "start-limit-hit" 확인
$ systemctl status httpd
  Active: failed (Result: start-limit-hit)

# 반복 실패 로그 확인
$ journalctl -u httpd --since "30 min ago" -p err
Apr 16 11:20:01 server httpd[3456]: AH00526: Syntax error on line 85 of /etc/httpd/conf/httpd.conf
Apr 16 11:20:01 server systemd[1]: httpd.service: Main process exited, code=exited, status=1
Apr 16 11:20:01 server systemd[1]: httpd.service: Failed with result 'exit-code'.
...  (동일 패턴 반복)

# 실패 카운터 초기화 후 재시도
sudo systemctl reset-failed httpd
sudo systemctl start httpd
```

> `**start-limit-hit`이란?** systemd는 서비스가 짧은 시간 내에 반복 실패하면 추가 재시작을 차단한다. 이 상태를 해소하려면 `systemctl reset-failed <서비스>`로 실패 카운터를 초기화해야 한다.

**시나리오 3: OOM(Out of Memory)으로 프로세스가 종료된 경우**

```bash
# 커널 메시지에서 OOM 확인
$ journalctl -k --since "1 hour ago" | grep -i "oom\|killed"
Apr 16 14:05:22 server kernel: httpd invoked oom-killer: ...
Apr 16 14:05:22 server kernel: Killed process 8765 (httpd) total-vm:2048000kB

# OOM 관련 전체 컨텍스트
$ journalctl -k --since "1 hour ago" -p err
```

---

## CPU·메모리·디스크·프로세스 사용량 점검 (복습)

### CPU 사용량 점검

`**top` — 실시간 프로세스·CPU 모니터링**

`top`은 시스템 상태를 실시간으로 보여주는 대화형 도구로, `procps-ng` 패키지에 포함되어 있다.

```bash
top
```

```
top - 14:30:00 up 38 days,  3:04,  2 users,  load average: 1.22, 0.83, 0.66
Tasks: 234 total,   1 running, 233 sleeping,   0 stopped,   0 zombie
%Cpu(s): 12.5 us,  3.1 sy,  0.0 ni, 83.7 id,  0.5 wa,  0.0 hi,  0.2 si,  0.0 st
MiB Mem :  15889.2 total,   8923.1 free,   4716.8 used,   2249.3 buff/cache
MiB Swap:   7936.0 total,   7936.0 free,      0.0 used.  10457.6 avail Mem

    PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND
   1234 root      20   0  512340  45678   6780 S  15.3   0.3   2:30.45 httpd
   5678 mysql     20   0 1234560 567890  12340 S   8.2   3.5  45:12.33 mysqld
```

**top 헤더 읽는 법:**


| **필드**                           | **의미**                                                 |
| -------------------------------- | ------------------------------------------------------ |
| `load average: 1.22, 0.83, 0.66` | 1분, 5분, 15분 평균 CPU 대기열. **코어 수 이하면 정상** (8코어 → 8.0 이하) |
| `us` (user)                      | 사용자 영역(애플리케이션) CPU 사용률                                 |
| `sy` (system)                    | 커널 영역(시스템 콜) CPU 사용률                                   |
| `id` (idle)                      | 유휴 CPU 비율. **이 값이 낮으면 CPU 포화**                         |
| `wa` (iowait)                    | I/O 대기 비율. **높으면 디스크 병목**                              |
| `st` (steal)                     | 가상화 환경에서 하이퍼바이저에게 빼앗긴 CPU 시간                           |


**top 대화형 키:**


| **키** | **동작**                |
| ----- | --------------------- |
| `P`   | CPU 사용률 기준 정렬         |
| `M`   | 메모리 사용률 기준 정렬         |
| `k`   | 프로세스 Kill (PID 입력)    |
| `1`   | CPU 코어별 사용률 개별 표시/숨기기 |
| `q`   | 종료                    |


`**uptime` — 시스템 부하 한 줄 요약**

```bash
$ uptime
 14:30:00 up 38 days,  3:04,  2 users,  load average: 1.22, 0.83, 0.66
```

```
Load Average 해석 기준 (8코어 시스템)

  0.00 ────── 완전 유휴
  4.00 ────── 50% 부하 (여유 있음)
  8.00 ────── 100% 포화 (코어 수와 동일)
 12.00 ────── 과부하 (대기열 발생)
```

`**mpstat` — CPU 코어별 상세 통계**

`sysstat` 패키지에 포함되어 있으며, 코어별 사용률을 세분화하여 확인할 수 있다.

```bash
# 설치 (없을 경우)
sudo dnf install -y sysstat

# 전체 CPU 코어별 사용률 표시
mpstat -P ALL

# 2초 간격으로 5회 반복 측정
mpstat -P ALL 2 5
```

### 메모리 사용량 점검

`**free` — 메모리 사용 요약**

```bash
$ free -h
               total        used        free      shared  buff/cache   available
Mem:            15Gi       4.6Gi       8.7Gi       715Mi       2.7Gi        10Gi
Swap:          7.7Gi          0B       7.7Gi
```

```
메모리 구조 이해

 ┌──────────────────────────────────────────────── total (15Gi) ────────────────────┐
 │                                                                                  │
 │  ┌─── used (4.6Gi) ───┐  ┌─── buff/cache (2.7Gi) ────┐  ┌─── free (8.7Gi) ───┐   │
 │  │  프로세스가 실제       │  │  커널이 I/O 성능 향상         │  │  완전히 비어있는       │   │
 │  │  점유 중인 메모리      │  │  위해 캐싱한 영역             │  │  메모리              │   │
 │  └────────────────────┘  └───────────────────────────┘  └────────────────────┘   │
 │                                                                                  │
 │  available (10Gi) = free + 회수 가능한 buff/cache                                   │
 │  → 새 프로세스가 실제로 사용할 수 있는 메모리 양                                            │
 └──────────────────────────────────────────────────────────────────────────────────┘
```

> `**free`가 낮아도 괜찮은 이유:** 리눅스는 유휴 메모리를 낭비하지 않고 적극적으로 디스크 캐시(buff/cache)에 활용한다. 프로세스가 메모리를 요청하면 캐시를 즉시 반환하므로, `**available` 값이 실제로 쓸 수 있는 메모리**다. `available`이 낮으면 메모리 부족을 의심해야 한다.

> **Swap이 사용되고 있으면?** Swap은 RAM이 부족할 때 디스크를 메모리처럼 쓰는 영역이다. Swap 사용량이 증가하면 **디스크 I/O 병목**이 발생하여 시스템이 느려진다. `free -h`에서 Swap used가 0이 아니라면 메모리 증설이나 프로세스 최적화를 검토해야 한다.

### 디스크 사용량 점검

`**df` — 파일시스템별 디스크 사용량**

```bash
$ df -h
Filesystem           Size  Used Avail Use% Mounted on
/dev/mapper/rl-root   70G  5.9G   65G   9% /
/dev/sda1             1.0G  229M  795M  23% /boot
tmpfs                 7.8G     0  7.8G   0% /dev/shm
```

> **경고 기준:** `Use%`가 **80%를 초과**하면 주의, **90% 이상**이면 긴급 조치가 필요하다. 특히 `/`(루트)와 `/var`(로그 저장) 파티션을 중점적으로 감시한다.

`**du` — 디렉터리별 실제 사용량**

```bash
# /var 아래 디렉터리별 용량 확인 (1단계 깊이)
sudo du -sh /var/* | sort -rh | head -10

# 특정 디렉터리의 상세 용량
sudo du -sh /var/log/*
```

`**lsblk` — 블록 디바이스 구조 확인**

```bash
$ lsblk
NAME          MAJ:MIN RM  SIZE RO TYPE MOUNTPOINTS
sda             8:0    0   80G  0 disk
├─sda1          8:1    0    1G  0 part /boot
└─sda2          8:2    0   79G  0 part
  ├─rl-root   253:0    0   70G  0 lvm  /
  └─rl-swap   253:1    0  7.7G  0 lvm  [SWAP]
```

### 프로세스 관리

`**ps` — 프로세스 스냅샷**

```bash
# 모든 프로세스 전체 정보
ps -ef

# CPU 사용률 TOP 10
ps aux --sort=-%cpu | head -11

# 메모리 사용률 TOP 10
ps aux --sort=-%mem | head -11

# 특정 프로세스 찾기
ps -ef | grep sshd | grep -v grep

# 프로세스 트리로 보기 (부모-자식 관계)
ps -ef --forest
```

**ps aux 필드 설명:**


| **필드**    | **의미**                                              |
| --------- | --------------------------------------------------- |
| `USER`    | 프로세스 소유 사용자                                         |
| `PID`     | 프로세스 ID                                             |
| `%CPU`    | CPU 사용률                                             |
| `%MEM`    | 메모리 사용률                                             |
| `VSZ`     | 가상 메모리 크기 (KiB)                                     |
| `RSS`     | 실제 물리 메모리 사용량 (KiB)                                 |
| `STAT`    | 프로세스 상태 (S=Sleep, R=Running, Z=Zombie, D=Disk Wait) |
| `COMMAND` | 실행 명령어                                              |


**프로세스 시그널 전송 (`kill`)**

```bash
# 정상 종료 요청 (SIGTERM, 기본)
kill <PID>
kill -15 <PID>

# 강제 종료 (SIGKILL — 프로세스가 무시할 수 없음)
kill -9 <PID>

# 이름으로 프로세스 종료
pkill httpd

# 설정 파일 다시 읽기 (SIGHUP)
kill -1 <PID>
```


| **시그널**   | **번호** | **동작**                 |
| --------- | ------ | ---------------------- |
| `SIGHUP`  | 1      | 설정 재로드 (데몬에서 관례적으로 사용) |
| `SIGINT`  | 2      | 인터럽트 (Ctrl+C와 동일)      |
| `SIGKILL` | 9      | 강제 종료 (차단 불가)          |
| `SIGTERM` | 15     | 정상 종료 요청 (기본 시그널)      |
| `SIGSTOP` | 19     | 프로세스 일시 정지             |
| `SIGCONT` | 18     | 일시 정지된 프로세스 재개         |


---

## 시스템 성능 측정 및 하드웨어 리소스 점검 도구 활용

### vmstat — 가상 메모리·CPU 통합 통계

`vmstat`은 커널 스레드, 가상 메모리, 페이징, 블록 I/O, 인터럽트, CPU 활동에 대한 통계를 실시간으로 보여주는 도구다. `procps-ng` 패키지에 포함되어 있다.

```bash
# 2초 간격으로 5회 측정
vmstat 2 5
```

```
procs -----------memory---------- ---swap-- -----io---- -system-- ------cpu-----
 r  b   swpd   free   buff  cache   si   so    bi    bo   in   cs us sy id wa st
 1  0      0 8923104  12340 2249300    0    0     5    15  120  230 12  3 84  1  0
 2  0      0 8900260  12340 2252100    0    0     0    85  155  289 18  5 76  1  0
```


| **필드**      | **의미**                     | **주의 기준**              |
| ----------- | -------------------------- | ---------------------- |
| `r`         | 실행 대기 중인 프로세스 수            | CPU 코어 수를 초과하면 포화      |
| `b`         | I/O 대기(Block) 중인 프로세스 수    | 지속적으로 0 이상이면 디스크 병목    |
| `swpd`      | 사용 중인 Swap (KiB)           | 증가 추세면 메모리 부족          |
| `si` / `so` | Swap In / Swap Out (KiB/s) | 0이 아니면 RAM ↔ 디스크 교환 발생 |
| `bi` / `bo` | 블록 장치 읽기 / 쓰기 (blocks/s)   | 높으면 디스크 I/O 부하         |
| `us`        | 사용자 영역 CPU (%)             | —                      |
| `sy`        | 시스템(커널) 영역 CPU (%)         | 20% 이상 지속되면 비효율적 시스템 콜 |
| `id`        | 유휴 CPU (%)                 | 10% 미만이면 CPU 포화        |
| `wa`        | I/O 대기 CPU (%)             | 높으면 디스크 병목             |
| `st`        | 가상화 Steal 타임 (%)           | VM에서 높으면 호스트 과부하       |


### iostat — 디스크 I/O 성능 분석

`iostat`은 CPU 통계와 함께 디스크 장치별 I/O 통계를 보여준다. `sysstat` 패키지에 포함되어 있다.

```bash
# 설치
sudo dnf install -y sysstat

# 기본 출력 (CPU + 디스크)
iostat

# 확장 통계 + 읽기 단위(MB) + 2초 간격 3회
iostat -xm 2 3
```

```
Device            r/s     rMB/s   w/s     wMB/s   await  %util
sda              15.20     0.12  42.80     1.35    5.23   12.56
dm-0             14.90     0.11  40.50     1.30    5.45   11.89
dm-1              0.30     0.00   2.30     0.05    1.20    0.34
```


| **필드**            | **의미**               | **주의 기준**                 |
| ----------------- | -------------------- | ------------------------- |
| `r/s` / `w/s`     | 초당 읽기/쓰기 요청 횟수       | —                         |
| `rMB/s` / `wMB/s` | 초당 읽기/쓰기 처리량         | —                         |
| `await`           | 평균 I/O 요청 대기 시간 (ms) | **10ms 이상이면 주의** (SSD 기준) |
| `%util`           | 디스크 사용률              | **80% 이상이면 병목 가능**        |


### sar — 시스템 활동 기록기

`sar`(System Activity Reporter)은 시스템 전반의 성능 데이터를 **수집·저장·리포트**하는 도구다. 다른 도구들이 "지금"을 보여주는 반면, sar은 **과거 데이터를 소급 분석**할 수 있다는 점에서 차별화된다. `sysstat` 패키지에 포함되어 있다.

```bash
# sysstat 설치 및 데이터 수집 서비스 활성화
sudo dnf install -y sysstat
sudo systemctl enable --now sysstat
```

```bash
# CPU 사용률 (2초 간격 5회)
sar -u 2 5

# 메모리 사용률 (2초 간격 5회)
sar -r 2 5

# 디스크 I/O 통계
sar -d 2 5

# 네트워크 인터페이스 통계
sar -n DEV 2 5

# 과거 데이터 조회 (오늘 오전 09시~12시)
sar -u -s 09:00:00 -e 12:00:00

# 특정 날짜의 데이터 조회 (sa16 = 16일자)
sar -u -f /var/log/sa/sa16
```

```
sar 데이터 소급 분석 흐름

  sysstat 서비스가 10분마다 자동 수집
         ↓
  /var/log/sa/saDD  (DD=일자, 바이너리 파일)
         ↓
  sar -u -f /var/log/sa/sa16   ← 16일자 CPU 이력 리포트
  sar -r -f /var/log/sa/sa16   ← 16일자 메모리 이력 리포트
```


| **sar 옵션** | **내용**                |
| ---------- | --------------------- |
| `-u`       | CPU 사용률               |
| `-r`       | 메모리 사용률               |
| `-d`       | 디스크 I/O 활동            |
| `-n DEV`   | 네트워크 인터페이스 통계         |
| `-n SOCK`  | 네트워크 소켓 통계            |
| `-b`       | I/O 전송률               |
| `-q`       | 실행 대기열 / Load Average |
| `-W`       | Swap 활동               |
| `-f FILE`  | 지정한 데이터 파일에서 읽기       |
| `-s / -e`  | 시작/종료 시각 지정           |


### ** Performance Co-Pilot (PCP) — 통합 성능 모니터링 프레임워크

[Performance Co-Pilot](https://pcp.io/)은 **시스템 수준 성능을 측정·관리하기 위한 도구·서비스·라이브러리 묶음**이다. Red Hat 문서에서는 *suite of tools, services, and libraries for managing and measuring system-level performance*로 정의한다. 단일 명령어가 아니라 **메트릭 수집 → 실시간·과거 분석 → (선택) 시각화·알림**까지 이어지는 프레임워크다.

공식 사이트([pcp.io — Features](https://pcp.io/features.html))에서 강조하는 역할은 세 가지로 요약할 수 있다.

- **Collect** — 호스트에서 성능 메트릭을 가볍게 수집하고, 여러 시스템·OS에서 메트릭을 모을 수 있는 **분산형** 구조. 주요 배포판에 패키지로 포함되는 경우가 많다.
- **Analyze** — 실시간 값과 **기록(아카이브)된 과거 데이터**를 함께 다룬다. 호스트·시간대를 나누어 비교하고 추세·이상 패턴을 볼 수 있다.
- **Extend** — 애플리케이션·서비스별 메트릭을 **PMDA** 등으로 확장하고, API·라이브러리로 자체 메트릭을 붙일 수 있다. 웹·JSON 인터페이스로 외부 도구와 연동하는 것도 가능하다([RHEL — Setting up PCP](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/monitoring_and_managing_system_status_and_performance/setting-up-pcp)).

Red Hat Enterprise Linux 문서가 짚는 **특징**은 다음과 같다.

- 가벼운 **분산 아키텍처**로 복잡한 환경을 중앙에서 분석하기에 적합하다.
- **실시간** 데이터 모니터링·관리가 가능하다.
- **기록·조회**를 통해 과거 데이터를 남기고 나중에 소급 분석할 수 있다.

**주요 구성 요소**(같은 Red Hat 장의 요약):


| 구성                                              | 역할                                                                                             |
| ----------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| **pmcd** (Performance Metric Collector Daemon)  | 설치된 **PMDA**(Performance Metric Domain Agent)로부터 메트릭을 모은다. PMDA는 호스트마다 개별 로드·언로드되며 pmcd가 제어한다. |
| **pmlogger**                                    | 성능 메트릭을 **아카이브(로그)**로 남긴다. 나중에 `pmrep`, 비교 도구 등으로 과거 구간을 분석할 때 쓴다.                             |
| **pmproxy**                                     | 실시간·과거 메트릭 **프록시**, 시계열·REST 등 상위 연동에 사용된다.                                                    |
| **pmie** (Performance Metrics Inference Engine) | 주기적으로 규칙·식을 평가하는 **추론 엔진**(임계값·알림 등에 활용).                                                      |


패키지 관점에서는 `**pcp`**, `**pcp-system-tools`**에 CLI와 핵심 기능이 들어 있고, `**pcp-gui`**는 `pmchart` 등 GUI, **`grafana-pcp`**는 Grafana 기반 시각화·알림을 제공한다.

```bash
# PCP 설치 (최소 구성)
sudo dnf install -y pcp pcp-system-tools

# PCP 데몬 시작 및 활성화
sudo systemctl enable --now pmcd       # PCP 수집 데몬
sudo systemctl enable --now pmlogger   # PCP 로그 기록 데몬
```

설치 확인은 `pcp` 명령으로 현재 플랫폼·실행 중인 `pmcd`·로드된 PMDA 개수 등을 볼 수 있다.

```
PCP 아키텍처

┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  PMDA (Agent)    │     │  pmcd (Daemon)   │     │  분석 도구         │
│                  │     │                  │     │                  │
│  CPU, 메모리,      │────→│  메트릭 수집 및     │────→│  pmstat          │
│  디스크, 네트워크    │     │  통합 관리         │     │  pmrep           │
│  프로세스 등        │     │                  │     │  pmlogger (기록)  │
│                  │     │                  │     │  Grafana (시각화)  │
└──────────────────┘     └──────────────────┘     └──────────────────┘
```

**PCP 주요 명령어:**

```bash
# 시스템 개요 (top과 유사하지만 더 상세)
pmstat

# 실시간 프로세스 모니터링 (atop과 유사)
pcp atop

# 사용 가능한 메트릭 검색
pminfo | grep cpu

# 특정 메트릭 실시간 값 조회
pmval kernel.all.load     # Load Average
pmval mem.util.available  # Available 메모리

# 커스텀 리포트 출력
pmrep kernel.all.load mem.util.available disk.all.total -t 2 -s 5
```

### 하드웨어 정보 확인 도구


| **명령어**             | **용도**                       | **예시**                     |
| ------------------- | ---------------------------- | -------------------------- |
| `lscpu`             | CPU 아키텍처, 코어 수, 클럭           | `lscpu`                    |
| `lsmem`             | 메모리 블록 구조, 총 용량              | `lsmem`                    |
| `lsblk`             | 블록 장치(디스크/파티션) 트리            | `lsblk`                    |
| `lspci`             | PCI 장치 목록 (NIC, GPU 등)       | `lspci`                    |
| `lsusb`             | USB 장치 목록                    | `lsusb`                    |
| `dmidecode`         | BIOS/하드웨어 상세 정보 (root)       | `sudo dmidecode -t memory` |
| `nproc`             | 사용 가능한 CPU 코어 수              | `nproc`                    |
| `cat /proc/cpuinfo` | CPU 모델, 캐시 크기 등              | `cat /proc/cpuinfo         |
| `cat /proc/meminfo` | 메모리 상세 (MemTotal, MemFree 등) | `cat /proc/meminfo`        |


### 리소스 점검 도구 요약 비교


| **도구**      | **패키지**   | **영역**          | **특징**                    |
| ----------- | --------- | --------------- | ------------------------- |
| `top`       | procps-ng | CPU + 프로세스      | 실시간, 대화형                  |
| `htop`      | htop      | CPU + 프로세스      | top의 향상판, 컬러 UI, 마우스 지원   |
| `free`      | procps-ng | 메모리             | 간결한 스냅샷                   |
| `df` / `du` | coreutils | 디스크             | 파일시스템 / 디렉터리별 용량          |
| `ps`        | procps-ng | 프로세스            | 스냅샷 (비대화형)                |
| `vmstat`    | procps-ng | CPU + 메모리 + I/O | CPU/메모리/Swap/I/O 통합       |
| `mpstat`    | sysstat   | CPU 코어별         | 코어별 세분화 통계                |
| `iostat`    | sysstat   | 디스크 I/O         | 장치별 I/O 성능 분석             |
| `sar`       | sysstat   | 전체 리소스          | **과거 데이터 소급 분석** 가능       |
| `PCP`       | pcp       | 전체 리소스          | 에이전트 기반 프레임워크, Grafana 연동 |


---

## Ref.


| 주제                | 문서                                                                                                                                                                                                                             |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 시스템 로그 구성 및 관리    | [Configuring and managing logging — RHEL 10](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/configuring_and_managing_logging/index)                                                                 |
| 시스템 상태 및 성능 모니터링  | [Monitoring and managing system status and performance — RHEL 10](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/monitoring_and_managing_system_status_and_performance/index)                       |
| PCP 설정 및 활용       | [Setting up PCP — RHEL 10](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/monitoring_and_managing_system_status_and_performance/setting-up-pcp)                                                     |
| PCP 프로젝트 공식       | [pcp.io](https://pcp.io/), [Documentation](https://pcp.io/documentation.html)                                                                                                                                                  |
| TuneD를 활용한 성능 최적화 | [Optimizing system performance with TuneD — RHEL 10](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/monitoring_and_managing_system_status_and_performance/optimizing-system-performance-with-tuned) |
| systemd 저널 설정     | [systemd-journald.service(8) man page](https://www.freedesktop.org/software/systemd/man/latest/systemd-journald.service.html)                                                                                                  |
| rsyslog 공식 문서     | [rsyslog documentation](https://www.rsyslog.com/doc/)                                                                                                                                                                          |


