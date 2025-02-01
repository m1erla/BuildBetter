package com.renovatipoint.business.responses;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ExpertRegisterResponse {
    String message;
    String expertId;
    String email;
    String paymentMethodId;
}
