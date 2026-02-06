$baseUri = "http://localhost:8080"
$currentDir = Get-Location

$files = @(
    "$currentDir\1.jpg",
    "$currentDir\2.jpg",
    "$currentDir\3.jpg",
    "$currentDir\4.jpg",
    "$currentDir\5.jpg",
    "$currentDir\6.jpg",
    "$currentDir\7.jpg",
    "$currentDir\8.jpg",
    "$currentDir\9.jpg",
    "$currentDir\10.jpg"
)

Write-Host "开始批量上传测试..."
Write-Host "上传文件数量: $($files.Count)"
Write-Host ""

$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"

$results = @()

foreach ($file in $files) {
    Write-Host "正在上传: $file"
    
    try {
        $fileBytes = [System.IO.File]::ReadAllBytes($file)
        $fileName = [System.IO.Path]::GetFileName($file)
        
        $header = "--$boundary$LF"
        $header += "Content-Disposition: form-data; name=`"files`"; filename=`"$fileName`"$LF"
        $header += "Content-Type: application/octet-stream$LF"
        $header += "$LF"
        
        $footer = "$LF--$boundary--$LF"
        
        $memStream = New-Object System.IO.MemoryStream
        $writer = New-Object System.IO.BinaryWriter($memStream)
        
        $writer.Write([System.Text.Encoding]::GetEncoding("iso-8859-1").GetBytes($header))
        $writer.Write($fileBytes)
        $writer.Write([System.Text.Encoding]::GetEncoding("iso-8859-1").GetBytes($footer))
        $writer.Flush()
        
        $uri = "$baseUri/data/batch"
        $response = Invoke-RestMethod -Uri $uri -Method POST -ContentType "multipart/form-data; boundary=$boundary" -Body $memStream.ToArray()
        
        $results += $response
        
        Start-Sleep -Milliseconds 500
    } catch {
        Write-Host "上传失败: $($_.Exception.Message)" -ForegroundColor Red
        $results += @{ success = $false; message = $_.Exception.Message }
    }
}

Write-Host ""
Write-Host "批量上传结果"
Write-Host ""

foreach ($result in $results) {
    if ($result -is [System.Management.Automation.PSCustomObject]) {
        Write-Host "总文件数: $($result.totalCount)"
        Write-Host "成功数: $($result.successCount)"
        Write-Host "失败数: $($result.failureCount)"
        Write-Host ""
        
        foreach ($item in $result.items) {
            if ($item.success) {
                Write-Host "  ✓ $($item.fileName)" -ForegroundColor Green
                Write-Host "    分析ID: $($item.analysisId), 类型ID: $($item.typeId)" -ForegroundColor Cyan
            } else {
                Write-Host "  ✗ $($item.fileName)" -ForegroundColor Red
                Write-Host "    原因: $($item.message)" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "请求失败: $result" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "完成"
