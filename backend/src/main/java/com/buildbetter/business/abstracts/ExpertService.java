package com.buildbetter.business.abstracts;

import com.buildbetter.business.requests.UpdateExpertRequest;
import com.buildbetter.business.responses.GetExpertResponse;
import com.buildbetter.entities.concretes.Expert;
import org.springframework.http.ResponseEntity;

public interface ExpertService {

    GetExpertResponse getExpertById(String expertId);

    Expert getById(String expertId);

    Expert getByEmail(String email);

    Expert save(Expert expert);

    ResponseEntity<?> update(UpdateExpertRequest updateExpertRequest);

}
