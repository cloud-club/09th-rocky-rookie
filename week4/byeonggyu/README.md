# Week4. 서비스 관리와 프로세스 제어

> systemd 구조와 서비스 관리 개념 이해, 서비스 시작·중지·재시작·자동 실행 설정(systemctl), 프로세스 조회 및 제어(ps, top, kill, nice), 작업 스케줄링(cron, at), cgroup 기반 자원 제어 기초

## systemd 구조와 서비스 관리
`systemd`는 System Daemon으로, 여기서 Daemon은 background에서 실행되는 process를 의미한다.
리눅스 커널이 부팅된 후 가장 먼저 실행되는, PID(Process ID)가 1번인 process이다.  
시스템 전체의 서비스와 자원을 관리하는 역할을 한다.

- Unit: `systemd`가 관리하는 대상(서비스, 마운트 지점, 장치 등)을 의미한다.
- Target: 여러 Unit들을 그룹화한 것이다.

### systemd process 조회
아래처럼 `pstree` 명령어를 통해 process의 부모-자식 관계를 트리 형태로 볼 수 있다.  
루트 process가 `systemd(1)`임을 볼 수 있다.
```
byeonggyu@localhost:~> pstree -p | head -n 5
systemd(1)-+-ModemManager(1062)-+-{ModemManager}(1068)
           |                    |-{ModemManager}(1069)
           |                    `-{ModemManager}(1072)
           |-NetworkManager(1115)-+-{NetworkManager}(1116)
           |                      |-{NetworkManager}(1117)
...
```

리눅스는 process들도 파일로 저장한다. `/proc/<pid>/` 안에는 현재 process들의 상태를 보여주기 위한 인터페이스가 저장된다. `/proc/1/comm`에 process 이름이 들어있다.
```
byeonggyu@localhost:/proc/1> cat comm
systemd
```

### `/etc/systemd/system/*.service` 파일 (Unit)
특정 기능을 수행하는 Demon들이 저장되어 있다.
```
byeonggyu@localhost:/etc/systemd/system> ls | grep .service
dbus-org.bluez.service
dbus-org.fedoraproject.FirewallD1.service
dbus-org.freedesktop.Avahi.service
dbus-org.freedesktop.ModemManager1.service
dbus-org.freedesktop.nm-dispatcher.service
dbus.service
display-manager.service
vmtoolsd.service.requires
```
- `firewalld.service`: 기본 방화벽 서비스, 네트워크 보안을 책임
- `display-manager.service`: GUI 환경을 로그인 화면으로 연결
- `vmtoolsd.service`: VM에서 호스트와 게스트 간의 통신
- `dbus-org.xxx.service`: D-Bus로, process간 통신(IPC) 시스템

### `/etc/systemd/system/*.target` 파일 (Target)
리눅스는 부팅 시 수백 개의 서비스를 동시에 띄운다. 이 때 어떤 Target이 실행될 때 어떤 Unit이 필요한가를 정의한다. `*.target.wants`는 target이 활성화될 때 같이 실행될 Unit들의 링크들이 들어있는 폴더이다.
```
byeonggyu@localhost:/etc/systemd/system> ls | grep .target
bluetooth.target.wants
ctrl-alt-del.target
default.target
default.target.wants
getty.target.wants
graphical.target.wants
multi-user.target.wants
network-online.target.wants
printer.target.wants
sockets.target.wants
sysinit.target.wants
timers.target.wants
```

### Target/Unit 구조 확인
#### `default.target` 확인
```
byeonggyu@localhost:/etc/systemd/system> cat default.target
#  SPDX-License-Identifier: LGPL-2.1-or-later
#
#  This file is part of systemd.
#
#  systemd is free software; you can redistribute it and/or modify it
#  under the terms of the GNU Lesser General Public License as published by
#  the Free Software Foundation; either version 2.1 of the License, or
#  (at your option) any later version.

[Unit]
Description=Graphical Interface
Documentation=man:systemd.special(7)
Requires=multi-user.target
Wants=display-manager.service
Conflicts=rescue.service rescue.target
After=multi-user.target rescue.service rescue.target display-manager.service
AllowIsolate=yes
```
- [Unit]으로 하나만 있는데, Target 자체도 하나의 Unit이자 수많은 다른 Unit들을 끌어오는 역할을 한다.
- Requires: 강한 의존성, `multi-user.target`에 속한 Unit들이 먼저 불림
- Wants: 약한 의존성, `Graphical Interface`이므로 `display-manager.service`가 불렸으면 좋겠다는 의미
- Conflicts: 상충 관계, `rescue.target`이 실행 중이면 켜지면 안됨
- After: 실행 순서, 리스트된 service/target이 모두 시작된 후에 완료된 것으로 간주하라는 의미

#### `default.target.wants` 확인
```
byeonggyu@localhost:/etc/systemd/system/default.target.wants> ll
lrwxrwxrwx. 1 root root 55 Mar 27 18:30 nvmefc-boot-connections.service -> /usr/lib/systemd/system/nvmefc-boot-connections.service
```
`nvmefc-boot-connections.service`도 같이 실행된다.

#### `display-manager.service` 확인
```
byeonggyu@localhost:/etc/systemd/system> cat display-manager.service
[Unit]
Description=GNOME Display Manager

# replaces the getty
Conflicts=getty@tty1.service
After=getty@tty1.service

# replaces plymouth-quit since it quits plymouth on its own
Conflicts=plymouth-quit.service
After=plymouth-quit.service

# Needs all the dependencies of the services it's replacing
# pulled from getty@.service and plymouth-quit.service
# (except for plymouth-quit-wait.service since it waits until
# plymouth is quit, which we do)
After=rc-local.service plymouth-start.service systemd-user-sessions.service

# GDM takes responsibility for stopping plymouth, so if it fails
# for any reason, make sure plymouth still stops
OnFailure=plymouth-quit.service

[Service]
ExecStart=/usr/sbin/gdm
KillMode=mixed
Restart=always
IgnoreSIGPIPE=no
BusName=org.gnome.DisplayManager
EnvironmentFile=-/etc/locale.conf
ExecReload=/bin/kill -SIGHUP $MAINPID
KeyringMode=shared

[Install]
Alias=display-manager.service
```
- `.service` 파일은 [Service]에 대한 정보도 포함되어 있다.

## 서비스 시작·중지·재시작·자동 실행 설정(systemctl)
Service 관리를 위한 명령어는 아래와 같다.
- 설정 반영: `systemctl daemon-reload`
- 서비스 시작: `systemctl start <service-name>`
- 서비스 중지: `systemctl stop <service-name>`
- 상태 확인: `systemctl status <service-name>`
- 부팅 시 자동 실행: `systemctl enable <service-name>`
- 활성화 여부 확인: `systemctl is-enable <service-name>`

### Service 실습
새로운 Service를 만들고 반영해본다.

#### 1. Service 시작 시 실행할 bash 파일 생성
Service 시작이 잘 됐는지 확인하기 위해, bash 파일을 하나 만든다.
```
byeonggyu@localhost:/usr/local/bin> sudo vi my-script.sh

#!/bin/bash
while true;
do echo "Hello Rocky!" >> /var/log/my-service.log;
sleep 5;
done
```
`/var/log/my-service.log`에 Hello Rocky!를 반복해서 출력하는 bash script이다.

#### 2. Service 파일 생성
`my-service.service` 파일을 아래와 같이 생성한다.
```
byeonggyu@localhost:/etc/systemd/system> vi my-service.service

[Unit]
Description=My Custom Hello Service

[Service]
ExecStart=/usr/local/bin/my-script.sh

[Install]
WantedBy=multi-user.target
```
Start 시 만들어둔 script를 실행한다.

#### 3. Service 시작
`systemctl` command를 통해 Service를 시작하고, .log 파일을 확인한다.
```
byeonggyu@localhost:~> sudo systemctl start my-service

byeonggyu@localhost:/var/log> vi my-service.log

Hello Rocky!
Hello Rocky!
```

#### 4. status 확인
`active` 상태임을 확인할 수 있다. 부팅 시 자동 시작 옵션을 켜지 않았기 때문에, `disabled` 상태이다.
```
byeonggyu@localhost:~> sudo systemctl status my-service
● my-service.service - My Custom Hello Service
     Loaded: loaded (/etc/systemd/system/my-service.service; disabled; preset: disabled)
     Active: active (running) since Sat 2026-03-28 01:23:01 KST; 7min ago
 Invocation: 3ed6d89b98314da891a1f5d2d7d498c6
   Main PID: 4664 (my-script.sh)
      Tasks: 2 (limit: 16956)
     Memory: 636K (peak: 1M)
        CPU: 357ms
     CGroup: /system.slice/my-service.service
             ├─4664 /bin/bash /usr/local/bin/my-script.sh
             └─4857 sleep 5

Mar 28 01:23:01 localhost.localdomain systemd[1]: Started my-service.service - My Custom Hello Service.
```

stop 후에는 `inactive` 상태로 변함을 확인할 수 있다.
```
byeonggyu@localhost:~> sudo systemctl stop my-service
byeonggyu@localhost:~> sudo systemctl status my-service
○ my-service.service - My Custom Hello Service
     Loaded: loaded (/etc/systemd/system/my-service.service; disabled; preset: disabled)
     Active: inactive (dead)

Mar 28 01:23:01 localhost.localdomain systemd[1]: Started my-service.service - My Custom Hello Service.
Mar 28 01:31:17 localhost.localdomain systemd[1]: Stopping my-service.service - My Custom Hello Service...
Mar 28 01:31:17 localhost.localdomain systemd[1]: my-service.service: Deactivated successfully.
Mar 28 01:31:17 localhost.localdomain systemd[1]: Stopped my-service.service - My Custom Hello Service
```

#### 4. 부팅 시 자동 시작
위에서 `WantedBy=multi-user.target`를 .service 파일에 추가했기 때문에, `multi-user.target.wants`폴더에 추가한 .service파일이 연결된다.
```
byeonggyu@localhost:~> sudo systemctl enable my-service
Created symlink '/etc/systemd/system/multi-user.target.wants/my-service.service' → '/etc/systemd/system/my-service.service'.
```

## 프로세스 조회 및 제어(ps, top, kill, nice)
process는 program의 instance이다. systemd를 포함한 수많은 process가 리눅스에서 실행 중이고, 아래 command를 통해 제어할 수 있다.
- `ps`: process snapshot 조회
- `top`: 실시간 process resource 모니터링
- `kill`: process 종료 신호 전달
- `nice`: 초기 우선순위 설정

### Process 관리 실습
#### 1. CPU 부하가 많이 발생하는 process 실행
PID=5328의 yes process가 실행된다.
```
byeonggyu@localhost:~> yes > /dev/null &
[2] 5328
[1]   Killed                  yes > /dev/null
```

#### 2. `top`으로 process 모니터링
실시간으로 변하는 process 상태를 모니터링할 수 있다.  
총 process 수, 실행 중인 process 수는 물론, 각 process의 priority, CPU%, MEM%, Running time 등도 확인할 수 있다.
```
byeonggyu@localhost:~> top
top - 01:40:07 up  1:49,  3 users,  load average: 0.88, 0.52, 0.36
Tasks: 224 total,   2 running, 222 sleeping,   0 stopped,   0 zombie
%Cpu(s): 13.6 us, 36.7 sy,  0.0 ni, 49.8 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st
MiB Mem :   2698.4 total,    154.0 free,   1156.6 used,   1506.9 buff/cache
MiB Swap:   3084.0 total,   3084.0 free,      0.0 used.   1541.9 avail Mem

    PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND
   5328 byeongg+  20   0  226636   1740   1628 R 100.0   0.1   0:06.68 yes
      1 root      20   0   50796  41584   9896 S   0.0   1.5   0:03.18 systemd
      2 root      20   0       0      0      0 S   0.0   0.0   0:00.02 kthreadd

```
`ps` command를 통해서도 확인할 수 있다.
```
byeonggyu@localhost:~> ps aux | grep yes
0   0:37 yes
byeongg+    5330  0.0  0.0 227496  1956 pts/4    S+   01:40   0:00 grep --color=auto yes
```

#### 3. renice로 priority 조정
우선순위를 더 낮게 조정할 수 있다. -20~19 범위이며, 숫자가 클 수록 낮은 우선순위이다.
```
byeonggyu@localhost:~> renice -n 20 -p 5314
5314 (process ID) old priority 10, new priority 19
```

#### 4. kill로 process 종료
`kill -9 <pid>`로 process를 종료한다.
```
byeonggyu@localhost:~> kill -9 5328
```

## 작업 스케줄링(cron, at)
- `cron`: 반복적인 작업을 스케줄링할 때 사용한다.
    - 첫 5개 token은 각각 분, 시, 일, 월, 요일을 의미한다.
    - `* * * * *` -> 매 분마다
    - `0 3 * * *` -> 매일 새벽 3시
- `at`: 일회성 작업을 스케줄링할 때 사용한다.

### crontab 실습
#### 1. crontab 생성
매 분마다 log 파일에 현재 시각을 기록하는 cronjob을 생성한다.
```
byeonggyu@localhost:~> crontab -e

* * * * * echo "Checked at $(date)" >> /tmp/cron_test.log
```
`/tmp/crontab.lgFwdp` 형식의 임시 파일이 생성되고, 바로 지워진다.  

매 분 로그가 찍히는걸 확인할 수 있다.
```
byeonggyu@localhost:/tmp> vi cron_test.log

Checked at Sat Mar 28 01:54:01 AM KST 2026
Checked at Sat Mar 28 01:55:01 AM KST 2026
Checked at Sat Mar 28 01:56:01 AM KST 2026
Checked at Sat Mar 28 01:57:02 AM KST 2026
Checked at Sat Mar 28 01:58:02 AM KST 2026
```

#### 2. crontab 확인
`crontab -l`을 통해 작업을 확인할 수 있다.
```
byeonggyu@localhost:~> crontab -l
* * * * * echo "Checked at $(date)" >> /tmp/cron_test.log
```

#### 3. 리눅스에 존재하는 crontab 확인
Rocky Linux에는 hourly 돌고 있는 cronjob이 하나 있다.  
매 시 1분마다 돌고 있는 job이다.
```
byeonggyu@localhost:/etc> cd cron.
cron.d/       cron.daily/   cron.hourly/  cron.monthly/ cron.weekly/

byeonggyu@localhost:/etc/cron.d> vi 0hourly
# Run the hourly jobs
SHELL=/bin/bash
PATH=/sbin:/bin:/usr/sbin:/usr/bin
MAILTO=root
01 * * * * root run-parts /etc/cron.hourly
```

1시간마다 어떤 job을 실행하는지 확인해본다.
```
byeonggyu@localhost:/etc/cron.hourly> vi 0anacron
```
- `/usr/sbin/anacron -s`을 실행하기 전 몇가지 체크를 하는 script이다.
- anacron을 오늘 실행했으면 skip한다.
- 전원 연결인지 배터리인지 확인하고, 배터리이면서 실행 금지 flag가 있으면 skip한다.
```bash
#!/usr/bin/sh
# Check whether 0anacron was run today already
if test -r /var/spool/anacron/cron.daily; then
    day=`cat /var/spool/anacron/cron.daily`
fi
if [ `date +%Y%m%d` = "$day" ]; then
    exit 0
fi

# Check whether run on battery should be allowed
if test -r /etc/default/anacron; then
    . /etc/default/anacron
fi

if [ "$ANACRON_RUN_ON_BATTERY_POWER" != "yes" ]; then

    # Do not run jobs when on battery power
    online=1
    for psupply in /sys/class/power_supply/* ; do
        if [ `cat "$psupply/type" 2>/dev/null`x = Mainsx ] && [ -f "$psupply/online" ]; then
            if [ `cat "$psupply/online" 2>/dev/null`x = 1x ]; then
                online=1
                break
            else
                online=0
            fi
        fi
    done
    if [ $online = 0 ]; then
        exit 0
    fi

fi
/usr/sbin/anacron -s
```
- `anacron`(Anachronistic Cron)는 `cron`과 달리 컴퓨터가 꺼져서 놓친 작업을 실행하는 시기가 다르다.

| 구분 | Cron (crond) | Anacron (anacron) |
|------|------|---------|
| 작동 철학 | "정해진 시각에 무조건 실행!" | "주기가 지났으면 지금이라도 실행!" |
| 부재중 작업 | 그냥 건너뜀 (내일 다시 시도) | 부팅 후 즉시 실행 (밀린 숙제 완료) |
| 최소 주기 | 1분 단위까지 가능 | 1일 단위가 최소 (시간 단위 불가) |
| 주요 대상 | 24시간 켜져 있는 서버 | 노트북, 일반 PC, 자주 꺼지는 가상머신 |
| 설정 위치 | crontab -e, /etc/cron.d/ | /etc/anacrontab |

`/etc/anacrontab` 파일은 실행할 crontab 설정이 담겨있다.
```
byeonggyu@localhost:/etc> vi anacrontab
# /etc/anacrontab: configuration file for anacron

# See anacron(8) and anacrontab(5) for details.

SHELL=/bin/sh
PATH=/sbin:/bin:/usr/sbin:/usr/bin
MAILTO=root
# the maximal random delay added to the base delay of the jobs
RANDOM_DELAY=45
# the jobs will be started during the following hours only
START_HOURS_RANGE=3-22

#period in days   delay in minutes   job-identifier   command
1       5       cron.daily              nice run-parts /etc/cron.daily
7       25      cron.weekly             nice run-parts /etc/cron.weekly
@monthly 45     cron.monthly            nice run-parts /etc/cron.monthly
```

## cgroup 기반 자원 제어
cgroup(Control Groups)는 process group별로 CPU, Memory, Disk I/O 자원 사용량을 제한한다. 현재 Docker/k8s의 핵심 기술이기도 하다.  
`systemd(1)`은 이 cgroup을 쉽게 다룰 수 있다. 또한, service(unit) 설정에서도 이를 제한할 수 있다.

### 자원제어 실습 (w/o cgroup)

#### 1. service 파일 수정
앞서 만든 service 파일에 `MemoryLimit=50M CPUQuota=20%`을 추가한다.
```
byeonggyu@localhost:/etc/systemd/system> vi my-service.service

[Unit]
Description=My Custom Hello Service

[Service]
ExecStart=/usr/local/bin/my-script.sh
MemoryLimit=50M
CPUQuota=20%

[Install]
WantedBy=multi-user.target
```

또한, 부하를 많이 주기 위해 bash script도 무한 loop를 쉬지않고 돌도록 수정한다.
```
#!/bin/bash
while true;
do echo "Hello Rocky!" >> /var/log/my-service.log;
done
```

service file이 수정되었으므로 `daemon-reload`를 수행한다.
```
byeonggyu@localhost:~> sudo systemctl daemon-reload
byeonggyu@localhost:~> sudo systemctl restart my-service
```
CPU를 최대 20% 미만으로 사용함을 볼 수 있다.
```
top - 02:30:06 up  2:39,  6 users,  load average: 0.52, 0.40, 0.31
Tasks: 233 total,   2 running, 230 sleeping,   1 stopped,   0 zombie
%Cpu(s):  7.3 us,  6.5 sy,  0.0 ni, 85.9 id,  0.0 wa,  0.0 hi,  0.3 si,  0.0 st
MiB Mem :   2698.4 total,    108.3 free,   1172.9 used,   1539.0 buff/cache
MiB Swap:   3084.0 total,   3084.0 free,      0.0 used.   1525.6 avail Mem

    PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND
   6215 root      20   0  228296   3152   2908 R  19.5   0.1   0:23.01 my-script.sh
    999 root      20   0  531332   8368   6624 S   4.0   0.3   0:07.65 accounts-daemon
   2397 byeongg+  20   0 4031180 313888 116428 S   1.0  11.4   0:31.85 gnome-shell
```
