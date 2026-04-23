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
### journalctl
- systemd journal 로그를 조회하고 관리하는 명령어 도구

journalctl의 휘발성 저장과 영구 저장
- 휘발성인 journal인 경우의 로그 저장 경로: /run/log/journal/
- 영구 저장 journal인 경우의 로그 저장 경로: /var/log/journal/

해당 설정은 밑의 경로에서 변경할 수 있다.
 code형태로 부탁: /usr/lib/systemd/journald.conf
 [Journal]
**#Storage=auto**
#Compress=yes
#Seal=yes
#SplitMode=uid
#SyncIntervalSec=5m
#RateLimitIntervalSec=30s
#RateLimitBurst=10000

auto = /var/log/journal 경로가 존재하면 영구 저장, 없으면 휘발성
persistent = 영구저장
volatile = 휘발성
none = 저장안함

**영구 저장으로 바꾸고 싶다면?**
sudo mkdir -p /var/log/journal
sudo systemctl restart systemd-journald
sudo journalctl --flush 

journal --flush는 뭐지?
- /run/log/journal 쪽에 임시로 잡혀있는 데이터를 /var/log/journal쪽의 영구 저장소로 넘기고, journald가 이후에는 영구 저장소를 쓰도록 전환시키기 위한 역할이다.
- 즉, 현재 남아있는 임시 journal를 디스크 쪽으로 넘기는 작업을 해준다. 

1. 영구 저장 경로 만들기 전 로그 범위
<img width="764" height="63" alt="image" src="https://github.com/user-attachments/assets/eafa85a4-35d8-4660-bd04-d8a99cdc7eab" />

2. 영구 저장 경로 생성 후 로그 범위
<img width="756" height="85" alt="image" src="https://github.com/user-attachments/assets/c3881c9d-9dcb-462e-bd1b-b59e639fe008" />

journald는 휘발성인 특성을 갖고 있기 때문에 컨테이너 환경에서도 많이 사용된다.
- 컨테이너 환경에선 로그를 컨테이너 내부의 파일에 저장하는 것이 아닌, stateless하게 로그를 처리한다.
   - 애초에 컨테이너 자체가 언제든 삭제되고 다시 생성될 수 있는 휘발성인 특징을 갖고 있다.
   - 데이터 소실: 컨테이너가 삭제되면 내부의 /var/log/app.log 파일도 함께 사라진다.
   - 디스크 오버헤드: 컨테이너 내부 파일 시스템에 로그가 쌓이면 컨테이너가 무거워지고 결국 호스트 디스크 전체를 마비시킬 수 있다.
   - 접근성: 수백 개의 컨테이너에 일일이 접속해서 tail -f를 할 수는 없다.



### syslog vs journalctl
<img width="689" height="175" alt="image" src="https://github.com/user-attachments/assets/571ddd37-377d-4d5e-85ce-cae65fd6ffc6" />


**현재는 왜 journalctl을 더 많이 사용할까?**
1. 통합 로깅 (Integrated Logging)
syslog는 시스템 이곳저곳에 흩어진 텍스트 로그 파일을 관리해야 했지만, journald는 시스템 부팅, 커널, 서비스 시작, 애플리케이션 로그를 하나의 중앙 집중식 바이너리 파일로 통합 관리한다.
2. 강력한 필터링 및 검색 능력
바이너리 형태이기 때문에, 텍스트 파일에서 grep을 사용하는 것보다 훨씬 강력하고 빠르게 데이터를 추출할 수 있습니다.
- 시간별 필터링: --since "1 hour ago"
- 서비스별 필터링: -u nginx.service
- 우선순위별 필터링: -p err (에러 메시지만 출력)
- 실시간 팔로우: -f (로그가 쌓이는 것을 실시간으로 확인)
3. 메타데이터 보존
journalctl은 로그를 기록할 때 단순히 메시지만 저장하는 것이 아니라, 해당 로그가 발생한 프로세스 ID, 사용자 ID, 실행 파일 경로 등 다양한 메타데이터를 함께 저장한다. ->Trouble Shooting때 중요


### journalctl 실습
1. 전체 로그 출력
 code형태로 부탁: journalctl
<img width="834" height="235" alt="image" src="https://github.com/user-attachments/assets/c49ac69a-d9f4-4e8e-a309-ba991f48fe1c" />

2. 특정 서비스 로그 확인
이것도 code형태로 부탁: journalctl -u {서비스이름}.service
<img width="950" height="108" alt="image" src="https://github.com/user-attachments/assets/2b442799-17ec-4bb2-8223-9b69d75dd464" />

3. 특정 갯수의 로그 확인
이것도 code형태로 부탁: journalctl -u {서비스이름} -n 30

4. 부팅 이후의 특정 서비스 로그 확인
이것도 code형태로 부탁: journalctl -b -u {서비스이름}

5. 특정 서비스 로그 실시간으로 확인
이것도 code형태로 부탁: journalctl -u {서비스이름} -f

6. 커널단의 오류만 확인
이것도 code형태로 부탁: journalctl -k -p err



# 서비스 장애 분석을 위한 로그 확인
1. systemctl로 서비스 상태 확인
2. journalctl로 해당 서비스 로그 확인


### nginx를 통한 장애 분석 실습 1(nginx 설정 문법 오류 생성)
1. /etc/nginx/conf.d/test.conf 에 일부러 문법 오류를 넣는다.
<img width="640" height="397" alt="image" src="https://github.com/user-attachments/assets/8f97e2ae-2995-4930-98df-580f0080300e" />

2. systemctl status 확인
<img width="922" height="339" alt="image" src="https://github.com/user-attachments/assets/2f874442-d5d0-4c12-a19b-077555ab415d" />

3. journalctl 확인
<img width="1015" height="449" alt="image" src="https://github.com/user-attachments/assets/f2562c22-d7f6-4e05-a0cf-b460d60a183e" />
- 46번째 줄에 문법 오류가 난 것을 알 수 있다.

### nginx를 통한 장애 분석 실습 2(nginx 포트 충돌 장애 생성)
1. nginx를 중지하고, 임시의 웹서버를 띄우고, 80번 포트에 연결한다.
<img width="761" height="109" alt="image" src="https://github.com/user-attachments/assets/b3333c29-63f8-4a1e-8d74-5344e60e1c61" />

2. 다른 터미널에 들어가서 nginx를 다시 실행시키고, 오류를 확인한다.
<img width="1020" height="550" alt="image" src="https://github.com/user-attachments/assets/e69414b9-822f-410c-910e-076b50741c2f" />


# CPU·메모리·디스크·프로세스 사용량 점검
**1. 실시간 CPU 사용량 확인**
<img width="815" height="261" alt="image" src="https://github.com/user-attachments/assets/2aba7db5-125b-4c8b-9208-9a5f2fc2a5da" />
{content: top }
주요 확인 목록
- %Cpu(s) : 전체 CPU 사용률
- us : 사용자 영역 사용률
- sy : 커널 영역 사용률
- id : 유휴 비율
- wa : I/O 대기

**2. 스냅샷 CPU 사용량 확인**
- top은 실시간, ps aux는 실행 순간의 스냅샷이라고 보면 된다.
- 시스템에 떠 있는 거의 모든 프로세스를 자세히 보여준다.
{content: ps aux}


**3. 메모리와 스왑 사용량 확인**
<img width="667" height="84" alt="image" src="https://github.com/user-attachments/assets/ba863497-7ab8-41b1-ac51-011121004654" />
- 리눅스가 캐시를 많이 사용해서 free는 거의 0이어도 available이 충분하면 아직 여유가 있는 상태일 수 있음
{content: free -h}

**4. 프로세스, 메모리, 스왑, I/O, CPU 상태를 주기적으로 확인**
<img width="881" height="165" alt="image" src="https://github.com/user-attachments/assets/a3b6c4b0-e5d4-4a24-a1ac-6baed3e15f7f" />
- 명령어 뒤의 숫자는 주기 시간을 의미.
{content: vmstat 1}

### 부하 실습
1-1. CPU에 부하 설정
<img width="889" height="87" alt="image" src="https://github.com/user-attachments/assets/f3935260-07df-4e10-90f8-9625e97f99de" />

1-2. top 명령어로 실시간 확인
<img width="783" height="194" alt="image" src="https://github.com/user-attachments/assets/a45478f7-0ee2-474b-b143-12e45f4ef4f2" />

1-3. ps aux 명령어로 스냅샷 확인
<img width="849" height="148" alt="image" src="https://github.com/user-attachments/assets/0167a02e-7b8f-40e8-aeda-87ff17f9823e" />

2-1. Memory에 부하 설정
<img width="567" height="163" alt="image" src="https://github.com/user-attachments/assets/8fc8bfc8-fd11-4e28-942a-511db3850ec7" />

2-2. free -h로 확인
<img width="567" height="163" alt="image" src="https://github.com/user-attachments/assets/8fc8bfc8-fd11-4e28-942a-511db3850ec7" />

2-3. ps aux로 확인
<img width="946" height="253" alt="image" src="https://github.com/user-attachments/assets/2d3ea3b5-137c-44c7-9255-b1fca5d2e1d6" />
- stress-ng가 위에서 메모리를 많이 잡아먹는 것을 확인할 수 있다.
- RSS: 실제 RAM에 올라가 있는 메모리 크기

2-4. vm stat로 swap
<img width="722" height="325" alt="image" src="https://github.com/user-attachments/assets/270ce013-d6ae-4ec4-a4ab-86e4e58cb18f" />
- 메모리 부하로 남은 RAM이 부족해졌다. -> 커널이 일부 메모리를 swap으로 내보냈다 -> vmstat에서 so값이 크게 올랐다.
- so(swap out): RAM에 있던 메모리를 SWAP 영역(디스크)로 내보낸 양
