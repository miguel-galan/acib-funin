package com.acibfunin.webscripts;

import com.acibfunin.util.GetTasksFiles;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.extensions.surf.util.I18NUtil;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ExportApprovedFilesToCSV extends GetTasksFiles {

	private final static String CSV_SEPARATOR = ";";

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		try {
			List<List<Object>> data = getData("*", req.getParameter("username"));

			String csvContent = "";
			csvContent = csvContent + getString("workflowId");
			csvContent = csvContent + getString("urlWorkflowId");
			csvContent = csvContent + getString("nameFile");
			csvContent = csvContent + getString("version");
			csvContent = csvContent + getString("urlShareFile");
			csvContent = csvContent + getString("noderefFile");
			csvContent = csvContent + getString("nameFolder");
			csvContent = csvContent + getString("urlShareFolder");
			csvContent = csvContent + getString("noderefFolder");
			csvContent = csvContent + getString("approvedDate");
			csvContent = csvContent + getString("owner") + "\n";

			for (List<Object> listResults : data) {
				for (Object object : listResults) {
					csvContent = csvContent + object.toString() + CSV_SEPARATOR;
				}
				csvContent = csvContent + "\n";
			}

			res.addHeader("Content-Disposition", "attachment; filename=" + getFileName(req.getParameter("username")));
			res.setContentType(MimetypeMap.MIMETYPE_TEXT_CSV);
			res.getOutputStream().write(csvContent.getBytes());

		} catch (Throwable e) {
			throw new IOException(e);
		}
	}

	private static @NonNull String getString(String key) {
		return I18NUtil.getMessage("csv.approvedTasksList." + key) + CSV_SEPARATOR;
	}

	private String getFileName(String paramUserName) {
		String filename = "tasks_files";
		
		if (paramUserName != null && !paramUserName.equals("")) {
			filename = filename + "-" + paramUserName;
		} else {
			filename = filename + "-" + AuthenticationUtil.getFullyAuthenticatedUser();
		}
		
		DateFormat df = new SimpleDateFormat("yyyyddMM");
		filename = filename + "-" + df.format(new Date());
		
		return filename + ".csv";
	}
}
