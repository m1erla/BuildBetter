package com.renovatipoint.business.abstracts;

import com.renovatipoint.business.requests.CreateServiceRequest;
import com.renovatipoint.business.requests.UpdateServiceRequest;
import com.renovatipoint.business.responses.GetAllServicesResponse;
import com.renovatipoint.business.responses.GetServiceByIdResponse;

import java.util.List;

public interface IService {

    List<GetAllServicesResponse> getAll();

    GetServiceByIdResponse getById(String id);

    void add(CreateServiceRequest createServiceRequest);

    void update(UpdateServiceRequest updateServiceRequest);

    void delete(String id);

}
