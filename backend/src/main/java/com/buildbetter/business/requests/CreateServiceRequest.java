package com.buildbetter.business.requests;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateServiceRequest {
    private String name;
    private String jobTitleName;
    private String categoryName;
    private boolean isActive;
}
