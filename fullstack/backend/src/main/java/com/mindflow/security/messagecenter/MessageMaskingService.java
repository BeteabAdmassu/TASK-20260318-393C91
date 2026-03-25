package com.mindflow.security.messagecenter;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class MessageMaskingService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\b\\d{3})\\d{4}(\\d{4}\\b)");
    private static final Pattern ID_PATTERN = Pattern.compile("(\\b[A-Za-z0-9]{3})[A-Za-z0-9]{8,12}([A-Za-z0-9]{3}\\b)");

    public String mask(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String maskedPhone = PHONE_PATTERN.matcher(value).replaceAll("$1****$2");
        return ID_PATTERN.matcher(maskedPhone).replaceAll("$1********$2");
    }
}
