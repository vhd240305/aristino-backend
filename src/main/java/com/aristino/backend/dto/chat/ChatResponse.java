package com.aristino.backend.dto.chat;

/**
 * Cau tra loi cua chatbot.
 * mode: AI (Gemini tra loi) | RULES (tra loi theo luat/FAQ, khi chua co key hoac Gemini loi) | LIMITED (khach nhan qua nhanh)
 */
public record ChatResponse(String sessionToken, String reply, String mode) {
}