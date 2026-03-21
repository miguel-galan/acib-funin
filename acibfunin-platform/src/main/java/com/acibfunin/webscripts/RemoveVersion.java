package com.acibfunin.webscripts;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class RemoveVersion extends DeclarativeWebScript {

    private static final Logger log = LoggerFactory.getLogger(RemoveVersion.class);
    private static final String SUCCESS_KEY = "success";
    private static final String SUCCESS_VALUE = "true";
    private static final String LABEL_PARAM = "label";

    private ServiceRegistry serviceRegistry;

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        log.error("Executing RemoveVersion webscript");

        validateAdminUser();
        NodeRef nodeRef = extractNodeRef(req);
        String versionLabel = extractVersionLabel(req);
        deleteVersion(nodeRef, versionLabel);

        return buildSuccessResponse();
    }

    private void validateAdminUser() {
        String currentUser = serviceRegistry.getAuthenticationService().getCurrentUserName();
        if (!serviceRegistry.getAuthorityService().isAdminAuthority(currentUser)) {
			String message = "User " + currentUser + " is not an administrator. Only administrators can remove versions.";
			log.error(message);
            throw new WebScriptException(Status.STATUS_FORBIDDEN, message);
        }
    }

    private NodeRef extractNodeRef(WebScriptRequest req) {
        Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
        return new NodeRef(templateArgs.get("store_type"), templateArgs.get("store_id"), templateArgs.get("id"));
    }

    private String extractVersionLabel(WebScriptRequest req) {
        try {
            JSONObject json = new JSONObject(new JSONTokener(req.getContent().getContent()));
            return json.getString(LABEL_PARAM);
        } catch (Exception e) {
            throw new WebScriptException(
                Status.STATUS_BAD_REQUEST,
                "Could not read parameter '" + LABEL_PARAM + "' from request content.",
                e
            );
        }
    }

    private void deleteVersion(NodeRef nodeRef, String versionLabel) {
        VersionService versionService = serviceRegistry.getVersionService();
        Version version = versionService.getVersionHistory(nodeRef).getVersion(versionLabel);
        versionService.deleteVersion(nodeRef, version);
    }

    private Map<String, Object> buildSuccessResponse() {
        Map<String, Object> model = new HashMap<>();
        model.put(SUCCESS_KEY, SUCCESS_VALUE);
        return model;
    }
}
