package com.mindflow.security.notification;

import com.mindflow.security.common.ResourceNotFoundException;
import com.mindflow.security.common.TenantContext;
import com.mindflow.security.user.UserEntity;
import com.mindflow.security.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
public class NotificationPreferenceService {

    private final UserRepository userRepository;
    private final NotificationRuleService notificationRuleService;

    public NotificationPreferenceService(UserRepository userRepository,
                                         NotificationRuleService notificationRuleService) {
        this.userRepository = userRepository;
        this.notificationRuleService = notificationRuleService;
    }

    @Transactional(readOnly = true)
    public NotificationPreferencesResponse getPreferences(String username) {
        UserEntity user = findUser(username);
        return toResponse(user);
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(String username, NotificationPreferencesRequest request) {
        UserEntity user = findUser(username);

        user.setArrivalReminderEnabled(request.arrivalReminderEnabled());
        user.setReservationSuccessEnabled(request.reservationSuccessEnabled());
        user.setReminderLeadMinutes(request.reminderLeadMinutes());

        boolean dndEnabled = Boolean.TRUE.equals(request.dndEnabled());
        if (dndEnabled) {
            if (request.dndStart() == null || request.dndEnd() == null) {
                throw new IllegalArgumentException("DND start and end are required when DND is enabled");
            }
            user.setDndStart(LocalTime.parse(request.dndStart()));
            user.setDndEnd(LocalTime.parse(request.dndEnd()));
        } else {
            user.setDndStart(null);
            user.setDndEnd(null);
        }

        UserEntity saved = userRepository.save(user);
        return toResponse(saved);
    }

    private NotificationPreferencesResponse toResponse(UserEntity user) {
        return new NotificationPreferencesResponse(
                user.getUsername(),
                user.isArrivalReminderEnabled(),
                user.isReservationSuccessEnabled(),
                user.getReminderLeadMinutes(),
                user.getDndStart() != null && user.getDndEnd() != null,
                user.getDndStart() == null ? null : user.getDndStart().toString(),
                user.getDndEnd() == null ? null : user.getDndEnd().toString(),
                notificationRuleService.isWithinDndWindow(user, LocalTime.now())
        );
    }

    private UserEntity findUser(String username) {
        return userRepository.findByUsernameAndTenantId(username, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
