package com.renovatipoint.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    NEW_REQUEST,
    REQUEST_ACCEPTED,
    REQUEST_REJECTED,
    REQUEST_CANCELLED,
    NEW_MESSAGE,
    CHAT_CREATED,
    CHAT_ROOM_CREATED,
    CHAT_ROOM_COMPLETED,
    CONTACT_INFO_SHARED,
    ACCOUNT_BLOCKED,
    INSUFFICIENT_BALANCE,

}
