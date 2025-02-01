package com.renovatipoint.business.responses;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GetAllImagesResponse {

    private String id;
    private String name;
    private String type;
    private String url;
}
