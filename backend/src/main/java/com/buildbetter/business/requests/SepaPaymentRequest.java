package com.renovatipoint.business.requests;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class SepaPaymentRequest {

    private String iban;
    private String bic;
    private String bankName;
    private String paymentMethodId;
}
