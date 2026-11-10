package com.aristino.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gioi han toc do (cua so truot 1 phut), luu trong bo nho:
 * - allowUser: moi nguoi (theo dia chi IP) toi da N tin nhan / phut, tranh spam.
 * - allowAiCall: toan he thong toi da M lan goi Gemini / phut, de khong vuot han muc goi mien phi.
 * Vuot gioi han khong gay loi: chatbot tu chuyen sang tra loi theo luat.
 */
@Component
public class ChatRateLimiter {

    private static final long WINDOW_MS = 60_000;

    @Value("${app.chat.user-messages-per-minute:10}")
    private int userLimit;

    @Value("${app.chat.ai-calls-per-minute:8}")
    private int aiLimit;

    private final Map<String, Deque<Long>> userHits = new ConcurrentHashMap<>();
    private final Deque<Long> aiHits = new ArrayDeque<>();

    public boolean allowUser(String key) {
        if (userHits.size() > 5000) {
            prune();
        }
        return allow(userHits.computeIfAbsent(key, k -> new ArrayDeque<>()), userLimit);
    }

    public boolean allowAiCall() {
        return allow(aiHits, aiLimit);
    }

    private boolean allow(Deque<Long> hits, int limit) {
        synchronized (hits) {
            long now = System.currentTimeMillis();
            while (!hits.isEmpty() && now - hits.peekFirst() > WINDOW_MS) {
                hits.pollFirst();
            }
            if (hits.size() >= limit) {
                return false;
            }
            hits.addLast(now);
            return true;
        }
    }

    // Don dep cac dia chi IP khong con hoat dong de bo nho khong phinh to
    private void prune() {
        long now = System.currentTimeMillis();
        userHits.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                Long last = entry.getValue().peekLast();
                return last == null || now - last > WINDOW_MS;
            }
        });
    }
}