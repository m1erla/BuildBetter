package com.renovatipoint.business.concretes;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PricingManager {

    private static final BigDecimal CONTACT_INFO_FEE = new BigDecimal("1.00");
    private static final BigDecimal JOB_ACCEPTANCE_FEE = new BigDecimal("5.00");

    public BigDecimal calculateTotalFee(boolean contactInfoShared, boolean jobAccepted){
        BigDecimal total = BigDecimal.ZERO;
        if (contactInfoShared){
            total = total.add(CONTACT_INFO_FEE);
        }
        if (jobAccepted){
            total = total.add(JOB_ACCEPTANCE_FEE);
        }
        return total;
    }
}
