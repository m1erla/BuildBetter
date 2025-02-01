package com.renovatipoint.business.abstracts;

import com.renovatipoint.business.requests.UpdateExpertRequest;
import com.renovatipoint.business.responses.GetExpertResponse;
import com.renovatipoint.business.responses.GetUsersResponse;
import com.renovatipoint.entities.concretes.Expert;
import org.springframework.http.ResponseEntity;

public interface ExpertService {

    GetExpertResponse getExpertById(String expertId);

    Expert getById(String expertId);

    Expert getByEmail(String email);

    Expert save(Expert expert);

    ResponseEntity<?> update(UpdateExpertRequest updateExpertRequest);

}
