package com.acibfunin.webscripts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Web Script to replace the content of a node with a sibling node named 'documento-fuente'.
 * Endpoint: POST /service/com/acibfunin/content/copy
 */
public class ReplaceCurrentVersionContentWebScript extends DeclarativeWebScript {

    private static final Logger logger = LoggerFactory.getLogger(ReplaceCurrentVersionContentWebScript.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SOURCE_NODE_NAME = "documento-fuente";
    private static final DateTimeFormatter BACKUP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private NodeService nodeService;
    private ContentService contentService;
    private VersionService versionService;
    private CopyService copyService;

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void setVersionService(VersionService versionService) {
        this.versionService = versionService;
    }

    public void setCopyService(CopyService copyService) {
        this.copyService = copyService;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = createResponseModel();
        List<String> notes = getNotes(model);
        executeCopyRequest(req, model, notes);
        return model;
    }

    private void executeCopyRequest(WebScriptRequest req, Map<String, Object> model, List<String> notes) {
        try {
            executeCopyAsSystem(req, model, notes);
        } catch (WebScriptException wse) {
            logger.error("WebScript error: %s".formatted(wse.getMessage()));
            throw wse;
        } catch (Exception e) {
            handleInternalCopyError(e, model);
        }
    }

    private void executeCopyAsSystem(WebScriptRequest req, Map<String, Object> model, List<String> notes) throws Exception {
        NodeRef targetRef = parseAndValidateTargetNodeRef(req);
        model.put("targetNodeRef", targetRef.toString());
        AuthenticationUtil.runAsSystem(() -> {
            processCopy(targetRef, model, notes);
            return null;
        });
        model.put("success", true);
    }

    private void handleInternalCopyError(Exception e, Map<String, Object> model) {
        logger.error("Internal error during copy operation", e);
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        error.put("type", e.getClass().getName());
        model.put("error", error);
        model.put("success", false);
    }

    private void processCopy(NodeRef targetRef, Map<String, Object> model, List<String> notes) {
        NodeRef parentRef = resolveParentFolder(targetRef);
        NodeRef sourceRef = resolveAndStoreSourceNode(parentRef, model);
        createAndStoreBackupCopy(targetRef, parentRef, model, notes);

        ContentReader sourceReader = getRequiredContentReader(sourceRef);
        addSourceContentMetadata(sourceReader, model);
        String versionBefore = addVersionLabelBefore(targetRef, model);
        copyContentWithoutVersioning(sourceReader, targetRef, notes);
        finalizeCopyResult(targetRef, versionBefore, model, notes);
    }

    private NodeRef resolveAndStoreSourceNode(NodeRef parentRef, Map<String, Object> model) {
        NodeRef sourceRef = resolveSourceNode(parentRef);
        model.put("resolvedSourceNodeRef", sourceRef.toString());
        return sourceRef;
    }

    private void createAndStoreBackupCopy(NodeRef targetRef, NodeRef parentRef, Map<String, Object> model, List<String> notes) {
        BackupInfo backupInfo = createBackupCopy(targetRef, parentRef);
        model.put("backupNodeRef", backupInfo.ref.toString());
        model.put("backupName", backupInfo.name);
        notes.add("Created backup of target node: %s".formatted(backupInfo.name));
    }

    private String addVersionLabelBefore(NodeRef targetRef, Map<String, Object> model) {
        String versionBefore = getVersionLabel(targetRef);
        model.put("versionLabelBefore", versionBefore);
        return versionBefore;
    }

    private void copyContentWithoutVersioning(ContentReader sourceReader, NodeRef targetRef, List<String> notes) {
        VersioningState versioningState = disableVersioning(targetRef, notes);
        try {
            copyContent(sourceReader, targetRef);
        } finally {
            restoreVersioning(targetRef, versioningState, notes);
        }
    }

    private void finalizeCopyResult(NodeRef targetRef, String versionBefore, Map<String, Object> model, List<String> notes) {
        String versionAfter = getVersionLabel(targetRef);
        model.put("versionLabelAfter", versionAfter);
        addTargetContentMetadata(targetRef, model);
        warnIfVersionChanged(versionBefore, versionAfter, notes);
    }

    private Map<String, Object> createResponseModel() {
        Map<String, Object> model = new HashMap<>();
        model.put("notes", new ArrayList<String>());
        model.put("success", false);
        return model;
    }

    @SuppressWarnings("unchecked")
    private List<String> getNotes(Map<String, Object> model) {
        return (List<String>) model.get("notes");
    }

    private NodeRef parseAndValidateTargetNodeRef(WebScriptRequest req) throws Exception {
        JsonNode requestJson = objectMapper.readTree(req.getContent().getContent());
        String targetNodeRefStr = requestJson.path("targetNodeRef").asText();

        if (targetNodeRefStr.isEmpty() || !NodeRef.isNodeRef(targetNodeRefStr)) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid or missing targetNodeRef");
        }

        NodeRef targetRef = new NodeRef(targetNodeRefStr);
        if (!nodeService.exists(targetRef)) {
            throw new WebScriptException(
                    Status.STATUS_NOT_FOUND,
                    "Target node not found: %s".formatted(targetNodeRefStr)
            );
        }
        return targetRef;
    }

    private NodeRef resolveParentFolder(NodeRef targetRef) {
        ChildAssociationRef parentAssoc = nodeService.getPrimaryParent(targetRef);
        if (parentAssoc == null || parentAssoc.getParentRef() == null) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Target node has no parent folder.");
        }
        return parentAssoc.getParentRef();
    }

    private NodeRef resolveSourceNode(NodeRef parentRef) {
        NodeRef sourceRef = nodeService.getChildByName(parentRef, ContentModel.ASSOC_CONTAINS, SOURCE_NODE_NAME);
        if (sourceRef == null) {
            throw new WebScriptException(
                Status.STATUS_NOT_FOUND,
                "Source sibling node '%s' not found in parent folder.".formatted(SOURCE_NODE_NAME)
            );
        }
        return sourceRef;
    }

    private BackupInfo createBackupCopy(NodeRef targetRef, NodeRef parentRef) {
        String backupName = buildBackupName(targetRef);
        QName assocQName = QName.createQName(
                NamespaceService.CONTENT_MODEL_1_0_URI,
                QName.createValidLocalName(backupName)
        );
        NodeRef backupRef = copyService.copy(targetRef, parentRef, ContentModel.ASSOC_CONTAINS, assocQName);
        nodeService.setProperty(backupRef, ContentModel.PROP_NAME, backupName);
        return new BackupInfo(backupRef, backupName);
    }

    private String buildBackupName(NodeRef targetRef) {
        String targetName = (String) nodeService.getProperty(targetRef, ContentModel.PROP_NAME);
        return "%s copia %s".formatted(targetName, LocalDate.now().format(BACKUP_DATE_FORMAT));
    }

    private ContentReader getRequiredContentReader(NodeRef sourceRef) {
        ContentReader reader = contentService.getReader(sourceRef, ContentModel.PROP_CONTENT);
        if (reader == null || !reader.exists()) {
            throw new WebScriptException(
                Status.STATUS_BAD_REQUEST,
                "Source node '%s' has no binary content.".formatted(SOURCE_NODE_NAME)
            );
        }
        return reader;
    }

    private void addSourceContentMetadata(ContentReader reader, Map<String, Object> model) {
        model.put("sourceMimetype", reader.getMimetype());
        model.put("sourceSize", reader.getSize());
    }

    private VersioningState disableVersioning(NodeRef targetRef, List<String> notes) {
        boolean hadVersionable = nodeService.hasAspect(targetRef, ContentModel.ASPECT_VERSIONABLE);
        Boolean originalAutoVersion = (Boolean) nodeService.getProperty(targetRef, ContentModel.PROP_AUTO_VERSION);
        Boolean originalAutoVersionProps = (Boolean) nodeService.getProperty(targetRef, ContentModel.PROP_AUTO_VERSION_PROPS);

        nodeService.setProperty(targetRef, ContentModel.PROP_AUTO_VERSION, false);
        nodeService.setProperty(targetRef, ContentModel.PROP_AUTO_VERSION_PROPS, false);
        notes.add("Temporarily disabled auto-versioning.");

        if (hadVersionable) {
            nodeService.removeAspect(targetRef, ContentModel.ASPECT_VERSIONABLE);
            notes.add("Temporarily removed cm:versionable aspect.");
        }

        return new VersioningState(hadVersionable, originalAutoVersion, originalAutoVersionProps);
    }

    private void copyContent(ContentReader sourceReader, NodeRef targetRef) {
        ContentWriter writer = contentService.getWriter(targetRef, ContentModel.PROP_CONTENT, true);
        writer.setMimetype(sourceReader.getMimetype());
        writer.setEncoding(sourceReader.getEncoding());
        writer.setLocale(sourceReader.getLocale());
        writer.putContent(sourceReader);
    }

    private void restoreVersioning(NodeRef targetRef, VersioningState state, List<String> notes) {
        if (state.hadVersionable && !nodeService.hasAspect(targetRef, ContentModel.ASPECT_VERSIONABLE)) {
            nodeService.addAspect(targetRef, ContentModel.ASPECT_VERSIONABLE, null);
            notes.add("Restored cm:versionable aspect.");
        }

        if (state.originalAutoVersion != null) {
            nodeService.setProperty(targetRef, ContentModel.PROP_AUTO_VERSION, state.originalAutoVersion);
        }
        if (state.originalAutoVersionProps != null) {
            nodeService.setProperty(targetRef, ContentModel.PROP_AUTO_VERSION_PROPS, state.originalAutoVersionProps);
        }
        notes.add("Restored original auto-versioning settings.");
    }

    private void addTargetContentMetadata(NodeRef targetRef, Map<String, Object> model) {
        ContentReader targetReader = contentService.getReader(targetRef, ContentModel.PROP_CONTENT);
        if (targetReader != null) {
            model.put("targetMimetype", targetReader.getMimetype());
            model.put("targetSize", targetReader.getSize());
        }
    }

    private void warnIfVersionChanged(String versionBefore, String versionAfter, List<String> notes) {
        if (!versionBefore.equals(versionAfter)) {
            logger.warn("Version label changed from {} to {} despite measures.", versionBefore, versionAfter);
            notes.add("WARNING: Version label changed unexpectedly.");
        }
    }

    private String getVersionLabel(NodeRef nodeRef) {
        if (nodeService.hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE)) {
            VersionHistory history = versionService.getVersionHistory(nodeRef);
            if (history != null && history.getHeadVersion() != null) {
                return history.getHeadVersion().getVersionLabel();
            }
        }
        // If not versionable or no history, return a default indicator
        return "1.0";
    }

    private static final class BackupInfo {
        private final NodeRef ref;
        private final String name;

        private BackupInfo(NodeRef ref, String name) {
            this.ref = ref;
            this.name = name;
        }
    }

    private static final class VersioningState {
        private final boolean hadVersionable;
        private final Boolean originalAutoVersion;
        private final Boolean originalAutoVersionProps;

        private VersioningState(boolean hadVersionable, Boolean originalAutoVersion, Boolean originalAutoVersionProps) {
            this.hadVersionable = hadVersionable;
            this.originalAutoVersion = originalAutoVersion;
            this.originalAutoVersionProps = originalAutoVersionProps;
        }
    }
}
