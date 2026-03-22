# Rocky Linux 스터디 — 3주차
## 파일 시스템 및 스토리지 관리

## 1. 디스크와 파티션 구조 이해

### 1-1. 전체 구조 한눈에

> 출처: [RHEL 10 Managing storage devices — PDF](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/pdf/managing_storage_devices/Red_Hat_Enterprise_Linux-10-Managing_storage_devices-en-US.pdf)

```
물리 디스크
  └── 파티션 테이블 (MBR 또는 GPT)
        ├── 파티션 1 (/dev/sda1)  → 파일시스템(XFS) → 마운트포인트(/boot)
        ├── 파티션 2 (/dev/sda2)  → LVM PV
        │     └── Volume Group (vg0)
        │           ├── Logical Volume (lv_root) → XFS → /
        │           ├── Logical Volume (lv_home) → XFS → /home
        │           └── Logical Volume (lv_swap) → swap
        └── 파티션 3 (/dev/sda3)  → XFS → /data
```

### 1-2. 디스크 장치 명명 규칙

```
/dev/sda    ← 첫 번째 SATA/SCSI/SSD 디스크
/dev/sdb    ← 두 번째 디스크
/dev/sdc    ← 세 번째 디스크

/dev/sda1   ← sda의 첫 번째 파티션
/dev/sda2   ← sda의 두 번째 파티션

/dev/nvme0n1    ← 첫 번째 NVMe 디스크
/dev/nvme0n1p1  ← NVMe 디스크의 첫 번째 파티션

/dev/vda    ← 가상머신(KVM) 환경의 디스크
```

### 1-3. 파티션 테이블 종류 — MBR vs GPT

> 출처: [RHEL 10 Disk partitions](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/managing_storage_devices/disk-partitions)

| 항목 | MBR | GPT |
|------|-----|-----|
| 최대 디스크 크기 | **2 TiB** | **8 ZiB** (512b 섹터 기준) |
| 최대 파티션 수 | 4개 (주 파티션) | **128개** (기본값) |
| 방식 | 구형 BIOS 기반 | 최신 UEFI 기반 |
| 파티션 식별 | 16진수 타입 코드 | GUID(전역 고유 식별자) |
| 안정성 | 낮음 (파티션 테이블 1개) | 높음 (백업 파티션 테이블 보유) |

```
2TB 이상 디스크 → 반드시 GPT 사용
현대 서버 환경  → 대부분 GPT 사용
```

#### MBR 파티션 구조 (2TB 이하 구형 환경)

MBR은 주(Primary) 파티션을 최대 4개까지만 만들 수 있다는 한계가 있다. 이를 극복하기 위해 **확장(Extended) 파티션** 개념이 등장했다.

```
MBR 파티션 구조:

/dev/sda1  Primary   (주 파티션)
/dev/sda2  Primary   (주 파티션)
/dev/sda3  Primary   (주 파티션)
/dev/sda4  Extended  (확장 파티션 — 컨테이너 역할)
  └─ /dev/sda5  Logical  (논리 파티션)
  └─ /dev/sda6  Logical  (논리 파티션)
  └─ /dev/sda7  Logical  (논리 파티션)
  ...
```

규칙:
- 주 파티션은 최대 4개
- 주 파티션 중 하나를 확장 파티션으로 만들면 그 안에 논리 파티션을 무제한 생성 가능
- RHEL 10 기준 하나의 디스크에 최대 60개 파티션 접근 가능

#### GPT 파티션 구조 (현대 환경 — 권장)

GPT는 GUID를 기반으로 하는 파티셔닝 방식으로 MBR의 한계를 해결한다. 기본적으로 128개의 주 파티션 생성을 지원하며, 파티션 테이블 공간을 더 할당하면 최대 파티션 수를 늘릴 수 있다.

```
GPT 파티션 구조:

/dev/sda1  EFI System Partition (ESP) — UEFI 부팅용
/dev/sda2  /boot 파티션
/dev/sda3  LVM PV 파티션
/dev/sda4  데이터 파티션
...최대 128개까지 주 파티션으로 생성 가능
```

### 1-4. 파티션이 마운트가 된다는 것의 의미

> 출처: [RHEL 10 Disk partitions](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/managing_storage_devices/disk-partitions)

RHEL에서 각 파티션은 하나의 파일과 디렉토리 집합을 지원하는 스토리지의 일부를 구성한다. 파티션을 마운트하면 해당 파티션의 스토리지가 마운트 포인트라는 특정 디렉토리부터 사용 가능해진다. 예를 들어 `/dev/sda5`가 `/usr/`에 마운트되면, `/usr/` 아래의 모든 파일과 디렉토리는 물리적으로 `/dev/sda5`에 존재한다.

```
마운트 전:
  /dev/sda2 → 그냥 블록 장치 (접근 불가)

마운트 후:
  /dev/sda2 → /home 디렉토리로 접근 가능
  /home/alice/  ← 실제로는 /dev/sda2에 저장된 데이터

마운트 = 블록 장치를 디렉토리 트리에 붙이는 것
```

---

## 2. 파티션 생성 및 관리

### 2-1. 디스크 상태 확인

```bash
# 블록 장치 목록 확인
lsblk
# NAME   MAJ:MIN RM  SIZE RO TYPE MOUNTPOINTS
# sda      8:0    0   50G  0 disk
# ├─sda1   8:1    0    1G  0 part /boot
# ├─sda2   8:2    0    1G  0 part [SWAP]
# └─sda3   8:3    0   48G  0 part /

# 파일시스템 정보 포함
lsblk -f
# NAME   FSTYPE FSVER LABEL UUID                                 MOUNTPOINTS
# sda
# ├─sda1 xfs          Boot  ea74bbec-536d-490c-b8d9-5b40bbd7545b /boot

# 디스크 상세 정보
fdisk -l /dev/sda

# 파티션 테이블 확인 (parted)
parted /dev/sda print
```

### 2-2. parted로 파티션 생성

> 출처: [RHEL 10 Getting started with partitions](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/managing_storage_devices/getting-started-with-partitions)

`parted`는 RHEL 10 공식 권장 도구다. GPT/MBR 모두 지원하며 2TB 이상 디스크도 처리 가능하다.

```bash
# parted 실행
parted /dev/sdb

# parted 내부 명령어
(parted) print                    # 현재 파티션 테이블 확인
(parted) mklabel gpt              # GPT 파티션 테이블 생성 (데이터 삭제!)
(parted) mklabel msdos            # MBR 파티션 테이블 생성

# 파티션 생성
(parted) mkpart primary xfs 1MiB 10GiB
#                         │       │    └── 끝 위치
#                         │       └── 시작 위치
#                         └── 파일시스템 타입 힌트 (실제 포맷은 별도로)

(parted) mkpart primary linux-swap 10GiB 12GiB   # 스왑 파티션
(parted) mkpart primary xfs 12GiB 100%            # 나머지 전체

# 플래그 설정
(parted) set 1 boot on     # 부팅 플래그
(parted) set 2 lvm on      # LVM 플래그
(parted) set 3 swap on     # 스왑 플래그

(parted) print             # 결과 확인
(parted) quit
```

> **⚠️ 주의:** `mklabel` 명령은 즉시 적용되며 기존 데이터를 삭제한다. `parted`는 변경사항을 즉시 적용하므로 별도의 저장 명령이 없다.

#### parted 출력 예시

```
(parted) print
Model: ATA SAMSUNG MZNLN256 (scsi)
Disk /dev/sda: 256GB
Sector size (logical/physical): 512B/512B
Partition Table: gpt
Disk Flags:

Number  Start   End     Size    File system  Name     Flags
 1      1049kB  269MB   268MB   xfs                   boot
 2      269MB   34.6GB  34.4GB               lvm      lvm
 3      34.6GB  256GB   221GB   xfs
```

### 2-3. fdisk로 파티션 생성 (MBR 환경)

```bash
fdisk /dev/sdb

# fdisk 내부 명령어
m    # 도움말
p    # 현재 파티션 테이블 출력
n    # 새 파티션 생성
  p  # 주 파티션
  1  # 파티션 번호
  (Enter)  # 기본 시작 섹터
  +10G     # 크기 지정
d    # 파티션 삭제
t    # 파티션 타입 변경
  8e # Linux LVM 타입
w    # 변경사항 저장 (실제 적용)
q    # 저장 없이 종료
```

> **fdisk vs parted:**
> - `fdisk` → MBR 전용, 대화형, `w`로 저장해야 적용
> - `parted` → MBR/GPT 모두 지원, 즉시 적용, RHEL 10 권장

### 2-4. 파티션 변경 커널 반영

파티션 생성 후 커널이 새 파티션을 인식하지 못할 때 사용한다.

```bash
# 커널에 파티션 테이블 변경 알림
partprobe /dev/sdb

# 또는
udevadm settle
```

---

## 3. LVM — 논리 볼륨 관리자

### 3-1. LVM이 필요한 이유

> 출처: [RHEL 10 Configuring and managing logical volumes — Overview](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/configuring_and_managing_logical_volumes)

```
파티션 방식의 한계:
/home 파티션이 꽉 찼다
→ 파티션 크기를 늘리려면 서비스 중단 + 포맷 + 재설치
→ 물리적으로 인접한 공간이 없으면 불가능

LVM 방식:
/home이 꽉 찼다
→ 디스크 추가 or 여유 공간에서
→ lvextend 명령 하나로 온라인 확장
→ 서비스 중단 없음
```

LVM은 물리 스토리지 위에 추상화 레이어를 생성하여 논리 스토리지 볼륨 생성에 더 많은 유연성을 제공한다. 하드웨어 스토리지 설정이 소프트웨어로부터 숨겨지기 때문에 애플리케이션을 중단하거나 파일시스템을 언마운트하지 않고도 크기 조정 및 이동이 가능하다.

### 3-2. LVM 3계층 구조

> 출처: [RHEL 10 Configuring and managing logical volumes — Overview](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/configuring_and_managing_logical_volumes)

```
┌──────────────────────────────────────────┐
│         Logical Volume (LV)              │  ← 실제 사용하는 볼륨
│  /dev/vg0/lv_root   /dev/vg0/lv_home    │     파일시스템 생성 + 마운트
└──────────────────┬───────────────────────┘
                   │ 포함
┌──────────────────┴───────────────────────┐
│         Volume Group (VG)                │  ← 디스크 공간 풀
│              vg0                         │     PV들을 묶어 하나의 공간으로
└──────────────────┬───────────────────────┘
                   │ 구성
┌──────────────────┴───────────────────────┐
│         Physical Volume (PV)             │  ← LVM용으로 초기화된 디스크/파티션
│  /dev/sda2    /dev/sdb1    /dev/sdc      │
└──────────────────────────────────────────┘
```

물리 볼륨(PV)은 LVM용으로 지정된 파티션이나 전체 디스크이고, 볼륨 그룹(VG)은 물리 볼륨의 모음으로 논리 볼륨을 할당할 디스크 공간 풀을 만들며, 논리 볼륨(LV)은 실제 사용 가능한 스토리지 장치를 나타낸다.

### 3-3. PE (Physical Extent) — LVM의 최소 할당 단위

```
VG를 만들 때 디스크 공간을 일정한 크기의 블록(PE)으로 나눈다
기본 PE 크기: 4MiB

예: 100GB 디스크
→ 100GB / 4MiB = 약 25,600개의 PE
→ LV 생성 시 PE 단위로 할당

PE 크기를 크게 하면: 큰 LV 생성에 유리
PE 크기를 작게 하면: 공간 낭비 최소화
```

### 3-4. LVM 구성 실습

#### 1단계: Physical Volume (PV) 생성

```bash
# 파티션을 PV로 초기화
pvcreate /dev/sdb1
# Physical volume "/dev/sdb1" successfully created.

# 디스크 전체를 PV로 초기화
pvcreate /dev/sdc

# PV 목록 확인
pvs
# PV         VG  Fmt  Attr PSize   PFree
# /dev/sdb1  vg0 lvm2 a--  <99.00g <99.00g

pvdisplay /dev/sdb1    # 상세 정보
```

#### 2단계: Volume Group (VG) 생성

```bash
# VG 생성 (PV 여러 개 묶기 가능)
vgcreate vg0 /dev/sdb1
vgcreate vg0 /dev/sdb1 /dev/sdc    # 여러 PV 동시에

# PE 크기 지정 (기본 4MiB)
vgcreate -s 8MiB vg0 /dev/sdb1

# VG 목록 확인
vgs
# VG  #PV #LV #SN Attr   VSize   VFree
# vg0   1   0   0 wz--n- <99.00g <99.00g

vgdisplay vg0    # 상세 정보

# 나중에 VG에 PV 추가
vgextend vg0 /dev/sdd1
```

#### 3단계: Logical Volume (LV) 생성

```bash
# 크기 지정으로 LV 생성
lvcreate -L 20G -n lv_root vg0
#          │       │         └── VG 이름
#          │       └── LV 이름
#          └── 크기

# VG 여유 공간 비율로 생성
lvcreate -l 50%FREE -n lv_home vg0
lvcreate -l 100%FREE -n lv_data vg0    # 남은 공간 전부

# LV 목록 확인
lvs
# LV      VG  Attr       LSize
# lv_root vg0 -wi-a----- 20.00g
# lv_home vg0 -wi-a----- 10.00g

lvdisplay /dev/vg0/lv_root    # 상세 정보
```

#### 4단계: LV에 파일시스템 생성 후 마운트

```bash
# XFS 파일시스템 생성
mkfs.xfs /dev/vg0/lv_root
mkfs.xfs /dev/vg0/lv_home

# 마운트
mount /dev/vg0/lv_root /mnt/root
```

### 3-5. LV 크기 조정

> 출처: [RHEL 10 Configuring and managing logical volumes — Basic LVM](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/configuring_and_managing_logical_volumes/basic-logical-volume-management)

```bash
# LV 확장 (온라인 가능)
lvextend -L +10G /dev/vg0/lv_home        # 10GB 추가
lvextend -l +100%FREE /dev/vg0/lv_home   # 여유 공간 전부 추가

# LV 확장 후 파일시스템도 확장 (XFS)
xfs_growfs /home
# XFS는 마운트된 상태에서 확장 가능

# LV + 파일시스템 한 번에 확장 (ext4만 가능)
lvextend -L +10G -r /dev/vg0/lv_data
#                 └── --resizefs: 파일시스템 자동 리사이즈

# LV 축소 (XFS는 축소 불가!)
# ext4만 가능, 반드시 언마운트 후 진행
umount /dev/vg0/lv_data
e2fsck -f /dev/vg0/lv_data          # 파일시스템 검사
resize2fs /dev/vg0/lv_data 5G       # 파일시스템 먼저 축소
lvreduce -L 5G /dev/vg0/lv_data     # LV 축소
```

> **⚠️ XFS는 축소(shrink) 불가:**  
> XFS 파일시스템은 확장만 가능하고 축소는 지원하지 않는다. 처음 LV 크기를 여유있게 잡고 시작하는 것이 중요하다.

### 3-6. LVM 상태 확인 명령어 요약

```bash
pvs          # PV 요약
pvdisplay    # PV 상세
vgs          # VG 요약
vgdisplay    # VG 상세
lvs          # LV 요약
lvdisplay    # LV 상세
lsblk        # 전체 블록 장치 트리
```

---

## 4. 파일시스템 생성 및 점검

### 4-1. XFS — Rocky Linux 10 기본 파일시스템

> 출처: [RHEL 10 Getting started with XFS](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/managing_file_systems/getting-started-with-xfs)

XFS는 확장성이 뛰어나고 고성능이며 안정적인 64비트 저널링 파일시스템으로 단일 호스트에서 매우 큰 파일과 파일시스템을 지원한다. RHEL 10의 기본 파일시스템이며, 1990년대 초 SGI가 개발했고 대형 서버와 스토리지 어레이에서의 긴 운용 이력을 가지고 있다.

XFS의 핵심 특징:
- **저널링:** 시스템 크래시 후 파일시스템 무결성 보장
- **메타데이터 저널링:** 재시작 시 재실행 가능한 파일시스템 작업 기록
- **확장 속성(xattr):** 파일당 여러 이름-값 쌍 저장 가능
- **쿼터 저널링:** 크래시 후 긴 쿼터 일관성 검사 불필요

```bash
# XFS 파일시스템 생성
mkfs.xfs /dev/sdb1
mkfs.xfs /dev/vg0/lv_data

# 레이블 지정
mkfs.xfs -L "mydata" /dev/sdb1

# 기존 파일시스템 덮어쓰기
mkfs.xfs -f /dev/sdb1

# 파일시스템 정보 확인
xfs_info /dev/sdb1

# XFS 파일시스템 점검 (반드시 언마운트 상태에서)
xfs_repair /dev/sdb1
```

> **XFS 특이사항:**
> - 마운트된 상태에서 `xfs_repair` 실행 불가 (언마운트 필수)
> - 복구 불가능한 메타데이터 오류 발생 시 파일시스템을 종료하고 EFSCORRUPTED 오류 반환
> - **축소(shrink) 불가, 확장(grow)만 가능**

### 4-2. ext4 — 레거시 파일시스템

> 출처: [RHEL 10 Getting started with an ext4 file system](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/managing_file_systems/getting-started-with-an-ext4-file-system)

ext4는 ext3의 확장 가능한 버전으로, RHEL 10에서 개별 파일 최대 16TB, 파일시스템 최대 50TB를 지원한다.

```bash
# ext4 파일시스템 생성
mkfs.ext4 /dev/sdb2

# 레이블 지정
mkfs.ext4 -L "backup" /dev/sdb2

# 파일시스템 점검 (언마운트 상태)
fsck.ext4 /dev/sdb2
e2fsck -f /dev/sdb2

# 파일시스템 정보 확인
tune2fs -l /dev/sdb2

# ext4 크기 조정 (확장/축소 모두 가능)
resize2fs /dev/sdb2 20G    # 축소
resize2fs /dev/sdb2        # 최대로 확장
```

### 4-3. XFS vs ext4 비교

> 출처: [RHEL 10 Managing file systems](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html-single/managing_file_systems/index)

| 항목 | XFS | ext4 |
|------|-----|------|
| RHEL 10 기본 | ✅ | ❌ |
| 최대 파일 크기 | 8 EiB | 16 TiB |
| 최대 파일시스템 | 8 EiB | 50 TiB |
| 파일시스템 축소 | **불가** | 가능 |
| 마운트 중 확장 | 가능 | 가능 |
| 점검 도구 | xfs_repair | e2fsck |
| 주요 강점 | 대용량, 고성능, 병렬 I/O | 소용량 파일 다수, 축소 필요 시 |
| 메타데이터 오류 처리 | 파일시스템 종료 | 계속 진행 (기본값) |

XFS는 규모가 작은 시스템에서도 비교적 잘 작동하지만 확장성과 대용량 데이터셋에 더 집중되어 있다.

### 4-4. 파일시스템 생성 전체 흐름

```
물리 디스크
    ↓ parted/fdisk로 파티션 생성
파티션 (/dev/sdb1)
    ↓ mkfs.xfs 또는 mkfs.ext4
파일시스템 (포맷 완료)
    ↓ mount 명령
마운트 포인트 (/data) → 사용 가능
    ↓ /etc/fstab 등록
재부팅 후에도 자동 마운트
```

---

## 5. 마운트 · 자동 마운트 설정

### 5-1. 마운트 기본 개념

> 출처: [RHEL 10 Mounting file systems](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/managing_file_systems/mounting-file-systems)

```bash
# 기본 마운트
mount /dev/sdb1 /data

# 파일시스템 타입 명시
mount -t xfs /dev/sdb1 /data

# 마운트 옵션 지정
mount -o ro /dev/sdb1 /data          # 읽기 전용
mount -o noexec /dev/sdb1 /data      # 실행 불가
mount -o nosuid /dev/sdb1 /data      # SetUID 비트 무효화

# 마운트 확인
mount | grep /data
findmnt /data

# 언마운트
umount /data
umount /dev/sdb1

# 강제 언마운트 (사용 중인 경우)
umount -l /data     # lazy unmount (프로세스 종료 후 처리)
umount -f /data     # force unmount
```

### 5-2. 현재 마운트 상태 확인

`findmnt` 유틸리티로 현재 마운트된 모든 파일시스템을 커맨드라인에서 목록으로 확인할 수 있다.

```bash
# 트리 형태로 마운트 현황 확인
findmnt
# TARGET                     SOURCE                                   FSTYPE  OPTIONS
# /                          /dev/mapper/rl-root                      xfs     rw,relatime
# ├─/boot                    /dev/sda1                                xfs     rw,relatime
# └─/home                    /dev/mapper/rl-home                      xfs     rw,relatime

# 특정 마운트 포인트 확인
findmnt /boot

# 디스크 사용량 확인
df -hT
# Filesystem            Type  Size  Used Avail Use% Mounted on
# /dev/mapper/rl-root   xfs    47G  3.2G   44G   7% /
# /dev/sda1             xfs   960M  300M  661M  32% /boot
```

### 5-3. /etc/fstab — 자동 마운트 설정

> 출처: [RHEL 10 Mounting file systems](https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/managing_file_systems/mounting-file-systems)

`/etc/fstab` 파일에는 장치 이름, 마운트될 디렉토리, 파일시스템 타입, 마운트 옵션 목록이 저장된다. 마운트 명령에 정보 일부가 없을 경우 `mount` 유틸리티는 `/etc/fstab`을 읽어 해당 파일시스템이 있는지 확인한다.

#### /etc/fstab 형식

```
장치          마운트포인트  FS타입  옵션      dump  fsck순서
UUID=xxxx-xx  /            xfs    defaults  0     0
```

필드별 의미:

```
1. 장치 식별자
   UUID=ea74bbec-536d-490c-b8d9-5b40bbd7545b  ← UUID (권장)
   /dev/sda1                                   ← 장치 경로 (비권장, 순서 바뀔 수 있음)
   LABEL=mydata                                ← 레이블
   /dev/vg0/lv_home                            ← LVM 경로

2. 마운트 포인트
   /boot, /home, /data 등

3. 파일시스템 타입
   xfs, ext4, swap, tmpfs, nfs 등

4. 마운트 옵션 (쉼표로 구분)
   defaults    = rw,suid,dev,exec,auto,nouser,async
   ro          = 읽기 전용
   noexec      = 실행 불가
   nosuid      = SetUID 무효화
   noatime     = 접근 시간 기록 안 함 (성능 향상)
   nofail      = 마운트 실패해도 부팅 계속

5. dump 백업 여부
   0 = 백업 안 함 (대부분 0)
   1 = dump 유틸리티로 백업

6. fsck 검사 순서
   0 = 검사 안 함
   1 = 루트 파티션 (/ 만 1)
   2 = 나머지 파티션 (순서대로 검사)
```

#### /etc/fstab 실제 예시

```bash
cat /etc/fstab

# /etc/fstab
# Created by anaconda
#
# Accessible filesystems, by reference, are maintained under '/dev/disk/'.
# See man pages fstab(5), findfs(8), mount(8) and/or blkid(8) for more info.
#
UUID=abc12345-... /                       xfs     defaults        0 0
UUID=def67890-... /boot                   xfs     defaults        0 0
UUID=ghi11111-... /home                   xfs     defaults        0 0
UUID=jkl22222-... none                    swap    defaults        0 0
/dev/vg0/lv_data  /data                   xfs     defaults        0 0
tmpfs             /tmp                    tmpfs   defaults        0 0
```

#### UUID 확인 방법

```bash
# UUID 확인
blkid /dev/sdb1
# /dev/sdb1: UUID="05e99ec8-def1-4a5e-8a9d-5945339ceb2a" TYPE="xfs"

lsblk -f
# NAME   FSTYPE LABEL UUID                                 MOUNTPOINTS
# sdb1   xfs          05e99ec8-def1-4a5e-8a9d-5945339ceb2a
```

> **UUID를 쓰는 이유:**  
> `/dev/sda`, `/dev/sdb` 같은 장치 이름은 디스크를 추가/제거하면 바뀔 수 있다. UUID는 파일시스템에 고정된 고유 식별자이므로 장치 순서가 바뀌어도 항상 올바른 파티션을 찾을 수 있다.

#### fstab 적용 및 검증

```bash
# fstab 수정 후 마운트 테스트 (재부팅 없이 확인)
mount -a
# 오류 없으면 fstab 설정 정상

# fstab 문법 검사
mount --fake -av    # 실제 마운트 없이 검사만

# systemd로 검증
systemctl daemon-reload
systemd-analyze verify
```

> **⚠️ fstab 오류 주의:**  
> `/etc/fstab`에 잘못된 항목이 있으면 부팅이 실패할 수 있다. 수정 후 반드시 `mount -a`로 검증하고, 특히 `nofail` 옵션은 비중요 디스크에 사용을 권장한다.

### 5-4. 주요 마운트 옵션 정리

```bash
# 읽기 전용으로 마운트
mount -o ro /dev/sdb1 /data

# 성능 최적화 옵션 (데이터 손실 위험 있음)
mount -o noatime /dev/sdb1 /data    # 접근 시간 업데이트 안 함

# 보안 강화 옵션
mount -o noexec,nosuid,nodev /dev/sdb1 /data

# 재마운트 (언마운트 없이 옵션 변경)
mount -o remount,rw /data           # 읽기 전용 → 읽기쓰기로 변경
```

---

## 6. 전체 실습 시나리오

새 디스크 `/dev/sdb` 추가 후 LVM으로 구성하고 자동 마운트까지 설정하는 전체 흐름이다.

```bash
# 1. 디스크 확인
lsblk

# 2. 파티션 생성 (GPT)
parted /dev/sdb mklabel gpt
parted /dev/sdb mkpart primary 1MiB 100%
parted /dev/sdb set 1 lvm on
partprobe /dev/sdb

# 3. LVM 구성
pvcreate /dev/sdb1
vgcreate vgdata /dev/sdb1
lvcreate -L 20G -n lv_app vgdata
lvcreate -l 100%FREE -n lv_log vgdata

# 4. 파일시스템 생성
mkfs.xfs /dev/vgdata/lv_app
mkfs.xfs /dev/vgdata/lv_log

# 5. 마운트 포인트 생성 및 임시 마운트
mkdir -p /app /var/log/app
mount /dev/vgdata/lv_app /app
mount /dev/vgdata/lv_log /var/log/app

# 6. UUID 확인
blkid /dev/vgdata/lv_app
blkid /dev/vgdata/lv_log

# 7. /etc/fstab 등록 (자동 마운트)
echo "/dev/vgdata/lv_app  /app          xfs  defaults  0 0" >> /etc/fstab
echo "/dev/vgdata/lv_log  /var/log/app  xfs  defaults  0 0" >> /etc/fstab

# 8. fstab 검증
mount -a

# 9. 상태 확인
lsblk -f
df -hT
findmnt
```

---

## 7. 빠른 참조 치트시트

```bash
# ── 디스크/파티션 확인 ────────────────────────
lsblk                      # 블록 장치 목록
lsblk -f                   # 파일시스템 정보 포함
fdisk -l /dev/sda          # 파티션 테이블 상세
parted /dev/sda print      # parted로 파티션 확인
blkid /dev/sda1            # UUID 확인

# ── 파티션 생성 ───────────────────────────────
parted /dev/sdb mklabel gpt                    # GPT 테이블 생성
parted /dev/sdb mkpart primary xfs 1MiB 100%   # 파티션 생성
partprobe /dev/sdb                             # 커널 반영

# ── LVM ──────────────────────────────────────
pvcreate /dev/sdb1                   # PV 생성
vgcreate vg0 /dev/sdb1               # VG 생성
lvcreate -L 20G -n lv0 vg0          # LV 생성
lvextend -L +10G /dev/vg0/lv0       # LV 확장
xfs_growfs /mountpoint               # XFS 파일시스템 확장
pvs / vgs / lvs                      # 상태 확인

# ── 파일시스템 ───────────────────────────────
mkfs.xfs /dev/sdb1                   # XFS 생성
mkfs.ext4 /dev/sdb1                  # ext4 생성
xfs_repair /dev/sdb1                 # XFS 점검 (언마운트 필수)
e2fsck -f /dev/sdb1                  # ext4 점검 (언마운트 필수)
xfs_info /dev/sdb1                   # XFS 정보
tune2fs -l /dev/sdb1                 # ext4 정보

# ── 마운트 ───────────────────────────────────
mount /dev/sdb1 /data                # 마운트
mount -o ro /dev/sdb1 /data          # 읽기 전용 마운트
umount /data                         # 언마운트
mount -a                             # fstab 전체 마운트 (검증용)
findmnt                              # 마운트 현황 트리
df -hT                               # 디스크 사용량
```

---

## 📚 주요 출처

| 문서 | URL |
|------|-----|
| RHEL 10 Managing storage devices | https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/managing_storage_devices |
| RHEL 10 Disk partitions | https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/managing_storage_devices/disk-partitions |
| RHEL 10 Getting started with partitions | https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/managing_storage_devices/getting-started-with-partitions |
| RHEL 10 Configuring and managing logical volumes | https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/configuring_and_managing_logical_volumes |
| RHEL 10 Managing file systems | https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html-single/managing_file_systems/index |
| RHEL 10 Getting started with XFS | https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/managing_file_systems/getting-started-with-xfs |
| RHEL 10 Creating an XFS file system | https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/managing_file_systems/creating-an-xfs-file-system |
| RHEL 10 Mounting file systems | https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/10/html/managing_file_systems/mounting-file-systems |

---

*문서 작성 기준: Rocky Linux 10 / RHEL 10 공식 문서 (2025년 기준)*