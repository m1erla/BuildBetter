package com.renovatipoint.business.requests;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateServiceRequest {
    private String id;
    private String name;
    private String categoryName;
    private String categoryId;
    private String jobTitleName;
    private String jobTitleId;
    private boolean isActive;
}
