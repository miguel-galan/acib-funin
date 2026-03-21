// Use your own JavaScript space
if (typeof acibfunin == "undefined" || !acibfunin)
{
   var acibfunin = {};
}
acibfunin.DocumentVersions = acibfunin.DocumentVersions || {};

// Alfresco aliases
var $html = Alfresco.util.encodeHTML,
    $userProfileLink = Alfresco.util.userProfileLink,
    $userAvatar = Alfresco.Share.userAvatar;

(function() {

   // Custom object declaration
   acibfunin.DocumentVersions.prototype=
   {

       // Overwrite method from parent object "Alfresco.DocumentVersions" to include Remove action
	   getDocumentVersionMarkup: function DocumentVersions_getDocumentVersionMarkup(doc)
       {
    	   
	    	   var downloadURL = Alfresco.constants.PROXY_URI + 'api/node/content/' + doc.nodeRef.replace(":/", "") + '/' + doc.name + '?a=true',
	    	       html = '';
	
	    	   html += '<div class="version-panel-left">'
	    		   html += '   <span class="document-version">' + $html(doc.label) + '</span>';
	    	   html += '</div>';
	    	   html += '<div class="version-panel-right">';
	    	   html += '   <h3 class="thin dark" style="width:' + (Dom.getViewportWidth() * 0.25) + 'px;">' + $html(doc.name) +  '</h3>';
	    	   html += '   <span class="actions">';
	    	   if (this.options.allowNewVersionUpload)
	    	   {
	    		   html += '   <a href="#" name=".onRevertVersionClick" rel="' + doc.label + '" class="' + this.id + ' revert" title="' + this.msg("label.revert") + '">&nbsp;</a>';
	    	   }
	    	   html += '      <a href="' + downloadURL + '" target="_blank" class="download" title="' + this.msg("label.download") + '">&nbsp;</a>';
	    	   html += '      <a href="#" name=".onViewHistoricPropertiesClick" rel="' + doc.nodeRef + '" class="' + this.id + ' historicProperties" title="' + this.msg("label.historicProperties") + '">&nbsp;</a>';

	    	   // Add placeholder for remove button (will be added dynamically if user is admin)
	    	   html += '      <span class="' + this.id + '-remove-placeholder" data-label="' + $html(doc.label) + '" style="display:none;"></span>';

	    	   html += '   </span>';
	    	   html += '   <div class="clear"></div>';
	    	   html += '   <div class="version-details">';
	    	   html += '      <div class="version-details-left">'
	    	   html += $userAvatar(doc.creator.userName, 32);
	    	   html += '      </div>';
	    	   html += '      <div class="version-details-right">';
	    	   html += $userProfileLink(doc.creator.userName, doc.creator.firstName + ' ' + doc.creator.lastName, 'class="theme-color-1"') + ' ';
	    	   html += Alfresco.util.relativeTime(Alfresco.util.fromISO8601(doc.createdDateISO)) + '<br />';
	    	   html += ((doc.description || "").length > 0) ? $html(doc.description, true) : '<span class="faded">(' + this.msg("label.noComment") + ')</span>';
	    	   html += '      </div>';
	    	   html += '   </div>';
	    	   html += '</div>';
	
	    	   html += '<div class="clear"></div>';

	    	   this._scheduleRemoveButtonsInitialization();
	    	   return html;
        
	   },

	   /**
	    * Schedule remove button initialization after version markup is rendered
	    */
	   _scheduleRemoveButtonsInitialization: function DocumentVersions_scheduleRemoveButtonsInitialization()
	   {
		   if (this._removeButtonsInitScheduled)
		   {
			   return;
		   }

		   this._removeButtonsInitScheduled = true;

		   var me = this;
		   window.setTimeout(function()
		   {
			   me._removeButtonsInitScheduled = false;
			   me._initializeRemoveButtons();
		   }, 0);
	   },

	   /**
	    * Remove all placeholders from DOM
	    */
	   _removePlaceholders: function DocumentVersions_removePlaceholders()
	   {
		   var placeholders = YAHOO.util.Dom.getElementsByClassName(this.id + '-remove-placeholder');
		   for (var i = placeholders.length - 1; i >= 0; i--)
		   {
			   placeholders[i].parentNode.removeChild(placeholders[i]);
		   }
	   },

	   /**
	    * Initialize remove buttons for admin users only
	    */
	   _initializeRemoveButtons: function DocumentVersions_initializeRemoveButtons()
	   {
		   var me = this;
		   var placeholders = YAHOO.util.Dom.getElementsByClassName(me.id + '-remove-placeholder');

		   if (!placeholders.length)
		   {
			   return;
		   }

		   if (me._isAdmin === true)
		   {
			   me._replacePlaceholdersWithRemoveButtons(placeholders);
			   return;
		   }

		   if (me._isAdmin === false)
		   {
			   me._removePlaceholders();
			   return;
		   }

		   if (me._adminCheckInProgress)
		   {
			   return;
		   }

		   me._adminCheckInProgress = true;
		   var username = Alfresco.constants.USERNAME;

		   // Call API to check if user is admin
		   Alfresco.util.Ajax.request(
		   {
			   method: Alfresco.util.Ajax.GET,
			   url: Alfresco.constants.PROXY_URI + 'api/people/' + encodeURIComponent(username) + '?groups=true',
			   successCallback:
			   {
				   fn: function(response)
				   {
					   try
					   {
						   var userData = Alfresco.util.parseJSON(response.serverResponse.responseText);
						   me._isAdmin = userData.capabilities && userData.capabilities.isAdmin === true;

						   if (me._isAdmin)
						   {
							   me._replacePlaceholdersWithRemoveButtons(YAHOO.util.Dom.getElementsByClassName(me.id + '-remove-placeholder'));
						   }
						   else
						   {
							   me._removePlaceholders();
						   }
					   }
					   catch (e)
					   {
						   me._isAdmin = false;
						   me._removePlaceholders();
					   }
					   me._adminCheckInProgress = false;
				   },
				   scope: this
			   },
			   failureCallback:
			   {
				   fn: function()
				   {
					   me._isAdmin = false;
					   me._adminCheckInProgress = false;
					   me._removePlaceholders();
				   },
				   scope: this
			   }
		   });
	   },

	   /**
	    * Replace version placeholders with remove buttons
	    */
	   _replacePlaceholdersWithRemoveButtons: function DocumentVersions_replacePlaceholdersWithRemoveButtons(placeholders)
	   {
		   for (var i = 0; i < placeholders.length; i++)
		   {
			   var placeholder = placeholders[i];
			   var label = placeholder.getAttribute('data-label');
			   var removeLink = document.createElement('a');
			   removeLink.href = '#';
			   removeLink.target = '_blank';
			   removeLink.setAttribute('name', '.beforeRemoveClick');
			   removeLink.setAttribute('rel', label);
			   removeLink.className = this.id + ' remove';
			   removeLink.title = this.msg("label.delete");
			   removeLink.innerHTML = '&nbsp;';
			   placeholder.parentNode.replaceChild(removeLink, placeholder);
		   }
	   },

	   // Call Alfresco repo to invoke DELETE Web Script for version removal
	   _onRemoveClick: function DocumentVersions_onRemoveClick(label)
	   {
		   
		   // Use nodeRef from parent object Alfresco.DocumentVersions
		   var nodeRef = this.options.nodeRef;
		   
		   // Web Script invocations are always asynchronous
		   Alfresco.util.Ajax.request(
		   {
			   // Using POST instead of DELETE due to parameters including "." (e.g label = 1.0)
			   method: Alfresco.util.Ajax.POST,
			   url:  Alfresco.constants.PROXY_URI + 'api/node/version/' + nodeRef.replace(":/", ""),
			   // Using POST because of Alfresco is intercepting "." in the URLs
			   dataObj:
			   {
				   label: label
			   },
			   requestContentType: Alfresco.util.Ajax.JSON,
			   successCallback:
			   {
				   fn: function onRequestSuccess(response)
				   {
					   // No response JSON is required, included following line just as a sample
					   var json = Alfresco.util.parseJSON(response.serverResponse.responseText);
					   Alfresco.util.PopupManager.displayMessage(
					   {
						   text: this.msg("message.removeComplete")
					   })
				   },
				   scope: this	            	
			   },
			   failureCallback:
			   {
				   fn: function onRequestFailure()
			       {
					   Alfresco.util.PopupManager.displayMessage(
					   {
					       text: this.msg("message.removeFailed")
					   })
			       },
			       scope: this	            	
			   }
		   });
			 
		   // Fixing the UI after invocation
		   YAHOO.Bubbling.fire("previewChangedEvent");
		   YAHOO.Bubbling.fire("metadataRefresh", {});
		   
       },
	   
       beforeRemoveClick: function DocumentVersions_beforeRemoveClick(label)
       {
          var me = this;
          
          Alfresco.util.PopupManager.displayPrompt(
          {
             title: this.msg("actions.version.delete"),
             text: this.msg("message.confirm.delete"),
             buttons: [
             {
                text: this.msg("button.delete"),
                handler: function DocumentVersions_onRemoveVersion_remove()
                {
                   this.destroy();
                   me._onRemoveClick.call(me, label);
                }
             },
             {
            	 text: this.msg("button.cancel"),
                handler: function DocumentVersions__onRemoveVersion_cancel()
                {
                   this.destroy();
                },
                isDefault: true
             }]
          });
       }

   };
   
})();

// Extending original Alfresco.DocumentVersions object
(function () {
    YAHOO.lang.augmentProto(Alfresco.DocumentVersions, acibfunin.DocumentVersions, true);
})();
