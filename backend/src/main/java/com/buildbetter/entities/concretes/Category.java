package com.renovatipoint.entities.concretes;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@Table(name = "categories")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@EqualsAndHashCode
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(unique = true)
    private String name;

    private boolean isActive;

    @OneToMany(mappedBy = "category", cascade = CascadeType.PERSIST)
    private List<Ads> ads;

    @OneToMany(mappedBy = "category", cascade = CascadeType.PERSIST)
    private List<ServiceEntity> services;

    @OneToMany(mappedBy = "category", cascade = CascadeType.PERSIST)
    private List<JobTitle> jobTitles;

    public void addJobTitle(JobTitle jobTitle){
        jobTitles.add(jobTitle);
        jobTitle.setCategory(this);
    }
}
