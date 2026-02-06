$baseUri = "http://localhost:8080"
$filePath = "E:\Code\ACASB\1.jpg"

Write-Host "测试单个上传..."
Write-Host "文件: $filePath"
Write-Host ""

$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"

$fileBytes = [System.IO.File]::ReadAllBytes($filePath)
$fileName = [System.IO.Path]::GetFileName($filePath)

$header = "--$boundary$LF"
$header += "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"$LF"
$header += "Content-Type: application/octet-stream$LF"
$header += "$LF"

$footer = "$LF--$boundary--$LF"

$memStream = New-Object System.IO.MemoryStream
$writer = New-Object System.IO.BinaryWriter($memStream)

$writer.Write([System.Text.Encoding]::GetEncoding("iso-8859-1").GetBytes($header))
$writer.Write($fileBytes)
$writer.Write([System.Text.Encoding]::GetEncoding("iso-8859-1").GetBytes($footer))
$writer.Flush()

$uri = "$baseUri/data/add"
$response = Invoke-RestMethod -Uri $uri -Method POST -ContentType "multipart/form-data; boundary=$boundary" -Body $memStream.ToArray()

Write-Host "响应:"
$response | ConvertTo-Json -Depth 10
