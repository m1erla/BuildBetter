package com.renovatipoint.business.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetUnreadCountResponse {
    private long count;
}
