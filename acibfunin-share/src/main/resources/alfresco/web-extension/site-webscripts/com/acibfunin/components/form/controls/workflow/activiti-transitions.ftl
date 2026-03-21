<#if form.mode == "edit" >

    <script>
        (function () {
            var transitions = new Alfresco.ActivitiTransitions("${fieldHtmlId}")
                .setOptions({
                    hiddenFieldName: "${field.name}"
                })
                .setMessages(${messages});             // REQUIRED for msg()

            var approveLabel = transitions.msg("control.approvedTasksList.approve") || "No ha funcionado";

            transitions.setOptions({
                currentValue: "Approve|" + approveLabel,
                hiddenFieldName: "${field.name}"
            });
        })();
    </script>

    <div class="form-field suggested-actions" id="${fieldHtmlId}">
        <div id="${fieldHtmlId}-buttons"></div>
    </div>

</#if>