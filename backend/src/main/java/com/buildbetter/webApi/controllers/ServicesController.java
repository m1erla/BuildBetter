package com.renovatipoint.webApi.controllers;

import com.renovatipoint.business.abstracts.IService;
import com.renovatipoint.business.requests.CreateServiceRequest;
import com.renovatipoint.business.requests.UpdateServiceRequest;
import com.renovatipoint.business.responses.GetAllServicesResponse;
import com.renovatipoint.business.responses.GetServiceByIdResponse;
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
    public List<GetAllServicesResponse> getAllServices(){
        return iService.getAll();
    }

    @GetMapping("/{id}")
    public GetServiceByIdResponse getServiceById(@PathVariable String id){
        return iService.getById(id);
    }

    @PostMapping("/service")
    @ResponseStatus(code = HttpStatus.CREATED)
    public void add(@RequestBody CreateServiceRequest createServiceRequest){
        this.iService.add(createServiceRequest);
    }

    @PutMapping("/service_update/{id}")
    public void update(@RequestBody UpdateServiceRequest updateServiceRequest){
        this.iService.update(updateServiceRequest);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id){
        this.iService.delete(id);
    }
}
