$baseUri = "http://localhost:8080"

Write-Host "从数据库查询数据..."
Write-Host ""

Write-Host "前5张图片（皇室建筑）:"
Write-Host ""

for ($i = 3; $i -le 7; $i++) {
    Write-Host "查询类型记录 ID: $i"
    $queryUri = "$baseUri/data/type/$i"
    
    try {
        $response = Invoke-WebRequest -Uri $queryUri -Method GET -UseBasicParsing
        $content = $response.Content
        $json = $content | ConvertFrom-Json
        
        if ($json.success -eq $true) {
            Write-Host "  ✓ 预测结果: $($json.data.prediction), 置信度: $($json.data.confidence)" -ForegroundColor Cyan
        } else {
            Write-Host "  ✗ 查询失败: $($json.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "  ✗ 查询异常: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "后5张图片（平民建筑）:"
Write-Host ""

for ($i = 8; $i -le 12; $i++) {
    Write-Host "查询类型记录 ID: $i"
    $queryUri = "$baseUri/data/type/$i"
    
    try {
        $response = Invoke-WebRequest -Uri $queryUri -Method GET -UseBasicParsing
        $content = $response.Content
        $json = $content | ConvertFrom-Json
        
        if ($json.success -eq $true) {
            Write-Host "  ✓ 预测结果: $($json.data.prediction), 置信度: $($json.data.confidence)" -ForegroundColor Cyan
        } else {
            Write-Host "  ✗ 查询失败: $($json.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "  ✗ 查询异常: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "完成"
