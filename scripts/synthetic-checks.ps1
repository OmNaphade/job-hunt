param(
    [string]$GatewayBase = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'

function Assert-StatusCode {
    param(
        [string]$Name,
        [string]$Url,
        [int[]]$ExpectedCodes
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -Method Get -UseBasicParsing
        $code = [int]$response.StatusCode
    }
    catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $code = [int]$_.Exception.Response.StatusCode
        }
        else {
            throw "[$Name] request failed: $($_.Exception.Message)"
        }
    }

    if ($ExpectedCodes -contains $code) {
        Write-Host "[PASS] $Name -> $code" -ForegroundColor Green
    }
    else {
        throw "[FAIL] $Name expected $($ExpectedCodes -join ',') but got $code"
    }
}

Write-Host 'Running synthetic checks...' -ForegroundColor Cyan

Assert-StatusCode -Name 'Gateway health endpoint reachable' -Url "$GatewayBase/actuator/health" -ExpectedCodes @(200, 401)
Assert-StatusCode -Name 'Auth login route availability' -Url "$GatewayBase/api/auth/login" -ExpectedCodes @(401, 403, 405)
Assert-StatusCode -Name 'Jobs protected endpoint guard' -Url "$GatewayBase/api/jobs" -ExpectedCodes @(401)
Assert-StatusCode -Name 'Notifications protected endpoint guard' -Url "$GatewayBase/api/notifications" -ExpectedCodes @(401)

Write-Host 'Synthetic checks completed.' -ForegroundColor Green
