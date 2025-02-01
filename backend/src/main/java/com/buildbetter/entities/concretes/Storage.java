package com.renovatipoint.entities.concretes;

import jakarta.persistence.*;
import lombok.*;

@Data
@Table(name = "image_data", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "name"})})
@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Storage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    private String type;

    @Lob
    @Column(name = "image_data", columnDefinition = "LONGBLOB")
    private byte[] imageData;

    private String url;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", referencedColumnName = "id")
    private Ads ads;


}

