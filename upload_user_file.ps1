$ErrorActionPreference = "Stop"

$username = "preethamn023"
$password = "Password@2004"

$loginBody = @{ username=$username; password=$password } | ConvertTo-Json

try {
    $loginRes = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginRes.token
} catch {
    # Try register if login fails
    $registerRes = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $registerRes.token
}

Write-Output "Authentication successful. Uploading user's testexcel.xlsx..."

$filePath = "C:\Users\preet\Desktop\testexcel.xlsx"
$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"
$fileBytes = [System.IO.File]::ReadAllBytes($filePath)
$fileEnc = [System.Text.Encoding]::GetEncoding("iso-8859-1").GetString($fileBytes)

$bodyLines = (
    "--$boundary",
    "Content-Disposition: form-data; name=`"file`"; filename=`"testexcel.xlsx`"",
    "Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "",
    $fileEnc,
    "--$boundary--"
) -join $LF

$uploadHeaders = @{ 
    Authorization = "Bearer $token" 
    "Content-Type" = "multipart/form-data; boundary=$boundary" 
}
$uploadRes = Invoke-RestMethod -Uri "http://localhost:8081/api/workbooks/upload" -Method Post -Headers $uploadHeaders -Body $bodyLines

Write-Output "Upload Complete!"
Write-Output "File Name: $($uploadRes.fileName)"
Write-Output "Status: $($uploadRes.status)"
Write-Output "Workbook ID: $($uploadRes.id)"
