package com.example.s3webapp.foldersize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class FolderSizeWebSocketHandler extends TextWebSocketHandler {

    private static final Pattern JOB_PATTERN = Pattern.compile(".*/folder-size/(?<id>[^/]+)$");
    private static final Logger log = LoggerFactory.getLogger(FolderSizeWebSocketHandler.class);

    private final FolderSizeJobService jobService;
    private final ObjectMapper objectMapper;

    public FolderSizeWebSocketHandler(FolderSizeJobService jobService, ObjectMapper objectMapper) {
        this.jobService = jobService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String jobId = extractJobId(session);
        session.getAttributes().put("jobId", jobId);
        jobService.attachListener(jobId, session.getId(), event -> sendEvent(session, event));
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload() != null ? message.getPayload().trim() : "";
        if ("cancel".equalsIgnoreCase(payload)) {
            String jobId = (String) session.getAttributes().get("jobId");
            if (jobId != null) {
                jobService.cancel(jobId);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String jobId = (String) session.getAttributes().get("jobId");
        if (jobId != null) {
            jobService.detachListener(jobId, session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("WebSocket transport error: {}", exception.getMessage());
        super.handleTransportError(session, exception);
    }

    private void sendEvent(WebSocketSession session, FolderSizeEvent event) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
                    if (isTerminal(event)) {
                        session.close(CloseStatus.NORMAL);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize folder size event: {}", e.getMessage());
        } catch (IOException e) {
            log.warn("Failed to send folder size event: {}", e.getMessage());
            try {
                session.close(CloseStatus.PROTOCOL_ERROR);
            } catch (IOException ignored) {
            }
        }
    }

    private boolean isTerminal(FolderSizeEvent event) {
        return switch (event.job().status()) {
            case COMPLETED, FAILED, CANCELED -> true;
            default -> false;
        };
    }

    private String extractJobId(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        Matcher matcher = JOB_PATTERN.matcher(path);
        if (matcher.matches()) {
            return matcher.group("id");
        }
        Map<String, Object> attributes = session.getAttributes();
        Object candidate = attributes.get("jobId");
        if (candidate instanceof String str && !str.isBlank()) {
            return str;
        }
        throw new IllegalArgumentException("Missing job id in websocket path");
    }
}
