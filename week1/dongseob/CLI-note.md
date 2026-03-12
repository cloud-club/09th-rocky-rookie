# CLI Commands 1

---

## [cd : 경로 관련]

### 현재 경로 확인
```bash
pwd
```

### 디렉토리 이동
```bash
# 해당 폴더로 이동
cd 폴더명

# 부모 루트로 이동
cd ..

# 부모 루트로 이동 후 해당 경로 내부의 폴더로 이동
cd ../폴더명

# 부모의 부모 루트로 이동
cd ../..

# 최상위 루트(홈)로 이동
cd ~

# 최상위 루트(홈) 내부의 해당 폴더로 이동
cd ~/폴더명

# 이전 경로로 이동
cd -
```

### 파일 및 폴더 목록 보기
```bash
# 현재 경로에서의 파일과 폴더 목록 보기
ls

# 해당 경로에서의 모든 파일과 폴더 목록 및 정보 보기
ls -l

# 해당 경로에서의 모든 파일과 폴더 목록 및 정보 보기(숨김 포함)
ls -la

# 부모 경로에서의 모든 파일과 폴더 목록 및 정보 보기(숨김 포함)
ls -la ..
```

### 화면 정리
```bash
# CLI 창 정리
clear
```

---

## [cp : 복사 관련]

### 파일 복사
```bash
# file1.txt의 내용을 file2.txt로 복사
# (기존 file2.txt가 있으면 덮어씌움, 없으면 새로 생성)
cp file1.txt file2.txt

# 덮어쓰기 전 확인 (기존 file2.txt가 있을 경우)
cp -i file1.txt file2.txt

# 기존 파일이 있으면 덮어쓰지 않음
cp -n file1.txt file2.txt

# 파일을 특정 경로 내부에 복사
cp file1.txt /home/user/폴더명/

# 여러 파일을 특정 경로 내부에 복사
cp file1.txt file2.txt file3.txt /home/user/폴더명/
```

### 폴더 복사
```bash
# 폴더 복사 
# (기존 dir2가 있으면 dir2 내부에 dir1 생성 후 복사: dir2/dir1)
cp -R dir1 dir2

# 모든 속성을 유지하며 폴더 복사
cp -a dir1 dir2
```

---

## [mv & mkdir : 파일 및 폴더 이동 및 생성 관련]

### 파일 이동 및 이름 변경
```bash
# file2가 없으면: file1 이름 변경
# file2가 있으면: file2 내용을 file1 내용으로 덮어쓰기
mv file1 file2

# 덮어쓰기 전 확인 (기존 file2가 있을 경우)
mv -i file1 file2

# file1을 dir 내부로 이동
mv file1 dir

# 여러 개의 파일을 dir 내부로 이동
mv file1 file2 file3 dir
```

### 폴더 이동 및 이름 변경
```bash
# dir2가 없으면: dir1 이름 변경
# dir2가 있으면: dir1이 dir2 내부로 이동
mv dir1 dir2
```

### 폴더 생성
```bash
# 폴더 생성
mkdir 폴더명
```

---

## [rm : 삭제 관련]

### 파일 삭제
```bash
# 파일 삭제
rm 파일명

# 여러 개의 파일 삭제
rm 파일명1 파일명2

# 파일 삭제 (삭제 전 확인 질문)
rm -i 파일명
```

### 폴더 삭제
```bash
# 폴더 삭제
rm -r dir
```
