package com.buildbetter.business.abstracts;

import com.buildbetter.business.requests.CreateJobTitleRequest;
import com.buildbetter.business.requests.UpdateJobTitleRequest;
import com.buildbetter.business.responses.GetAllJobTitlesResponse;

import java.util.List;

public interface JobTitleService {
    List<GetAllJobTitlesResponse> getAllJobTitlesResponseList();


    public void add(CreateJobTitleRequest createJobTitleRequest);

    public void update(UpdateJobTitleRequest updateJobTitleRequest);

    public void delete(String id);

   // public void addJobTitleToCategory(CreateJobTitleRequest createJobTitleRequest, String categoryName);
}
