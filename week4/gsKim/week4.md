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
## 프로세스 : 실행 중인 프로그램의 인스턴스
예시: 파이썬 앱, 웹 브라우저 등등

## 서비스: 시스템이 지속적으로 관리해야 하는 백그라운드 작업
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

  






