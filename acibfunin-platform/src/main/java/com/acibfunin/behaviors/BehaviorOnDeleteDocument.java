package com.acibfunin.behaviors;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.node.NodeServicePolicies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * Repository behavior that vetoes deletion of documents that are part of an active workflow.
 */
public class BehaviorOnDeleteDocument implements NodeServicePolicies.BeforeDeleteNodePolicy, InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(BehaviorOnDeleteDocument.class);

    private PolicyComponent policyComponent;
    private NodeService nodeService;
    private DictionaryService dictionaryService;
    private WorkflowService workflowService;

    // Setters for Spring
    public void setPolicyComponent(PolicyComponent policyComponent) { this.policyComponent = policyComponent; }
    public void setNodeService(NodeService nodeService) { this.nodeService = nodeService; }
    public void setDictionaryService(DictionaryService dictionaryService) { this.dictionaryService = dictionaryService; }
    public void setWorkflowService(WorkflowService workflowService) { this.workflowService = workflowService; }

    @Override
    public void afterPropertiesSet() {
        // Bind behaviour to all types derived from cm:content (i.e., documents)
        policyComponent.bindClassBehaviour(
                NodeServicePolicies.BeforeDeleteNodePolicy.QNAME,
                ContentModel.TYPE_CONTENT,
                new JavaBehaviour(this, "beforeDeleteNode", NotificationFrequency.EVERY_EVENT)
        );
    }

    @Override
    public void beforeDeleteNode(NodeRef nodeRef) {
        if (!canContinue(nodeRef)) {
            return;
        }

        // Check for active and completed workflows that reference the node
        List<WorkflowInstance> allWorkflows = workflowService.getWorkflowsForContent(nodeRef, true);
        List<WorkflowInstance> completedWorkflows = workflowService.getWorkflowsForContent(nodeRef, false);
        allWorkflows.addAll(completedWorkflows);

        if (!allWorkflows.isEmpty()) {
            // Throwing a runtime exception vetoes the delete operation
            String message = "Deletion blocked: the document " + nodeRef.getId() + " is part of a workflow.";
            LOG.warn(message);
            throw new AlfrescoRuntimeException(message);
        }
    }

    private boolean canContinue(NodeRef nodeRef) {
        // Verify node exists and is a document (cm:content or subtype)
        QName type;
        try {
            type = nodeService.getType(nodeRef);
        } catch (InvalidNodeRefException e) {
            return false; // If it doesn't exist anymore, nothing to do
        }
        return dictionaryService.isSubClass(type, ContentModel.TYPE_CONTENT); // Only apply to documents
    }
}
