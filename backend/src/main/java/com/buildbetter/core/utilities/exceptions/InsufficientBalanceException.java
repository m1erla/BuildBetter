package com.renovatipoint.core.utilities.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class InsufficientBalanceException extends RuntimeException {
    private final String message;

}
