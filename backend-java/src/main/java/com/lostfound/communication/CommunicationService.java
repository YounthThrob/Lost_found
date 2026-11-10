package com.lostfound.communication;

import com.lostfound.account.InMemoryUserStore;
import com.lostfound.claim.Claim;
import com.lostfound.claim.ClaimStore;
import com.lostfound.listing.ItemPost;
import com.lostfound.listing.ItemPostStore;
import com.lostfound.shared.BusinessException;
import com.lostfound.shared.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 有限消息与拉黑服务。会话（conversationId）MVP 下复用认领申请标识。 */
@Service
public class CommunicationService {

    public record BlockCreate(String blockedUserId) {
    }

    public record MessageCreate(String type, String content) {
    }

    public record Pagination(int page, int pageSize, int total, boolean hasMore) {
    }

    public record BlockListResult(List<BlockRelation> items, Pagination pagination) {
    }

    public record MessageListResult(List<Message> items, Pagination pagination) {
    }

    private static final int MAX_CONTENT = 2000;
    private static final int MAX_PAGE_SIZE = 100;
    private static final List<String> MESSAGE_TYPES =
        List.of("TEXT", "SUGGEST_TIME", "SUGGEST_PLACE", "LATE_REMINDER");

    private final BlockStore blockStore;
    private final MessageStore messageStore;
    private final ClaimStore claimStore;
    private final ItemPostStore postStore;
    private final InMemoryUserStore userStore;

    public CommunicationService(BlockStore blockStore, MessageStore messageStore,
                                ClaimStore claimStore, ItemPostStore postStore,
                                InMemoryUserStore userStore) {
        this.blockStore = blockStore;
        this.messageStore = messageStore;
        this.claimStore = claimStore;
        this.postStore = postStore;
        this.userStore = userStore;
    }

    public BlockRelation block(String blockerId, BlockCreate input) {
        String blockedId = input.blockedUserId();
        if (blockedId == null || blockedId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (blockerId.equals(blockedId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (userStore.findById(blockedId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        BlockRelation existing = blockStore.find(blockerId, blockedId);
        if (existing != null) {
            return existing;
        }
        BlockRelation relation = new BlockRelation(blockerId, blockedId, Instant.now().toString());
        return blockStore.put(relation);
    }

    public void unblock(String blockerId, String blockedId) {
        blockStore.remove(blockerId, blockedId);
    }

    public BlockListResult listBlocks(String blockerId, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        List<BlockRelation> all = blockStore.listByBlocker(blockerId);
        int total = all.size();
        int from = (page - 1) * pageSize;
        List<BlockRelation> items = from >= total
            ? List.of()
            : all.subList(from, Math.min(from + pageSize, total));
        boolean hasMore = from + pageSize < total;
        return new BlockListResult(items, new Pagination(page, pageSize, total, hasMore));
    }

    public Message send(String senderId, String conversationId, MessageCreate input) {
        Claim claim = claimStore.findById(conversationId);
        if (claim == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        ItemPost post = postStore.findById(claim.postId());
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        String ownerId = post.authorId();
        String applicantId = claim.applicantId();
        if (!senderId.equals(ownerId) && !senderId.equals(applicantId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!"ACCEPTED".equals(claim.status())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }
        String recipientId = senderId.equals(ownerId) ? applicantId : ownerId;
        if (blockStore.find(recipientId, senderId) != null) {
            throw new BusinessException(ErrorCode.BLOCKED);
        }
        String type = requireType(input.type());
        String content = requireContent(input.content());
        Message message = new Message(UUID.randomUUID().toString(), conversationId, senderId,
            type, content, Instant.now().toString());
        messageStore.save(message);
        return message;
    }

    public MessageListResult listMessages(String viewerId, String conversationId, int page, int pageSize) {
        Claim claim = claimStore.findById(conversationId);
        if (claim == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        ItemPost post = postStore.findById(claim.postId());
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        String ownerId = post.authorId();
        String applicantId = claim.applicantId();
        if (!viewerId.equals(ownerId) && !viewerId.equals(applicantId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        List<Message> all = messageStore.listByConversation(conversationId);
        int total = all.size();
        int from = (page - 1) * pageSize;
        List<Message> items = from >= total
            ? List.of()
            : all.subList(from, Math.min(from + pageSize, total));
        boolean hasMore = from + pageSize < total;
        return new MessageListResult(items, new Pagination(page, pageSize, total, hasMore));
    }

    private String requireType(String type) {
        if (type == null || !MESSAGE_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return type;
    }

    private String requireContent(String content) {
        if (content == null || content.trim().isEmpty() || content.trim().length() > MAX_CONTENT) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return content.trim();
    }
}
