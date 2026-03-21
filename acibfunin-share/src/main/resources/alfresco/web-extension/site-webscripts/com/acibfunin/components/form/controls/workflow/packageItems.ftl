<div id="${fieldHtmlId?js_string}-task-entries" style="margin-top:12px"></div>

<script type="text/javascript">
    (function () {
        var PDF_ICON = Alfresco.constants.URL_RESCONTEXT + "components/images/filetypes/pdf-file-32.png";
        var el = document.getElementById("${fieldHtmlId?js_string}-task-entries");
        var taskId = (Alfresco.util.getQueryStringParameter("taskId") || "").trim();

        if (!taskId) {
            el.innerHTML = '<div style="opacity:.8">(taskId no disponible)</div>';
            return;
        }

        var url = Alfresco.constants.PROXY_URI + "task/info?taskId=" + encodeURIComponent(taskId);

        Alfresco.util.Ajax.request({
            url: url,
            method: "GET",
            successCallback: {
                fn: function (resp) {
                    try {
                        var data = JSON.parse(resp.serverResponse.responseText) || {};
                        var entries = data.entries || [];
                        if (!entries.length) {
                            el.innerHTML = '<div class="viewmode-field"><div style="opacity:.8">(sin entradas)</div></div>';
                            return;
                        }

                        var table =
                            '<div class="viewmode-field">' +
                            '<table class="yui-dt-table" style="width:100%; border-spacing:0;" summary="">' +
                            '  <colgroup><col style="width:50px"><col><col style="width:200px"></colgroup>' +
                            '  <tbody class="yui-dt-data">';

                        entries.forEach(function (e) {
                            var name = e.NameFile || "(no name)";
                            var ver  = e.Version || "";
                            var href = e.URLShareFile || "#";

                            table +=
                                '<tr class="yui-dt-rec">' +
                                '  <td class="yui-dt-col-nodeRef yui-dt-first" style="padding-top: 1em">' +
                                '    <div class="yui-dt-liner" style="width:50px;">' +
                                '      <div class="icon32"><a target="_blank" href="' + href + '"><img src="' + PDF_ICON + '" width="32" alt="PDF"/></a></div>' +
                                '    </div>' +
                                '  </td>' +
                                '  <td class="yui-dt-col-name" style="padding-top: 1em">' +
                                '    <div class="yui-dt-liner">' +
                                '      <h3 class="name"><a target="_blank" href="' + href + '">' + name + '</a></h3>' +
                                (ver ? '<div class="viewmode-label">v' + ver + '</div>' : '') +
                                '    </div>' +
                                '  </td>' +
                                '  <td class="yui-dt-col-action yui-dt-last"><div class="yui-dt-liner"></div></td>' +
                                '</tr>';
                        });

                        table += '</tbody></table></div>';
                        el.innerHTML = table;
                    } catch (e) {
                        el.innerHTML = '<div style="color:#b00">Error parseando JSON</div>';
                        if (window.console) console.warn(e);
                    }
                }
            },
            failureCallback: {
                fn: function (resp) {
                    var status = (resp.serverResponse && resp.serverResponse.status) || '??';
                    el.innerHTML = '<div style="color:#b00">Error ' + status + ' llamando al webscript</div>';
                }
            }
        });
    })();
</script>
