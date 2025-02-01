package com.renovatipoint.business.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetJobTitlesByName {
    private String id;
    private String name;
    private String descriptions;
    private String categoryName;
}
