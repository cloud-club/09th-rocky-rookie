# 리눅스 시스템 로그 기초부터 서비스 장애 분석까지

---

## 1. 로그(Log)의 역할과 RHEL 10의 로깅 아키텍처

로그는 커널 레벨에서 발생하는 하드웨어 이벤트부터 사용자가 실행하는 애플리케이션의 메시지까지, 시스템에서 일어나는 모든 활동을 기록한 데이터입니다. RHEL 10은 보다 효율적인 로그 관리를 위해 두 가지 핵심 로깅 엔진이 상호 보완적으로 작동하는 이중 구조를 사용합니다.

- **systemd-journald (저널디)**: 시스템 부팅 초기 단계부터 커널, 서비스 출력, 에러 등을 구조화된 이진(Binary) 데이터 형식으로 수집합니다. 인덱싱이 되어 있어 검색 속도가 매우 빠르다는 장점이 있습니다.
- **rsyslog (알시스로그)**: journald로부터 데이터를 전달받아 우리가 읽을 수 있는 전통적인 텍스트 파일(Plain Text) 형태로 분류하여 저장하거나, 원격 서버로 로그를 전송하는 역할을 합니다.

---

## 2. 주요 로그 파일 구조 이해 (`/var/log` 해부하기)

rsyslog 서비스는 수집된 텍스트 기반 로그들을 용도에 맞게 `/var/log/` 디렉터리 하위의 여러 파일로 나누어 저장합니다. 초보자가 반드시 알아야 할 핵심 로그 파일은 다음과 같습니다.

| 파일 경로 | 설명 |
|---|---|
| `/var/log/messages` | 시스템 전체의 활동을 기록하는 통합 로그 파일. 특정 전용 로그를 제외한 커널, 서비스, 일반 운영 메시지 대부분이 기록되며, 장애 발생 시 가장 먼저 확인해야 할 시작점 |
| `/var/log/secure` | 인증 및 보안 관련 이벤트 전용 파일. SSH 접속 시도(성공/실패), `sudo` 권한 상승 기록이 저장됨 |
| `/var/log/cron` | 예약된 작업(cron 또는 anacron)의 실행 여부와 오류 상태를 기록 |
| `/var/log/boot.log` | 시스템 부팅 과정에서 각 서비스의 시작 성공/실패를 요약 |
| `/var/log/dmesg` | 커널 링 버퍼 메시지. 하드웨어 인식 및 장치 드라이버 로드 관련 기록. 부팅 시마다 갱신됨 |
| `/var/log/wtmp` `/var/log/lastlog` | 사용자 로그인/로그아웃 기록을 담은 이진(Binary) 파일. `last` 및 `lastlog` 명령어로 조회 |

---

## 3. `journalctl`을 활용한 시스템 로그 조회

systemd-journald가 수집한 로그는 이진 파일이므로 일반 텍스트 편집기(`cat`, `vi` 등)로는 볼 수 없습니다. 대신 **`journalctl`** 이라는 전용 명령어를 사용하여 로그를 쉽고 강력하게 검색할 수 있습니다.

### 기본 사용법 및 화면 탐색

```bash
journalctl          # 수집된 전체 저널 로그 출력 (방향키로 이동, q로 종료)
journalctl -n 50    # 가장 최근 50줄만 출력
journalctl -f       # 실시간으로 추가되는 로그 출력 (Ctrl+C로 종료)
```

### 시간 및 부팅 단위 필터링

```bash
journalctl -b                                                          # 현재 부팅 이후 로그만 표시
journalctl --since "1 hour ago"                                        # 최근 1시간 로그 추출
journalctl --since "2025-03-20 10:00:00" --until "2025-03-21 09:00:00" # 특정 시간 범위 로그 조회
```

### 특정 서비스 로그 필터링 (`-u`)

장애가 발생한 특정 서비스(Unit)의 로그만 골라볼 때 사용합니다.

```bash
journalctl -u sshd.service         # SSH 데몬 관련 로그만 조회
journalctl -u httpd -u mariadb     # 2개 이상의 서비스 로그 동시 조회
```

### 중요도(Severity) 기반 필터링 (`-p`)

로그는 0부터 7까지 8단계의 중요도(우선순위)를 가집니다.

| 레벨 | 이름 | 설명 |
|---|---|---|
| 0 | emerg | 시스템 사용 불가 |
| 1 | alert | 즉각적인 조치 필요 |
| 2 | crit | 치명적 상태 |
| 3 | err | 에러 |
| 4 | warning | 경고 |
| 5 | notice | 일반적이지만 유의미한 이벤트 |
| 6 | info | 일반 정보 |
| 7 | debug | 디버깅 메시지 |

```bash
journalctl -p err   # 에러(err) 이상의 심각한 문제(0~3단계)만 필터링
journalctl -p 3     # 위와 동일 (숫자로도 지정 가능)
```

---

## 4. 서비스 장애 분석을 위한 로그 확인 (실전 시나리오)

서버 운영 중 장애가 발생했을 때 로그를 통해 원인을 추적하는 실전 기법입니다.

### 시나리오 A: 서비스가 시작되지 않고 실패할 때

특정 서비스를 시작했는데 `Job for service.service failed` 에러가 발생한 경우 상세 원인을 확인합니다.

```bash
journalctl -u 서비스명.service -n 50 --no-pager
```

설정 파일 오타, 구문 오류, 라이브러리 파일 누락, 접근 권한 부족 등 서비스가 구동되지 못한 결정적인 원인이 텍스트로 출력됩니다.

### 시나리오 B: 네트워크 연결 장애

서버가 IP를 할당받지 못하거나 네트워크 통신이 되지 않을 때 확인합니다.

```bash
journalctl -u NetworkManager
journalctl -u systemd-networkd
```

하드웨어(랜카드) 자체의 인식 문제인지 확인하려면 커널 드라이버 메시지를 함께 분석합니다.

```bash
journalctl -k | grep -E "eth|wlan|enp"
```

### 시나리오 C: 알 수 없는 권한 거부 (Permission denied)

파일 권한을 `777`로 설정했는데도 애플리케이션에서 접근 거부가 발생한다면, **SELinux**에 의해 차단되었을 가능성이 높습니다.

```bash
journalctl -t audit           # 저널에 기록된 audit 관련 메시지 조회
ausearch -m AVC -ts recent    # 최근 SELinux AVC 차단 이벤트 검색
cat /var/log/audit/audit.log  # SELinux AVC 로그 직접 확인
```

SELinux가 어떤 프로세스의 접근을 막았는지(AVC Denial)를 확인하고, 올바른 보안 컨텍스트(레이블)를 부여하여 문제를 해결해야 합니다.

---

## 5. 저널 로그 영구 보존(Persistence) 설정

> RHEL 8부터 `systemd` 패키지가 `/var/log/journal/` 디렉터리를 기본으로 생성하기 때문에, RHEL 8 / 9 / 10 모두 **영구 저장이 기본값**입니다(`Storage=auto` 설정에서 해당 디렉터리가 존재하면 자동으로 영구 저장).

### 기본 동작 확인

```bash
# 현재 저널 저장 위치 확인
ls /var/log/journal/   # 디렉터리가 존재하면 영구 저장 중
```

### 명시적으로 영구 저장을 강제하는 방법

시스템 정책상 명시적으로 설정을 고정하고 싶다면 다음과 같이 구성합니다.

**1단계**: `/etc/systemd/journald.conf` 파일 편집

```ini
[Journal]
Storage=persistent
```

> RHEL 10은 배포판 기본 설정을 `/usr/lib/systemd/journald.conf.d/`에서 관리하고, 사용자 커스텀 오버라이드는 `/etc/systemd/journald.conf` 또는 `/etc/systemd/journald.conf.d/`에서 관리합니다.

**2단계**: 데몬 재시작하여 설정 적용

```bash
systemctl restart systemd-journald
```

**3단계**: 현재 메모리에 남아 있는 로그를 디스크로 즉시 플러시

```bash
journalctl --flush
```

이 설정을 완료하면 시스템이 예기치 않게 재부팅되더라도 과거의 시스템 장애 로그를 완벽하게 추적할 수 있습니다.

---

## 정리: 장애 발생 시 로그 확인 체크리스트

1. `journalctl -p err -b` — 현재 부팅 이후 에러 이상 심각도 로그 전체 확인
2. `journalctl -u <서비스명> -n 50` — 문제가 된 특정 서비스 로그 집중 확인
3. `journalctl -f` — 실시간 로그 모니터링으로 재현 시도
4. `ausearch -m AVC -ts recent` — SELinux 차단 여부 확인
5. `/var/log/secure` — 인증/권한 관련 이상 여부 확인
