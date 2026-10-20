# Shows a Windows toast when Claude Code finishes a turn (Stop hook).
# Falls back to a tray balloon if toasts are unavailable.
param(
    [string]$Title = "Claude Code",
    [string]$Message = ""
)

$project = Split-Path -Leaf ($env:CLAUDE_PROJECT_DIR, (Get-Location).Path | Where-Object { $_ } | Select-Object -First 1)
if (-not $Message) { $Message = "Finished working on $project - your turn." }

function Escape-Xml([string]$s) { [System.Security.SecurityElement]::Escape($s) }

try {
    [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
    [Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType = WindowsRuntime] | Out-Null

    $xml = New-Object Windows.Data.Xml.Dom.XmlDocument
    $xml.LoadXml(@"
<toast>
  <visual>
    <binding template="ToastGeneric">
      <text>$(Escape-Xml $Title)</text>
      <text>$(Escape-Xml $Message)</text>
    </binding>
  </visual>
  <audio src="ms-winsoundevent:Notification.Default"/>
</toast>
"@)
    # Windows PowerShell's registered AppUserModelID, so the toast is allowed to show.
    $appId = '{1AC14E77-02E7-4E5D-B744-2EB1AE5198B7}\WindowsPowerShell\v1.0\powershell.exe'
    [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier($appId).Show(
        [Windows.UI.Notifications.ToastNotification]::new($xml))
}
catch {
    Add-Type -AssemblyName System.Windows.Forms
    $icon = New-Object System.Windows.Forms.NotifyIcon
    $icon.Icon = [System.Drawing.SystemIcons]::Information
    $icon.Visible = $true
    $icon.ShowBalloonTip(5000, $Title, $Message, [System.Windows.Forms.ToolTipIcon]::Info)
    Start-Sleep -Seconds 6
    $icon.Dispose()
}
