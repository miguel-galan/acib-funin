package com.acibfunin.webscripts;

import com.acibfunin.util.GetTasksFiles;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.util.Date;
import java.util.List;

public class GetTaskInfo extends GetTasksFiles {
    private WorkflowService workflowService;

    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public void execute(WebScriptRequest req, WebScriptResponse res) {
        try {
            JSONArray jsonTask = new JSONArray();
            List<List<Object>> data = getTaskInfo(req);

            for (List<Object> informationLine : data) {
                JSONObject jsonTaskInfo = new JSONObject();
                jsonTaskInfo.put("ID", informationLine.get(0));
                jsonTaskInfo.put("URLID", informationLine.get(1));
                jsonTaskInfo.put("NameFile", informationLine.get(2));
                jsonTaskInfo.put("Version", informationLine.get(3));
                jsonTaskInfo.put("URLShareFile", informationLine.get(4));
                jsonTaskInfo.put("NodeRefFile", informationLine.get(5));
                jsonTaskInfo.put("NameFolder", informationLine.get(6));
                jsonTaskInfo.put("URLShareFolder", informationLine.get(7));
                jsonTaskInfo.put("NodeRefFolder", informationLine.get(8));
                jsonTaskInfo.put("ApprovedDate", informationLine.get(9));
                jsonTaskInfo.put("Owner", informationLine.get(10));

                jsonTask.put(jsonTaskInfo);
            }
            JSONObject jsonAnswer = new JSONObject();
            jsonAnswer.put("entries", jsonTask);

            res.setContentType(ContentType.APPLICATION_JSON.getMimeType());
            res.setContentEncoding("UTF-8");
            res.getWriter().write(jsonAnswer.toString());

        } catch (Exception re) {
            throw new RuntimeException(re);
        }
    }

    private List<List<Object>> getTaskInfo(WebScriptRequest req) {
        String taskId = req.getParameter("taskId");

        if (taskId == null || taskId.trim().isEmpty()) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Missing required parameter: taskId");
        }

        WorkflowTask workflowTask = workflowService.getTaskById(taskId);

        if (workflowTask == null) {
            throw new WebScriptException(Status.STATUS_NOT_FOUND, "Task not found: " + taskId);
        }

        Date taskCompletionDate = getTaskCompletionDate(workflowTask);

        return getTaskFileItem(workflowTask, taskCompletionDate, workflowTask.getId());
    }

}
