# 5G Signal Checker

An Android 16 (API 36) application designed to monitor 5G cellular signal strength periodically.

## Features

- **Real-time 5G Monitoring**: View current SS-RSRP, SS-SINR, and network type.
- **Wi-Fi Signal Page**: Open a dedicated screen to view current Wi-Fi details such as SSID, BSSID, RSSI, signal level, and link speed.
- **Background Tracking**: Uses `WorkManager` to check signal strength every X minutes (minimum 15 minutes as per system limits).
- **History Logs**: Keeps track of the last 50 signal checks.
- **Data Export**: Export signal history as a CSV file via the share button.
- **Notifications**: Get a system notification each time a background check is performed.

## Permissions

The app requires the following permissions to function correctly:
- `ACCESS_FINE_LOCATION`: Required to access cellular network information.
- `ACCESS_COARSE_LOCATION`: Required for basic location-based signal access.
- `ACCESS_WIFI_STATE`: Required to read current Wi-Fi connection information on the new Wi-Fi signal page.
- `READ_PHONE_STATE`: Required to detect the cellular network type.
- `POST_NOTIFICATIONS`: Required to show results from background checks.

## How to Test

### 1. Using the Android Emulator
To simulate 5G conditions on an emulator:
1.  Start an Android emulator (API 30 or higher).
2.  Open the **Extended Controls** (click the three dots `...` in the sidebar).
3.  Go to the **Cellular** tab.
4.  Set **Network type** to `NR` (New Radio / 5G).
5.  Adjust the **Signal strength** slider to see values change in the app.

### 2. Testing Background Checks
Since `WorkManager` may delay periodic tasks for battery optimization:
-   Set the interval to `15` minutes in the UI.
-   Start Monitoring.
-   You can force the worker to run immediately via ADB for debugging:
    ```bash
    adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS -p com.example.signalchecker
    ```

### 3. Local Build
To build the project from the command line:
```bash
./gradlew assembleDebug
```

### 4. Using the New Wi-Fi Screen
1. Launch the app on a device or emulator with Wi-Fi enabled.
2. Open the main screen and tap **View Wi-Fi Signal**.
3. Review the current Wi-Fi connection details shown on the new page.

## Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Background Processing**: WorkManager
- **Architecture**: MVVM
- **Serialization**: Kotlinx Serialization
- **Language**: Kotlin (JDK 21)
