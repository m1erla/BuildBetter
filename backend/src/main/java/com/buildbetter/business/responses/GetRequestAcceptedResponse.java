package com.renovatipoint.business.responses;

import com.renovatipoint.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetRequestAcceptedResponse {
    private String requestId;
    private RequestStatus status;
    private String chatRoomId;
    private String expertId;
    private String userId;
    private String adId;
    private String adTitle;
}
