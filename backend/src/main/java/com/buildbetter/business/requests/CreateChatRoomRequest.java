package com.renovatipoint.business.requests;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateChatRoomRequest {

    private String userId;
    private String expertId;
    private String adId;
    private String requestId;
}
