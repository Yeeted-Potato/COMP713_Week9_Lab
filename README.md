# COMP713 Week 9 Lab Starter

This starter project supports the Android networking and location lab. It runs before any activity is completed and deliberately contains no networking or location implementation.

## Requirements

- Android Studio 2024.1 or later
- Android SDK 34 installed
- Medium Phone emulator running Android 15 (API 35)
- Java 17 (the embedded Android Studio JDK is suitable)

The project uses Android Gradle Plugin 8.5.2 and Gradle 8.7. It has no external Android library dependencies.

## Open and run

1. Extract the ZIP file to a normal writable folder.
2. In Android Studio, choose **Open** and select the extracted `COMP713_Week9_Lab_Starter` folder.
3. Allow Gradle sync to finish.
4. Start the Medium Phone API 35 emulator.
5. Run the `app` configuration.

The starter screen should open. Tapping either button should report that its feature is not implemented yet.

## Main files

- `app/src/main/java/nz/ac/aut/comp713/week9lab/MainActivity.java`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/AndroidManifest.xml`

Search for `TODO Activity` in `MainActivity.java` to find the first changes.

Do not add a Maps API key. The lab uses the Android platform location API and an emulator-injected position.
