package com.renovatipoint.business.responses;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GetPaymentInfoResponse {
    private String iban;
    private String bic;
    private String bankName;
    private String stripeCustomerId;
    private String paymentMethodId;
    private Date createdAt;
}
