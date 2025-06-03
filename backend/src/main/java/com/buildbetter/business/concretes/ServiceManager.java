package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.IService;
import com.buildbetter.business.requests.CreateServiceRequest;
import com.buildbetter.business.requests.UpdateServiceRequest;
import com.buildbetter.business.responses.GetAllServicesResponse;
import com.buildbetter.business.responses.GetServiceByIdResponse;
import com.buildbetter.business.rules.ServiceBusinessRules;
import com.buildbetter.core.utilities.mappers.ModelMapperService;
import com.buildbetter.dataAccess.abstracts.CategoryRepository;
import com.buildbetter.dataAccess.abstracts.JobTitleRepository;
import com.buildbetter.dataAccess.abstracts.ServiceRepository;
import com.buildbetter.entities.concretes.Category;
import com.buildbetter.entities.concretes.JobTitle;
import com.buildbetter.entities.concretes.ServiceEntity;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public ServiceManager(ModelMapperService modelMapperService, ServiceRepository serviceRepository,
            ServiceBusinessRules serviceBusinessRules, CategoryRepository categoryRepository,
            JobTitleRepository jobTitleRepository) {
        this.modelMapperService = modelMapperService;
        this.serviceRepository = serviceRepository;
        this.serviceBusinessRules = serviceBusinessRules;
        this.categoryRepository = categoryRepository;
        this.jobTitleRepository = jobTitleRepository;
    }

    @Override
    public List<GetAllServicesResponse> getAll() {
        List<ServiceEntity> services = serviceRepository.findAll();

        List<GetAllServicesResponse> employmentResponses = services.stream()
                .map(employment -> this.modelMapperService.forResponse()
                        .map(employment, GetAllServicesResponse.class))
                .collect(Collectors.toList());

        return employmentResponses;
    }

    @Override
    public GetServiceByIdResponse getById(String id) {
        ServiceEntity service = this.serviceRepository.findById(id).orElseThrow();

        GetServiceByIdResponse response = this.modelMapperService.forResponse().map(service,
                GetServiceByIdResponse.class);
        return response;
    }

    @Override
    public void add(CreateServiceRequest createServiceRequest) {
        this.serviceBusinessRules.checkIfServiceExists(createServiceRequest.getName());
        Category category = categoryRepository.findByName(createServiceRequest.getCategoryName())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        JobTitle jobTitle = jobTitleRepository.findByName(createServiceRequest.getJobTitleName())
                .orElseThrow(() -> new EntityNotFoundException("Job Title not found"));

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

    @Override
    public List<GetAllServicesResponse> getAll(String categoryId, String exclude, int limit) {
        List<ServiceEntity> services;

        if (categoryId != null && !categoryId.isEmpty()) {
            // Kategori ID'ye göre filtreleme
            if (exclude != null && !exclude.isEmpty()) {
                // Belirli bir servis ID'sini hariç tut
                if (limit > 0) {
                    // Sayfa boyutunu sınırla
                    Pageable pageable = PageRequest.of(0, limit);
                    services = serviceRepository.findByCategoryIdAndIdNot(categoryId, exclude, pageable);
                } else {
                    // Limit yoksa tüm sonuçları getir
                    services = serviceRepository.findByCategoryIdAndIdNot(categoryId, exclude);
                }
            } else {
                // Hariç tutma parametresi yoksa
                if (limit > 0) {
                    // Sayfa boyutunu sınırla
                    Pageable pageable = PageRequest.of(0, limit);
                    services = serviceRepository.findByCategoryId(categoryId, pageable);
                } else {
                    // Limit yoksa tüm sonuçları getir
                    services = serviceRepository.findByCategoryId(categoryId);
                }
            }
        } else {
            // Kategori ID parametresi yoksa
            if (limit > 0) {
                // Sayfa boyutunu sınırla
                Pageable pageable = PageRequest.of(0, limit);
                services = serviceRepository.findAll(pageable).getContent();
            } else {
                // Limit yoksa tüm servis listesini getir
                services = serviceRepository.findAll();
            }
        }

        // Servis entity'lerini response modellerine dönüştürme
        List<GetAllServicesResponse> serviceResponses = services.stream()
                .map(service -> {
                    GetAllServicesResponse response = this.modelMapperService.forResponse()
                            .map(service, GetAllServicesResponse.class);

                    // Eğer servis entity'sinde kategori ve iş unvanı varsa
                    if (service.getCategory() != null) {
                        response.setCategoryId(service.getCategory().getId());
                        response.setCategoryName(service.getCategory().getName());
                    }

                    if (service.getJobTitle() != null) {
                        response.setJobTitleName(service.getJobTitle().getName());
                    }

                    return response;
                })
                .collect(Collectors.toList());

        return serviceResponses;
    }
}
