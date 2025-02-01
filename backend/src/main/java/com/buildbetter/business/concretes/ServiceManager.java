package com.renovatipoint.business.concretes;

import com.renovatipoint.business.abstracts.IService;
import com.renovatipoint.business.requests.CreateServiceRequest;
import com.renovatipoint.business.requests.UpdateServiceRequest;
import com.renovatipoint.business.responses.GetAllServicesResponse;
import com.renovatipoint.business.responses.GetServiceByIdResponse;
import com.renovatipoint.business.rules.ServiceBusinessRules;
import com.renovatipoint.core.utilities.mappers.ModelMapperService;
import com.renovatipoint.dataAccess.abstracts.CategoryRepository;
import com.renovatipoint.dataAccess.abstracts.JobTitleRepository;
import com.renovatipoint.dataAccess.abstracts.ServiceRepository;
import com.renovatipoint.entities.concretes.Category;
import com.renovatipoint.entities.concretes.ServiceEntity;
import com.renovatipoint.entities.concretes.JobTitle;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class ServiceManager implements IService {


    public final ModelMapperService modelMapperService;
    private final ServiceRepository serviceRepository;
    private final ServiceBusinessRules serviceBusinessRules;

    private final CategoryRepository categoryRepository;

    private final JobTitleRepository jobTitleRepository;

    public ServiceManager(ModelMapperService modelMapperService, ServiceRepository serviceRepository, ServiceBusinessRules serviceBusinessRules, CategoryRepository categoryRepository, JobTitleRepository jobTitleRepository) {
        this.modelMapperService = modelMapperService;
        this.serviceRepository = serviceRepository;
        this.serviceBusinessRules = serviceBusinessRules;
        this.categoryRepository = categoryRepository;
        this.jobTitleRepository = jobTitleRepository;
    }

    @Override
    public List<GetAllServicesResponse> getAll() {
        List<ServiceEntity> services = serviceRepository.findAll();

        List<GetAllServicesResponse> employmentResponses =
                services.stream().map(employment -> this.modelMapperService.forResponse()
                        .map(employment, GetAllServicesResponse.class)).collect(Collectors.toList());

        return employmentResponses;
    }

    @Override
    public GetServiceByIdResponse getById(String id) {
        ServiceEntity service = this.serviceRepository.findById(id).orElseThrow();

        GetServiceByIdResponse response =
                this.modelMapperService.forResponse().map(service, GetServiceByIdResponse.class);
        return response;
    }

    @Override
    public void add(CreateServiceRequest createServiceRequest) {
        this.serviceBusinessRules.checkIfServiceExists(createServiceRequest.getName());
        Category category = categoryRepository.findByName(createServiceRequest.getCategoryName()).orElseThrow(()-> new EntityNotFoundException("Category not found"));

        JobTitle jobTitle = jobTitleRepository.findByName(createServiceRequest.getJobTitleName()).orElseThrow(() -> new EntityNotFoundException("Job Title not found"));


        ServiceEntity service = this.modelMapperService.forRequest().map(createServiceRequest, ServiceEntity.class);

        service.setCategory(category);
        service.setJobTitle(jobTitle);

        this.serviceRepository.save(service);
    }

    @Override
    public void update(UpdateServiceRequest updateServiceRequest) {
        ServiceEntity service = this.modelMapperService.forRequest().map(updateServiceRequest, ServiceEntity.class);
        this.serviceRepository.save(service);
    }

    @Override
    public void delete(String id) {
        this.serviceRepository.deleteById(id);

    }
}
