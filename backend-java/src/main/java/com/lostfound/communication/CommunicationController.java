package com.lostfound.communication;

import com.lostfound.account.RequiresAgreement;
import com.lostfound.shared.Envelope;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CommunicationController {

    private final CommunicationService service;

    public CommunicationController(CommunicationService service) {
        this.service = service;
    }

    public record BlockView(String blockedUserId, String createdAt) {
        static BlockView from(BlockRelation r) {
            return new BlockView(r.blockedId(), r.createdAt());
        }
    }

    public record BlockListData(List<BlockView> items, CommunicationService.Pagination pagination) {
    }

    public record MessageView(String id, String conversationId, String senderId, String type,
                              String content, String createdAt) {
        static MessageView from(Message m) {
            return new MessageView(m.id(), m.conversationId(), m.senderId(), m.type(), m.content(),
                m.createdAt());
        }
    }

    public record MessageListData(List<MessageView> items, CommunicationService.Pagination pagination) {
    }

    @PostMapping("/blocks")
    public Envelope.Response<BlockView> block(Authentication authentication,
                                              @RequestBody CommunicationService.BlockCreate request) {
        return Envelope.ok(BlockView.from(service.block(authentication.getName(), request)));
    }

    @DeleteMapping("/blocks/{userId}")
    public Envelope.Response<Map<String, Object>> unblock(Authentication authentication,
                                                          @PathVariable String userId) {
        service.unblock(authentication.getName(), userId);
        return Envelope.ok(Map.of());
    }

    @GetMapping("/me/blocks")
    public Envelope.Response<BlockListData> listBlocks(Authentication authentication,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int pageSize) {
        CommunicationService.BlockListResult result =
            service.listBlocks(authentication.getName(), page, pageSize);
        List<BlockView> views = result.items().stream().map(BlockView::from).toList();
        return Envelope.ok(new BlockListData(views, result.pagination()));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Envelope.Response<MessageListData> listMessages(Authentication authentication,
                                                           @PathVariable String conversationId,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        CommunicationService.MessageListResult result =
            service.listMessages(authentication.getName(), conversationId, page, pageSize);
        List<MessageView> views = result.items().stream().map(MessageView::from).toList();
        return Envelope.ok(new MessageListData(views, result.pagination()));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @RequiresAgreement
    public Envelope.Response<MessageView> sendMessage(Authentication authentication,
                                                      @PathVariable String conversationId,
                                                      @RequestBody CommunicationService.MessageCreate request) {
        return Envelope.ok(MessageView.from(
            service.send(authentication.getName(), conversationId, request)));
    }
}
