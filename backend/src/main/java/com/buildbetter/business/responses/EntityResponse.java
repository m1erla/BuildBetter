package com.renovatipoint.business.responses;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class EntityResponse {

    public static ResponseEntity<Object> generateResponse(String message, HttpStatus status, Object responseObj) {

        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("TimeStamp", new Date());
        map.put("Message", message);
        map.put("Status", status.value());
        map.put("Data", responseObj);

        return new ResponseEntity<Object>(map, status);

    }

    public static ResponseEntity<Object> generateErrorResponse(String s, HttpStatus httpStatus) {
        return new ResponseEntity<Object>(s, httpStatus);
    }
}