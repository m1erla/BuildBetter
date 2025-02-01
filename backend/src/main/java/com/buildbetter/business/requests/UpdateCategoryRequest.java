package com.renovatipoint.business.requests;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateCategoryRequest {
    private String id;
    private String name;
    private boolean isActive;
}
