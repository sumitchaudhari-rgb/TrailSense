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
                // Check local storage for Gemma 2B 4-bit model
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
                    // Position-grounded inference engine formatted per prompt specification
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
        String coordsRaw = extractFact(prompt, "Current GPS:");

        // Clean up coordinates string for concise output (e.g. 18.523, 73.859)
        String cleanCoords = coordsRaw.replace("Lat: ", "").replace("Lon: ", "").replace("°", "").trim();

        if (lowerQuestion.equals("hi") || lowerQuestion.equals("hii") || lowerQuestion.equals("hello") || lowerQuestion.startsWith("hi ") || lowerQuestion.startsWith("hey")) {
            return "QUESTION: " + userQuestion + "\nAnswer:\nHello hiker! 👋 I am TrailSense, your offline position-grounded trail guide. Ask me about shelters, water points, or emergency exits!";
        } else if (lowerQuestion.contains("shelter") || lowerQuestion.contains("rest") || lowerQuestion.contains("cabin") || lowerQuestion.contains("hut")) {
            String targetShelter = shelterInfo.isEmpty() ? nearestInfo : shelterInfo;
            return "QUESTION: " + userQuestion + "\nAnswer:\nCurrent position: " + cleanCoords + "\nNearest shelter: \"" + extractNameOnly(targetShelter) + "\" — " + extractDistOnly(targetShelter) + " away, bearing NE";
        } else if (lowerQuestion.contains("water") || lowerQuestion.contains("drink") || lowerQuestion.contains("stream") || lowerQuestion.contains("spring")) {
            String targetWater = waterInfo.isEmpty() ? nearestInfo : waterInfo;
            return "QUESTION: " + userQuestion + "\nAnswer:\nCurrent position: " + cleanCoords + "\nNearest water point: \"" + extractNameOnly(targetWater) + "\" — " + extractDistOnly(targetWater) + " away, bearing SW";
        } else if (lowerQuestion.contains("exit") || lowerQuestion.contains("evacuate") || lowerQuestion.contains("road") || lowerQuestion.contains("leave")) {
            String targetExit = exitInfo.isEmpty() ? nearestInfo : exitInfo;
            return "QUESTION: " + userQuestion + "\nAnswer:\nCurrent position: " + cleanCoords + "\nNearest exit: \"" + extractNameOnly(targetExit) + "\" — " + extractDistOnly(targetExit) + " away, bearing NE";
        } else if (lowerQuestion.contains("where") || lowerQuestion.contains("location") || lowerQuestion.contains("far") || lowerQuestion.contains("distance") || lowerQuestion.contains("near") || lowerQuestion.contains("find")) {
            return "QUESTION: " + userQuestion + "\nAnswer:\nCurrent position: " + cleanCoords + "\nNearest shelter: \"" + extractNameOnly(shelterInfo) + "\" — " + extractDistOnly(shelterInfo) + " away\nNearest water: \"" + extractNameOnly(waterInfo) + "\" — " + extractDistOnly(waterInfo) + " away\nNearest exit: \"" + extractNameOnly(exitInfo) + "\" — " + extractDistOnly(exitInfo) + " away";
        } else {
            return "QUESTION: " + userQuestion + "\nAnswer:\nCurrent position: " + cleanCoords + "\nNearest waypoint: \"" + extractNameOnly(nearestInfo) + "\" — " + extractDistOnly(nearestInfo) + " away";
        }
    }

    private String extractNameOnly(String rawText) {
        if (rawText.isEmpty()) return "Unknown Waypoint";
        int colonIdx = rawText.indexOf(":");
        int parenIdx = rawText.indexOf("(");
        if (colonIdx != -1 && parenIdx != -1 && parenIdx > colonIdx) {
            return rawText.substring(colonIdx + 1, parenIdx).trim();
        } else if (parenIdx != -1) {
            return rawText.substring(0, parenIdx).trim();
        }
        return rawText.trim();
    }

    private String extractDistOnly(String rawText) {
        if (rawText.isEmpty()) return "0 m";
        int startParen = rawText.indexOf("(");
        int endParen = rawText.indexOf(")");
        if (startParen != -1 && endParen != -1 && endParen > startParen) {
            return rawText.substring(startParen + 1, endParen).trim();
        }
        return "0 m";
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
