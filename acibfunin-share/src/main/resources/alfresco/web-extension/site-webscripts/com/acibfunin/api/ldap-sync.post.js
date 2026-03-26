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
   logger.warn("LDAP sync Share proxy denied for non-admin user: " + user.name);
   status.code = status.STATUS_FORBIDDEN;
   model.payload = buildErrorPayload("Administrator permissions are required.", status.code);
}
else {
   var connector = remote.connect("alfresco");
   var result = connector.post("/acibfunin/ldap/sync", "", "application/json");

   status.code = result.status;
   model.payload = result.response && String(result.response).length > 0
      ? result.response
      : buildErrorPayload("LDAP synchronization returned an empty response.", result.status);
}
