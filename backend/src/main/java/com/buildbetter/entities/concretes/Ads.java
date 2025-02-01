package com.renovatipoint.entities.concretes;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.renovatipoint.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ads")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Ads {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "title")
    private String title;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "service_id", referencedColumnName = "id")
    private ServiceEntity service;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @OneToMany(mappedBy = "ads", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Storage> storages = new ArrayList<>();

    @Column(name = "ad_release_date")
    private LocalDateTime adReleaseDate;
    @Column(length = 1000)
    private String descriptions;
    private String imageUrl;

    private boolean isActive;

    @Enumerated(EnumType.STRING)
    private JobStatus status = null;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    @PrePersist
    protected void onCreate(){
        adReleaseDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
       updatedAt = LocalDateTime.now();
    }
}
