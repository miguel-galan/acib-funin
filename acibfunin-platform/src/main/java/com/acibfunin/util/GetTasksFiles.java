package com.acibfunin.util;

import com.ibm.icu.text.SimpleDateFormat;
import com.acibfunin.constants.TaskProperties;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.cmr.workflow.WorkflowTaskQuery;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.surf.util.URLEncoder;
import org.springframework.extensions.webscripts.AbstractWebScript;

import java.io.Serializable;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GetTasksFiles extends AbstractWebScript {

    private static final String reviewerGroup = "GROUP_CONTROL_CALIDAD";
    private static final Logger LOGGER = LoggerFactory.getLogger(GetTasksFiles.class);

    private SysAdminParams sysAdminParams;
    private ServiceRegistry serviceRegistry;

    private String taskShareUrl;

    public List<List<Object>> getData(String year, String userId) {

        boolean runSystem = false;
        List<List<Object>> listAnswer = new ArrayList<>();

        try {

            // Current user does not include userId in the request
            if (userId == null) {
                userId = AuthenticationUtil.getFullyAuthenticatedUser();
            } else {

                AuthenticationUtil.pushAuthentication();
                AuthenticationUtil.setRunAsUserSystem();
                runSystem = true;

                Set<String> authorities = serviceRegistry.getAuthorityService().getAuthoritiesForUser(AuthenticationUtil.getFullyAuthenticatedUser());
                boolean member = false;
                for (String authority : authorities) {
                    if (authority.equals(reviewerGroup)) {
                        member = true;
                        break;
                    }
                }

                if (!member) {
                    throw new RuntimeException("Users must belong to group " + reviewerGroup + " in order to access other users approval list tasks!");
                }

            }

            // Local services
            WorkflowService workflowService = serviceRegistry.getWorkflowService();

            // Create qnames need for get information
            QName qnameOutcome = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "outcome");

            Date filterYearStart = null;
            Date filterYearEnd = null;
            Boolean filterDate = true;

            // Create filters for the year
            if (year == null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                year = String.valueOf(cal.get(Calendar.YEAR));
            }

            if (!year.equals("*")) {
                Calendar calendar = Calendar.getInstance();
                calendar.clear();
                calendar.set(Calendar.YEAR, Integer.valueOf(year));
                calendar.set(Calendar.DAY_OF_YEAR, 1);
                filterYearStart = calendar.getTime();

                calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, Integer.valueOf(year));
                calendar.set(Calendar.MONTH, 11);
                calendar.set(Calendar.DAY_OF_MONTH, 31);
                calendar.set(Calendar.HOUR_OF_DAY, 24);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);

                filterYearEnd = calendar.getTime();
            }

            // Query for all the task of the user that are completed and order by completion
            // date
            WorkflowTaskQuery workflowTaskQuery = new WorkflowTaskQuery();
            workflowTaskQuery.setActorId(userId);
            workflowTaskQuery.setActive(null);
            workflowTaskQuery.setTaskState(null);
            workflowTaskQuery.setOrderBy(new WorkflowTaskQuery.OrderBy[]{WorkflowTaskQuery.OrderBy.TaskCreated_Desc});

            List<WorkflowTask> taskList = workflowService.queryTasks(workflowTaskQuery, true);

            for (WorkflowTask workflowTask : taskList) {

                if (workflowTask.getProperties().get(qnameOutcome) != null && getTaskCompletionDate(workflowTask) != null) {

                    Date dateTask = getTaskCompletionDate(workflowTask);
                    String outcome = workflowTask.getProperties().get(qnameOutcome).toString();
                    String idWorkflowInstance = workflowTask.getId();

                    // Check if the completion date is in the desire date and if is for approved
                    // documents
                    if (!year.equals("*")) {
                        if (dateTask != null && filterYearStart.before(dateTask) && filterYearEnd.after(dateTask)) {
                            filterDate = true;
                        } else {
                            filterDate = false;
                        }
                    } else {
                        filterDate = true;
                    }

                    if (outcome.equals("Approve") && filterDate) {
                        listAnswer.addAll(getTaskFileItem(workflowTask, dateTask, idWorkflowInstance));
                    }
                }
            }
        } finally {
            if (runSystem) {
                AuthenticationUtil.popAuthentication();
            }
        }

        return listAnswer;
    }

    protected Date getTaskCompletionDate(WorkflowTask workflowTask) {
        QName qnameCompletionDate = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "completionDate");
        // print workflow task properties
        for (QName key : workflowTask.getProperties().keySet()) {
            LOGGER.debug("Task properties: " + key.getLocalName() + " = " + workflowTask.getProperties().get(key));
        }
        return (Date) workflowTask.getProperties().get(qnameCompletionDate);
    }

    protected List<List<Object>> getTaskFileItem(WorkflowTask workflowTask, Date dateTask, String idWorkflowInstance) {
        if (isLegacyTask(workflowTask)) {
            LOGGER.debug("Legacy task: " + idWorkflowInstance);
            return getLegacyTaskFileItem(workflowTask, dateTask, idWorkflowInstance);
        } else {
            LOGGER.debug("Modern task: " + idWorkflowInstance);
            return getModernTaskFileItem(workflowTask, idWorkflowInstance);
        }
    }

    private boolean isLegacyTask(WorkflowTask workflowTask) {
        return getInfoListProperty(workflowTask) == null;
    }

    private String getInfoListProperty(WorkflowTask workflowTask) {
        Map<QName, Serializable> properties = workflowTask.getProperties();
        for (QName key : properties.keySet()) {
            if (key.getLocalName().equals(TaskProperties.INFO_LIST)) {
                LOGGER.debug("Found: " + key.getLocalName() + " = " + properties.get(key));
                return properties.get(key).toString();
            }
        }
        return null;
    }

    private List<List<Object>> getModernTaskFileItem(WorkflowTask workflowTask, String idWorkflowInstance) {
        List<List<Object>> listAnswer = new ArrayList<>();
        String raw = getInfoListProperty(workflowTask);
        LOGGER.debug("InfoList property raw: " + raw);

        if (raw != null) {
            JSONArray infoList = new JSONArray(raw);

            for (int i = 0; i < infoList.length(); i++) {
                JSONObject infoItem = infoList.getJSONObject(i);
                LOGGER.debug("Info item " + i + ": " + infoItem);
                listAnswer.add(buildTaskFileItem(workflowTask, idWorkflowInstance, infoItem));
            }
        }

        logProgress(listAnswer, workflowTask);

        return listAnswer;
    }

    private void logProgress (List<List<Object>> listAnswer, WorkflowTask workflowTask) {
        if (listAnswer.isEmpty()) {
            LOGGER.debug("No properties found for task " + workflowTask.getId());
        } else {
            LOGGER.debug("Added successfully " + workflowTask.getId() + " with " + listAnswer.size() + " items: " + listAnswer);
        }
    }

    private List<Object> buildTaskFileItem(WorkflowTask workflowTask, String idWorkflowInstance, JSONObject infoItem) {
        List<Object> listData = new ArrayList<>();

        if (infoItem != null) {
            String urlFile = buildUrlFile(getJsonProperty(infoItem, TaskProperties.DOC_NODE_REF));
            String urlParentFolder = buildUrlParentFolder(getJsonProperty(infoItem, TaskProperties.PARENT_FOLDER_PATH));
            listData.add(idWorkflowInstance);
            listData.add(getUrlTask(idWorkflowInstance));
            listData.add(getJsonProperty(infoItem, TaskProperties.FILENAME));
            listData.add(getJsonProperty(infoItem, TaskProperties.DOC_VERSION));
            listData.add(urlFile);
            listData.add(getJsonProperty(infoItem, TaskProperties.DOC_NODE_REF));
            listData.add(getJsonProperty(infoItem, TaskProperties.DOC_PATH));
            listData.add(urlParentFolder);
            listData.add(getJsonProperty(infoItem, TaskProperties.PARENT_FOLDER_NODE_REF));
            listData.add(getJsonProperty(infoItem, TaskProperties.COMPLETION_DATE));
            listData.add(getTaskOwner(workflowTask));
        }

        return listData;
    }

    private String getJsonProperty(JSONObject infoItem, String property) {
        if (infoItem.has(property)) {
            return (String) infoItem.get(property);
        } else {
            return "-";
        }
    }

    private List<List<Object>> getLegacyTaskFileItem(WorkflowTask workflowTask, Date dateTask, String idWorkflowInstance) {
        QName qnamePackage = QName.createQName(NamespaceService.BPM_MODEL_1_0_URI, "package");
        QName qnameFileName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "name");

        SimpleDateFormat dateFormatUser = new SimpleDateFormat("dd/MM/YY HH:mm:ss");

        NodeService nodeService = serviceRegistry.getNodeService();
        VersionService versionService = serviceRegistry.getVersionService();

        List<List<Object>> listAnswer = new ArrayList<>();
        List<Object> listData;

        NodeRef nodeRefToDocuments = new NodeRef(workflowTask.getProperties().get(qnamePackage).toString());
        List<ChildAssociationRef> listNodeRefDocuments = nodeService.getChildAssocs(nodeRefToDocuments);

        // Check if the node of approved documents have any file inside
        if (!listNodeRefDocuments.isEmpty()) {

            for (ChildAssociationRef childAssociationRef : listNodeRefDocuments) {
                NodeRef fileOfTask = childAssociationRef.getChildRef();

                // Get the path of parent folder
                String parentFolderPath = getParentFolderPath(fileOfTask);
                NodeRef folderNodeRef = getFolderNodeRef(fileOfTask);

                // Build the URL to access by share
                String urlFile = buildUrlFile(fileOfTask.toString());
                String urlParentFolder = buildUrlParentFolder(parentFolderPath);

                // Find version label by using task date as reference
                String versionLabel = "1.0";
                if (versionService.getVersionHistory(fileOfTask) != null) {
                    Collection<Version> versions = versionService.getVersionHistory(fileOfTask).getAllVersions();
                    Map<Date, String> labels = new HashMap<Date, String>();
                    Set<Date> previousDates = new TreeSet<Date>();
                    for (Version version : versions) {
                        // if the task date is after version date, take the version label 1.0
                        if (dateTask == null || version.getFrozenModifiedDate().before(dateTask)) {
                            previousDates.add(version.getFrozenModifiedDate());
                            labels.put(version.getFrozenModifiedDate(), version.getVersionLabel());
                        }
                    }
                    Date lastDate = getLastElement(previousDates);
                    versionLabel = lastDate != null ? labels.get(lastDate) : "1.0";
                }

                String nodeName = nodeService.getProperties(fileOfTask).get(qnameFileName).toString();

                // Build answer
                listData = new ArrayList<>();
                listData.add(idWorkflowInstance);
                listData.add(getUrlTask(idWorkflowInstance));
                listData.add(nodeName);
                listData.add(versionLabel);
                listData.add(urlFile);
                listData.add(fileOfTask.toString());
                listData.add(parentFolderPath);
                listData.add(urlParentFolder);
                listData.add(folderNodeRef);
                listData.add(dateTask != null ? dateFormatUser.format(dateTask) : null);
                listData.add(getTaskOwner(workflowTask));

                listAnswer.add(listData);
            }

        } else {

            // Build answer
            listData = new ArrayList<>();
            listData.add(idWorkflowInstance);
            listData.add(getUrlTask(idWorkflowInstance));
            listData.add("-");
            listData.add("-");
            listData.add("-");
            listData.add("-");
            listData.add("-");
            listData.add("-");
            listData.add("-");
            listData.add(dateTask != null ? dateFormatUser.format(dateTask) : null);
            listData.add(getTaskOwner(workflowTask));

            listAnswer.add(listData);
        }

        return listAnswer;
    }

    private NodeRef getFolderNodeRef(NodeRef fileNodeRef) {
        NodeService nodeService = serviceRegistry.getNodeService();
        NodeRef nodeRefFolder = nodeService.getPrimaryParent(fileNodeRef).getParentRef();
        return nodeRefFolder;
    }

    private String getParentFolderPath(NodeRef fileNodeRef) {
        NodeService nodeService = serviceRegistry.getNodeService();
        NodeRef nodeRefFolder = nodeService.getPrimaryParent(fileNodeRef).getParentRef();
        String path =
                nodeService.getPath(nodeRefFolder).toDisplayPath(nodeService, serviceRegistry.getPermissionService()) + "/" +
                        nodeService.getProperty(nodeRefFolder, ContentModel.PROP_NAME);
        return path;
    }

    private String buildUrlParentFolder(String folderPath) {
        String substring
                ;
        if (folderPath != null && folderPath.contains("/")) {
            substring = folderPath.substring(folderPath.indexOf("/", 1));
        } else {
            substring = folderPath;
            LOGGER.warn("No substring found for path: " + folderPath);
        }
        // Skip first "Company Home" folder as it is not required for Share URL
        return taskShareUrl + "/page/repository#filter=path" +
                URLEncoder.encodeUriComponent("|" + substring);
    }

    private String buildUrlFile(String fileNodeRef) {
        return taskShareUrl + "/page/document-details?nodeRef=" + fileNodeRef;
    }

    private String getTaskOwner(WorkflowTask workflowTask) {
        NodeService nodeService = serviceRegistry.getNodeService();
        PersonService personService = serviceRegistry.getPersonService();

        NodeRef packageNodeRef = (NodeRef) workflowTask.getProperties().get(QName.createQName("http://www.alfresco.org/model/bpm/1.0", "package"));
        String userOwner = nodeService.getProperties(packageNodeRef).get(QName.createQName("http://www.alfresco.org/model/content/1.0", "creator")).toString();
        PersonInfo personInfo = personService.getPerson(personService.getPerson(userOwner));

        return personInfo.getFirstName() + " " + personInfo.getLastName();
    }

    private String getUrlTask(String idWorkflowInstance) {
        return taskShareUrl + "/page/task-details?taskId=" + idWorkflowInstance;
    }

    private String getFileName(String nodeName, String fileNameTaskVariable) {
        return fileNameTaskVariable != null && !fileNameTaskVariable.isEmpty() ? fileNameTaskVariable : nodeName;
    }

    public static <T> T getLastElement(final Iterable<T> elements) {
        final Iterator<T> itr = elements.iterator();

        T lastElement = null;

        while (itr.hasNext()) {
            lastElement = itr.next();
        }

        return lastElement;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public void setSysAdminParams(SysAdminParams sysAdminParams) {
        this.sysAdminParams = sysAdminParams;
    }

    public void setTaskShareUrl(String taskShareUrl) {
        this.taskShareUrl = taskShareUrl;
    }

}
