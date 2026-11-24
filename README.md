# Beat BPM (Android)

A compact metronome and beat-accuracy training app project.


## Features in this version
- BPM 40–240; slider, ±1, ±5 and numeric entry
- Audible metronome
- Audible timing test: 4-beat count-in, then 16 beats with sound
- Silent timing test: 4-beat audible count-in, then 16 silent target beats
- Adjustable pass tolerance: ±10/20/30/50/100/200 ms
- Tap anywhere in the large practice area; result shows individual tap offsets and average absolute error

## Cloud APK build (GitHub Actions)
1. Upload all project files and folders (not the ZIP file itself) into the root of a GitHub repository.
2. Ensure `.github/workflows/android.yml` is included.
3. Open the repository's **Actions** tab, choose **Build BeatBPM APK**, then **Run workflow**. A push to `main` also triggers a build.
4. When the build finishes, open the run, scroll to **Artifacts**, and download `BeatBPM-debug-apk`.
5. Extract the downloaded artifact ZIP to find `app-debug.apk`, then install it on Android.


## Measurement note
This version uses Android touch-event timestamps and software scheduling. Device touch/audio latency can affect millisecond accuracy, so it is intended for practice, not laboratory measurement. Silent mode's four-beat count-in anchors the expected sequence; 16 taps are scored against that tempo.
