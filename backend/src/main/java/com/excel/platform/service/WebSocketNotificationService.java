package com.excel.platform.service;

import com.excel.platform.dto.CellUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastCellUpdate(Long workbookId, Long sheetId, List<CellUpdateMessage> updates) {
        String destination = "/topic/workbook/" + workbookId + "/sheet/" + sheetId;
        log.debug("Broadcasting {} cell updates to {}", updates.size(), destination);
        messagingTemplate.convertAndSend(destination, updates);
    }

    public void broadcastWorkbookListChange() {
        log.debug("Broadcasting workbook list change notification");
        messagingTemplate.convertAndSend("/topic/workbooks", "WORKBOOK_LIST_CHANGED");
    }
}
