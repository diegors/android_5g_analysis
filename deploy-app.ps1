# PowerShell script to run emulator and install APK

# Start emulator with medium_phone AVD
Write-Host "Starting emulator with medium_phone AVD..."
emulator -avd medium_phone

# Wait for emulator to fully boot
Write-Host "Waiting for emulator to fully boot..."
Start-Sleep -Seconds 10

# Uninstall the app
Write-Host "Uninstalling existing app..."
adb uninstall com.example.signalchecker

# Install the APK
Write-Host "Installing app-debug.apk..."
adb install -r .\app-debug.apk

Write-Host "Done!"
