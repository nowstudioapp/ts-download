# 打包顺序

## 1. 构建前端
```powershell
cd frontend
npm install
npm run build
cd ..
```

## 2. 构建应用
```powershell
wails build -nsis
```

## 3. 输出文件
```
build/bin/TS-Merge-1.0.0-amd64-installer.exe
```

## 开发模式
```powershell
wails dev
```


# 构建前端
cd frontend
npm run build
cd ..
wails dev

# 构建 EXE
cd frontend
npm run build
cd ..
wails build -nsis
