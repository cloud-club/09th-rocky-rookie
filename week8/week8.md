## 컨테이너와 VM의 차이
<img width="868" height="337" alt="image" src="https://github.com/user-attachments/assets/161dfdf9-8c23-45ae-b469-3120230ecf80" />


### VM
**구조**
<img width="314" height="328" alt="image" src="https://github.com/user-attachments/assets/1df2d3d2-d3ee-42a5-abbe-a84dbbcc35f1" />

**흐름**
<img width="295" height="268" alt="image" src="https://github.com/user-attachments/assets/8f6c0268-c4c3-4626-92d9-3b40ecc0b881" />

공통적으로 하나의 서버가 있고 한 서버에는 어떤 운영 체제가 있건 HostOS가 올라간다.
VM의 경우 HostOS에 의해 VM을 가상화 시켜주는 Hypervisor가 존재한다. 이를 통해 사용하여 각 VM마다 원하는 OS를 적용시켜 생성할 수 있다. 
앱마다 독립된 컴퓨터를 하나씩 만들어서 실행시킨다고 생각하면 된다.
Container에 비해 무겁지만, 격리가 강하다.
보안 격리가 매우 중요한 환경에 사용된다.
ex)
1. 서로 다른 고객사의 워크로드를 강하게 분리해야 할 때
2. 다른 OS 커널이 필요할 때
3. 보안상 완전한 격리가 중요할 때

### Container
**구조**
<img width="256" height="290" alt="image" src="https://github.com/user-attachments/assets/82a5c2fb-3744-424a-a886-e77be6771a04" />

**흐름**
<img width="289" height="146" alt="image" src="https://github.com/user-attachments/assets/56d4679a-e578-4847-bedf-234667258876" />

같은 OS 커널 위에서 여러개의 애플리케이션의 실행 환경을 나눠서 실행시킨다. 
같은 OS 위에서 앱 실행 공간만 분리해서 실행시킨다고 생각하면 된다. 
따라서 VM에 비해 빠르게 실행시킬 수 있고, 적은 메모리를 사용하고, 배포도 쉽고, 이미지 기반으로 실행되기 때문에 동일한 환경을 재현할 수 있다.
하지만 격리에 대한 수준은 VM보다 낮다. 

따라서 VM과 Container의 가장 큰 차이점은...
VM은 OS를 통째로 나눠 가진다.(OS 단위로 분리)
Container는 OS 커널을 공유하고 **실행환경만 나눠 가진다. **(프로세스 단위로 분리)

**예시**
<img width="194" height="107" alt="image" src="https://github.com/user-attachments/assets/6d939c93-7ece-41c5-983b-bb4aa9b069f1" />

개발한 애플리케이션을 다른 서버나 다른 개발자 PC에서 실행할 때 가장 큰 문제는 환경 불일치이다. 이 문제는 로컬에 설치된 라이브러리, 런타임, DB, 설정 파일, 버전 등이 서로 다르기 때문에 발생한다.
-> 이러한 상황에서 컨테이너를 통해 애플리케이션과 필요한 의존성을 하나로 묶어 어디서든 동일하게 실행할 수 있게 해준다. 

**컨테이너 가상화에 필요한 리눅스 기반 기술 2가지**
ch(change) root: 최상위 디렉토리로 변경하는 명령어
Cgroup(Control group): 지원을 컨트롤

## Docker와 Podman의 개념 및 기본 사용법
### Docker
컨테이너 생성 및 관리 도구


**Docker의 핵심 구성 요소**
<img width="612" height="346" alt="image" src="https://github.com/user-attachments/assets/fc1db97d-a0e3-4372-9ae4-771f146a8e61" />

**Docker의 실행 흐름**
docker run -d -p 80:80 nginx를 입력하면 아래와 같은 과정으로 nginx가 실행된다. 
1. nginx 이미지가 로컬에 있는지 확인
2. 없으면 Docker Hub에서 다운로드
3. 컨테이너 파일시스템 생성
4. 네트워크 연결
5. 포트 매핑 설정
6. nginx 프로세스 실행

**Docker 명령어**
```bash
# Docker 상태 확인
sudo systemctl status docker

# httpd 이미지 다운로드
sudo docker pull httpd

# httpd 컨테이너 실행
sudo docker run -d --name my-httpd -p 8080:80 httpd

# 실행 확인
sudo docker ps

# 접속 확인
curl http://localhost:8080

# 로그 확인
sudo docker logs my-httpd

# 컨테이너 내부 접속
sudo docker exec -it my-httpd bash

# 기본 웹 파일 위치
ls /usr/local/apache2/htdocs/

# 페이지 수정
echo "Hello Docker httpd" > /usr/local/apache2/htdocs/index.html

# 컨테이너 종료
exit

# 중지 및 삭제
sudo docker stop my-httpd
sudo docker rm my-httpd
```


**1. Docker를 통한 httpd 실습**
1-1. Docker 저장소 추가
```bash
sudo dnf config-manager --add-repo https://download.docker.com/linux/rhel/docker-ce.repo
```
<img width="945" height="120" alt="image" src="https://github.com/user-attachments/assets/119abb92-43d2-4479-857b-025d7f2e2165" />

1-2. Docker 설치
```bash
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```
<img width="944" height="338" alt="image" src="https://github.com/user-attachments/assets/de68b400-c531-496b-9fe4-a862cad709a1" />

1-3. Docker 서비스 시작
```bash
sudo systemctl start docker
```

1-4. Docker 상태 확인
<img width="746" height="183" alt="image" src="https://github.com/user-attachments/assets/d857a556-8df0-4cb8-8ce9-ff9ce6d7c68e" />
- Active: active (running)을 보면 정상적으로 작동된다는 것을 알 수 있다.

1-5. httpd 이미지 다운로드
```bash
sudo docker pull httpd
```

1-6. 이미지 확인
```bash
sudo docker images
```
<img width="941" height="147" alt="image" src="https://github.com/user-attachments/assets/795845ce-3e60-4fdc-b340-ea2d1eb16a1d" />

1-7. httpd 컨테이너 실행
```bash
sudo docker run -d --name my-httpd -p 8080:80 httpd
```
<img width="635" height="37" alt="image" src="https://github.com/user-attachments/assets/36b7e43d-1628-45c7-aed1-25773fced01e" />
- docker run: 컨테이너 실행
- -d: 백그라운드 실행
- --name my-httpd: 컨테이너 이름은 my-httpd로
- -p 8080:80 : Host의 8080 포트를 컨테이너의 80포트와 연결 -> curl을 통해 localhost:8080로 접속하면, 실제로는 컨테이너 안의 Apache 80번 포트로 연결이 된다. 
- httpd: 사용할 이미지 이름

1-8. 접속 확인
```bash
curl http://localhost:8080
```
<img width="757" height="205" alt="image" src="https://github.com/user-attachments/assets/abc9f6fb-96f2-4cc4-a245-13ea5be7691a" />

**2. 컨테이너 내부 접속 후 httpd의 index.html 편집**
2-1. 컨테이너 내부 접속
```bash
sudo docker exec -it my-httpd bash
```
<img width="453" height="62" alt="image" src="https://github.com/user-attachments/assets/97b3584f-ff39-40f5-8c1b-8e63927175f0" />
- sudo: docker exec: 이미 실행 중인(running) 컨테이너에 특정 명령을 내리겠다는 도커 명령어.
- -i (interactive): 표준 입력(STDIN)을 열어두어 사용자가 명령어를 입력할 수 있게 한다.
- -t (tty): 가상 터미널을 할당하여 실제 터미널 환경처럼 보이게 해준다. (이게 없으면 화면 출력이 어색할 수 있다.)
- my-httpd: 접속하려는 컨테이너의 이름(또는 ID)이다.
- bash: 컨테이너 안에서 실행할 명령어입니다. 여기서는 Bash 쉘을 실행하여 내부 서버 구조를 직접 조작하겠다는 뜻이다.

2-2. httpd 웹 파일 경로로 들어가서 페이지 편집
```bash
echo "Hello Docker httpd" > /usr/local/apache2/htdocs/index.html
```

2-3. Docker에서 빠져나오기
```bash
exit
```

2-4. 수정된 index.html 확인
<img width="404" height="43" alt="image" src="https://github.com/user-attachments/assets/07d67b81-5180-4c8d-ae03-db44129e2e4f" />

**3. Host 파일을 Container에 연결해서 실행**
보통 실무에선 Container에 직접 접속해서 작업을 하지 않고, Host의 파일을 Container에 연결해서 작업을 한다.

3-1. 기존에 생성한 Container 삭제
```bash
sudo docker stop my-httpd
sudo docker rm my-httpd
```
<img width="597" height="145" alt="image" src="https://github.com/user-attachments/assets/6205911f-197d-4987-b830-59af88dda401" />
- 컨테이너를 지우고, docker ps를 통해 현재 실행 중인 컨테이너를 확인했을 때 아무것도 없는 것을 확인할 수 있다.
- 사용중인 컨테이너는 삭제가 안되니, 꼭 중지 먼저 시키고 삭제를 해야한다. 

3-2. Host 환경에서 웹 페이지 폴더 생성 후 index.html 생성
<img width="642" height="40" alt="image" src="https://github.com/user-attachments/assets/70a0dfb8-f592-4808-a932-a7c28fbac904" />

3-3. 볼륨 마운트로 httpd 실행
```bash
sudo docker run -d --name my-httpd -p 8080:80 -v ~/httpd-test:/usr/local/apache2/htdocs httpd
```
<img width="557" height="125" alt="image" src="https://github.com/user-attachments/assets/1dba7e15-3798-4bcd-a8e7-05cbb9a7174b" />

3-4. 결과 확인
<img width="402" height="44" alt="image" src="https://github.com/user-attachments/assets/342ef4fb-e21f-4aee-81aa-7e93a43b3cc1" />
- 이제 Host에서도 index파일 수정을 통해 컨테이너의 웹 페이지 내용을 바꿀 수 있다.

### Podman
RedHat에서 개발한 Deamonless(Docker와의 차이점), 리눅스 네이티브 컨테이너 엔진.
  - Docker는 dockerd(daemon)에게 요청해서 컨테이너를 실행하지만, Podman은 사용자의 명령으로 컨테이너를 직접 실행한다. 
Docker와 호환되는 CLI를 제공하고, 루트 권한 없이(Rootless) 컨테이너를 실행할 수 있어 보안성이 높고, k8s 스타일의 pod 관리 기능을 제공한다.
명령어도 docker -> podman으로만 변경하면 될만큼 큰 차이가 없다.
```bash
| 작업           | Docker                          | Podman                          |
| ------------ | ------------------------------- | ------------------------------- |
| 버전 확인        | `docker --version`              | `podman --version`              |
| 이미지 다운로드     | `docker pull httpd`             | `podman pull httpd`             |
| 이미지 목록       | `docker images`                 | `podman images`                 |
| 컨테이너 실행      | `docker run`                    | `podman run`                    |
| 실행 중 컨테이너 확인 | `docker ps`                     | `podman ps`                     |
| 전체 컨테이너 확인   | `docker ps -a`                  | `podman ps -a`                  |
| 컨테이너 중지      | `docker stop my-httpd`          | `podman stop my-httpd`          |
| 컨테이너 삭제      | `docker rm my-httpd`            | `podman rm my-httpd`            |
| 이미지 삭제       | `docker rmi httpd`              | `podman rmi httpd`              |
| 로그 확인        | `docker logs my-httpd`          | `podman logs my-httpd`          |
| 내부 접속        | `docker exec -it my-httpd bash` | `podman exec -it my-httpd bash` |

```

## 이미지 검색·다운로드·삭제·관리
이미지: 컨테이너를 실행하기 위한 템플릿
예시: httpd 이미지
```bash
httpd 이미지
├─ Apache 실행 파일
├─ 필요한 라이브러리
├─ 기본 설정 파일
└─ 기본 웹 페이지
```
이러한 이미지를 실행하면 컨테이너가 된다. 

**이미지 관련 명령어**
1. 이미지 검색
```bash
docker search httpd
```
<img width="840" height="301" alt="image" src="https://github.com/user-attachments/assets/c923968d-e41a-4d79-81ec-3e920936e76b" />
- OFFICIAL: 공식 이미지 여부
- STAR: Docker Hub에서 받은 STAR 수

2. 이미지 다운로드
```bash
docker pull httpd
```

```bash
podman pull docker.io/library/httpd
```
- podman에선 docker와 다르게 저장소를 명확하게 쓰는 것이 좋다. 

3. 로컬에 존재하는 이미지 목록 확인
```bash
docker images or docker images ls
```
목록에서 확인해야할 요소
- REPOSITORY: 이미지 이름
- TAG: 이미지 태그
- IMAGE ID: 이미지 고유 ID
- CREATED: 이미지 생성 시점
- SIZE: 이미지 크기

4. 이미지 상세 정보 확인
```bash
docker image inspect httpd
```
<img width="855" height="402" alt="image" src="https://github.com/user-attachments/assets/fbfe930c-2a1c-46f0-b5ef-017b913e6def" />

필요한 정보만 보려면?
```bash
docker image inspect httpd --format '{{.Architecture}}'
docker image inspect httpd --format '{{.Os}}' 
docker image inspect httpd --format '{{.Config.ExposedPorts}}' #이미지가 기본적으로 사용하는 포트
```

5. 이미지 삭제
```bash
docker rmi httpd or docker image rm httpd
```

6. 특정 태그 삭제
```bash
docker rmi httpd:2.4
```

7. 강제 삭제
```bash
docker rmi -f httpd
```

8. 안쓰는 이미지 정리
```bash
docker image prune
```
- <none>으로 보이는 이미지들을 정리할 때 사용된다.

```bash
docker image prune -a
```
- 현재 컨테이너에서 사용하지 않는 이미지가 전부 삭제될 수 있다. 

### 태그의 개념
이미지 태그 = 이미지의 버전 또는 구분 이름으로 알면 된다.
같은 이미지라도 버전이 여러 개 존재한다. 

명령어에 태그를 명시하지 않으면 보통 latest로 처리가 된다. 
- latest는 항상 최신 버전이라는 의미는 아니다. 이미지 관리자가 latest를 어떤 버전에 붙였는지에 따라 달라진다.

**태그별 차이 예시**
버전태그
- 소프트웨어의 기능 업데이트나 변경 사항에 따라 버전을 명시하는 방식
- 문제가 생겼을 때 이를 통해 이전 버전 태그로 쉽게 롤백할 수 있다.
예시
- mysql:5.7
- mysql:8.0
- mysql:8.4

배포판 기반 태그
- 컨테이너 이미지가 어떤 운영체제나 기본 환경 위에서 작동하는지를 명시하는 방식
- OS의 이름과 버전을 포함하여, 빌드 환경이나 라이브러리 호환성을 보장한다.
- apline/slim과 같은 가벼운 OS를 선택하여 이미지의 크기를 경량화할 수 있다.

예시
- nginx:alpine
- python:3.12-slim
- ndoe:20-bookworm

### 포트 매핑·볼륨 연결·기본 네트워크 설정
격리된 컨테이너들에게 생길 수 있는 문제점
- 외부에서 컨테이너 앱에 접속하려면?
- Host 파일을 컨테이너 안에서 쓰려면?
- 컨테이너끼리 통신하려면?

이때 필요한 개념들
- 포트 매핑 = 외부에서 컨테이너로 들어오는 입구 연결
- 볼륨 연결 = Host 파일/디렉토리를 컨테이너와 공유
- 네트워크 설정 = 컨테이너 간 통신 경로 구성

**포트 매핑**
```bash
docker run -d -p 8080:80 httpd (호스트포트:컨테이너포트)
```
- Host의 8080포트로 들어온 요청을 Container의 80번 포트로 연결

```bash
브라우저
   ↓
localhost:8080
   ↓
Host 8080 포트
   ↓
Docker/Podman 포트 매핑
   ↓
Container 80 포트
   ↓
httpd 응답
```
- 실제 요청 흐름

**볼륨 연결**
컨테이너 내부 파일은 컨테이너를 삭제하면 같이 사라질 수 있으니, 중요한 데이터나 자주 수행이 필요한 데이터는 Host와의 볼륨 연결을 한다. 
