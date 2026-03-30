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
[Unit]
Description=My Web App -> 서비스에 대한 설명
After=network.target -> 네트워크가 준비된 뒤 실행 시작

[Service]
ExecStart=/usr/bin/python3 /app/main.py -> 실제 실행 명령
Restart=always -> 죽으면 다시 복구

[Install]
WantedBy=multi-user.target -> 일반 서버 모드 부팅 시 자동 실행 대상에 포함

```



