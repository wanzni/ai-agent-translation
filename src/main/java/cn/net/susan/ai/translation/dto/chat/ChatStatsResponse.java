package cn.net.susan.ai.translation.dto.chat;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 聊天统计响应DTO
 * 
 * @author 苏三
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatStatsResponse {
    
    private String sessionId;
    private int totalMessages;
    private int totalParticipants;
    private int activeParticipants;
    private double averageResponseTime;
    private double translationAccuracy;
    private long sessionDuration;
    private List<String> languagePairs;
}