$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$outputDir = 'C:\Users\User\OneDrive\Desktop\TimeSheetLeaveManagementSystem\diagrams'
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

function New-Canvas {
    param(
        [int]$Width,
        [int]$Height
    )
    $bmp = New-Object System.Drawing.Bitmap $Width, $Height
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.Clear([System.Drawing.Color]::FromArgb(247,250,252))
    return @{ Bitmap = $bmp; Graphics = $g }
}

function Save-Canvas {
    param(
        $Canvas,
        [string]$Path
    )
    $Canvas.Bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $Canvas.Graphics.Dispose()
    $Canvas.Bitmap.Dispose()
}

function Draw-Title {
    param(
        [System.Drawing.Graphics]$G,
        [string]$Text,
        [int]$Width
    )
    $font = New-Object System.Drawing.Font('Segoe UI', 22, [System.Drawing.FontStyle]::Bold)
    $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(15, 76, 92))
    $rect = New-Object System.Drawing.RectangleF(24, 18, ($Width - 48), 40)
    $format = New-Object System.Drawing.StringFormat
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    $G.DrawString($Text, $font, $brush, $rect, $format)
    $font.Dispose()
    $brush.Dispose()
    $format.Dispose()
}

function Draw-Box {
    param(
        [System.Drawing.Graphics]$G,
        [int]$X,
        [int]$Y,
        [int]$W,
        [int]$H,
        [string]$Text,
        [string]$Fill = '#D9EEF7',
        [string]$Border = '#2C7DA0',
        [int]$FontSize = 12
    )
    $fillColor = [System.Drawing.ColorTranslator]::FromHtml($Fill)
    $borderColor = [System.Drawing.ColorTranslator]::FromHtml($Border)
    $brush = New-Object System.Drawing.SolidBrush($fillColor)
    $pen = New-Object System.Drawing.Pen($borderColor, 2)
    $radius = 14
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddArc($X, $Y, $radius, $radius, 180, 90)
    $path.AddArc($X + $W - $radius, $Y, $radius, $radius, 270, 90)
    $path.AddArc($X + $W - $radius, $Y + $H - $radius, $radius, $radius, 0, 90)
    $path.AddArc($X, $Y + $H - $radius, $radius, $radius, 90, 90)
    $path.CloseFigure()
    $G.FillPath($brush, $path)
    $G.DrawPath($pen, $path)

    $font = New-Object System.Drawing.Font('Segoe UI', $FontSize, [System.Drawing.FontStyle]::Regular)
    $textBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(31,31,31))
    $format = New-Object System.Drawing.StringFormat
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center
    $format.Trimming = [System.Drawing.StringTrimming]::EllipsisWord
    $format.FormatFlags = [System.Drawing.StringFormatFlags]::LineLimit
    $textRect = [System.Drawing.RectangleF]::new(
        [single]($X + 8),
        [single]($Y + 6),
        [single]($W - 16),
        [single]($H - 12)
    )
    $G.DrawString($Text, $font, $textBrush, $textRect, $format)

    $font.Dispose()
    $textBrush.Dispose()
    $format.Dispose()
    $brush.Dispose()
    $pen.Dispose()
    $path.Dispose()
}

function Draw-Arrow {
    param(
        [System.Drawing.Graphics]$G,
        [int]$X1,
        [int]$Y1,
        [int]$X2,
        [int]$Y2,
        [string]$Label = ''
    )
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(70, 143, 175), 3)
    $cap = New-Object System.Drawing.Drawing2D.AdjustableArrowCap(5, 6, $true)
    $pen.CustomEndCap = $cap
    $G.DrawLine($pen, $X1, $Y1, $X2, $Y2)

    if ($Label) {
        $font = New-Object System.Drawing.Font('Segoe UI', 10, [System.Drawing.FontStyle]::Regular)
        $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(15,76,92))
        $midX = [int](($X1 + $X2) / 2)
        $midY = [int](($Y1 + $Y2) / 2)
        $rect = [System.Drawing.RectangleF]::new(
            [single]($midX - 95),
            [single]($midY - 18),
            [single]190,
            [single]30
        )
        $format = New-Object System.Drawing.StringFormat
        $format.Alignment = [System.Drawing.StringAlignment]::Center
        $format.LineAlignment = [System.Drawing.StringAlignment]::Center
        $G.DrawString($Label, $font, $brush, $rect, $format)
        $font.Dispose()
        $brush.Dispose()
        $format.Dispose()
    }

    $cap.Dispose()
    $pen.Dispose()
}

function New-DiagramPath {
    param([string]$FileName)
    return (Join-Path $outputDir $FileName)
}

function Draw-HighLevelArchitecture {
    $canvas = New-Canvas 1500 900
    $g = $canvas.Graphics
    Draw-Title $g 'High-Level System Architecture' 1500

    Draw-Box $g 560 90 380 70 'Users / Browser / Swagger' '#EAF4EA' '#4C956C' 15
    Draw-Box $g 560 200 380 70 'API Gateway' '#FFF4D6' '#D08C00' 16

    Draw-Box $g 150 360 240 80 'auth-service'
    Draw-Box $g 460 360 240 80 'timesheet-service'
    Draw-Box $g 770 360 240 80 'leave-service'
    Draw-Box $g 1080 360 240 80 'admin-service'

    Draw-Box $g 120 620 270 80 'Eureka Server' '#F6E8FF' '#7B2CBF'
    Draw-Box $g 430 620 270 80 'Config Server' '#F6E8FF' '#7B2CBF'
    Draw-Box $g 740 620 270 80 'RabbitMQ' '#FFE5EC' '#C9184A'

    Draw-Box $g 1080 560 120 60 'Auth DB' '#EDF6F9' '#006D77'
    Draw-Box $g 1210 560 120 60 'Time DB' '#EDF6F9' '#006D77'
    Draw-Box $g 1080 640 120 60 'Leave DB' '#EDF6F9' '#006D77'
    Draw-Box $g 1210 640 120 60 'Admin DB' '#EDF6F9' '#006D77'

    Draw-Arrow $g 750 160 750 200 ''
    Draw-Arrow $g 750 270 270 360 ''
    Draw-Arrow $g 750 270 580 360 ''
    Draw-Arrow $g 750 270 890 360 ''
    Draw-Arrow $g 750 270 1200 360 ''

    Draw-Arrow $g 270 440 255 620 ''
    Draw-Arrow $g 580 440 255 620 ''
    Draw-Arrow $g 890 440 255 620 ''
    Draw-Arrow $g 1200 440 255 620 ''

    Draw-Arrow $g 270 440 565 620 ''
    Draw-Arrow $g 580 440 565 620 ''
    Draw-Arrow $g 890 440 565 620 ''
    Draw-Arrow $g 1200 440 565 620 ''

    Draw-Arrow $g 270 440 875 620 ''
    Draw-Arrow $g 580 440 875 620 ''
    Draw-Arrow $g 890 440 875 620 ''
    Draw-Arrow $g 1200 440 875 620 ''

    Draw-Arrow $g 1200 440 1140 560 ''
    Draw-Arrow $g 1200 440 1270 560 ''
    Draw-Arrow $g 1200 440 1140 640 ''
    Draw-Arrow $g 1200 440 1270 640 ''

    Save-Canvas $canvas (New-DiagramPath '01_high_level_architecture.png')
}

function Draw-GatewayRouting {
    $canvas = New-Canvas 1400 850
    $g = $canvas.Graphics
    Draw-Title $g 'API Gateway Routing View' 1400

    Draw-Box $g 520 100 360 70 'Client Requests' '#EAF4EA' '#4C956C' 15
    Draw-Box $g 520 220 360 70 'API Gateway' '#FFF4D6' '#D08C00' 16

    Draw-Box $g 100 430 260 80 '/api/v1/auth/**\n-> auth-service'
    Draw-Box $g 400 430 260 80 '/api/v1/timesheets/**\n-> timesheet-service'
    Draw-Box $g 700 430 260 80 '/api/v1/leave/**\n-> leave-service'
    Draw-Box $g 1000 430 260 80 '/api/v1/admin/**\n-> admin-service'
    Draw-Box $g 400 580 260 80 '/api/v1/projects/**\n-> timesheet-service'

    Draw-Arrow $g 700 170 700 220 ''
    Draw-Arrow $g 700 290 230 430 ''
    Draw-Arrow $g 700 290 530 430 ''
    Draw-Arrow $g 700 290 830 430 ''
    Draw-Arrow $g 700 290 1130 430 ''
    Draw-Arrow $g 700 290 530 580 ''

    Save-Canvas $canvas (New-DiagramPath '02_gateway_routing.png')
}

function Draw-AuthWorkflow {
    $canvas = New-Canvas 1600 700
    $g = $canvas.Graphics
    Draw-Title $g 'Authentication Workflow' 1600

    Draw-Box $g 60 280 220 70 'User'
    Draw-Box $g 360 280 240 70 'API Gateway'
    Draw-Box $g 680 280 240 70 'auth-service'
    Draw-Box $g 1000 280 220 70 'Auth DB'
    Draw-Box $g 1300 280 220 70 'JWT Token Response'

    Draw-Arrow $g 280 315 360 315 'POST /login'
    Draw-Arrow $g 600 315 680 315 'forward'
    Draw-Arrow $g 920 315 1000 315 'find user'
    Draw-Arrow $g 1220 315 1300 315 'issue token'

    Save-Canvas $canvas (New-DiagramPath '03_auth_workflow.png')
}

function Draw-LeaveWorkflow {
    $canvas = New-Canvas 1800 800
    $g = $canvas.Graphics
    Draw-Title $g 'Leave Request Workflow' 1800

    Draw-Box $g 40 300 180 70 'Employee'
    Draw-Box $g 280 300 220 70 'API Gateway'
    Draw-Box $g 560 300 240 70 'leave-service'
    Draw-Box $g 860 180 220 70 'auth-service'
    Draw-Box $g 860 420 220 70 'Leave DB'
    Draw-Box $g 1140 300 220 70 'RabbitMQ'
    Draw-Box $g 1420 300 220 70 'admin-service'
    Draw-Box $g 1420 460 220 70 'Admin DB'

    Draw-Arrow $g 220 335 280 335 'POST /leave/requests'
    Draw-Arrow $g 500 335 560 335 'forward'
    Draw-Arrow $g 800 320 860 215 'get manager'
    Draw-Arrow $g 800 350 860 455 'save request + pending balance'
    Draw-Arrow $g 800 335 1140 335 'publish LeaveRequestedEvent'
    Draw-Arrow $g 1360 335 1420 335 'deliver event'
    Draw-Arrow $g 1530 370 1530 460 'create ApprovalTask'

    Save-Canvas $canvas (New-DiagramPath '04_leave_workflow.png')
}

function Draw-TimesheetWorkflow {
    $canvas = New-Canvas 1800 800
    $g = $canvas.Graphics
    Draw-Title $g 'Timesheet Submission Workflow' 1800

    Draw-Box $g 40 300 180 70 'Employee'
    Draw-Box $g 280 300 220 70 'API Gateway'
    Draw-Box $g 560 300 240 70 'timesheet-service'
    Draw-Box $g 860 180 220 70 'auth-service'
    Draw-Box $g 860 420 220 70 'Timesheet DB'
    Draw-Box $g 1140 300 220 70 'RabbitMQ'
    Draw-Box $g 1420 300 220 70 'admin-service'
    Draw-Box $g 1420 460 220 70 'Admin DB'

    Draw-Arrow $g 220 335 280 335 'POST /timesheets/submit'
    Draw-Arrow $g 500 335 560 335 'forward'
    Draw-Arrow $g 800 320 860 215 'get manager'
    Draw-Arrow $g 800 350 860 455 'save SUBMITTED timesheet'
    Draw-Arrow $g 800 335 1140 335 'publish TimesheetSubmittedEvent'
    Draw-Arrow $g 1360 335 1420 335 'deliver event'
    Draw-Arrow $g 1530 370 1530 460 'create ApprovalTask'

    Save-Canvas $canvas (New-DiagramPath '05_timesheet_workflow.png')
}

function Draw-RabbitHighLevel {
    $canvas = New-Canvas 1850 950
    $g = $canvas.Graphics
    Draw-Title $g 'RabbitMQ High-Level Event Flow' 1850

    Draw-Box $g 60 120 250 70 'timesheet-service'
    Draw-Box $g 60 250 250 70 'leave-service'
    Draw-Box $g 60 380 250 70 'auth-service'

    Draw-Box $g 420 120 250 70 'timesheet.exchange' '#FFE5EC' '#C9184A'
    Draw-Box $g 420 250 250 70 'leave.exchange' '#FFE5EC' '#C9184A'
    Draw-Box $g 420 380 250 70 'notification.exchange' '#FFE5EC' '#C9184A'
    Draw-Box $g 420 610 250 70 'admin.exchange' '#FFE5EC' '#C9184A'

    Draw-Box $g 780 120 260 70 'admin.timesheet.queue'
    Draw-Box $g 780 250 260 70 'admin.leave.queue'
    Draw-Box $g 780 380 260 70 'notification.user.registered.queue'
    Draw-Box $g 780 610 260 70 'timesheet.approval.completed'
    Draw-Box $g 780 740 260 70 'leave.approval.completed'

    Draw-Box $g 1160 250 260 80 'admin-service'
    Draw-Box $g 1520 610 250 70 'timesheet-service'
    Draw-Box $g 1520 740 250 70 'leave-service'

    Draw-Arrow $g 310 155 420 155 'TimesheetSubmittedEvent'
    Draw-Arrow $g 310 285 420 285 'LeaveRequestedEvent'
    Draw-Arrow $g 310 415 420 415 'UserRegisteredEvent'

    Draw-Arrow $g 670 155 780 155 ''
    Draw-Arrow $g 670 285 780 285 ''
    Draw-Arrow $g 670 415 780 415 ''

    Draw-Arrow $g 1040 155 1160 290 ''
    Draw-Arrow $g 1040 285 1160 290 ''
    Draw-Arrow $g 1040 415 1160 290 ''

    Draw-Arrow $g 1290 330 420 645 'ApprovalCompletedEvent'
    Draw-Arrow $g 670 645 780 645 ''
    Draw-Arrow $g 670 775 780 775 ''
    Draw-Arrow $g 1040 645 1520 645 ''
    Draw-Arrow $g 1040 775 1520 775 ''

    Save-Canvas $canvas (New-DiagramPath '06_rabbitmq_high_level.png')
}

function Draw-LeaveApprovalLowLevel {
    $canvas = New-Canvas 1700 1000
    $g = $canvas.Graphics
    Draw-Title $g 'Leave Approval Low-Level Flow' 1700

    $steps = @(
        'LeaveController.requestLeave',
        'LeaveServiceImpl.requestLeave',
        'Validate request',
        'AuthServiceClient.getManagerIdForEmployee',
        'Save LeaveRequest',
        'Update LeaveBalance.pending',
        'Create LeaveRequestedEvent',
        'RabbitTemplate.convertAndSend',
        'Exchange: leave.exchange',
        'Binding -> admin.leave.queue',
        'Queue: admin.leave.queue',
        'EventConsumer.handleLeaveRequested',
        'Create ApprovalTask',
        'Save ApprovalTask in Admin DB'
    )

    $y = 110
    foreach ($step in $steps) {
        Draw-Box $g 520 $y 660 48 $step
        if ($y -gt 110) {
            Draw-Arrow $g 850 ($y - 14) 850 $y ''
        }
        $y += 62
    }

    Save-Canvas $canvas (New-DiagramPath '07_leave_approval_low_level.png')
}

function Draw-TimesheetApprovalLowLevel {
    $canvas = New-Canvas 1700 1000
    $g = $canvas.Graphics
    Draw-Title $g 'Timesheet Approval Low-Level Flow' 1700

    $steps = @(
        'TimesheetController.submitTimesheet',
        'TimesheetServiceImpl.submitTimesheet',
        'Validate timesheet',
        'AuthServiceClient.getManagerIdForEmployee',
        'Save Timesheet as SUBMITTED',
        'Create TimesheetSubmittedEvent',
        'RabbitTemplate.convertAndSend',
        'Exchange: timesheet.exchange',
        'Binding -> admin.timesheet.queue',
        'Queue: admin.timesheet.queue',
        'EventConsumer.handleTimesheetSubmitted',
        'Create ApprovalTask',
        'Save ApprovalTask in Admin DB'
    )

    $y = 130
    foreach ($step in $steps) {
        Draw-Box $g 520 $y 660 52 $step
        if ($y -gt 130) {
            Draw-Arrow $g 850 ($y - 10) 850 $y ''
        }
        $y += 68
    }

    Save-Canvas $canvas (New-DiagramPath '08_timesheet_approval_low_level.png')
}

function Draw-DatabaseOwnership {
    $canvas = New-Canvas 1600 700
    $g = $canvas.Graphics
    Draw-Title $g 'Database Ownership by Service' 1600

    Draw-Box $g 120 220 240 70 'auth-service'
    Draw-Box $g 120 340 240 70 'timesheet-service'
    Draw-Box $g 120 460 240 70 'leave-service'
    Draw-Box $g 120 580 240 70 'admin-service'

    Draw-Box $g 520 200 900 100 'users\nmanager relationships' '#EDF6F9' '#006D77'
    Draw-Box $g 520 320 900 100 'projects\ntimesheets\ntimesheet entries' '#EDF6F9' '#006D77'
    Draw-Box $g 520 440 900 100 'leave requests\nleave balances\nleave policies\nholidays' '#EDF6F9' '#006D77'
    Draw-Box $g 520 560 900 100 'approval tasks\nreporting / policy config' '#EDF6F9' '#006D77'

    Draw-Arrow $g 360 255 520 250 ''
    Draw-Arrow $g 360 375 520 370 ''
    Draw-Arrow $g 360 495 520 490 ''
    Draw-Arrow $g 360 615 520 610 ''

    Save-Canvas $canvas (New-DiagramPath '09_database_ownership.png')
}

Draw-HighLevelArchitecture
Draw-GatewayRouting
Draw-AuthWorkflow
Draw-LeaveWorkflow
Draw-TimesheetWorkflow
Draw-RabbitHighLevel
Draw-LeaveApprovalLowLevel
Draw-TimesheetApprovalLowLevel
Draw-DatabaseOwnership

Get-ChildItem -Path $outputDir -Filter *.png | Select-Object FullName, Length
