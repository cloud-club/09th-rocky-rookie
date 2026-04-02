# systemd 구조와 서비스 관리 개념 이해

## OS의 핵심, Kernel
컴퓨터 전원을 켜면 가장 먼저 하드웨어가 초기화되고, 그 다음 부트로더가 리눅스 kernel 을 메모리에 올린다.
Kernel은 실제 자원을 직접 컨트롤하는 관리자이다.
1. CPU, 메모리 할당
2. 디스크, USB, 네트워크와 같은 장치 제어
3. 프로세스 생성/종료 관리
4. 파일 시스템 관리
5. 권한과 보안 관리
이와 같은 역할을 하는 Kernel이 올라온 뒤에는  User Space에서 실제 응용 프로그램(word, ppt, 웹 브라우저 등)이 실행된다. 이때 Kernel은 User Space에서 돌아갈
PID1 이라는 첫 번째 프로세스를 실행시킨다. 

## PID 1
- Kernel이 부팅을 마치면 가장 먼저 실행되는 프로세스이다.
- 이 프로세스가 나머지 시스템을 관리

## systemd & init
- 과거에는 init이 PID1로 서, 다른 모든 프로세스의 부모가 되어 시스템을 관리했다.(순차 실행 방식)
- 현재는 systemd를 통해 PID1의 작업을 실행한다.(병렬 실행 방식)

### systemd란?
리눅스에서 시스템 부팅, 서비스 실행, 로그, 의존성, 자원 관리 등을 담당하는 초기화 시스템이다.

### systemd의 구조
systemd는 여러 개의 unit을 기반으로 하여 종속성 관리와 병렬 서비스를 실행시킨다. 
1. service : 서비스 실행 단위
2. socket : 소켓 기반 활성화
3. target : 여러 유닛을 묶는 목표 상태
4. mount : 파일 시스템 마운트
5. timer : 예약 작업
6. path : 특정 파일/디렉터리 변화 감지

또한 각 서비스를 유닛 파일로 정의한다. 
유닛 파일에는 아래와 같은 내용들이 들어간다.
1. 서비스에 대한 설명
2. 무엇이 먼저 떠야하는지?
3. 실제 어떤 명령으로 실행하는지
4. 실패 시 재시작할지
5. 부팅 때 자동 실행할지 여부와 연결 관계

```ini
# /usr/lib/systemd/system/sshd.service
[Unit] #이 서비스가 무슨 서비스인지, 무엇 뒤에 실행되는지, 어떤 유닛을 함께 원하는지
Description=OpenSSH server daemon #OpenSSH 서버 데몬
Documentation=man:sshd(8) man:sshd_config(5) #관련 문서를 알려주는 줄, man:sshd(8)은 sshd 실행 프로그램 설명서이고, man:sshd_config(5)는 sshd 설정 파일 설명서이다. 
After=network.target sshd-keygen.target #실행 순서를 의미. 네트워크 관련 기본 준비가 끝나고 sshd-keygen.target이 처리된 뒤에 이 sshd 서비스를 시작하라는 뜻이다. 
Wants=sshd-keygen.target #약한 의존성이 들어간다. "가능하면" sshd-keygen.target도 같이 끌고와서 실행하라는 의미이다. 동작이 실패한다고 해도 무조건 sshd까지 막는 강한 의존 관계는 아니다.
Wants=ssh-host-keys-migration.service #sshd가 실행되기 전에 키 관련 상태를 최신 정책에 맞게 정리해주는 서비스

[Service] #실제로 어떻게 실행할지, 재시작은 어떻게 할지
Type=notify #Type은 서비스 시작 완료를 systemd가 어떻게 판달할지를 정하는 옵션이다. 즉 이 줄(Type=notify)는 프로세스가 뜨기만 했다고 바로 성공으로 보지 말고, 프로그램이 systemd에게 준비가 완료됐다고 알릴 때까지 기다리라는 옵션이다. 
EnvironmentFile=-/etc/sysconfig/sshd # 환경 변수를 외부 파일에서 읽겠다
ExecStart=/usr/sbin/sshd -D $OPTIONS #서비스를 시작핼 때 실제 실행할 명령어
ExecReload=/bin/kill -HUP $MAINPID #서비스 reload 명령어
KillMode=process #서비스를 종료할 때 어디까지 죽일지
Restart=on-failure #서비스가 비정상 종료하면 자동으로 다시 시작
RestartSec=42s #재시작 시, 42초 기다려라
[Install] #부팅 시 어떤 target에 연결할지 -> 자동 실행과 관련된 부분
WantedBy=multi-user.target # 이 서비스를 enable하면 multi-user.target에 연결. 서버가 일반 운영 모드로 부팅될 때 sshd도 함께 자동 시작되게 하라
#여기서 target은 시스템이 도달하려는 상태로 생각하면 된다.
#multi-user.target = CLI 기반 일반 서버 상태
#graphical.target = GUI 포함 상태
# rescue.target = 복구 모드 비슷한 상태
```

# 서비스 시작·중지·재시작·자동 실행 설정
##systemctl
systemctl은 systemd를 직접적으로 관리하게 해주는 명령어이다. 

##대표적인 systemctl 예시
```ini
systemctl start httpd  #서비스 시작
systemctl status httpd #서비스 상태 확인
systemctl stop httpd #서비스 중단
systemctl restart httpd #서비스 재시작
```
# 프로세스란?
## 서비스 vs 프로세스
### 프로세스 : 실행 중인 프로그램의 인스턴스
예시: 파이썬 앱, 웹 브라우저 등등

### 서비스: 시스템이 지속적으로 관리해야 하는 백그라운드 작업
예시: sshd, mysqld 등등

## 프로세스 안에 포함되어 있는 정보들
1. PID(프로세스 아이디)
2. PPID(프로세스 부모 아이디)
3. 사용자
4. CPU 사용량
5. 메모리 사용량
6. 상태
7. 우선 순위

## 프로세스 관련 명령어
ps : 프로세스 조회
```ini
ps -ef | grep nginx #nginx의 프로세스를 검색해서 상세 정보를 출력
```
kill : 프로세스 종료/제어
```ini
kill {PID} #프로세스 종료
```
top : 실시간 프로세스 모니터링
```ini
top -p $(pgrep -d',' nginx) #현재 실행 중인 모든 nginx 프로세스의 상태를 실시간으로 모니터링
```
nice : 프로세스 우선순위 조정(renice는 이미 작동하는 프로세스의 우선순위를 재조정)
```ini
sudo renice 5 -p {PID} #해당 PID의 우선순위를 재조정
```
#작업 스케줄링
명령어나 스크립트를 특정 시점에 자동 실행
- 매일 새벽 2시에 로그 백업
- 매주 일요일마다 임시 파일 삭제
- 오늘 오후 5시에 서버 재시작 스크립트 실행

##cron
반복적으로 실행해야 하는 작업을 위해 사용된다. 
백그라운드에서 동작하는 crond이 시간을 확인하다가, 지정한 시간이 되면 설정한 명령어를 실행한다. 
cron 작업은 crontab에 등록되어 실행된다. 

###crontab 명령 형식
```ini
* * * * * 명령어 #분 시 일 월 요일 명령어
0 2 * * * /home/user/backup.sh #매일 새벽 2시에 백업 실행
0 2 * * * /home/user/backup.sh >> /home/user/backup.log 2>&1 # 위와 같은 명령어지만, 실행 결과와 에러 로그를 남길 수 있다.
*/10 * * * * /home/user/check.sh #10분마다 check.sh 실행
30 9 * * 1 /home/user/report.sh #매주 9시 30분에 report.sh 실행
```
###crontab 관련 명령어
```ini
crontab -e #스크립트 파일에서 cron 작업 편집.
```

```ini
crontab -l #현재 등록된 cron 작업 목록 출력.
```

```ini
crontab -r #cron 작업 삭제 
```

##at
반복이 아니라 특정 시각에 딱 1회 실행을 위한 작업에 실행된다. 예약성 작업을 위해 사용.
- at now + 10 minutes
- at now + 1 hour
- at 5pm tomorrow
- at midnight
위와 같은 방법으로 시간을 예약할 수 있다.

#cgroup 기반 자원 제어 기초
croup(control group): 리눅스에서 프로세스 그룹 단위로 자원(CPU, 메모리, Block I/O, PID 수 등)을 제한하거나 관리할 수 있게 해주는 기능
프로세스와 마찬가지로 cgroup은 계층적으로 구성되어 있으며 하위 cgroup 부모 cgroup 속성의 일부를 상속하도록되어 있다.
croup을 왜 사용할까?
서버에서 여러 프로세스가 같이 돌 때, 하나가 자원을 과도하게 먹어버리면 다른 서비스가 느려질 수 있다. 이럴 때 cgroup으로 자원 사용량을 제한해서 시스템 전체 안정성을 유지시킬 수 있다.

##cgroup v1 & cgroup v2
###cgroup v1
- 자원별로 독립적인 계층 구조를 가지며, 유연하지만 설정이 복잡하고 일관성이 부족하여 현재는 v2로 전환되는 추세이다.
- 프로세스를 어느 노드에도 위치 가능하다.
- 각 컨트롤러마다 파일명과 동작이 다르다.

###cgroup v2
- v1의 분리된 계층 구조를 단일 통합 구조로 개선하여 컨테이너 환경(k8s)에서 더 정교한 리소스 관리, 안전한 위임, 효율적인 압력 인지를 제공한다.
- 프로세스는 리프 노드에만 위치 가능하다.
- 모든 컨트롤러가 동일한 규칙을 가지고 있다.

cgroup v2의 구조
```ini
/sys/fs/cgroup
└── /
    ├── system.slice
    │   ├── sshd.service
    │   ├── nginx.service
    │   ├── cron.service
    │   └── docker.service
    │
    ├── user.slice
    │   ├── user-1000.slice
    │   │   ├── user@1000.service
    │   │   │   ├── app.slice
    │   │   │   ├── session.slice
    │   │   │   └── ...
    │   │   ├── session-1.scope
    │   │   └── session-2.scope
    │   │
    │   └── user-1001.slice
    │       ├── user@1001.service
    │       └── session-3.scope
    │
    └── machine.slice
        ├── docker-<id>.scope
        ├── libpod-<id>.scope
        └── vm-<name>.scope 
```
1. system.slice
부팅하면서 올라오는 주요 데몬들이 여기 들어간다. 즉, 운영체제 차원에서 돌아가는 서비스들이 system.slice 아래에 배치된다.

예시
- nginx.service
- sshd.service
- httpd.service
- NetworkManager.service

2. user.slice
사용자 로그인 세션과 사용자별 프로세스용 공간이다. 즉, 사람이 로그인해서 쓰는 프로그램들은 보통 user.slice 계층에 들어간다.

예시(사용자가 해당 vm에 로그인하면)
- user-1000.slice → UID 1000 사용자의 자원 그룹
- user@1000.service → 해당 사용자용 systemd 인스턴스
- session-1.scope → 로그인 세션 하나
- 그 안에서 실행한 shell, editor, app 등이 포함

3. machine.slice
VM, 컨테이너 같은 격리된 머신 단위 워크로드를 위한 공간이다. 즉, 시스템 서비스도 아니고 일반 로그인 사용자 세션도 아닌, 독립된 실행 환경을 보통 여기에 넣는다.

예시
- Docker 컨테이너
- Podman 컨테이너
- vm







