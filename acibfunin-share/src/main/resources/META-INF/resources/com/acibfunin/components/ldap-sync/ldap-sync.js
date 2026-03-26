(function() {
   var Dom = YAHOO.util.Dom;

   var Acibfunin = window.Acibfunin || {};
   window.Acibfunin = Acibfunin;
   Acibfunin.component = Acibfunin.component || {};

   Acibfunin.component.LdapSyncAdminTool = function LdapSyncAdminTool_constructor(htmlId) {
      return Acibfunin.component.LdapSyncAdminTool.superclass.constructor.call(this, "Acibfunin.component.LdapSyncAdminTool", htmlId);
   };

   YAHOO.extend(Acibfunin.component.LdapSyncAdminTool, Alfresco.component.Base, {
      options: {
         syncUrl: Alfresco.constants.URL_SERVICECONTEXT + "acibfunin/api/ldap-sync",
         syncConfigUrl: Alfresco.constants.URL_SERVICECONTEXT + "acibfunin/api/ldap-sync-config"
      },

      onReady: function LdapSyncAdminTool_onReady() {
         Alfresco.util.ComponentManager.register(this);
         this.widgets.syncButton = Alfresco.util.createYUIButton(this, "syncButton", this.onSyncClick);
         this.loadSyncConfiguration();
      },

      loadSyncConfiguration: function LdapSyncAdminTool_loadSyncConfiguration() {
         this.showConfigMessage(this.msg("page.ldapSync.properties.loading"), false);
         Alfresco.util.Ajax.request({
            url: this.options.syncConfigUrl,
            method: Alfresco.util.Ajax.GET,
            responseContentType: Alfresco.util.Ajax.JSON,
            successCallback: {
               fn: this.onSyncConfigurationSuccess,
               scope: this
            },
            failureCallback: {
               fn: this.onSyncConfigurationFailure,
               scope: this
            }
         });
      },

      onSyncConfigurationSuccess: function LdapSyncAdminTool_onSyncConfigurationSuccess(response) {
         var payload = this.extractPayload(response);
         if (payload.success === false) {
            this.onSyncConfigurationFailure(response);
            return;
         }
         this.renderLdapProperties(payload.properties || {});
      },

      onSyncConfigurationFailure: function LdapSyncAdminTool_onSyncConfigurationFailure(response) {
         var payload = this.extractPayload(response);
         this.clearLdapProperties();
         this.showConfigMessage(payload.message || this.msg("page.ldapSync.properties.error"), true);
      },

      onSyncClick: function LdapSyncAdminTool_onSyncClick() {
         this.widgets.syncButton.set("disabled", true);
         this.showMessage("", false);

         Alfresco.util.Ajax.request({
            url: this.options.syncUrl,
            method: Alfresco.util.Ajax.POST,
            requestContentType: Alfresco.util.Ajax.JSON,
            responseContentType: Alfresco.util.Ajax.JSON,
            dataObj: {},
            successCallback: {
               fn: this.onSyncSuccess,
               scope: this
            },
            failureCallback: {
               fn: this.onSyncFailure,
               scope: this
            }
         });
      },

      onSyncSuccess: function LdapSyncAdminTool_onSyncSuccess(response) {
         this.widgets.syncButton.set("disabled", false);

         var payload = response.json || {};
         if (payload.success === false) {
            this.onSyncFailure(response);
            return;
         }

         this.showMessage(this.msg("page.ldapSync.success"), false);
         this.showResults(payload);
      },

      onSyncFailure: function LdapSyncAdminTool_onSyncFailure(response) {
         this.widgets.syncButton.set("disabled", false);

         var payload = response.json || {};
         var message = payload.message || this.msg("page.ldapSync.failure");
         this.showMessage(message, true);
         this.showResults(payload);
      },

      showMessage: function LdapSyncAdminTool_showMessage(message, isError) {
         var element = Dom.get(this.id + "-message");
         if (!message) {
            Dom.addClass(element, "hidden");
            element.innerHTML = "";
            return;
         }

         Dom.removeClass(element, "hidden");
         element.innerHTML = Alfresco.util.encodeHTML(message);
         element.style.color = isError ? "#b00" : "";
      },

      showResults: function LdapSyncAdminTool_showResults(payload) {
         var results = Dom.get(this.id + "-results");
         Dom.removeClass(results, "hidden");

         this.setField("status", payload.status);
         this.setField("summary", payload.summary);
         this.setField("startedAt", payload.syncStartTime);
         this.setField("finishedAt", payload.syncEndTime);
         this.setField("server", payload.lastRunOnServer);
         this.setField("lastError", payload.lastErrorMessage);
      },

      renderLdapProperties: function LdapSyncAdminTool_renderLdapProperties(properties) {
         this.clearLdapProperties();
         var keys = [];
         var key;
         for (key in properties) {
            if (Object.prototype.hasOwnProperty.call(properties, key)) {
               keys.push(key);
            }
         }
         keys.sort();
         if (keys.length === 0) {
            this.showConfigMessage(this.msg("page.ldapSync.properties.empty"), false);
            return;
         }

         this.showConfigMessage("", false);
         var table = this.createPropertiesTable();
         var body = table.getElementsByTagName("tbody")[0];
         var index;
         for (index = 0; index < keys.length; index++) {
            this.addPropertyRow(body, keys[index], properties[keys[index]]);
         }
         Dom.get(this.id + "-configProperties").appendChild(table);
      },

      extractPayload: function LdapSyncAdminTool_extractPayload(response) {
         if (response && response.json) {
            return response.json;
         }
         if (!response || !response.serverResponse || !response.serverResponse.responseText) {
            return {};
         }
         try {
            return YAHOO.lang.JSON.parse(response.serverResponse.responseText);
         }
         catch (exception) {
            return {};
         }
      },

      createPropertiesTable: function LdapSyncAdminTool_createPropertiesTable() {
         var table = document.createElement("table");
         table.className = "ldap-sync-properties-table";
         var header = document.createElement("thead");
         var row = document.createElement("tr");
         row.appendChild(this.createHeaderCell(this.msg("page.ldapSync.property.key")));
         row.appendChild(this.createHeaderCell(this.msg("page.ldapSync.property.value")));
         header.appendChild(row);
         table.appendChild(header);
         table.appendChild(document.createElement("tbody"));
         return table;
      },

      createHeaderCell: function LdapSyncAdminTool_createHeaderCell(text) {
         var cell = document.createElement("th");
         cell.appendChild(document.createTextNode(text));
         return cell;
      },

      addPropertyRow: function LdapSyncAdminTool_addPropertyRow(body, key, value) {
         var row = document.createElement("tr");
         var keyCell = document.createElement("td");
         var valueCell = document.createElement("td");
         keyCell.appendChild(document.createTextNode(key || ""));
         valueCell.appendChild(document.createTextNode(value || ""));
         row.appendChild(keyCell);
         row.appendChild(valueCell);
         body.appendChild(row);
      },

      clearLdapProperties: function LdapSyncAdminTool_clearLdapProperties() {
         var container = Dom.get(this.id + "-configProperties");
         while (container.firstChild) {
            container.removeChild(container.firstChild);
         }
      },

      showConfigMessage: function LdapSyncAdminTool_showConfigMessage(message, isError) {
         var element = Dom.get(this.id + "-configMessage");
         if (!message) {
            Dom.addClass(element, "hidden");
            element.innerHTML = "";
            return;
         }

         Dom.removeClass(element, "hidden");
         element.innerHTML = Alfresco.util.encodeHTML(message);
         element.style.color = isError ? "#b00" : "";
      },

      setField: function LdapSyncAdminTool_setField(suffix, value) {
         var element = Dom.get(this.id + "-" + suffix);
         if (element) {
            element.innerHTML = Alfresco.util.encodeHTML(value || "");
         }
      }
   });
})();
