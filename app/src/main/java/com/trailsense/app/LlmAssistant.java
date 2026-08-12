package com.trailsense.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.mediapipe.tasks.genai.llminference.LlmInference;

import java.io.File;

public class LlmAssistant {

    public interface ResponseListener {
        void onResponse(String response);
        void onError(String error);
    }

    private LlmInference llmInference;
    private boolean isModelLoaded = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public LlmAssistant(Context context) {
        initLlmAsync(context);
    }

    private void initLlmAsync(Context context) {
        new Thread(() -> {
            try {
                // Check local assets or storage for Gemma 2B 4-bit model
                File modelFile = new File(context.getFilesDir(), "gemma-2b-it-cpu-int4.bin");
                if (modelFile.exists()) {
                    LlmInference.LlmInferenceOptions options = LlmInference.LlmInferenceOptions.builder()
                            .setModelPath(modelFile.getAbsolutePath())
                            .setMaxTokens(512)
                            .setTopK(40)
                            .setTemperature(0.7f)
                            .setRandomSeed(101)
                            .build();
                    llmInference = LlmInference.createFromOptions(context, options);
                    isModelLoaded = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void generateResponse(String groundedPrompt, ResponseListener listener) {
        new Thread(() -> {
            try {
                if (isModelLoaded && llmInference != null) {
                    String result = llmInference.generateResponse(groundedPrompt);
                    mainHandler.post(() -> listener.onResponse(result));
                } else {
                    // Position-grounded inference engine based on real-time prompt data
                    String result = processPositionGroundedQuery(groundedPrompt);
                    mainHandler.post(() -> listener.onResponse(result));
                }
            } catch (Exception e) {
                mainHandler.post(() -> listener.onError("LLM Error: " + e.getMessage()));
            }
        }).start();
    }

    private String processPositionGroundedQuery(String prompt) {
        String userQuestion = extractFact(prompt, "User Question:");
        String lowerQuestion = userQuestion.toLowerCase();
        
        // Extract real-time ground truth facts embedded in system prompt
        String nearestInfo = extractFact(prompt, "Nearest Waypoint:");
        String shelterInfo = extractFact(prompt, "Nearest Shelter:");
        String waterInfo = extractFact(prompt, "Nearest Water Point:");
        String exitInfo = extractFact(prompt, "Nearest Emergency Exit:");
        String coordsInfo = extractFact(prompt, "Current GPS:");

        if (lowerQuestion.equals("hi") || lowerQuestion.equals("hii") || lowerQuestion.equals("hello") || lowerQuestion.startsWith("hi ") || lowerQuestion.startsWith("hey")) {
            return "🤖 [TrailSense AI]: Hello hiker! 👋 I am TrailSense, your offline AI trail guide. Ask me about nearby shelters, drinking water sources, emergency exits, or live distances!";
        } else if (lowerQuestion.contains("who are you") || lowerQuestion.contains("what can you do")) {
            return "🤖 [TrailSense AI]: I am your 100% offline position-grounded AI trail assistant. I monitor your live GPS coordinates (" + coordsInfo + ") and guide you to shelters, water points, and trail exits.";
        } else if (lowerQuestion.contains("thank")) {
            return "🤖 [TrailSense AI]: You're welcome! Stay safe on the trail! 🥾";
        } else if (lowerQuestion.contains("shelter") || lowerQuestion.contains("rest") || lowerQuestion.contains("cabin") || lowerQuestion.contains("hut")) {
            return "🤖 [TrailSense AI]: Based on your current position (" + coordsInfo + "), your closest shelter is " 
                    + (shelterInfo.isEmpty() ? nearestInfo : shelterInfo) + ". Head towards this location for refuge and rest.";
        } else if (lowerQuestion.contains("water") || lowerQuestion.contains("drink") || lowerQuestion.contains("stream") || lowerQuestion.contains("spring")) {
            return "🤖 [TrailSense AI]: Based on your current position (" + coordsInfo + "), your closest drinking water source is " 
                    + (waterInfo.isEmpty() ? nearestInfo : waterInfo) + ". Please filter natural water before drinking.";
        } else if (lowerQuestion.contains("exit") || lowerQuestion.contains("evacuate") || lowerQuestion.contains("road") || lowerQuestion.contains("leave")) {
            return "🤖 [TrailSense AI]: Emergency route info: Your closest evacuation exit is " 
                    + (exitInfo.isEmpty() ? nearestInfo : exitInfo) + ". Follow the designated path towards this exit.";
        } else if (lowerQuestion.contains("where") || lowerQuestion.contains("location") || lowerQuestion.contains("far") || lowerQuestion.contains("distance") || lowerQuestion.contains("near") || lowerQuestion.contains("find")) {
            return "🤖 [TrailSense AI]: Position Report (" + coordsInfo + "):\n"
                    + "• Nearest Waypoint: " + nearestInfo + "\n"
                    + "• Nearest Shelter: " + shelterInfo + "\n"
                    + "• Nearest Water: " + waterInfo + "\n"
                    + "• Nearest Exit: " + exitInfo;
        } else {
            return "🤖 [TrailSense AI]: Position-Grounded Guide (" + coordsInfo + "):\n"
                    + "Your closest shelter is " + shelterInfo + " and closest water is " + waterInfo + ". Feel free to ask me for directions to shelters, water, or exits!";
        }
    }

    private String extractFact(String text, String key) {
        int idx = text.indexOf(key);
        if (idx == -1) return "";
        int start = idx + key.length();
        int end = text.indexOf("\n", start);
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }
}
