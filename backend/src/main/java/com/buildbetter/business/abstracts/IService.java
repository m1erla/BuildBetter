package com.buildbetter.business.abstracts;

import com.buildbetter.business.requests.CreateServiceRequest;
import com.buildbetter.business.requests.UpdateServiceRequest;
import com.buildbetter.business.responses.GetAllServicesResponse;
import com.buildbetter.business.responses.GetServiceByIdResponse;

import java.util.List;

public interface IService {

    List<GetAllServicesResponse> getAll();

    GetServiceByIdResponse getById(String id);

    void add(CreateServiceRequest createServiceRequest);

    void update(UpdateServiceRequest updateServiceRequest);

    void delete(String id);

}
