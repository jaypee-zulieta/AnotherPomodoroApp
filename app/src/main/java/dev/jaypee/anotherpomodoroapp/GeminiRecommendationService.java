package dev.jaypee.anotherpomodoroapp;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiRecommendationService {

    private static final String TAG = "GeminiService";
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface RecommendationCallback {
        void onSuccess(String aiResponse, int workTime, int breakTime, int longBreakTime, int longBreakInterval);
        void onError(Throwable t);
    }

    public void getChatResponse(List<ChatMessage> chatHistory, RecommendationCallback callback) {
        executorService.execute(() -> {
            try {
                URL url = new URL(API_BASE_URL + "?key=" + API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                StringBuilder promptBuilder = new StringBuilder();
                promptBuilder.append("You are a helpful Pomodoro coach. When the user gives their task description, immediately respond conversationally and suggest an optimal work time, short break time, long break time, and long break interval (number of sessions before a long break) in minutes. Conclude your response by asking the user if they want to add more details or adjust these intervals. You must include the exact structured string format \"work: X, break: Y, long_break: Z, interval: W\" (where X, Y, Z, and W are numbers) somewhere in your message text response so the app can pre-fill input values.\n\n");
                promptBuilder.append("Chat History:\n");
                for (ChatMessage msg : chatHistory) {
                    if (msg.isUser()) {
                        promptBuilder.append("User: ").append(msg.getText()).append("\n");
                    } else {
                        promptBuilder.append("Coach: ").append(msg.getText()).append("\n");
                    }
                }
                promptBuilder.append("Coach: ");
                String prompt = promptBuilder.toString();

                JSONObject jsonPrompt = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", prompt);
                parts.put(part);
                content.put("parts", parts);
                contents.put(content);
                jsonPrompt.put("contents", contents);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPrompt.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String text = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    parseAndCallback(text, callback);
                } else {
                    StringBuilder errorResponse = new StringBuilder();
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            errorResponse.append(line.trim());
                        }
                    } catch (Exception ignored) {}
                    String errorMsg = "HTTP Error: " + responseCode + (errorResponse.length() > 0 ? " - " + errorResponse.toString() : "");
                    mainHandler.post(() -> callback.onError(new Exception(errorMsg)));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error getting recommendation", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private void parseAndCallback(String text, RecommendationCallback callback) {
        int workTime = -1;
        int breakTime = -1;
        int longBreakTime = -1;
        int longBreakInterval = -1;
        try {
            String cleanedText = text.toLowerCase().replaceAll("\\s+", "");
            if (cleanedText.contains("work:") && cleanedText.contains("break:")) {
                workTime = extractNumberAfter(cleanedText, "work:");
                breakTime = extractNumberAfter(cleanedText, "break:");
            }
            if (cleanedText.contains("long_break:")) {
                longBreakTime = extractNumberAfter(cleanedText, "long_break:");
            }
            if (cleanedText.contains("interval:")) {
                longBreakInterval = extractNumberAfter(cleanedText, "interval:");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing recommendation: " + text, e);
        }

        int finalWorkTime = workTime;
        int finalBreakTime = breakTime;
        int finalLongBreakTime = longBreakTime;
        int finalLongBreakInterval = longBreakInterval;
        mainHandler.post(() -> callback.onSuccess(text, finalWorkTime, finalBreakTime, finalLongBreakTime, finalLongBreakInterval));
    }

    private int extractNumberAfter(String text, String key) {
        int idx = text.indexOf(key);
        if (idx == -1) return -1;
        int start = idx + key.length();
        int end = start;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
        }
        if (end > start) {
            return Integer.parseInt(text.substring(start, end));
        }
        return -1;
    }
}
