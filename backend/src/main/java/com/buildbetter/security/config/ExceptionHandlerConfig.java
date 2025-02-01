package com.renovatipoint.security.config;

import com.renovatipoint.core.utilities.exceptions.BusinessException;
import com.renovatipoint.core.utilities.exceptions.ProblemDetails;
import com.renovatipoint.core.utilities.exceptions.ValidationProblemDetails;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
@Component
@RequiredArgsConstructor
public class ExceptionHandlerConfig {

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ProblemDetails handleBusinessException(BusinessException businessException){
        ProblemDetails problemDetails = new ProblemDetails();
        problemDetails.setMessage(businessException.getMessage());

        return problemDetails;
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ProblemDetails handleValidationException(MethodArgumentNotValidException validException){
        ValidationProblemDetails validationProblemDetails = new ValidationProblemDetails();
        validationProblemDetails.setMessage("VALIDATION.EXCEPTION");
        validationProblemDetails.setValidationErrors(new HashMap<String, String>());

        for(FieldError fieldError : validException.getBindingResult().getFieldErrors()){
            validationProblemDetails.getValidationErrors().put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return validationProblemDetails;
    }
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
