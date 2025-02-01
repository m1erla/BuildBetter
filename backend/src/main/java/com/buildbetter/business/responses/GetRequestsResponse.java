package com.renovatipoint.business.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.renovatipoint.entities.concretes.Request;
import com.renovatipoint.enums.RequestStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetRequestsResponse {
    private String id;
    private String expertId;
    private String expertName;
    private String userId;
    private String userName;
    private String adId;
    private String adTitle;
    private RequestStatus status;
    private String createdAt;
    private String updatedAt;
    private String message;

    public static GetRequestsResponse fromEntity(Request request) {
        return GetRequestsResponse.builder()
                .id(request.getId())
                .expertId(request.getExpert().getId())
                .expertName(request.getExpert().getName())
                .userId(request.getUser().getId())
                .userName(request.getUser().getName())
                .adId(request.getAd().getId())
                .adTitle(request.getAd().getTitle())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .updatedAt(request.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .message(request.getMessage())
                .build();
    }
}
