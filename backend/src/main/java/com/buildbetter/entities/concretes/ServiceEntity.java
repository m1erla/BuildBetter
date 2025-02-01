package com.renovatipoint.entities.concretes;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Table(name = "services")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Data
public class ServiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    private boolean isActive;


    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "service", cascade = CascadeType.PERSIST)
    private List<Ads> ads;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "job_title_id")
    private JobTitle jobTitle;

}
