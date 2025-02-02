package com.buildbetter.enums;

import lombok.Getter;

@Getter
public enum RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COMPLETED,
    CANCELLED
}
