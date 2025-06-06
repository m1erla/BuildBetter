package com.buildbetter.business.concretes;

import com.buildbetter.business.abstracts.JobTitleService;
import com.buildbetter.business.requests.CreateJobTitleRequest;
import com.buildbetter.business.requests.UpdateJobTitleRequest;
import com.buildbetter.business.responses.GetAllJobTitlesResponse;
import com.buildbetter.business.rules.JobTitleBusinessRules;
import com.buildbetter.core.utilities.mappers.ModelMapperService;
import com.buildbetter.dataAccess.abstracts.CategoryRepository;
import com.buildbetter.dataAccess.abstracts.JobTitleRepository;
import com.buildbetter.entities.concretes.JobTitle;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Lazy
public class JobTitleManager implements JobTitleService {
    public ModelMapperService modelMapperService;
    private JobTitleRepository jobTitleRepository;
    private JobTitleBusinessRules jobTitleBusinessRules;
    private CategoryRepository categoryRepository;

    @Override
    public List<GetAllJobTitlesResponse> getAllJobTitlesResponseList() {
        List<JobTitle> jobTitles = jobTitleRepository.findAll();

        List<GetAllJobTitlesResponse> jobTitlesResponses =
                jobTitles.stream().map(jobTitle -> this.modelMapperService.forResponse().map(jobTitle, GetAllJobTitlesResponse.class)).collect(Collectors.toList());

            return jobTitlesResponses;


    }

    @Override
    public void add(CreateJobTitleRequest createJobTitleRequest) {
        this.jobTitleBusinessRules.checkIfJobTitleNameExists(createJobTitleRequest.getName());
         JobTitle jobTitle = this.modelMapperService.forRequest()
                 .map(createJobTitleRequest, JobTitle.class);
         this.jobTitleRepository.save(jobTitle);
    }

    @Override
    public void update(UpdateJobTitleRequest updateJobTitleRequest) {
          JobTitle jobTitle = this.modelMapperService.forRequest()
                  .map(updateJobTitleRequest, JobTitle.class);
          this.jobTitleRepository.save(jobTitle);
    }

    @Override
    public void delete(String id) {
        this.jobTitleRepository.deleteById(id);
    }
//    @Override
//    public void addJobTitleToCategory(CreateJobTitleRequest createJobTitleRequest, String categoryName){
//        Category category = categoryRepository.findByName(categoryName)
//                .orElseGet(() -> {
//                    Category newCategory = Category.builder()
//                            .name(categoryName)
//                            .isActive(true)
//                            .build();
//                    return categoryRepository.save(newCategory);
//                });
//
//
//        JobTitle jobTitle = modelMapperService.forRequest().map(createJobTitleRequest, JobTitle.class);
//
//        category.addJobTitle(jobTitle);
//
//        categoryRepository.save(category);
//    }
}
