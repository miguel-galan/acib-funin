package com.acibfunin.webscripts;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.security.sync.ChainingUserRegistrySynchronizerStatus;
import org.alfresco.repo.security.sync.UserRegistrySynchronizer;
import org.alfresco.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class LdapSyncWebScript extends DeclarativeWebScript {
    private static final Logger log = LoggerFactory.getLogger(LdapSyncWebScript.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    private ServiceRegistry serviceRegistry;
    private UserRegistrySynchronizer userRegistrySynchronizer;
    private ChainingUserRegistrySynchronizerStatus synchronizerStatus;

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        String currentUser = getCurrentUser();
        validateAdminUser(currentUser);
        log.info("LDAP sync webscript called by user={}. Starting synchronization.", currentUser);
        return synchronizeAndBuildResponse(status);
    }

    private Map<String, Object> synchronizeAndBuildResponse(Status status) {
        try {
            userRegistrySynchronizer.synchronize(true, true);
            return buildSuccessResponse();
        }
        catch (Exception exception) {
            return buildFailureResponse(status, exception);
        }
    }

    private Map<String, Object> buildSuccessResponse() {
        Map<String, Object> response = buildResponse(true, "LDAP synchronization completed successfully.");
        log.info(
            "LDAP sync finished successfully. status='{}' summary='{}' lastRunOnServer='{}'",
            response.get("status"),
            response.get("summary"),
            response.get("lastRunOnServer")
        );
        return response;
    }

    private Map<String, Object> buildFailureResponse(Status status, Exception exception) {
        log.error("LDAP synchronization failed", exception);
        status.setCode(Status.STATUS_INTERNAL_SERVER_ERROR);
        Map<String, Object> response = buildResponse(false, exception.getMessage());
        log.error(
            "LDAP sync failed, status='{}' summary='{}' lastError='{}'",
            response.get("status"),
            response.get("summary"),
            response.get("lastErrorMessage")
        );
        return response;
    }

    private String getCurrentUser() {
        return serviceRegistry.getAuthenticationService().getCurrentUserName();
    }

    private void validateAdminUser(String currentUser) {
        if (!serviceRegistry.getAuthorityService().isAdminAuthority(currentUser)) {
            String message = "User %s is not an administrator. Only administrators can synchronize LDAP.".formatted(currentUser);
            log.warn("LDAP sync denied for non-admin user={}", currentUser);
            throw new WebScriptException(Status.STATUS_FORBIDDEN, message);
        }
    }

    private Map<String, Object> buildResponse(boolean success, String message) {
        Map<String, Object> model = new HashMap<>();
        model.put("success", success);
        model.put("message", message);
        model.put("status", synchronizerStatus.getSynchronizationStatus());
        model.put("summary", safeValue(synchronizerStatus.getSynchronizationStatus()));
        model.put("lastRunOnServer", safeValue(synchronizerStatus.getLastRunOnServer()));
        model.put("lastErrorMessage", safeValue(synchronizerStatus.getLastErrorMessage()));
        model.put("syncStartTime", formatDate(synchronizerStatus.getSyncStartTime()));
        model.put("syncEndTime", formatDate(synchronizerStatus.getSyncEndTime()));
        return model;
    }

    private String formatDate(Date date) {
        return date == null ? "" : DATE_FORMATTER.format(date.toInstant());
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public void setUserRegistrySynchronizer(UserRegistrySynchronizer userRegistrySynchronizer) {
        this.userRegistrySynchronizer = userRegistrySynchronizer;
    }

    public void setSynchronizerStatus(ChainingUserRegistrySynchronizerStatus synchronizerStatus) {
        this.synchronizerStatus = synchronizerStatus;
    }
}
