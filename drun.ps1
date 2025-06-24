# 获取最新构建ID（增强错误处理）
try {
    # 捕获gh命令输出并转换为对象
    $runList = gh run list --workflow=build --json databaseId,status,updatedAt,number --limit 1 2>&1 | ConvertFrom-Json
    
    # 验证结果有效性
    if (-not $runList -or -not $runList.databaseId) {
        throw "No build found in GitHub Actions"
    }
    
    $runId = $runList.databaseId
    Write-Host " Build ID: $runId (Status: $($runList.status), Updated: $($runList.updatedAt), Number: $($runList.number))" -ForegroundColor Green
}
catch {
    Write-Host " Error fetching build ID: $_" -ForegroundColor Red
    exit 1
}

# 准备制品目录
$artifactPath = "./artifacts"
if (-not (Test-Path $artifactPath)) {
    New-Item -ItemType Directory -Path $artifactPath -Force | Out-Null
}

# 下载制品（增加进度显示）
try {
    Write-Host " Downloading artifacts (ID: $runId)..." -ForegroundColor Cyan
    $downloadOutput = gh run download $runId -p "*1.18.2*" -D $artifactPath 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        throw "Download failed: $downloadOutput"
    }
    
    # 验证下载内容
    $downloadedFiles = Get-ChildItem $artifactPath -Recurse -ErrorAction SilentlyContinue
    if (-not $downloadedFiles) {
        throw "Artifacts downloaded but directory is empty"
    }
    
    Write-Host " Downloaded $($downloadedFiles.Count) files" -ForegroundColor Green
}
catch {
    Write-Host " Download error: $_" -ForegroundColor Red
    exit 2
}

# 查找并复制JAR文件
try {
    $targetFiles = Get-ChildItem $artifactPath -Recurse -Filter "*1.18.2*.jar" |
                   Sort-Object LastWriteTime -Descending
                   
    if (-not $targetFiles) {
        throw "No JAR files matching '*1.18.2*.jar' found"
    }

    $latestJar = $targetFiles[0]
    $buildDir = "./build"
    
    if (-not (Test-Path $buildDir)) {
        New-Item -ItemType Directory -Path $buildDir -Force | Out-Null
    }

    $destPath = Join-Path $buildDir $latestJar.Name
    Copy-Item $latestJar.FullName $destPath -Force
    
    Write-Host " Copied JAR file: $($latestJar.Name)" -ForegroundColor Green
    Write-Host "   Source: $($latestJar.FullName)" -ForegroundColor DarkGray
    Write-Host "   Target: $destPath" -ForegroundColor DarkGray
}
catch {
    Write-Host " JAR processing error: $_" -ForegroundColor Red
    exit 3
}

# 执行启动脚本（增强兼容性）
$scriptPath = "./runc.ps1"
if (Test-Path $scriptPath) {
    try {
        Write-Host " Executing run script: $scriptPath" -ForegroundColor Cyan
        & $scriptPath
        if ($LASTEXITCODE -ne 0) {
            Write-Host " Script exited with code $LASTEXITCODE" -ForegroundColor Yellow
        }
    }
    catch {
        Write-Host " Script execution failed: $_" -ForegroundColor Red
    }
}
else {
    Write-Host " No run script found at $scriptPath" -ForegroundColor Yellow
}