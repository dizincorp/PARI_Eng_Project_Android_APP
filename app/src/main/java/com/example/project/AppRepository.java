package com.example.project;

/**
 * Forwarder class for backward compatibility.
 *
 * @deprecated Moved to {@link com.example.project.repository.AppRepository}
 */
@Deprecated
public class AppRepository {
    public static com.example.project.repository.AppRepository getInstance() {
        return com.example.project.repository.AppRepository.getInstance();
    }
}