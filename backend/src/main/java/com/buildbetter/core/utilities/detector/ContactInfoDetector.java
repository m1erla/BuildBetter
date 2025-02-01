package com.renovatipoint.core.utilities.detector;

public class ContactInfoDetector {
    private static final String EMAIL_REGEX ="\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b";
    private static final String PHONE_REGEX = "\\b\\+?\\d{10,14}\\b";

    public static boolean containsContactInformation(String content){
        return content.matches(".*(" + EMAIL_REGEX + "|" + PHONE_REGEX + ").*");
    }
}
