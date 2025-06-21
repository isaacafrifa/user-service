package iam.userservice.config;

/**
 * This class contains constant values used throughout the application.
 */
public class AppConstants {
    private  AppConstants() {
        // Private constructor to prevent instantiation!!
        // Explicitly declaring private constructor to prevent Java from adding a default public constructor
    }

    // Field name constants for building dynamic specifications and queries.
    public static final String FIRST_NAME_FIELD = "firstName";
    public static final String LAST_NAME_FIELD = "lastName";
    public static final String EMAIL_FIELD = "email";
    public static final String PHONE_NUMBER_FIELD = "phoneNumber";
}
