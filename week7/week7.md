# 로그의 역할과 주요 로그 파일 구조 이해
## Log란?
- Linux OS의 log는 커널, 서비스, 애플리케이션 등 시스템에 발생한 이벤트를 분류하여 기록한 파일이다.
## Linux Log Architecture
[프로세스/커널]
   ↓
[로그 생성]
   ↓
[수집 데몬: rsyslog / syslog-ng / systemd-journald]
   ↓
[저장 위치]
   ├─ /var/log/*.log
   └─ journalctl이 보는 binary journal
   ↓
[관리]
   ├─ logrotate
   └─ 원격 로그 전송 / 보안 정책 / 모니터링

## Kerner Logging
### 의미
- 리눅스 커널 자체가 남기는 로그
- 부팅 과정, 드라이버, 하드웨어 오류, 커널 이벤트 등이 포함됨

### 구조
<img width="842" height="589" alt="image" src="https://github.com/user-attachments/assets/a15187f0-eaf3-475f-8cf2-9c4db704af7c" />
- kernel space에서 logging은 kernel의 "Ring Buffer" 를 통해 수행된다. 커널 링 버퍼는 시스템이 부팅될 때 로그 메시지를 저장하는 첫 번째 데이터 구조인 순환 버퍼이다.
- 링 버퍼는 고정 크기의 순환 버퍼이며, 최신 데이터를 기록하고 오래된 데이터는 덮어쓰는 방식으로 동작한다. 이를 통해 제한된 메모리 공간을 효율적으로 활용하면서도 중요한 로그 정보를 유지할 수 있다.
- 커널 메시지(printk: 커널에서 사용되는 로그 출력 함수)를 기록하는 dmesg 명령어는 링 버퍼를 통해 커널 메시지를 읽어오고, 로깅 데몬은 링 버퍼를 사용하여 로그를 수집하고 저장한다.

dmesg란?
- 커널 메시지 버퍼(Kernel Ring Buffer)의 내용을 출력하는 도구이다.
- /dev/kmsg(링 버퍼에 접근할 수 있게 해주는 디바이스 인터페이스, 로그 데이터의 원천)에서 로그들을 긁어와 사용자가 파악하기 쉽게 로그들을 정제해서 보여준다.

dmesg를 통해 memory 로그 내역 확인
<img width="1061" height="212" alt="image" src="https://github.com/user-attachments/assets/5dcf3a0e-9698-44c8-a306-d8870661468d" />
- 처음에 설정한 2GB(2097152K)로 용량이 잡혀있는 것을 확인할 수 있다.
- 

예시
- 디스크 인식 실패
- 네트워크 인터페이스 활성화
- 커널 패닉 관련 메시지

로그 데몬
- OS나 프로그램에서 발생하는 로그를 계속 받아서, 분류하고, 저장하고, 필요하면 다른 곳으로 보내는 백그라운드 프로세스다.
- 커널 부팅 메시지, 서비스 시작/종료 기록, 로그인/로그아웃 기록, 에러 메시지, 보안 관련 이벤트, 애플리케이션 출력과 같은 기록들을 중앙에서 관리해준다. 

## User Logging
- user space에서 로그는 최근 배포판 기준으로 크게 syslog과 journal 2가지로 나눠서 비교가 된다.
| 항목      | syslog(rsyslog)               | journal                                    |
| ------- | ----------------------------- | ------------------------------------------ |
| 저장하는 정보 | 설정된 시스템/서비스 로그를 분류 저장         | 커널, 서비스, systemd 유닛 등 로그를 통합 수집            |
| 저장 위치   | `/var/log/` 아래 텍스트 로그 파일      | `/var/log/journal/` 또는 `/run/log/journal/` |
| 저장 형식   | 텍스트 파일                        | journal 형식(바이너리 기반)                        |
| 서비스(데몬) | `syslogd`, `rsyslogd`         | `systemd-journald`                         |
| 조회 방법   | `cat`, `less`, `tail`, `grep` | `journalctl`                               |
| 순환/보존   | `logrotate` 정책에 따름            | journald 설정에 따름, persistent/volatile 가능    |

### syslog
네트워크를 통해 로그 메시지를 전달하기 위한 표준 프로토콜(RFC 5424)이다. 
애플리케이션이나 시스템 구성 요소가 로그 메시지를 생성하면, 이를 특정 형식으로 변환하여 로그 서버나 로컬 저장소로 전송하는 규격

### rsyslogd
전통적인 syslogd를 계승한 현대 리눅스 배포판의 기본 로깅 데몬이다.
- 역할: 시스템에서 발생하는 로그를 수신, 처리, 필터링하여 파일에 기록하거나 원격 서버로 전송
- 장점: 멀티스레딩을 지원하여 대량의 로그 처리에 최적화 -> 고성능
- 유연성: 로그를 데이터베이스(MySQL, PostgreSQL 등)에 직접 저장하거나, 복잡한 필터링 규칙을 적용할 수 있다.
- 보안: TLS/SSL을 통한 암호화 전송을 지원.
- 작동 방식: /etc/rsyslog.conf 설정 파일에 정의된 규칙에 따라 로그들이 쌓인다.
  - /var/log/syslog: Debian/Ubuntu 계열
  - /var/log/messages: RHEL 계열
<img width="909" height="316" alt="image" src="https://github.com/user-attachments/assets/7e258ecd-1e63-4b2e-ab7f-e1b0b5089dd1" />

### rsyslogd와 journalctl
1. systemd-journald (1차 수집처)
커널 로그(kmsg), 시스템 서비스 로그, 초기 부팅 로그를 가장 먼저 받는다.
저장 방식: 바이너리(Binary) 형태. (journalctl 명령어로만 읽을 수 있음)
특징: 휘발적이라 기본 설정인 경우가 많아 속도가 매우 빠릅니다.

2. rsyslog (2차 처리 및 저장)
journald가 수집한 로그를 전달받아 텍스트 파일 형식으로 저장한다.
저장 방식: 일반 텍스트(Plain Text) 형태.
특징: 로그를 파일별로 분류하거나, 외부 원격 서버로 전송하는 등 복잡한 관리에 특화되어 있다.

**왜 두개를 같이 사용할까?**
<img width="722" height="416" alt="image" src="https://github.com/user-attachments/assets/a3dd5188-12d6-40d0-b63e-83559f70f1d9" />

**흐름**
로그 발생 → systemd-journald 수집 → journal 저장 → rsyslog 전달 → 텍스트 파일 저장/원격 전송

# journalctl을 활용한 시스템 로그 조회
# 서비스 장애 분석을 위한 로그 확인
# CPU·메모리·디스크·프로세스 사용량 점검
# 시스템 성능 측정 및 하드웨어 리소스 점검 도구 활용
