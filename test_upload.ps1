$filePath = "e:\Code\ACASB\2.jpg"
$uri = "http://localhost:8080/data/add"

$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"

$fileBytes = [System.IO.File]::ReadAllBytes($filePath)
$fileName = Split-Path $filePath -Leaf

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

try {
    $response = Invoke-RestMethod -Uri $uri -Method POST -ContentType "multipart/form-data; boundary=$boundary" -Body $memStream.ToArray()
    Write-Host "Response:"
    $response | ConvertTo-Json -Depth 10
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)"
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response: $($reader.ReadToEnd())"
    }
}
