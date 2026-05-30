$ErrorActionPreference = "Stop"

Write-Output "--- 1. Registering User ---"
$registerBody = @{ username="preethamn023"; password="Password@2004" } | ConvertTo-Json
try {
    $registerRes = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register" -Method Post -Body $registerBody -ContentType "application/json"
    Write-Output "Registration Successful."
} catch {
    Write-Output "Registration Failed or user already exists."
}

Write-Output "--- 2. Logging In ---"
$loginRes = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method Post -Body $registerBody -ContentType "application/json"
$token = $loginRes.token
$headers = @{ Authorization = "Bearer $token" }
Write-Output "Login Successful. Token obtained."

Write-Output "--- 3. Uploading Workbook ---"
$filePath = "C:\Users\preet\.gemini\antigravity\scratch\dynamic-excel-platform\test_workbook.xlsx"
$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"
$fileBytes = [System.IO.File]::ReadAllBytes($filePath)
$fileEnc = [System.Text.Encoding]::GetEncoding("iso-8859-1").GetString($fileBytes)

$bodyLines = (
    "--$boundary",
    "Content-Disposition: form-data; name=`"file`"; filename=`"test_workbook.xlsx`"",
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
Write-Output "Upload Successful. WorkbookId: $($uploadRes.id)"
$workbookId = $uploadRes.id

Write-Output "--- 4. Fetching Initial Cells ---"
$sheets = Invoke-RestMethod -Uri "http://localhost:8081/api/workbooks/$workbookId/sheets" -Method Get -Headers $headers
$sheetId = $sheets[0].id
$cells = Invoke-RestMethod -Uri "http://localhost:8081/api/cells/sheet/$sheetId" -Method Get -Headers $headers
$cellA1 = $cells | Where-Object { $_.cellRef -eq "A1" }
$cellC1 = $cells | Where-Object { $_.cellRef -eq "C1" }
Write-Output "Initial A1: $($cellA1.calculatedValue) | Initial C1 (A1+B1): $($cellC1.calculatedValue)"

Write-Output "--- 5. Updating Cell A1 to 100 ---"
$updateBody = @{
    sheetId = $sheetId
    cellRef = "A1"
    newValue = "100"
} | ConvertTo-Json
$updateRes = Invoke-RestMethod -Uri "http://localhost:8081/api/cells/update" -Method Post -Headers @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" } -Body $updateBody
Write-Output "Update triggered."

Write-Output "--- 6. Verifying Recalculation ---"
$newCells = Invoke-RestMethod -Uri "http://localhost:8081/api/cells/sheet/$sheetId" -Method Get -Headers $headers
$newCellA1 = $newCells | Where-Object { $_.cellRef -eq "A1" }
$newCellC1 = $newCells | Where-Object { $_.cellRef -eq "C1" }
Write-Output "New A1: $($newCellA1.calculatedValue) | New C1 (A1+B1): $($newCellC1.calculatedValue)"

Write-Output "--- E2E TEST COMPLETE ---"
