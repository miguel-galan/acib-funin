<@markup id="js">
   <@script src="${url.context}/res/components/console/consoletool.js" group="console"/>
   <@script src="${url.context}/res/resources/com/acibfunin/components/ldap-sync/ldap-sync.js" group="console"/>
</@>

<@markup id="widgets">
   <@createWidgets group="console"/>
</@>

<@markup id="html">
   <@uniqueIdDiv>
      <#assign el = args.htmlid?html>
      <div id="${el}-body" class="users">
         <div class="header-bar">${msg("page.ldapSync.title")}</div>
         <style>
            #${el}-layout {
               display: flex;
               gap: 16px;
               align-items: flex-start;
            }
            #${el}-left {
               flex: 0 0 30%;
               max-width: 30%;
            }
            #${el}-right {
               flex: 0 0 70%;
               max-width: 70%;
            }
            #${el}-right .title {
               margin-bottom: 8px;
            }
            #${el}-configProperties {
               border: 1px solid #d5d5d5;
               background: #fff;
               max-height: 540px;
               overflow: auto;
            }
            #${el}-configProperties table {
               width: 100%;
               border-collapse: collapse;
            }
            #${el}-configProperties th,
            #${el}-configProperties td {
               text-align: left;
               padding: 8px 10px;
               border-bottom: 1px solid #ececec;
               vertical-align: top;
               word-break: break-word;
            }
            #${el}-configProperties th {
               font-weight: bold;
               background: #f7f7f7;
            }
            @media (max-width: 900px) {
               #${el}-layout {
                  display: block;
               }
               #${el}-left,
               #${el}-right {
                  max-width: 100%;
               }
               #${el}-right {
                  margin-top: 12px;
               }
            }
         </style>

         <div id="${el}-layout" class="separator">
            <div id="${el}-left">
               <div class="title separator">${msg("page.ldapSync.help")}</div>
               <div class="separator">
                  <span class="yui-button yui-push-button" id="${el}-syncButton">
                     <span class="first-child"><button>${msg("page.ldapSync.action")}</button></span>
                  </span>
               </div>
               <div id="${el}-message" class="search-bar theme-bg-color-3 hidden"></div>
               <div id="${el}-results" class="view-main separator hidden">
                  <div class="field-row">
                     <span class="field-label-right">${msg("page.ldapSync.status")}:</span>
                     <span id="${el}-status" class="field-value"></span>
                  </div>
                  <div class="field-row">
                     <span class="field-label-right">${msg("page.ldapSync.summary")}:</span>
                     <span id="${el}-summary" class="field-value"></span>
                  </div>
                  <div class="field-row">
                     <span class="field-label-right">${msg("page.ldapSync.startedAt")}:</span>
                     <span id="${el}-startedAt" class="field-value"></span>
                  </div>
                  <div class="field-row">
                     <span class="field-label-right">${msg("page.ldapSync.finishedAt")}:</span>
                     <span id="${el}-finishedAt" class="field-value"></span>
                  </div>
                  <div class="field-row">
                     <span class="field-label-right">${msg("page.ldapSync.server")}:</span>
                     <span id="${el}-server" class="field-value"></span>
                  </div>
                  <div class="field-row">
                     <span class="field-label-right">${msg("page.ldapSync.lastError")}:</span>
                     <span id="${el}-lastError" class="field-value"></span>
                  </div>
               </div>
            </div>

            <div id="${el}-right">
               <div class="title">${msg("page.ldapSync.properties.title")}</div>
               <div id="${el}-configMessage" class="search-bar theme-bg-color-3 hidden"></div>
               <div id="${el}-configProperties"></div>
            </div>
         </div>
      </div>
   </@>
</@>
