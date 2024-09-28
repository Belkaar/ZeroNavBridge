# ZeroNavBridge
This is an Android app that reads navigation instructions from supported navigation apps and sends it via Bluetooth to a Zero Motorcycles Bike (Cipher III).
Supported apps:
* Kurviger (preliminary limited support)
* Google Maps (works reasonably well)
* OsmAnd (preliminary limited support)

## Usage
Install the APK and open the app to set it up. After that it will work completely in the background.
The APK can be found on the "Releases" area to the right in GitHub

## Integration
To use this bridge from another app, integrate appTest/src/main/java/team/burkart/zero/navbridge/Sender.kt.

## Contributiom
Contributions are welcome! Feel free to open issues or pull requests.

## Compatibility
Known to work on these bikes, please feel free to report more:
* DSR/X (2023)
* SR/F (2023)
* SR/S (2021-2023)
* S gen 3

## Known issues
* The bike firmware does some funky rounding sometimes on the distance to destination
* Sometimes the dash will stay on if you switch the ignition key to off and navigation instructions are displayed. Just cycle the ignition again.
* While the nav display on the bike is shown, the grip heat overlay is not shown probalby, you can still change the settings.
* Some device manufacturers have heavy battery restrictions. If the Bridge does not start automatically, apply https://dontkillmyapp.com steps to the Bridge and your navigation app.

## Privacy
The app does not store any data besides the selected settings. It does not make any network requests. It does not access files on your phone.
To use the notification integration the app needs access to your notifications. It only reads notifications for the enabled navigation app(s). It does not save notification data.