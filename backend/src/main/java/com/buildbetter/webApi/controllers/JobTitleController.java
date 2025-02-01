package com.renovatipoint.webApi.controllers;

import com.renovatipoint.business.abstracts.JobTitleService;
import com.renovatipoint.business.requests.CreateJobTitleRequest;
import com.renovatipoint.business.requests.UpdateJobTitleRequest;
import com.renovatipoint.business.responses.GetAllJobTitlesResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job_titles")
@AllArgsConstructor
public class JobTitleController {
    private JobTitleService jobTitleService;


    @GetMapping
    public List<GetAllJobTitlesResponse> getAllJobTitlesResponseList(){
        return jobTitleService.getAllJobTitlesResponseList();
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    public void add(@RequestBody CreateJobTitleRequest jobTitleRequest){
        this.jobTitleService.add(jobTitleRequest);
    }

    @PutMapping("/job_titles_update")
    public void update(@RequestBody UpdateJobTitleRequest jobTitleRequest){
        this.jobTitleService.update(jobTitleRequest);
    }


//    @PostMapping("/category/{categoryName}/jobTitle")
//    @ResponseStatus(code = HttpStatus.CREATED)
//    public void addJobTitleToCategory(@PathVariable String categoryName, @RequestBody CreateJobTitleRequest createJobTitleRequest){
//        this.jobTitleService.addJobTitleToCategory(createJobTitleRequest, categoryName);
//    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id){
        this.jobTitleService.delete(id);
    }
}
