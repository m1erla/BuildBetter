package com.buildbetter.dataAccess.abstracts;

import com.buildbetter.entities.concretes.EmailTemplate;
import com.buildbetter.enums.EmailTemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, String> {
    Optional<EmailTemplate> findByType(EmailTemplateType type);
    List<EmailTemplate> findByIsActive(boolean isActive);
}
