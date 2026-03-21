package com.acibfunin.webscripts;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * GET webscript that returns a JSON with the list of documents currently in the trashcan
 * (i.e., nodes in the archive store). The JSON structure is:
 * { "deleted": [ "<name> (<nodeRef>)", ... ] }
 */
public class ListDeletedDocuments extends AbstractWebScript {
    private static final Logger LOG = LoggerFactory.getLogger(ListDeletedDocuments.class);
    private static final String APPLICATION_JSON = "application/json";
    private static final String UTF_8 = "UTF-8";
    private static final String UNKNOWN = "unknown";
    private static final String NO_NAME = "(no-name)";
    private static final String CONTENT_QUERY = "TYPE:\"cm:content\"";
    private static final int MAX_RESULTS = 10000;
    private static final Pattern ALFRESCO_MODEL_NAMESPACE_QNAME_PATTERN =
            Pattern.compile("\\{http://www\\.alfresco\\.org/model/[^}]+/1\\.0\\}");

    private ServiceRegistry serviceRegistry;
    private transient SearchService searchService;
    private transient NodeService nodeService;

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        this.searchService = serviceRegistry.getSearchService();
        this.nodeService = serviceRegistry.getNodeService();
    }

    @Override
    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
        try {
            DeletedDocumentsResult result = collectDeletedDocuments();
            writeJsonResponse(res, createResponseJson(result));
        } catch (Exception e) {
            LOG.error("Failed to list deleted documents", e);
            setBadRequestResponse(res, e.getMessage());
        }
    }

    private void setBadRequestResponse(WebScriptResponse response, String message) throws IOException {
        response.setContentType(APPLICATION_JSON);
        response.setStatus(Status.STATUS_BAD_REQUEST);
        response.getWriter().write(message);
    }

    private DeletedDocumentsResult collectDeletedDocuments() {
        DeletedDocumentsResult result = new DeletedDocumentsResult();
        ResultSet rs = null;
        try {
            rs = searchService.query(createArchiveSearchParameters());
            for (NodeRef archivedRef : rs.getNodeRefs()) {
                classifyArchivedNode(archivedRef, result);
            }
        } finally {
            closeQuietly(rs);
        }
        return result;
    }

    private SearchParameters createArchiveSearchParameters() {
        SearchParameters sp = new SearchParameters();
        sp.addStore(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
        sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
        sp.setQuery(CONTENT_QUERY);
        sp.setMaxItems(MAX_RESULTS);
        return sp;
    }

    private void classifyArchivedNode(NodeRef archivedNodeRef, DeletedDocumentsResult result) {
        ArchivedDocument archivedDocument = readArchivedDocument(archivedNodeRef);
        List<String> target = archivedDocument.replaced() ? result.replaced : result.deleted;
        target.add(buildEntryString(archivedDocument));
    }

    private ArchivedDocument readArchivedDocument(NodeRef archivedNodeRef) {
        String name = (String) nodeService.getProperty(archivedNodeRef, ContentModel.PROP_NAME);
        String deletedWhen = formatArchivedDate((Date) nodeService.getProperty(archivedNodeRef, ContentModel.PROP_ARCHIVED_DATE));
        NodeRef originalParent = getOriginalParent(archivedNodeRef);
        String originalPath = resolveOriginalPath(originalParent);
        boolean replaced = isReplacement(name, originalParent);
        return new ArchivedDocument(name, archivedNodeRef, originalPath, deletedWhen, replaced);
    }

    private boolean isReplacement(String name, NodeRef originalParent) {
        if (name == null || originalParent == null || !nodeService.exists(originalParent)) {
            return false;
        }
        NodeRef child = nodeService.getChildByName(originalParent, ContentModel.ASSOC_CONTAINS, name);
        return child != null && nodeService.exists(child);
    }

    private String buildEntryString(ArchivedDocument archivedDocument) {
        String safeName = archivedDocument.name != null ? archivedDocument.name : NO_NAME;
        String cleanedPath = stripAlfrescoModelNamespaces(archivedDocument.originalPath);
        return "%s, (%s), originalPath: %s, deleted: %s".formatted(
                safeName, archivedDocument.nodeRef, cleanedPath, archivedDocument.deletedWhen);
    }

    private String stripAlfrescoModelNamespaces(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        return ALFRESCO_MODEL_NAMESPACE_QNAME_PATTERN.matcher(path).replaceAll("");
    }

    private NodeRef getOriginalParent(NodeRef archivedNodeRef) {
        Serializable property = nodeService.getProperty(archivedNodeRef, ContentModel.PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC);
        if (!(property instanceof ChildAssociationRef originalParentAssoc)) {
            return null;
        }
        return originalParentAssoc.getParentRef();
    }

    private String resolveOriginalPath(NodeRef originalParent) {
        if (originalParent == null || !nodeService.exists(originalParent)) {
            return UNKNOWN;
        }
        Path parentPath = nodeService.getPath(originalParent);
        return parentPath.toString();
    }

    private String formatArchivedDate(Date archivedDate) {
        if (archivedDate == null) {
            return UNKNOWN;
        }
        Instant instant = archivedDate.toInstant();
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atOffset(ZoneOffset.UTC));
    }

    private JSONObject createResponseJson(DeletedDocumentsResult result) {
        JSONObject json = new JSONObject();
        json.put("replaced", new JSONArray(result.replaced));
        json.put("deleted", new JSONArray(result.deleted));
        return json;
    }

    private void writeJsonResponse(WebScriptResponse response, JSONObject json) throws IOException {
        response.setContentType(APPLICATION_JSON);
        response.setContentEncoding(UTF_8);
        response.getWriter().write(json.toString());
    }

    private void closeQuietly(ResultSet resultSet) {
        if (resultSet != null) {
            resultSet.close();
        }
    }

    private static final class DeletedDocumentsResult {
        private final List<String> replaced = new ArrayList<>();
        private final List<String> deleted = new ArrayList<>();
    }

    private record ArchivedDocument(String name, NodeRef nodeRef, String originalPath, String deletedWhen,
                                    boolean replaced) {
    }
}
