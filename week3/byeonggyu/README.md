# Week3. 파일 시스템 및 스토리지 관리

> 디스크와 파티션 구조 이해, 파티션 생성 및 관리, LVM의 개념과 볼륨 구성, 파일 시스템 생성 및 점검, 마운트·자동 마운트 설정(/etc/fstab) 실습

## Disk와 Partition
- Disk 장치명: 리눅스는 Disk를 파일로 인식한다.
    - `/dev/sda`, `/dev/sdb` 와 같이 이름이 붙는다.
- Partition: 하나의 물리 Disk를 논리적으로 나눈 구역이다.
    - MBR: 최대 2TB, Partition 4개 제한
    - GPT: 2TB 이상 가능, Partition 제한 거의 없음 <- 요즘 방식

### `lsblk` - Disk/Partition 관계와 Mount
아래와 같이 트리 형태로 리눅스 시스템 저장소의 구조를 볼 수 있다.
```
byeonggyu@localhost:~> lsblk
NAME        MAJ:MIN RM  SIZE RO TYPE MOUNTPOINTS
sr0          11:0    1 1024M  0 rom
vda         252:0    0   30G  0 disk
├─vda1      252:1    0  600M  0 part /boot/efi
├─vda2      252:2    0    1G  0 part /boot
└─vda3      252:3    0 28.4G  0 part
  ├─rl-root 253:0    0 26.4G  0 lvm  /
  └─rl-swap 253:1    0    2G  0 lvm  [SWAP]
```
- `NAME`: 장치의 이름, 어떤 Partition이 어떤 Disk에 속해있는지에 대한 정보
- `MAJ:MIN`: 장치를 식별하는 번호, `MAJ`는 장치 유형, `MIN`는 유형 내 순번
    - `vda`는 252이고, 가상 Disk(VirtIO block)을 뜻하는 번호
    - `rl-root`, `rl-swap`은 253이고 Device Mapper를 뜻하는 번호
- `RM`: 제거 가능한지를 의미하는 번호 (1이면 가능, 0이면 불가능)
    - `sr0`(ROM, Read-Only Memory)은 제거 가능함을 의미
- `SIZE`: 장치의 전체 용량
- `RO`: 읽기 전용인지의 여부 (1이면 읽기만 가능, 0이면 읽기/쓰기 모두 가능)
- `TYPE`: 장치의 종류 (disk/part/lvm/rom)
- `MOUNTPOINT`: 해당 장치가 파일시스템의 어느 경로에 연결되어 있는지

- `vda`(Disk)는 Virtual Disk A의 약자로, 가상화 환경에서 사용하는 가상디스크이다. (물리 서버라면 `sda`로 표시)
    - 30GiB Disk가 3개의 Partition으로 구분
    - `vda1(600M)`: `/boot/efi`에 부팅용 partition
    - `vda2(1G)`: `/boot`에 커널 이미지 저장용 partition
    - `vda3(28.4G)`: 실제 운영체제와 데이터가 들어가는 partition
        - `rl-root`: 리눅스 최상위 경로 `/`, 모든 리눅스 설정과 파일이 포함
        - `rl-swap`: 메모리(RAM)가 부족할 때 Disk 일부를 메모리처럼 사용하는 Swap 공간

### `parted` - Disk의 물리적 정보와 규격
```
byeonggyu@localhost:~> sudo parted -l
Model: Virtio Block Device (virtblk)
Disk /dev/vda: 32.2GB
Sector size (logical/physical): 512B/512B
Partition Table: gpt
Disk Flags:

Number  Start   End     Size    File system  Name                  Flags
 1      1049kB  630MB   629MB   fat32        EFI System Partition  boot, esp
 2      630MB   1704MB  1074MB  xfs                                bls_boot
 3      1704MB  32.2GB  30.5GB                                     lvm
```
- Disk Metadata
    - Model: Virtio Block Device (virtblk)
        - 가상화 환경 위에서 동작함을 의미, `virtio`를 통해 Disk에 접근
    - Disk /dev/vda: 32.2GB
        - Disk의 전체 용량, 30GiB = 32.2GB
    - Sector size (logical/physical): 512B/512B
        - Disk에서 데이터를 저장하는 최소 단위인 Sector 크기
    - Partition Table: gpt
        - GPT(GUID Partition Table) 방식을 사용해, 2TB 이상의 대용량 Disk 지원, Partition 128개까지 생성 가능
- Partition 분석
    - `vda1` (FAT32, EFI System Partition)
    - `vda2` (XFS Partition)
        - `bls_boot`의 의미는, Boot Loader Specification을 의미, Rocky Lnux 8/9부터 표준
    - `vda3` (LVM Partition)
        - File system이 없는 이유는, LVM이라는 논리적인 layer를 쌓아올렸기 때문, 실제 File system은 `rl-root`에 존재

즉, 리눅스 시스템 부팅은 아래 흐름으로 진행
1. 1번 Partition(EFI)에 부팅 정보 저장
2. 2번 Partition(XFS)에서 리눅스 커널 실행
3. 3번 Partition(LVM)안에서 `rl-root`를 찾아 Mount

## Partition 생성 및 관리

### Partition 생성 실습 (loop 생성 -> Partition 생성)
1. 1GB 크기의 `test_disk.img` 파일 생성
```
byeonggyu@localhost:~> dd if=/dev/zero of=test_disk.img bs=1M count=1024
1024+0 records in
1024+0 records out
1073741824 bytes (1.1 GB, 1.0 GiB) copied, 0.162726 s, 6.6 GB/s

byeonggyu@localhost:~> ll -tr test_disk.img
-rw-r--r--. 1 byeonggyu byeonggyu 1073741824 Mar 23 02:03 test_disk.img
```

2. 생성한 파일을 loop 장치 (가상 Disk)로 연결 -> `loop0` 및 `/dev/loop0` 생성
```
byeonggyu@localhost:~> sudo losetup -fP test_disk.img

byeonggyu@localhost:~> lsblk
NAME        MAJ:MIN RM  SIZE RO TYPE MOUNTPOINTS
loop0         7:0    0    1G  0 loop
sr0          11:0    1 1024M  0 rom
vda         252:0    0   30G  0 disk
├─vda1      252:1    0  600M  0 part /boot/efi
├─vda2      252:2    0    1G  0 part /boot
└─vda3      252:3    0 28.4G  0 part
  ├─rl-root 253:0    0 26.4G  0 lvm  /
  └─rl-swap 253:1    0    2G  0 lvm  [SWAP]

byeonggyu@localhost:/dev> ll -tr | grep loop0
brw-rw----.  1 root      disk      7,   0 Mar 23 02:04 loop0
```

3. `fdisk`를 통한 Partition 생성 (500M)
```
byeonggyu@localhost:~> sudo fdisk /dev/loop0

Welcome to fdisk (util-linux 2.40.2).
Changes will remain in memory only, until you decide to write them.
Be careful before using the write command.

Device does not contain a recognized partition table.
Created a new DOS (MBR) disklabel with disk identifier 0x9b64e2aa.

Command (m for help): n
Partition type
   p   primary (0 primary, 0 extended, 4 free)
   e   extended (container for logical partitions)
Select (default p): p
Partition number (1-4, default 1): 1
First sector (2048-2097151, default 2048):
Last sector, +/-sectors or +/-size{K,M,G,T,P} (2048-2097151, default 2097151): +500M

Created a new partition 1 of type 'Linux' and of size 500 MiB.

Command (m for help): p
Disk /dev/loop0: 1 GiB, 1073741824 bytes, 2097152 sectors
Units: sectors of 1 * 512 = 512 bytes
Sector size (logical/physical): 512 bytes / 512 bytes
I/O size (minimum/optimal): 512 bytes / 512 bytes
Disklabel type: dos
Disk identifier: 0x9b64e2aa

Device       Boot Start     End Sectors  Size Id Type
/dev/loop0p1       2048 1026047 1024000  500M 83 Linux

Command (m for help): w
The partition table has been altered.
Calling ioctl() to re-read partition table.
Syncing disks.
```

4. Partition 생성 확인 (`loop0p1`)
```
byeonggyu@localhost:~> lsblk
NAME        MAJ:MIN RM  SIZE RO TYPE MOUNTPOINTS
loop0         7:0    0    1G  0 loop
└─loop0p1   259:0    0  500M  0 part
sr0          11:0    1 1024M  0 rom
vda         252:0    0   30G  0 disk
├─vda1      252:1    0  600M  0 part /boot/efi
├─vda2      252:2    0    1G  0 part /boot
└─vda3      252:3    0 28.4G  0 part
  ├─rl-root 253:0    0 26.4G  0 lvm  /
  └─rl-swap 253:1    0    2G  0 lvm  [SWAP]
```

5. `mkfs`를 통한 XFS File system 포맷
```
byeonggyu@localhost:~> sudo mkfs.xfs /dev/loop0p1
meta-data=/dev/loop0p1           isize=512    agcount=4, agsize=32000 blks
         =                       sectsz=512   attr=2, projid32bit=1
         =                       crc=1        finobt=1, sparse=1, rmapbt=1
         =                       reflink=1    bigtime=1 inobtcount=1 nrext64=1
         =                       exchange=0
data     =                       bsize=4096   blocks=128000, imaxpct=25
         =                       sunit=0      swidth=0 blks
naming   =version 2              bsize=4096   ascii-ci=0, ftype=1, parent=0
log      =internal log           bsize=4096   blocks=16384, version=2
         =                       sectsz=512   sunit=0 blks, lazy-count=1
realtime =none                   extsz=4096   blocks=0, rtextents=0
Discarding blocks...Done.
```

6. Mount할 디렉토리 생성 및 Mount
```
byeonggyu@localhost:~> sudo mkdir /mnt/test_partition
byeonggyu@localhost:~> sudo mount /dev/loop0p1 /mnt/test_partition

byeonggyu@localhost:/mnt/test_partition> df -h /mnt/test_partition
Filesystem      Size  Used Avail Use% Mounted on
/dev/loop0p1    436M   34M  403M   8% /mnt/test_partition
```

## LVM(Logical Volume Manager)의 개념과 Volume 구성
LVM은 물리적인 Hard Disk와 파일 시스템 사이에 논리적인 Layer를 하나 더 두어, 유연한 리눅스 저장소 관리가 가능하다.  
LVM을 통해 Partition을 병합, 용량 조절, 스냅샷을 통한 데이터 복구하는 연산이 가능해진다.

### LVM의 Layer
- PV (Physical Volume): 실제 Disk나 Partition을 LVM에서 쓸 수 있는 상태
- VG (Volume Group): 여러개의 PV를 하나로 합친 그룹
- LV (Logical Volume): VG에서 필요한 만큼 실제 Partition으로 쓰는 공간

### LVM 실습 (loop 2개 생성 -> 2개 Disk를 병합 -> 용량 증가)

1. 가상 Disk 2개 생성 (각 500M)
```
byeonggyu@localhost:~> dd if=/dev/zero of=disk1.img bs=1M count=500; dd if=/dev/zero of=disk2.img bs=1M count=500
500+0 records in
500+0 records out
524288000 bytes (524 MB, 500 MiB) copied, 0.167074 s, 3.1 GB/s
500+0 records in
500+0 records out
524288000 bytes (524 MB, 500 MiB) copied, 0.0610754 s, 8.6 GB/s
byeonggyu@localhost:~> sudo losetup -fP disk1.img; sudo losetup -fP disk2.img

byeonggyu@localhost:~> lsblk
NAME        MAJ:MIN RM  SIZE RO TYPE MOUNTPOINTS
loop0         7:0    0  500M  0 loop
loop1         7:1    0  500M  0 loop
sr0          11:0    1 1024M  0 rom
vda         252:0    0   30G  0 disk
├─vda1      252:1    0  600M  0 part /boot/efi
├─vda2      252:2    0    1G  0 part /boot
└─vda3      252:3    0 28.4G  0 part
  ├─rl-root 253:0    0 26.4G  0 lvm  /
  └─rl-swap 253:1    0    2G  0 lvm  [SWAP]
```

2. LVM 체인 생성 (`pvcreate` -> `vgcreate` -> `lvcreate`)
```
byeonggyu@localhost:~> sudo pvcreate /dev/loop0 /dev/loop1
  Physical volume "/dev/loop0" successfully created.
  Physical volume "/dev/loop1" successfully created.
byeonggyu@localhost:~> sudo vgcreate my_vg /dev/loop0 /dev/loop1
  Volume group "my_vg" successfully created
byeonggyu@localhost:~> sudo lvcreate -L 700M -n my_lv my_vg
  Logical volume "my_lv" created.

byeonggyu@localhost:~> sudo lvs
  LV    VG    Attr       LSize   Pool Origin Data%  Meta%  Move Log Cpy%Sync Convert
  my_lv my_vg -wi-a----- 700.00m
  root  rl    -wi-ao---- <26.38g
  swap  rl    -wi-ao----  <2.04g
```
PV 생성 -> 2개의 PV를 합쳐 VG 생성(1G) -> VG에서 700M를 떼어 LV 생성

3. File system 생성 및 Mount
```
byeonggyu@localhost:~> sudo mkfs.xfs /dev/my_vg/my_lv
meta-data=/dev/my_vg/my_lv       isize=512    agcount=4, agsize=44800 blks
         =                       sectsz=512   attr=2, projid32bit=1
         =                       crc=1        finobt=1, sparse=1, rmapbt=1
         =                       reflink=1    bigtime=1 inobtcount=1 nrext64=1
         =                       exchange=0
data     =                       bsize=4096   blocks=179200, imaxpct=25
         =                       sunit=0      swidth=0 blks
naming   =version 2              bsize=4096   ascii-ci=0, ftype=1, parent=0
log      =internal log           bsize=4096   blocks=16384, version=2
         =                       sectsz=512   sunit=0 blks, lazy-count=1
realtime =none                   extsz=4096   blocks=0, rtextents=0
Discarding blocks...Done.
byeonggyu@localhost:~> sudo mkdir /mnt/lvm_test
byeonggyu@localhost:~> sudo mount /dev/my_vg/my_lv /mnt/lvm_test

byeonggyu@localhost:~> df -h /mnt/lvm_test
Filesystem               Size  Used Avail Use% Mounted on
/dev/mapper/my_vg-my_lv  636M   45M  592M   7% /mnt/lvm_test
```

4. `lvextend`를 통한 용량 확장 (700M -> 900M)
```
byeonggyu@localhost:~> sudo lvextend -L 900M -r /dev/my_vg/my_lv
  File system xfs found on my_vg/my_lv mounted at /mnt/lvm_test.
  Size of logical volume my_vg/my_lv changed from 700.00 MiB (175 extents) to 900.00 MiB (225 extents).
  Extending file system xfs to 900.00 MiB (943718400 bytes) on my_vg/my_lv...
xfs_growfs /dev/my_vg/my_lv
meta-data=/dev/mapper/my_vg-my_lv isize=512    agcount=4, agsize=44800 blks
         =                       sectsz=512   attr=2, projid32bit=1
         =                       crc=1        finobt=1, sparse=1, rmapbt=1
         =                       reflink=1    bigtime=1 inobtcount=1 nrext64=1
         =                       exchange=0
data     =                       bsize=4096   blocks=179200, imaxpct=25
         =                       sunit=0      swidth=0 blks
naming   =version 2              bsize=4096   ascii-ci=0, ftype=1, parent=0
log      =internal log           bsize=4096   blocks=16384, version=2
         =                       sectsz=512   sunit=0 blks, lazy-count=1
realtime =none                   extsz=4096   blocks=0, rtextents=0
data blocks changed from 179200 to 230400
xfs_growfs done
  Extended file system xfs on my_vg/my_lv.
  Logical volume my_vg/my_lv successfully resized.

byeonggyu@localhost:~> df -h /mnt/lvm_test
Filesystem               Size  Used Avail Use% Mounted on
/dev/mapper/my_vg-my_lv  836M   49M  788M   6% /mnt/lvm_test
```

## File System 생성 및 점검
Partition이나 LVM을 생성했다면, 그 위에 데이터를 담을 규칙인 File system도 입혀야 합니다.
- `XFS`: Rocky Linux의 기본 파일 시스템, 대용량 파일 처리와 병렬 I/O에 최적화
- `EXT4`: 전통적인 파일 시스템, 작은 파일을 다룰 때 안정적이며 복구가 쉬움

### File System 점검/복구 실습
LVM 실습에 `XFS` 파일 시스템을 생성했고, 이에 대한 점검 및 복구

1. `/dev/my_vg/my_lv`에서 XFS 확인 및 `umount`
```
byeonggyu@localhost:~> sudo xfs_info /dev/my_vg/my_lv
meta-data=/dev/mapper/my_vg-my_lv isize=512    agcount=6, agsize=44800 blks
         =                       sectsz=512   attr=2, projid32bit=1
         =                       crc=1        finobt=1, sparse=1, rmapbt=1
         =                       reflink=1    bigtime=1 inobtcount=1 nrext64=1
         =                       exchange=0
data     =                       bsize=4096   blocks=230400, imaxpct=25
         =                       sunit=0      swidth=0 blks
naming   =version 2              bsize=4096   ascii-ci=0, ftype=1, parent=0
log      =internal log           bsize=4096   blocks=16384, version=2
         =                       sectsz=512   sunit=0 blks, lazy-count=1
realtime =none                   extsz=4096   blocks=0, rtextents=0

byeonggyu@localhost:/dev> lsblk
NAME          MAJ:MIN RM  SIZE RO TYPE MOUNTPOINTS
loop0           7:0    0  500M  0 loop
└─my_vg-my_lv 253:2    0  900M  0 lvm  /mnt/lvm_test
loop1           7:1    0  500M  0 loop
└─my_vg-my_lv 253:2    0  900M  0 lvm  /mnt/lvm_test
sr0            11:0    1 1024M  0 rom
vda           252:0    0   30G  0 disk
├─vda1        252:1    0  600M  0 part /boot/efi
├─vda2        252:2    0    1G  0 part /boot
└─vda3        252:3    0 28.4G  0 part
  ├─rl-root   253:0    0 26.4G  0 lvm  /
  └─rl-swap   253:1    0    2G  0 lvm  [SWAP]

byeonggyu@localhost:~> sudo umount /dev/my_vg/my_lv 2>/dev/null

byeonggyu@localhost:/dev> lsblk
NAME          MAJ:MIN RM  SIZE RO TYPE MOUNTPOINTS
loop0           7:0    0  500M  0 loop
└─my_vg-my_lv 253:2    0  900M  0 lvm
loop1           7:1    0  500M  0 loop
└─my_vg-my_lv 253:2    0  900M  0 lvm
sr0            11:0    1 1024M  0 rom
vda           252:0    0   30G  0 disk
├─vda1        252:1    0  600M  0 part /boot/efi
├─vda2        252:2    0    1G  0 part /boot
└─vda3        252:3    0 28.4G  0 part
  ├─rl-root   253:0    0 26.4G  0 lvm  /
  └─rl-swap   253:1    0    2G  0 lvm  [SWAP]
```

2. XFS 점검 (`-n`은 점검만 수행)
```
byeonggyu@localhost:~> sudo xfs_repair -n /dev/my_vg/my_lv
Phase 1 - find and verify superblock...
Phase 2 - using internal log
        - zero log...
        - scan filesystem freespace and inode maps...
        - found root inode chunk
Phase 3 - for each AG...
        - scan (but don't clear) agi unlinked lists...
        - process known inodes and perform inode discovery...
        - agno = 0
        - agno = 1
        - agno = 2
        - agno = 3
        - agno = 4
        - agno = 5
        - process newly discovered inodes...
Phase 4 - check for duplicate blocks...
        - setting up duplicate extent list...
        - check for inodes claiming duplicate blocks...
        - agno = 0
        - agno = 1
        - agno = 2
        - agno = 3
        - agno = 4
        - agno = 5
No modify flag set, skipping phase 5
Phase 6 - check inode connectivity...
        - traversing filesystem ...
        - traversal finished ...
        - moving disconnected inodes to lost+found ...
Phase 7 - verify link counts...
No modify flag set, skipping filesystem flush and exiting.
```

3. EXT4로 다시 포맷 후 점검
```
byeonggyu@localhost:~> sudo mkfs.ext4 -F /dev/my_vg/my_lv
mke2fs 1.47.1 (20-May-2024)
/dev/my_vg/my_lv contains a xfs file system
Discarding device blocks: done
Creating filesystem with 230400 4k blocks and 57600 inodes
Filesystem UUID: d6425af2-2b86-4d26-a68f-c971435c9da0
Superblock backups stored on blocks:
        32768, 98304, 163840, 229376

Allocating group tables: done
Writing inode tables: done
Creating journal (4096 blocks): done
Writing superblocks and filesystem accounting information: done

byeonggyu@localhost:~> sudo fsck -f /dev/my_vg/my_lv
fsck from util-linux 2.40.2
e2fsck 1.47.1 (20-May-2024)
Pass 1: Checking inodes, blocks, and sizes
Pass 2: Checking directory structure
Pass 3: Checking directory connectivity
Pass 4: Checking reference counts
Pass 5: Checking group summary information
/dev/mapper/my_vg-my_lv: 11/57600 files (0.0% non-contiguous), 8288/230400 blocks
```

## Mount 및 자동 Mount 설정(/etc/fstab)
Mount란 물리적인 장치(Partition)을 특정 디렉토리(Mount Point)에 연결하는 행위이다.  
자동 Mount는 리눅스가 부팅될 때 `mount` 명령어를 입력하지 않고도 자동으로 디렉토리와 저장 장치를 연결해주는 기능이다.

자동 Mount의 핵심은 `/etc/fstab` 파일로, 아래 형식의 데이터를 갖는다.  
```
#
# /etc/fstab
# Created by anaconda on Fri Mar 20 16:04:18 2026
#
# Accessible filesystems, by reference, are maintained under '/dev/disk/'.
# See man pages fstab(5), findfs(8), mount(8) and/or blkid(8) for more info.
#
# After editing this file, run 'systemctl daemon-reload' to update systemd
# units generated from this file.
#
UUID=7111b929-0dab-44d6-bb9e-9bccc05a74d2 /                       xfs     defaults        0 0
UUID=ee8c5b16-96bb-44c7-a72e-7ae9ed6cd128 /boot                   xfs     defaults        0 0
UUID=FFF4-3DC2          /boot/efi               vfat    umask=0077,shortname=winnt 0 2
UUID=e4d6510f-fed1-4e86-99cd-05facc72f224 none                    swap    defaults        0 0
```
UUID(누가), 디렉토리(어디에), 파일 시스템(어떻게) 정보를 갖고 있다.

### 자동 Mount 실습

1. 수동 Mount 확인
```
byeonggyu@localhost:~> sudo blkid /dev/my_vg/my_lv
/dev/my_vg/my_lv: UUID="d6425af2-2b86-4d26-a68f-c971435c9da0" BLOCK_SIZE="4096" TYPE="ext4"

byeonggyu@localhost:~> sudo mkdir /example
byeonggyu@localhost:~> sudo mount /dev/my_vg/my_lv /example

byeonggyu@localhost:~> df -h | grep example
/dev/mapper/my_vg-my_lv  868M   24K  807M   1% /example
```

2. `/etc/fstab` 변경 

위에서 확인한 UUID 및 File System 정보를 추가한다.
```
#
# /etc/fstab
# Created by anaconda on Fri Mar 20 16:04:18 2026
#
# Accessible filesystems, by reference, are maintained under '/dev/disk/'.
# See man pages fstab(5), findfs(8), mount(8) and/or blkid(8) for more info.
#
# After editing this file, run 'systemctl daemon-reload' to update systemd
# units generated from this file.
#
UUID=7111b929-0dab-44d6-bb9e-9bccc05a74d2 /                       xfs     defaults        0 0
UUID=ee8c5b16-96bb-44c7-a72e-7ae9ed6cd128 /boot                   xfs     defaults        0 0
UUID=FFF4-3DC2          /boot/efi               vfat    umask=0077,shortname=winnt 0 2
UUID=e4d6510f-fed1-4e86-99cd-05facc72f224 none                    swap    defaults        0 0
UUID=d6425af2-2b86-4d26-a68f-c971435c9da0 /example ext4 defaults 0 0
```

3. `mount -a`를 통한 확인 (매우 중요!)
```
byeonggyu@localhost:~> sudo umount /example
byeonggyu@localhost:~> sudo mount -a
mount: (hint) your fstab has been modified, but systemd still uses
       the old version; use 'systemctl daemon-reload' to reload.

byeonggyu@localhost:~> df -h | grep example
/dev/mapper/my_vg-my_lv  868M   24K  807M   1% /example
```
잘 mount됨을 볼 수 있다. 재부팅해도 잘 mount됨을 보장하기 위해 필수로 확인해야 한다.
