package com.renovatipoint.business.abstracts;

import com.renovatipoint.business.requests.CreateJobTitleRequest;
import com.renovatipoint.business.requests.UpdateJobTitleRequest;
import com.renovatipoint.business.responses.GetAllJobTitlesResponse;

import java.util.List;

public interface JobTitleService {
    List<GetAllJobTitlesResponse> getAllJobTitlesResponseList();


    public void add(CreateJobTitleRequest createJobTitleRequest);

    public void update(UpdateJobTitleRequest updateJobTitleRequest);

    public void delete(String id);

   // public void addJobTitleToCategory(CreateJobTitleRequest createJobTitleRequest, String categoryName);
}
