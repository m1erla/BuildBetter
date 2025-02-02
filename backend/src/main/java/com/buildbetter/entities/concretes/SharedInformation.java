package com.buildbetter.entities.concretes;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Date;

@Entity
@Table(name = "shared_information")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SharedInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id", nullable = false)
    private Expert expert;

    @Column(nullable = false)
    private String information;

    @Column(nullable = false)
    private Date sharedAt;

    @Column(nullable = false)
    private boolean charged;

    @PrePersist
    protected void onCreate(){
        sharedAt = Date.from(Instant.now());
    }
}
