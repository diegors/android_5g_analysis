# Cellular Signal Checker

An Android 16 (API 36) application designed to monitor 5G cellular signal strength periodically.

## Features

- **Real-time Signal Monitoring**: View current signal metrics including RSRP, RSRQ, SINR, DBm, and ASU for both 5G NR and 4G LTE.
- **Detailed Cell Information**: Display LTE cell identity (CI, PCI, TAC), mobile country code (MCC), mobile network code (MNC), and frequency bands.
- **GPS Location Tracking**: Show current GPS location (latitude, longitude) with accuracy in meters.
- **Timestamp Display**: Every signal reading includes a precise timestamp.
- **Wi-Fi Signal Page**: Dedicated screen to view Wi-Fi SSID, BSSID, RSSI, signal level, and link speed.
- **Background Monitoring**: Uses `WorkManager` to check signal strength every X minutes (minimum 15 minutes).
- **History Logs**: Keeps track of the last 50 signal checks in memory and history.
- **Data Export**: Export all signal data to `signal_results.csv` with automatic `.txt` copy in Downloads folder. Appends all results to the same files.
- **Comprehensive CSV Export**: Includes 23 columns: timestamp, network type, registration status, signal metrics (RSRP/RSRQ/SINR/RSSI), cell info, location data, and more.
- **Notifications**: System notifications on each background check completion.

## Permissions

The app requires the following permissions to function correctly:
- `ACCESS_FINE_LOCATION`: Required to access GPS location and cellular network information.
- `ACCESS_COARSE_LOCATION`: Required for basic location-based signal access.
- `ACCESS_WIFI_STATE`: Required to read current Wi-Fi connection information.
- `READ_PHONE_STATE`: Required to detect cellular network type and cell identity.
- `POST_NOTIFICATIONS`: Required to show results from background checks.
- `WRITE_EXTERNAL_STORAGE`: Required on Android 9 and below for file export.

## Signal Data Fields

The app collects and displays the following information:

### Network & Registration
- **Network Type**: 5G NR, 4G LTE, or combined
- **Registered**: Whether the device is registered on the network
- **Signal Level**: 0–4 rating of signal strength

### Signal Quality Metrics
- **RSRP** (Reference Signal Received Power): dBm units (5G and 4G)
- **RSRQ** (Reference Signal Received Quality): dB units (5G and 4G)
- **SINR** (Signal-to-Interference+Noise Ratio): dB units (5G and 4G)
- **RSSI** (Received Signal Strength Indicator): dBm units
- **DBm**: Signal power in decibels relative to 1 milliwatt
- **ASU** (Arbitrary Strength Unit): 0–31 scale

### Cell Identity
- **CI** (Cell ID): Unique cell identifier
- **PCI** (Physical Cell ID): Physical cell identifier
- **TAC** (Tracking Area Code): Geographic tracking area
- **MCC** (Mobile Country Code): Country code
- **MNC** (Mobile Network Code): Network operator code
- **Bands**: Frequency bands in use

### Timing & Location
- **Timing Advance**: Signal propagation delay (4G only)
- **Timestamp**: UTC time of the reading
- **Latitude/Longitude**: GPS coordinates (6 decimal precision)
- **Location Accuracy**: GPS accuracy in meters

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

## Data Export

### CSV/TXT Export
- Tap the **Share** button (📤) in the top bar to export all recorded signal data.
- Two files are automatically created/updated in the Downloads folder:
  - `signal_results.csv` — Full data in comma-separated values format
  - `signal_results.txt` — Identical copy with plain text MIME type
- All new measurements are **appended** to existing files (no data loss).
- CSV includes 23 columns covering all signal metrics, cell info, and GPS location.
- Bands are pipe-separated within cells (e.g., `3|5|20` for bands 3, 5, and 20).

### CSV Export Format
```
Timestamp,Network Type,Registered,Level,DBm,ASU,...,Latitude,Longitude,Location Accuracy (m)
2024-07-21 14:23:45,4G LTE,true,3,-85,24,...,52.520008,13.404954,15.5
```

## Tech Stack

- **Target**: Android 16 (API 36)
- **Min SDK**: API 24 (Android 7.0)
- **UI**: Jetpack Compose (Material 3)
- **Background Processing**: WorkManager
- **Architecture**: MVVM with Kotlin Flow
- **Serialization**: Kotlinx Serialization
- **Location**: Android LocationManager (GPS, Network, Passive providers)
- **Language**: Kotlin
- **JVM Target**: Java 21
