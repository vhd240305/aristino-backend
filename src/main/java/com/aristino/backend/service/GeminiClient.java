package com.aristino.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Goi Gemini API (Google) de sinh cau tra loi.
 *
 * - API key lay tu bien moi truong GEMINI_API_KEY (hoac app.chat.gemini.api-key). KHONG BAO GIO ghi key ra log.
 * - Ten model doi duoc bang app.chat.gemini.model (Google doi ten model thuong xuyen).
 * - Gui "store": false de yeu cau Google khong luu lai noi dung cuoc tro chuyen o phia ho (neu duoc ho tro).
 * - Gap loi (het luot, sai key, sai ten model, mat mang...) thi KHONG nem loi: tra Optional.empty() de chatbot
 *   chuyen sang tra loi theo luat, dong thoi tam dung goi Gemini mot luc de khong goi lien tuc khi dang loi.
 */
@Slf4j
@Component
public class GeminiClient {

    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/interactions";

    @Value("${app.chat.gemini.api-key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${app.chat.gemini.model:gemini-3.8-flash}")
    private String model;

    // Phien ban API (Google dung header Api-Revision de tranh thay doi dot ngot)
    @Value("${app.chat.gemini.api-revision:2026-05-20}")
    private String apiRevision;

    private final RestClient restClient;
    private volatile long pausedUntil = 0;

    public GeminiClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(30_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @PostConstruct
    void logStatus() {
        if (isConfigured()) {
            log.info("Chatbot: DA cau hinh Gemini API key (model = {}). Che do AI se duoc dung.", model);
        } else {
            log.info("Chatbot: CHUA co Gemini API key -> chatbot tra loi theo luat/FAQ. "
                    + "Dat bien moi truong GEMINI_API_KEY de bat che do AI.");
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    // Co key va khong dang trong thoi gian tam dung do loi truoc do
    public boolean isAvailable() {
        return isConfigured() && System.currentTimeMillis() >= pausedUntil;
    }

    public Optional<String> generate(String systemInstruction, String input) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("system_instruction", systemInstruction);
            body.put("input", input);
            body.put("store", false);

            JsonNode response = restClient.post()
                    .uri(ENDPOINT)
                    .header("x-goog-api-key", apiKey.trim())
                    .header("Api-Revision", apiRevision)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String text = extractText(response);
            if (text == null || text.isBlank()) {
                log.warn("Gemini tra ve noi dung rong hoac sai dinh dang: {}", abbreviate(String.valueOf(response)));
                pausedUntil = System.currentTimeMillis() + 15_000;
                return Optional.empty();
            }
            return Optional.of(text.trim());

        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.warn("Gemini bao loi HTTP {}: {}", status, abbreviate(ex.getResponseBodyAsString()));
            // 429 = het luot: nghi 1 phut. Loi 4xx khac (sai key, sai ten model...) khong tu het: nghi 5 phut. 5xx: nghi 15 giay
            long pauseMs = status == 429 ? 60_000 : status >= 500 ? 15_000 : 300_000;
            pausedUntil = System.currentTimeMillis() + pauseMs;
            return Optional.empty();

        } catch (Exception ex) {
            log.warn("Khong goi duoc Gemini: {}", ex.toString());
            pausedUntil = System.currentTimeMillis() + 15_000;
            return Optional.empty();
        }
    }

    // Dinh dang phan hoi cua Gemini co the doi theo Api-Revision nen doc theo nhieu cach:
    // 1) truong "output_text"; 2) neu khong co, lay doan "text" cuoi cung trong danh sach "steps"
    private String extractText(JsonNode root) {
        if (root == null) {
            return null;
        }
        JsonNode direct = root.get("output_text");
        if (direct != null && direct.isTextual()) {
            return direct.asText();
        }
        List<String> texts = new ArrayList<>();
        collectText(root.has("steps") ? root.get("steps") : root, texts);
        return texts.isEmpty() ? null : texts.get(texts.size() - 1);
    }

    private void collectText(JsonNode node, List<String> out) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            JsonNode text = node.get("text");
            if (text != null && text.isTextual()) {
                out.add(text.asText());
            }
            node.fields().forEachRemaining(entry -> {
                if (!"text".equals(entry.getKey())) {
                    collectText(entry.getValue(), out);
                }
            });
        } else if (node.isArray()) {
            node.forEach(child -> collectText(child, out));
        }
    }

    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replaceAll("\\s+", " ");
        return oneLine.length() > 400 ? oneLine.substring(0, 400) + "..." : oneLine;
    }
}