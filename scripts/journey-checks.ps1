param(
    [string]$GatewayBase = 'http://localhost:8080',
    [string]$CandidateEmail = '',
    [string]$CandidatePassword = 'Pass123!',
    [string]$EmployerEmail = '',
    [string]$EmployerPassword = 'Pass123!',
    [string]$AdminEmail = 'admin@jobportal.local',
    [string]$AdminPassword = 'Pass123!'
)

$ErrorActionPreference = 'Stop'

function Invoke-WithStatus {
    param(
        [string]$Method,
        [string]$Url,
        [int[]]$ExpectedCodes,
        [hashtable]$Headers,
        [object]$Body
    )

    try {
        $params = @{ Uri = $Url; Method = $Method; ErrorAction = 'Stop'; UseBasicParsing = $true }
        if ($Headers) { $params.Headers = $Headers }
        if ($null -ne $Body) {
            $params.ContentType = 'application/json'
            $params.Body = ($Body | ConvertTo-Json)
        }

        $response = Invoke-WebRequest @params
        $code = [int]$response.StatusCode
        $content = $response.Content
    }
    catch {
        if ($_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
            $content = $_.ErrorDetails.Message
        }
        else {
            throw
        }
    }

    if (-not ($ExpectedCodes -contains $code)) {
        throw "Expected status $($ExpectedCodes -join ',') but got $code for $Method $Url"
    }

    if ($content) {
        try { return ($content | ConvertFrom-Json) } catch { return $content }
    }

    return $null
}

function Register-IfNeeded {
    param(
        [string]$Email,
        [string]$Password,
        [string]$Role,
        [string]$FirstName,
        [string]$LastName
    )

    $body = @{
        email = $Email
        password = $Password
        role = $Role
        firstName = $FirstName
        lastName = $LastName
    }

    $null = Invoke-WithStatus -Method Post -Url "$GatewayBase/api/auth/register" -ExpectedCodes @(200, 201, 409) -Body $body
}

function Login-And-Token {
    param([string]$Email, [string]$Password, [int[]]$ExpectedCodes = @(200))

    $body = @{ email = $Email; password = $Password }
    $resp = Invoke-WithStatus -Method Post -Url "$GatewayBase/api/auth/login" -ExpectedCodes $ExpectedCodes -Body $body
    if (-not $resp.accessToken) {
        throw "Login did not return an access token for $Email"
    }
    return $resp.accessToken
}

function Assert-Call {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers
    )

    try {
        $null = Invoke-WithStatus -Method $Method -Url $Url -ExpectedCodes @(200) -Headers $Headers
        Write-Host "[PASS] $Name" -ForegroundColor Green
    }
    catch {
        throw "[FAIL] $Name -> $($_.Exception.Message)"
    }
}

Write-Host 'Running journey checks...' -ForegroundColor Cyan

$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
if ([string]::IsNullOrWhiteSpace($CandidateEmail)) {
    $CandidateEmail = "candidate.$suffix@jobportal.local"
}
if ([string]::IsNullOrWhiteSpace($EmployerEmail)) {
    $EmployerEmail = "recruiter.$suffix@jobportal.local"
}

Register-IfNeeded -Email $CandidateEmail -Password $CandidatePassword -Role 'JOB_SEEKER' -FirstName 'Journey' -LastName 'Candidate'
Register-IfNeeded -Email $EmployerEmail -Password $EmployerPassword -Role 'RECRUITER' -FirstName 'Journey' -LastName 'Recruiter'

$candidateToken = Login-And-Token -Email $CandidateEmail -Password $CandidatePassword
$employerToken = Login-And-Token -Email $EmployerEmail -Password $EmployerPassword

$adminToken = $null
try {
    $adminToken = Login-And-Token -Email $AdminEmail -Password $AdminPassword
}
catch {
    Write-Host "[WARN] Admin login unavailable for $AdminEmail; running monitoring authorization guard check instead." -ForegroundColor Yellow
}

$candidateHeaders = @{ Authorization = "Bearer $candidateToken" }
$employerHeaders = @{ Authorization = "Bearer $employerToken" }

Assert-Call -Name 'Candidate my applications' -Method Get -Url "$GatewayBase/api/applications/my" -Headers $candidateHeaders
Assert-Call -Name 'Employer jobs list' -Method Get -Url "$GatewayBase/api/jobs" -Headers $employerHeaders

if ($adminToken) {
    $adminHeaders = @{ Authorization = "Bearer $adminToken" }
    Assert-Call -Name 'Admin monitoring summary' -Method Get -Url "$GatewayBase/api/monitoring/summary" -Headers $adminHeaders
}
else {
    $null = Invoke-WithStatus -Method Get -Url "$GatewayBase/api/monitoring/summary" -ExpectedCodes @(401, 403) -Headers $employerHeaders
    Write-Host '[PASS] Monitoring summary is protected for non-admin users' -ForegroundColor Green
}

Write-Host 'Journey checks completed.' -ForegroundColor Green
