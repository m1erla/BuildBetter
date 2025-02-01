package com.renovatipoint.enums;

import lombok.Getter;

@Getter
public enum MessageType {
    CHAT("Regular chat message"),
    SYSTEM("System notification"),
    CONTACT_INFO("Contact information shared"),
    NOTIFICATION("User notification"),
    PAYMENT_REQUEST("Payment request"),
    USER_INFO("User information update"),
    JOB_COMPLETION("Job completion notification");

    private final String description;

    MessageType(String description) {
        this.description = description;
    }
}
