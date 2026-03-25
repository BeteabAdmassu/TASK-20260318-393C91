package com.mindflow.security.messagecenter;

import com.mindflow.security.common.OwnershipDeniedException;
import com.mindflow.security.common.ResourceNotFoundException;
import com.mindflow.security.message.MessagePrivacyService;
import com.mindflow.security.message.SensitivityLevel;
import com.mindflow.security.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageCenterService {

    private final MessageRepository messageRepository;
    private final MessageMaskingService maskingService;
    private final MessagePrivacyService privacyService;

    public MessageCenterService(MessageRepository messageRepository,
                                MessageMaskingService maskingService,
                                MessagePrivacyService privacyService) {
        this.messageRepository = messageRepository;
        this.maskingService = maskingService;
        this.privacyService = privacyService;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(String username, Role role, MessageType type) {
        List<MessageEntity> rows = type == null
                ? messageRepository.findByUsernameOrderByCreatedAtDesc(username)
                : messageRepository.findByUsernameAndTypeOrderByCreatedAtDesc(username, type);
        return rows.stream().map(row -> toResponse(row, role)).toList();
    }

    @Transactional
    public MessageResponse markRead(String username, Role role, Long id, boolean read) {
        MessageEntity row = findOwned(username, id);
        row.setRead(read);
        return toResponse(messageRepository.save(row), role);
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

    private MessageResponse toResponse(MessageEntity row, Role role) {
        SensitivityLevel level = row.getSensitivityLevel() == null ? SensitivityLevel.MEDIUM : row.getSensitivityLevel();
        String sensitivityMasked = privacyService.desensitize(row.getContent(), level, role);
        String regexMasked = maskingService.mask(sensitivityMasked);
        boolean masked = !regexMasked.equals(row.getContent());
        return new MessageResponse(
                row.getId(),
                row.getType(),
                row.getTitle(),
                regexMasked,
                level,
                row.isRead(),
                masked,
                row.getCreatedAt()
        );
    }
}
