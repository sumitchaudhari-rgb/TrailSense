# 🌲 TrailSense

> **Offline-First Emergency Trail Navigation & On-Device AI Safety Assistant**

TrailSense is a mobile application engineered for hikers, trekkers, and emergency responders operating in remote wilderness environments with zero internet or cellular connectivity. Combining **100% offline map rendering**, **satellite GPS tracking**, **categorized safe-point routing**, and an **on-device local Large Language Model (LLM)**, TrailSense provides life-saving navigation guidance and emergency assistance offline.

---

## ✨ Key Features

- 🗺️ **100% Offline Map Rendering & 360° Navigation**  
  Powered by `osmdroid`, supporting dynamic tile caching, high-DPI scaling, 360-degree pinch-to-rotate touch gestures, orientation compass needle, and dynamic distance scale bars.
- 🛰️ **Cellular-Independent GPS Satellite Tracking**  
  Connects directly to onboard device GPS satellite receivers (`LocationManager.GPS_PROVIDER`) with zero reliance on cellular towers or internet data.
- 🚨 **Categorized Emergency Safe-Point Routing**  
  Calculates real-time spherical distances (via Haversine formula) to nearby critical waypoints:
  - 💧 **Drinking Water Sources**
  - ⛺ **Emergency Shelters**
  - 🚪 **Evacuation / Trail Exits**
  - 🏥 **Medical Stations**
- 🤖 **On-Device Local AI Safety Assistant**  
  Integrates Google MediaPipe GenAI and GGUF 4-bit quantized models (`Gemma 2B` / `Llama`) for instant, offline navigation advice, emergency survival guidance, and position-grounded wayfinding.
- 🎙️ **Hands-Free Voice Input & TTS Audio Responses**  
  Integrated speech-to-text recognition (`SpeechRecognizer`) and audio text-to-speech feedback (`TextToSpeech`) for hands-free operations in critical situations.
- 🎨 **Modern Jetpack Compose UI & Glassmorphism Theme**  
  Dark mode user interface with overlay header cards, bottom chat sheets, and interactive modal dialogs built with Jetpack Compose.
- 🇮🇳 **Multilingual Fine-Tuning Pipeline**  
  Includes a fine-tuning pipeline ([phase11_generate_notebook.py](file:///c:/D_drive/SIH/TrailSense/phase11_generate_notebook.py)) for training Hindi, Marathi, and regional disaster response datasets exported to GGUF format (`Q4_K_M`).

---

## 🛠️ Technology Stack

| Layer | Technology / Library |
| :--- | :--- |
| **Language & Runtime** | Java 17, Kotlin 1.9+, Android SDK (API 28–34) |
| **UI Framework** | Jetpack Compose (Material3), XML Layout Bridge |
| **Map Engine** | `org.osmdroid:osmdroid-android:6.1.20` |
| **Location Services** | Hardware GPS Provider (`android.location.LocationManager`) |
| **Local AI Engine** | `com.google.mediapipe:tasks-genai:0.10.14` & `llama.cpp` (GGUF Q4_K_M) |
| **Voice / Audio** | Android Native `SpeechRecognizer` & `TextToSpeech` |
| **Distance Math** | Haversine Spherical Trigonometry Formula ($R = 6371 \text{ km}$) |

---

## 📂 Project Structure

```
TrailSense/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   └── waypoints.json             # Offline trail waypoints database
│   │   │   ├── java/com/trailsense/app/
│   │   │   │   ├── MainActivity.java           # Core Activity & Map Controller
│   │   │   │   ├── MainActivityComposeBridge.kt # Compose State Bridge
│   │   │   │   ├── LlmAssistant.java          # On-Device AI Engine & Grounded RAG
│   │   │   │   ├── Waypoint.java              # Waypoint Data Model
│   │   │   │   └── ui/                        # Jetpack Compose Screens & Theme
│   │   │   │       ├── TrailSenseAppScreen.kt
│   │   │   │       ├── TrailSenseHeaderCards.kt
│   │   │   │       ├── TrailSenseChatSheet.kt
│   │   │   │       ├── OnboardingMapDownloadCompose.kt
│   │   │   │       └── SafePointsPanelCompose.kt
│   │   │   └── res/ layout & drawable resources
│   └── build.gradle.kts
├── phase11_dataset.json                         # Fine-tuning disaster response dataset
├── phase11_finetune.ipynb                       # Google Colab notebook for LoRA fine-tuning
├── phase11_generate_notebook.py                 # Notebook generation script
├── build.gradle.kts                             # Top-level Gradle configuration
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio:** Jellyfish / Ladybug (2024.1+) or newer
* **JDK:** Java 17
* **Android Device / Emulator:** Running Android 9.0+ (API Level 28 or higher) with GPS enabled

### Building from Source

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/sumitchaudhari-rgb/TrailSense.git
   cd TrailSense
   ```

2. **Open in Android Studio:**
   - Launch Android Studio, select **Open**, and browse to the `TrailSense` folder.
   - Allow Gradle to sync dependencies.

3. **Build the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on Connected Android Device:**
   ```bash
   ./gradlew installDebug
   ```

---

## 🌐 Offline Map Pre-Caching

When TrailSense is launched for the first time with an active internet connection:
1. The onboarding setup uses osmdroid's `CacheManager` to pre-download ~42 MB of map tiles for a **4 km × 4 km bounding box** (`lat ± 0.04`, `lon ± 0.04`) around your starting position across zoom levels 6–17.
2. **Pre-Exploring Remote Trails:** To ensure seamless offline map coverage for a trek, open the app while online and pan/zoom over your intended route. The app automatically caches all viewed tiles to internal SQLite storage for offline use.

---

## 🧠 AI Model Fine-Tuning Pipeline

TrailSense features a local fine-tuning pipeline to train specialized Hindi/Marathi disaster response and trail guidance models:

1. **Dataset:** `phase11_dataset.json` contains specialized emergency QA pairs.
2. **Notebook Generation:** Run `python phase11_generate_notebook.py` to generate `phase11_finetune.ipynb`.
3. **Training & Export:** Open the notebook in Google Colab (T4 GPU), train with Unsloth LoRA, and export to `trailsense_hindi_marathi-Q4_K_M.gguf`.
4. **Android Deployment:** Place the exported GGUF model into `app/src/main/assets/` to load on-device inference via `LlmAssistant.java`.

---

## 📱 Cross-Platform (iOS & Android) Roadmap

An architectural plan has been designed to port TrailSense to **React Native** for simultaneous iOS & Android support:
- **Map Engine:** Migration from `osmdroid` to MapLibre Native (`@rnmaplibre/maplibre-react-native`) with offline `.mbtiles` packages.
- **Local AI Engine:** Migration to `react-native-llama` for C++ `llama.cpp` JSI inference across iOS Metal and Android NDK.

---

## 📄 License

This project is developed for emergency navigation and research purposes. Distributed under the **MIT License**.
