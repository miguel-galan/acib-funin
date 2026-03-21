{
  "success": ${success?string},
  <#if targetNodeRef??>"targetNodeRef": "${targetNodeRef}",</#if>
  <#if backupNodeRef??>"backupNodeRef": "${backupNodeRef}",</#if>
  <#if backupName??>"backupName": "${backupName}",</#if>
  <#if resolvedSourceNodeRef??>"resolvedSourceNodeRef": "${resolvedSourceNodeRef}",</#if>
  <#if sourceMimetype??>"sourceMimetype": "${sourceMimetype}",</#if>
  <#if sourceSize??>"sourceSize": ${sourceSize?c},</#if>
  <#if targetMimetype??>"targetMimetype": "${targetMimetype}",</#if>
  <#if targetSize??>"targetSize": ${targetSize?c},</#if>
  <#if versionLabelBefore??>"versionLabelBefore": "${versionLabelBefore}",</#if>
  <#if versionLabelAfter??>"versionLabelAfter": "${versionLabelAfter}",</#if>
  "notes": [
    <#list notes as note>
    "${note}"<#if note_has_next>,</#if>
    </#list>
  ]
  <#if error??>
  ,"error": {
    "message": "${error.message?json_string}",
    "type": "${error.type?json_string}"
  }
  </#if>
}
