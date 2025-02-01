package com.renovatipoint.enums;

import lombok.Getter;

@Getter
public enum RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COMPLETED,
    CANCELLED
}
