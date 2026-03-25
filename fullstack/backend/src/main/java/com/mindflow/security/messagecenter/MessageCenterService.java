package com.mindflow.security.messagecenter;

import com.mindflow.security.common.OwnershipDeniedException;
import com.mindflow.security.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageCenterService {

    private final MessageRepository messageRepository;
    private final MessageMaskingService maskingService;

    public MessageCenterService(MessageRepository messageRepository,
                                MessageMaskingService maskingService) {
        this.messageRepository = messageRepository;
        this.maskingService = maskingService;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(String username, MessageType type) {
        List<MessageEntity> rows = type == null
                ? messageRepository.findByUsernameOrderByCreatedAtDesc(username)
                : messageRepository.findByUsernameAndTypeOrderByCreatedAtDesc(username, type);
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional
    public MessageResponse markRead(String username, Long id, boolean read) {
        MessageEntity row = findOwned(username, id);
        row.setRead(read);
        return toResponse(messageRepository.save(row));
    }

    @Transactional
    public void delete(String username, Long id) {
        MessageEntity row = findOwned(username, id);
        messageRepository.delete(row);
    }

    private MessageEntity findOwned(String username, Long id) {
        MessageEntity row = messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        if (!row.getUsername().equals(username)) {
            throw new OwnershipDeniedException("Message access denied for this user");
        }
        return row;
    }

    private MessageResponse toResponse(MessageEntity row) {
        String maskedContent = maskingService.mask(row.getContent());
        return new MessageResponse(
                row.getId(),
                row.getType(),
                row.getTitle(),
                maskedContent,
                row.isRead(),
                !maskedContent.equals(row.getContent()),
                row.getCreatedAt()
        );
    }
}
