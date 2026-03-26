{
  "success": ${success?string("true", "false")},
  "count": ${count?c},
  "properties": {
<#list properties?keys?sort as key>
    "${key?js_string}": "${(properties[key])?js_string}"<#if key_has_next>,</#if>
</#list>
  }
}
