package com.renovatipoint.business.requests;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateJobTitleRequest {
    private String id;
    private String name;
    private String descriptions;
    private String categoryName;
    private String categoryId;
}
