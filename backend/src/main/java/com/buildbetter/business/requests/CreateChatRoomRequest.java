package com.buildbetter.business.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
