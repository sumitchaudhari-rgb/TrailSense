# TrailSense — Antigravity Build Prompts

Copy-paste task prompts for each build phase, in order. Each phase ends with a progress.md update — read it at the start of the next phase for context.

## Phase 1: Project Setup & Offline Map Library

Set up an Android Studio project (Java) for an offline navigation app called TrailSense.

Steps to complete:
1. Create a new Android Studio project using Java, minimum SDK Android 9 (API 28), targeting current stable Android.
2. Integrate an offline-capable map library — use osmdroid (OpenStreetMap-based, free, no API key needed) so maps can be rendered without depending on Google Maps' online tile service.
3. Add and configure location permissions (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION) in the manifest, with a runtime permission request flow on first launch.
4. Build a minimal one-screen app that just launches successfully and shows a blank map view placeholder.
5. Run the app on an emulator or connected device and confirm it launches without crashing and correctly prompts for location permission.

Read progress.md first if it exists. At the end of this phase, create progress.md in the project root (if it doesn't exist) and append an entry recording: phase name and date, what was completed, verification result (pass/fail with actual output/screenshot description), any issues hit and how they were resolved, and what the next phase should pick up.

Do not proceed to GPS logic or route data yet — this phase is project scaffolding only. Stop after the app launches successfully and progress.md is updated. Report the result to me.

## Phase 2: Live GPS Positioning (No Map Yet)

Implement live GPS location reading in the TrailSense app, independent of the map UI for now.

Steps to complete:
1. Read progress.md to see what was completed in Phase 1.
2. Implement location updates using FusedLocationProviderClient (Google Play Services) or, if avoiding Play Services dependency, Android's built-in LocationManager.
3. Display the live latitude/longitude on screen as plain text (no map needed yet), updating in real time as position changes.
4. Add basic handling for GPS not yet available (e.g. "Acquiring GPS signal...").
5. Test instructions for me: I will enable airplane mode, keep GPS/location toggled on, and walk or use a GPS simulator to confirm coordinates update with zero network connection. Wait for me to confirm this works before proceeding.

Update progress.md with what was completed, the verification result once I confirm it, any issues and resolutions, and what Phase 3 should pick up.

Stop after live coordinates are displaying correctly and I've confirmed the offline GPS test. Report the result to me.

## Phase 3: Offline Map Rendering + Live Position Marker

Add offline map rendering to TrailSense and plot the live GPS position on it.

Steps to complete:
1. Read progress.md to see what was completed in Phases 1-2.
2. Set up osmdroid to use a small pre-downloaded offline map tile set for one test area (I will specify the area — start with a small local park or trail near me for testing).
3. Render this offline map as the main screen.
4. Plot the live GPS position (from Phase 2's location logic) as a marker on the map, updating in real time.
5. Confirm the map renders and the marker updates using only the bundled offline tiles, with no network calls for map tiles at runtime.

Test instructions for me: I will walk with the device (or simulate GPS movement) and confirm the marker moves accurately on the offline map with zero connectivity.

Update progress.md with results and what Phase 4 should pick up. Stop after I confirm the marker tracks correctly offline. Report the result to me.

## Phase 4: Route/Waypoint Data Structure (One Test Route)

Create a structured offline dataset of waypoints for one test route and load it into the TrailSense app.

Steps to complete:
1. Read progress.md to see what was completed in Phases 1-3.
2. Design a simple JSON schema for waypoints, each with: id, name, category (e.g. "shelter", "water", "exit", "viewpoint"), latitude, longitude, and an optional short description.
3. Create a sample JSON file with 5-8 waypoints for the same test area/route used in Phase 3 (I will provide real coordinates, or use placeholder nearby points for now).
4. Bundle this JSON file with the app (local asset, not downloaded) and write loading logic to parse it at app startup.
5. Render each waypoint as a marker on the offline map from Phase 3, visually distinct from the live position marker (e.g. different icon/color per category).

Test instructions for me: I will visually confirm all waypoints appear on the map at their correct real-world locations.

Update progress.md with results and what Phase 5 should pick up. Stop after waypoints render correctly. Report the result to me.

## Phase 5: Position-to-Route Matching Logic

Implement distance calculation and route-matching logic between the live GPS position and the bundled waypoints.

Steps to complete:
1. Read progress.md to see what was completed in Phases 1-4.
2. Write a function that calculates real-world distance (use the Haversine formula) between the current GPS position and each waypoint from Phase 4.
3. Implement logic to find and return the nearest waypoint overall, and optionally the nearest waypoint per category (nearest shelter, nearest water point, etc).
4. Add a simple on-screen debug display showing: nearest waypoint name, category, and calculated distance, updating live as position changes.
5. Optionally implement basic "snap to route" logic if waypoints represent a linear trail, so position can be expressed as "X km along the route" not just raw distance to a point.

Test instructions for me: I will manually measure real-world distance to a waypoint (using a separate mapping tool) and compare it against the app's calculated distance to verify accuracy within a reasonable margin.

Update progress.md with results and what Phase 6 should pick up. Stop after I confirm calculated distances are accurate. Report the result to me.

## Phase 6: Local LLM Integration with Position Grounding

This is the core differentiator phase — the test here matters more than any other phase. Do not skip or rush the verification step.

Integrate a quantized on-device LLM into TrailSense and ground its answers in the real position/distance data from Phase 5.

Steps to complete:
1. Read progress.md to see what was completed in Phases 1-5.
2. Integrate Google's MediaPipe LLM Inference API for Android.
3. Download and bundle a quantized small model — start with Gemma 2B (4-bit, .task/.bin format required by MediaPipe) — as a local asset or one-time downloadable file (not called live from the internet at inference time).
4. Build a simple chat-style UI: a text input box, "Ask" button, and response display area.
5. Before sending the user's question to the LLM, construct a prompt that inserts the REAL current data from Phase 5's matching logic — e.g. "User's current position: [coordinates]. Nearest shelter: '[name]', [X] km away, bearing [direction]." — followed by the user's actual question.
6. Send this combined prompt to the on-device LLM and display its response.

Test instructions for me — this is the most important test in the whole project: I will stand at two different points along the test route and ask the same question (e.g. "how far to the next shelter?") at each point. The two answers must reflect genuinely different, accurate distances based on my real position — not similar or generic answers. If the answers don't change correctly between the two positions, do not proceed — the position data isn't reaching the prompt correctly, and this must be fixed before any later phase is attempted.

Update progress.md with results, including the actual before/after test answers from both positions, and what Phase 7 should pick up. Report the result to me and wait for my confirmation before proceeding.

## Phase 7: Full Offline Verification

Manual test phase — no new code unless issues are found.

Verify that the complete TrailSense app (Phases 1-6) works with zero network connectivity end to end.

Steps to complete:
1. Read progress.md to see what was completed in Phases 1-6.
2. Do not build new features in this phase — this is a verification and bug-fix phase only.
3. If I report any issue during offline testing (crash, incorrect answer, map not loading, GPS not updating), diagnose and fix it, then ask me to re-test.

Test instructions for me: I will switch the device to airplane mode (leaving GPS/location toggled on separately), walk the test route or simulate GPS movement, and ask several position-based questions through the app. I am checking for: zero crashes, live position tracking still works, and LLM answers still correctly reflect my real position — all with no network connection at all.

Update progress.md with the final verification result once I confirm everything works, and note this as the core proof-of-concept milestone for the project. Report the result to me.

## Phase 8: Voice Input/Output

Add offline voice input and output to TrailSense for hands-free use.

Steps to complete:
1. Read progress.md to see what was completed in Phases 1-7.
2. Integrate Android's built-in SpeechRecognizer for voice-to-text input, configured to work offline where the device supports offline language packs.
3. Integrate Android's built-in TextToSpeech for reading LLM responses aloud.
4. Add a microphone button to the chat UI from Phase 6 as an alternative to typing.
5. Ensure the voice input feeds into the exact same position-grounded prompt logic from Phase 6 — no separate code path.

Test instructions for me: I will repeat the Phase 7 offline test, but using voice questions instead of typed text, and confirm quality holds up under the same fully offline conditions.

Update progress.md with results and what Phase 9 should pick up. Report the result to me.

## Phase 9: Nearest Safe-Point Finder & Polish

Build a dedicated nearest-safe-point feature and polish the overall UI.

Steps to complete:
1. Read progress.md to see what was completed in Phases 1-8.
2. Add a UI element (e.g. a button or quick-access panel) that, when tapped, immediately shows the nearest waypoint for each category (nearest shelter, nearest water point, nearest exit) without requiring the user to type a question.
3. Reuse the matching logic from Phase 5 for this — do not duplicate distance calculation code.
4. Apply basic UI polish: consistent styling, clear iconography per waypoint category, readable text sizing for outdoor/sunlight use.

Test instructions for me: I will test from several different positions along the route and confirm the nearest-point-per-category feature returns correct results at each position.

Update progress.md with results and what Phase 10 should pick up. Report the result to me.

## Phase 10: Opportunistic Sync (Build Last, Lowest Priority)

Implement optional background sync for route updates and alerts, without ever affecting core offline functionality.

Steps to complete:
1. Read progress.md to see what was completed in Phases 1-9.
2. Implement a background check for network connectivity that, when available, attempts to sync updated route/waypoint data or safety alerts from a placeholder remote source (a local test JSON file simulating a server response is fine for now).
3. Ensure this sync process runs asynchronously and never blocks app startup, GPS tracking, map rendering, or LLM Q&A — all core features must work identically whether sync succeeds, fails, or there is no connectivity at all.
4. Add a small, unobtrusive UI indicator showing last sync status (e.g. "Last updated: [date]" or "Offline — using bundled data").

Test instructions for me: I will test the app with connectivity on, with connectivity off, and with a simulated failed sync, and confirm the app behaves identically for all core features in every case.

Update progress.md with results and what Phase 11 should pick up. Report the result to me.

## Phase 11: Hindi/Marathi Fine-Tuning for Native-Language Answers

This phase runs primarily in Python on your laptop (not inside the Android project) using the same QLoRA/Unsloth pipeline from the SmartTutor plan, then the fine-tuned model is swapped into the Android app.

Fine-tune the on-device LLM used in TrailSense to answer navigation and safety questions fluently in Hindi and Marathi, not just English.

Steps to complete:
1. Read progress.md to see what was completed in Phases 1-10.
2. Set up a Python environment (separate from the Android project) with Unsloth, PEFT, TRL, and bitsandbytes, verifying CUDA/GPU detection first.
3. I will provide (or we will jointly generate) a dataset of 200-500 navigation/safety-style question-answer pairs in Hindi and Marathi, formatted as instruction-tuning pairs, grounded in route-style context similar to Phase 6's prompt structure.
4. Fine-tune the same base model used in Phase 6 (Gemma 2B or Phi-3-mini) using QLoRA via Unsloth on the available GPU.
5. Evaluate the fine-tuned model against the same position-based test questions from Phase 6, in Hindi and Marathi, comparing answers before vs. after fine-tuning for fluency and accuracy.
6. If and only if the fine-tuned model shows genuine improvement, merge the LoRA adapter into the base model and quantize to 4-bit GGUF for deployment.
7. Swap the fine-tuned quantized model into the Android app from Phase 6, replacing the English-only model, and re-run the Phase 7 offline verification test in Hindi/Marathi.

Test instructions for me: I will review before/after answers in-language for fluency and accuracy, and only approve replacing the English-only model if the fine-tuned version is clearly better, not just different.

Update progress.md with the final results of the fine-tuning comparison and the outcome of the in-language offline verification test. Report the result to me.
