# Chamberly

## Local Setup

- Install [Android Studio](https://developer.android.com/studio)
- Clone this Repository

```bash
git clone https://github.com/ChamberlyAB/ChamberlyAndroid.git
cd ChamberlyAndroid
```

- Create a new project in Firebase.
- Enable cloud firestore, realtime database, authentication (anonymous), firebase analytics and crashlytics for the project through firebase console.
- Download google-services.json
- Place the google-services.json under `app` folder
- Open Android Studio
- Run Code in Emulator

## Generating APK for sharing

Windows

```bash
./gradlew.bat assembleDebug
```

Linux

```bash
./gradlew assembleDebug
```

This creates an APK named `module_name-debug.apk` in `project_name/module_name/build/outputs/apk/`

## Contributing

- Make sure you don't push your changes directly to `main` branch.
- Checkout `main` branch

```bash
git checkout main
```

- Always pull the latest code before making changes

```bash
git pull origin main
```

- Create your topic branch from `main` branch

```bash
git checkout -b <your_branch_name>
```

- Make your changes to code and test them
- Stage, Commit and Push  your changes

```bash
git add .
git commit -m "commit text"
git push
```

- Create a Pull Request with your topic branch as source branch and `main` as your target branch
- Get your Pull Request reviewed by other developers
- After your Pull Request is approved by other developers, merge your changes
