function escapeJson(value) {
   return String(value || "")
      .replace(/\\/g, "\\\\")
      .replace(/"/g, "\\\"")
      .replace(/\r/g, "\\r")
      .replace(/\n/g, "\\n");
}

function buildErrorPayload(message, statusCode) {
   return '{"success":false,"message":"' + escapeJson(message) + '","statusCode":' + statusCode + '}';
}

if (!user.isAdmin) {
   status.code = status.STATUS_FORBIDDEN;
   model.payload = buildErrorPayload("Administrator permissions are required.", status.code);
}
else {
   try {
      logger.log("LDAP sync config Share proxy: BEFORE connector.get('/acibfunin/ldap/sync/config')");
      var connector = remote.connect("alfresco");
      var result = connector.get("/acibfunin/ldap/sync/config");
      logger.log("LDAP sync config Share proxy: AFTER connector.get('/acibfunin/ldap/sync/config') status=" + result.status);
      status.code = result.status;
      model.payload = result.response && String(result.response).length > 0
         ? result.response
         : buildErrorPayload("LDAP synchronization configuration returned an empty response.", result.status);
   }
   catch (exception) {
      logger.warn("LDAP sync config Share proxy: connector.get exception=" + String(exception));
      status.code = status.STATUS_INTERNAL_SERVER_ERROR;
      model.payload = buildErrorPayload("Share proxy exception while calling repository.", status.code);
   }
}
