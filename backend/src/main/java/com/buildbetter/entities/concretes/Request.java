package com.renovatipoint.entities.concretes;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.renovatipoint.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@NoArgsConstructor
@Setter
@Getter
@AllArgsConstructor
@Data
@Builder
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id", nullable = false)
    @JsonBackReference(value = "expert-requests")
    private Expert expert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference(value = "user-requests")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false)
    @JsonIgnore
    private Ads ad;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private String message;

    @OneToOne(mappedBy = "request", cascade = CascadeType.ALL)
    private ChatRoom chatRoom;

//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        status = RequestStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }

}
