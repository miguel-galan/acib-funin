package com.acibfunin.listeners;

import com.acibfunin.constants.TaskProperties;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReviewTaskListener implements TaskListener {
    private static final Logger LOG = LoggerFactory.getLogger(ReviewTaskListener.class);

    private NodeService nodeService;
    private WorkflowService workflowService;
    private SysAdminParams sysAdminParams;
    private PermissionService permissionService;
    private VersionService versionService;

    private String shareUrl;

    @Override
    public void notify(DelegateTask task) {
        List<ChildAssociationRef> nodeRefDocumentList = getNodeRefDocumentList(task);
        setTaskDocumentsVariables(task, nodeRefDocumentList);
    }

    public void setTaskDocumentsVariables(DelegateTask task, List<ChildAssociationRef> nodeRefDocumentList) {
        JSONArray arr = new JSONArray();

        for (ChildAssociationRef oneDocAssociation : nodeRefDocumentList) {
            NodeRef oneDocNodeRef = oneDocAssociation.getChildRef();
            JSONObject o = new JSONObject();
            o.put(TaskProperties.FILENAME, getFilename(oneDocNodeRef));
            o.put(TaskProperties.DOC_NODE_REF, oneDocNodeRef);
            o.put(TaskProperties.PARENT_FOLDER_NODE_REF, getParentFolder(oneDocNodeRef));
            o.put(TaskProperties.PARENT_FOLDER_PATH, getParentFolderPath(oneDocNodeRef));
            o.put(TaskProperties.DOC_PATH, getDocumentPath(oneDocNodeRef));
            o.put(TaskProperties.COMPLETION_DATE, new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
            o.put(TaskProperties.USER, task.getAssignee());
            o.put(TaskProperties.DOC_VERSION, getVersion(oneDocNodeRef));
            arr.put(o);
        }
        task.setVariableLocal(TaskProperties.INFO_LIST, arr.toString());
        LOG.debug("Task variables: " + task.getVariables());

    }

    private List<ChildAssociationRef> getNodeRefDocumentList(DelegateTask task) {
        QName qnamePackage = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "package");
        WorkflowTask workflowTask = workflowService.getTaskById("activiti$" + task.getId());

        String packageNodeId = workflowTask.getProperties().get(qnamePackage).toString();
        NodeRef packageNodeRef = new NodeRef(packageNodeId);
        return nodeService.getChildAssocs(packageNodeRef);
    }

    private String getFilename(NodeRef oneDocNodeRef) {
        return (String) nodeService.getProperty(oneDocNodeRef, ContentModel.PROP_NAME);
    }

    private String getParentFolderPath(NodeRef fileNodeRef) {
        NodeRef folderNodeRef = getParentFolder(fileNodeRef);
        String parentFolderPath =
                nodeService.getPath(folderNodeRef).toDisplayPath(nodeService, permissionService) + "/" +
                        nodeService.getProperty(folderNodeRef, ContentModel.PROP_NAME);
        return parentFolderPath;
    }

    private String getDocumentPath(NodeRef oneDocNodeRef) {
        NodeRef parentFolderNodeRef = getParentFolder(oneDocNodeRef);
        return nodeService.getPath(parentFolderNodeRef).toDisplayPath(nodeService, permissionService) + "/" +
                nodeService.getProperty(parentFolderNodeRef, ContentModel.PROP_NAME);
    }

    private NodeRef getParentFolder(NodeRef oneDocNodeRef) {
        return nodeService.getPrimaryParent(oneDocNodeRef).getParentRef();
    }

    private String getVersion(NodeRef nodeRef) {
        VersionHistory history = versionService.getVersionHistory(nodeRef);
        return history != null ? history.getHeadVersion().getVersionLabel() : null;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void setSysAdminParams(SysAdminParams sysAdminParams) {
        this.sysAdminParams = sysAdminParams;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setVersionService(VersionService versionService) {
        this.versionService = versionService;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }
}