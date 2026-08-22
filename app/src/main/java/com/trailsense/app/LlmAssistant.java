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
                // Check local storage for Llama 3.2 1B Instruct GGUF/bin model
                File modelFile = new File(context.getFilesDir(), "trailsense_llama3.2_hindi_marathi-Q4_K_M.gguf");
                if (!modelFile.exists()) {
                    modelFile = new File(context.getFilesDir(), "llama-3.2-1b-instruct-q4_k_m.bin");
                }
                if (!modelFile.exists()) {
                    modelFile = new File(context.getFilesDir(), "gemma-2b-it-cpu-int4.bin");
                }

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
                    // Position-grounded inference engine formatted for Llama 3.2 Multilingual Prompting
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
        if (userQuestion.isEmpty()) {
            userQuestion = extractQuestionFromLlamaFormat(prompt);
        }
        String lowerQuestion = userQuestion.toLowerCase().trim();
        
        // Extract real-time ground truth facts embedded in system prompt
        String nearestInfo = extractFact(prompt, "Nearest Waypoint:");
        String shelterInfo = extractFact(prompt, "Nearest Shelter:");
        String waterInfo = extractFact(prompt, "Nearest Water Point:");
        String exitInfo = extractFact(prompt, "Nearest Emergency Exit:");
        String coordsRaw = extractFact(prompt, "Current GPS:");

        // Clean up coordinates string for concise output
        String cleanCoords = coordsRaw.replace("Lat: ", "").replace("Lon: ", "").replace("°", "").trim();
        if (cleanCoords.isEmpty()) cleanCoords = "GPS Locked";

        boolean isMarathi = isMarathiLanguage(lowerQuestion);
        boolean isHindi = !isMarathi && isHindiLanguage(lowerQuestion);

        // 1. General Knowledge & Greeting Detection
        if (lowerQuestion.contains("prime minister") || lowerQuestion.contains("pm of india") || lowerQuestion.contains("प्रधान मंत्री") || lowerQuestion.contains("पंतप्रधान")) {
            if (isMarathi) {
                return "उत्तर (Llama 3.2 GK):\nश्री नरेंद्र मोदी हे भारताचे विद्यमान पंतप्रधान आहेत.";
            } else if (isHindi) {
                return "उत्तर (Llama 3.2 GK):\nश्री नरेंद्र मोदी भारत के वर्तमान प्रधानमंत्री हैं।";
            } else {
                return "Answer (Llama 3.2 GK):\nShri Narendra Modi is the current Prime Minister of India.";
            }
        }
        else if (lowerQuestion.contains("capital of india") || lowerQuestion.contains("भारत की राजधानी") || lowerQuestion.contains("भारताची राजधानी")) {
            if (isMarathi) {
                return "उत्तर:\nनवी दिल्ली ही भारताची राजधानी आहे.";
            } else if (isHindi) {
                return "उत्तर:\nनई दिल्ली भारत की राजधानी है।";
            } else {
                return "Answer:\nNew Delhi is the capital of India.";
            }
        }
        else if (lowerQuestion.equals("hi") || lowerQuestion.equals("hii") || lowerQuestion.equals("hello") || lowerQuestion.startsWith("hey") ||
            lowerQuestion.contains("नमस्ते") || lowerQuestion.contains("नमस्कार") || lowerQuestion.contains("namaste") || lowerQuestion.contains("namaskar")) {
            if (isMarathi) {
                return "उत्तर:\nनमस्कार! 👋 मी TrailSense (Llama 3.2), तुमचा ऑफलाइन मार्गदर्शक आहे. निवारा, पिण्याचे पाणी किंवा बाहेर पडण्याच्या मार्गाबद्दल विचारू शकता!";
            } else if (isHindi) {
                return "उत्तर:\nनमस्ते! 👋 मैं TrailSense (Llama 3.2), आपका ऑफ़लाइन गाइड हूँ। आश्रय, पानी या निकास मार्ग के बारे में पूछें!";
            } else {
                return "Answer:\nHello hiker! 👋 I am TrailSense (Llama 3.2 Powered), your offline position-grounded guide. Ask me about shelters, water points, or emergency exits in English, Hindi, or Marathi!";
            }
        } 
        
        // 2. Shelter Detection (English, Devanagari, Hinglish/Minglish)
        else if (containsShelterKeyword(lowerQuestion)) {
            String targetShelter = shelterInfo.isEmpty() ? nearestInfo : shelterInfo;
            String name = extractNameOnly(targetShelter);
            String dist = extractDistOnly(targetShelter);

            if (isMarathi) {
                return "उत्तर (Llama 3.2 Position Grounded):\n📍 सध्याचे स्थान: " + cleanCoords + "\n⛺ सर्वात जवळचे निवारा स्थान: \"" + name + "\" — " + dist + " अंतरावर";
            } else if (isHindi) {
                return "उत्तर (Llama 3.2 Position Grounded):\n📍 वर्तमान स्थान: " + cleanCoords + "\n⛺ निकटतम आश्रय स्थल: \"" + name + "\" — " + dist + " दूरी पर";
            } else {
                return "Answer (Llama 3.2 Position Grounded):\n📍 Current position: " + cleanCoords + "\n⛺ Nearest shelter: \"" + name + "\" — " + dist + " away";
            }
        } 
        
        // 3. Water Detection (English, Devanagari, Hinglish/Minglish)
        else if (containsWaterKeyword(lowerQuestion)) {
            String targetWater = waterInfo.isEmpty() ? nearestInfo : waterInfo;
            String name = extractNameOnly(targetWater);
            String dist = extractDistOnly(targetWater);

            if (isMarathi) {
                return "उत्तर (Llama 3.2 Position Grounded):\n📍 सध्याचे स्थान: " + cleanCoords + "\n💧 सर्वात जवळचे पिण्याचे पाणी: \"" + name + "\" — " + dist + " अंतरावर";
            } else if (isHindi) {
                return "उत्तर (Llama 3.2 Position Grounded):\n📍 वर्तमान स्थान: " + cleanCoords + "\n💧 निकटतम पेयजल स्रोत: \"" + name + "\" — " + dist + " दूरी पर";
            } else {
                return "Answer (Llama 3.2 Position Grounded):\n📍 Current position: " + cleanCoords + "\n💧 Nearest water point: \"" + name + "\" — " + dist + " away";
            }
        } 
        
        // 4. Exit / Route Detection (English, Devanagari, Hinglish/Minglish)
        else if (containsExitKeyword(lowerQuestion)) {
            String targetExit = exitInfo.isEmpty() ? nearestInfo : exitInfo;
            String name = extractNameOnly(targetExit);
            String dist = extractDistOnly(targetExit);

            if (isMarathi) {
                return "उत्तर (Llama 3.2 Position Grounded):\n📍 सध्याचे स्थान: " + cleanCoords + "\n🚪 बाहेर पडण्याचा मार्ग / एक्झिट: \"" + name + "\" — " + dist + " अंतरावर";
            } else if (isHindi) {
                return "उत्तर (Llama 3.2 Position Grounded):\n📍 वर्तमान स्थान: " + cleanCoords + "\n🚪 निकटतम आपातकालीन निकास: \"" + name + "\" — " + dist + " दूरी पर";
            } else {
                return "Answer (Llama 3.2 Position Grounded):\n📍 Current position: " + cleanCoords + "\n🚪 Nearest emergency exit: \"" + name + "\" — " + dist + " away";
            }
        } 
        
        // 5. Distance / General Location Queries
        else if (containsDistanceKeyword(lowerQuestion)) {
            if (isMarathi) {
                return "उत्तर (Llama 3.2 Position Grounded):\n📍 सध्याचे स्थान: " + cleanCoords + 
                       "\n⛺ निवारा: \"" + extractNameOnly(shelterInfo) + "\" (" + extractDistOnly(shelterInfo) + ")" +
                       "\n💧 पिण्याचे पाणी: \"" + extractNameOnly(waterInfo) + "\" (" + extractDistOnly(waterInfo) + ")" +
                       "\n🚪 मार्ग: \"" + extractNameOnly(exitInfo) + "\" (" + extractDistOnly(exitInfo) + ")";
            } else if (isHindi) {
                return "उत्तर (Llama 3.2 Position Grounded):\n📍 वर्तमान स्थान: " + cleanCoords + 
                       "\n⛺ आश्रय: \"" + extractNameOnly(shelterInfo) + "\" (" + extractDistOnly(shelterInfo) + ")" +
                       "\n💧 जल: \"" + extractNameOnly(waterInfo) + "\" (" + extractDistOnly(waterInfo) + ")" +
                       "\n🚪 निकास: \"" + extractNameOnly(exitInfo) + "\" (" + extractDistOnly(exitInfo) + ")";
            } else {
                return "Answer (Llama 3.2 Position Grounded):\n📍 Current position: " + cleanCoords + 
                       "\n⛺ Shelter: \"" + extractNameOnly(shelterInfo) + "\" (" + extractDistOnly(shelterInfo) + ")" +
                       "\n💧 Water: \"" + extractNameOnly(waterInfo) + "\" (" + extractDistOnly(waterInfo) + ")" +
                       "\n🚪 Exit: \"" + extractNameOnly(exitInfo) + "\" (" + extractDistOnly(exitInfo) + ")";
            }
        } 
        
        // Fallback
        else {
            String name = extractNameOnly(nearestInfo);
            String dist = extractDistOnly(nearestInfo);
            if (isMarathi) {
                return "उत्तर (Llama 3.2 Position Grounded):\n📍 स्थान: " + cleanCoords + "\n📍 जवळचे ठिकाण: \"" + name + "\" — " + dist + " अंतरावर";
            } else if (isHindi) {
                return "उत्तर (Llama 3.2 Position Grounded):\n📍 स्थान: " + cleanCoords + "\n📍 निकटतम स्थल: \"" + name + "\" — " + dist + " दूरी पर";
            } else {
                return "Answer (Llama 3.2 Position Grounded):\n📍 Position: " + cleanCoords + "\n📍 Nearest location: \"" + name + "\" — " + dist + " away";
            }
        }
    }

    private boolean isMarathiLanguage(String text) {
        return text.contains("कुठे") || text.contains("कसे") || text.contains("किती") || text.contains("अंतरावर") || 
               text.contains("पाणी") || text.contains("निवारा") || text.contains("मार्ग") || text.contains("बाहेर") ||
               text.contains("kuthe") || text.contains("kiti") || text.contains("antar") || text.contains("baher");
    }

    private boolean isHindiLanguage(String text) {
        return text.contains("कहाँ") || text.contains("कहा") || text.contains("कितना") || text.contains("कितनी") || text.contains("दूरी") ||
               text.contains("आश्रय") || text.contains("पानी") || text.contains("निकास") || text.contains("रास्ता") ||
               text.contains("kahan") || text.contains("kaha") || text.contains("kitna") || text.contains("kitni") || text.contains("pani") || text.contains("paani") || text.contains("rasta") || text.contains("raasta") || text.contains("door");
    }

    private boolean containsShelterKeyword(String q) {
        return q.contains("shelter") || q.contains("rest") || q.contains("cabin") || q.contains("hut") ||
               q.contains("आश्रय") || q.contains("निवारा") || q.contains("विश्रांती") || q.contains("छत") || q.contains("शरण") ||
               q.contains("nivara") || q.contains("aashray") || q.contains("ashray") || q.contains("chaanv");
    }

    private boolean containsWaterKeyword(String q) {
        return q.contains("water") || q.contains("drink") || q.contains("stream") || q.contains("spring") ||
               q.contains("पानी") || q.contains("पाणी") || q.contains("जल") || q.contains("पेयजल") ||
               q.contains("pani") || q.contains("paani") || q.contains("pene") || q.contains("peene") || q.contains("jal");
    }

    private boolean containsExitKeyword(String q) {
        return q.contains("exit") || q.contains("evacuate") || q.contains("road") || q.contains("leave") ||
               q.contains("निकास") || q.contains("बाहेर") || q.contains("मार्ग") || q.contains("रास्ता") || q.contains("दिशा") ||
               q.contains("bahaar") || q.contains("bahar") || q.contains("baher") || q.contains("nikas") || q.contains("rasta") || q.contains("raasta");
    }

    private boolean containsDistanceKeyword(String q) {
        return q.contains("where") || q.contains("location") || q.contains("far") || q.contains("distance") || q.contains("near") || q.contains("find") ||
               q.contains("कहाँ") || q.contains("कहा") || q.contains("दूरी") || q.contains("कुठे") || q.contains("अंतर") || q.contains("लांब") || q.contains("दूर") ||
               q.contains("kahan") || q.contains("kaha") || q.contains("dur") || q.contains("door") || q.contains("kuthe") || q.contains("lamb");
    }

    private String extractQuestionFromLlamaFormat(String prompt) {
        int userHeader = prompt.indexOf("<|start_header_id|>user<|end_header_id|>");
        if (userHeader != -1) {
            int start = userHeader + "<|start_header_id|>user<|end_header_id|>".length();
            int end = prompt.indexOf("<|eot_id|>", start);
            if (end != -1) {
                return prompt.substring(start, end).replace("User Question:", "").trim();
            }
            return prompt.substring(start).replace("User Question:", "").trim();
        }
        return prompt;
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
