package com.buildbetter.webApi.controllers;

import com.buildbetter.business.abstracts.IService;
import com.buildbetter.business.requests.CreateServiceRequest;
import com.buildbetter.business.requests.UpdateServiceRequest;
import com.buildbetter.business.responses.GetAllServicesResponse;
import com.buildbetter.business.responses.GetServiceByIdResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@AllArgsConstructor
public class ServicesController {
    private IService iService;

    @GetMapping
    public List<GetAllServicesResponse> getAllServices(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String exclude,
            @RequestParam(required = false, defaultValue = "10") int limit // Örnek limit
    // Gerekirse diğer parametreler
    ) {
        // iService.getAll() metodunu bu parametreleri alacak şekilde güncelleyin
        return iService.getAll(categoryId, exclude, limit);
    }

    @GetMapping("/{id}")
    public GetServiceByIdResponse getServiceById(@PathVariable String id) {
        return iService.getById(id);
    }

    @PostMapping("/service")
    @ResponseStatus(code = HttpStatus.CREATED)
    public void add(@RequestBody CreateServiceRequest createServiceRequest) {
        this.iService.add(createServiceRequest);
    }

    @PutMapping("/service_update/{id}")
    public void update(@RequestBody UpdateServiceRequest updateServiceRequest) {
        this.iService.update(updateServiceRequest);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        this.iService.delete(id);
    }
}
