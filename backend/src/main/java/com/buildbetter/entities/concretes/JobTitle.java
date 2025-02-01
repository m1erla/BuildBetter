package com.renovatipoint.entities.concretes;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Table(name = "job_title")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Builder
public class JobTitle {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true)
    private String name;

    private String descriptions;

    @OneToMany(mappedBy = "jobTitle", cascade = CascadeType.PERSIST)
    private List<ServiceEntity> services;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

    @OneToMany(mappedBy = "jobTitle", cascade = CascadeType.PERSIST)
    private List<Expert> experts = new ArrayList<>();
}
//    @ManyToOne(cascade = CascadeType.PERSIST)
//    @JoinColumn(name = "user_id", referencedColumnName = "id")
//    private User user;