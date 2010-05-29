<#macro richtable config entityName action='' actionColumnWidth='150px' actionColumnButtons='' bottomButtons='' resizable=true sortable=true readonly=false createable=true celleditable=true deleteable=true searchable=false searchButtons='' includeParameters=true>
<@rtstart action=action?has_content?string(action,entityName) readonly=readonly resizable=resizable sortable=sortable includeParameters=includeParameters/>
<#list config?keys as name>
<#local cellName=((config[name]['trimPrefix']??)?string('',entityName+'.'))+name>
<@rttheadtd name=name class=config[name]['class']! width=config[name]['width']! cellName=cellName cellEdit=config[name]['cellEdit'] readonly=readonly resizable=resizable/>
</#list>
<@rtmiddle width=actionColumnWidth readonly=readonly/>
<#local index=0>
<#if resultPage??><#local list=resultPage.result></#if>
<#list list as entity>
<#local index=index+1>
<@rttbodytrstart entity=entity odd=(index%2==1) readonly=readonly/>
<#list config?keys as name>
	<#if config[name]['value']??>
	<#local value=config[name]['value']>
	<#else>
	<#local value=entity[name]!>
	</#if>
	<@rttbodytd entity=entity value=value celleditable=config[name]['cellEdit']?? template=config[name]['template']!/>
</#list>
<@rttbodytrend entity=entity buttons=actionColumnButtons readonly=readonly celleditable=celleditable deleteable=deleteable/>
</#list>
<@rtend buttons=bottomButtons readonly=readonly createable=createable celleditable=celleditable deleteable=deleteable searchable=searchable searchButtons=searchButtons/>
</#macro>

<#macro rtstart action='',readonly=false,resizable=true,sortable=true,includeParameters=true>
<form id="${action}_form" action="${getUrl(action)}" method="post" class="richtable ajax view"<#if resizable> resizable="true" minColWidth="40"</#if>>
<#if includeParameters>
<#list Parameters?keys as name>
<#if !name?starts_with('resultPage.')&&name!='keyword'>
<input type="hidden" name="${name}" value="${Parameters[name]}" />
</#if>
</#list>
</#if>
<table class="richtable<#if sortable> sortable</#if>">
<thead>
<tr>
<#if !readonly>
<td class="nosort" width="30px"><input type="checkbox" class="checkbox"/></td>
</#if>
</#macro>

<#macro rttheadtd name,cellName='',cellEdit='',class='',width='',readonly=false,resizable=true>
<td class="tableHeader<#if class!=''> ${class}</#if>"<#if width!=''> width="${width}"</#if><#if !(readonly||cellEdit=='')> cellName="${cellName}"</#if><#if cellEdit!=''> cellEdit="${cellEdit}"</#if>>
<#if resizable>
<span class="resizeTitle">${action.getText(name)}</span>
<span class="resizeBar"></span>
<#else>
${action.getText(name)}
</#if>
</td>
</#macro>
<#macro rtmiddle width='150px' readonly=false>
<#if !readonly>
<td class="nosort" width="${width}"></td>
</#if>
</tr>
</thead>
<tbody>
</#macro>

<#macro rttbodytrstart entity odd readonly=false>
<tr class="${odd?string('odd','even')}"<#if entity.id??> rowid="${entity.id?string}"</#if>>
<#if !readonly><td><input type="checkbox" name="check"/></td></#if>
</#macro>

<#macro rttbodytd value,entity,celleditable=true,template=''>
<td<#if celleditable><#if value?string=='true'||value?string=='false'> cellValue="${value?string}"</#if><#if value?is_hash&&value.displayName??> cellValue="${value.name()}"</#if></#if>><#rt>
<#if template==''><#t>
<#if value?string=='true'||value?string=='false'><#t>
${action.getText(value?string)}<#t>
<#else><#t>
<#if value?is_hash&&value.displayName??>
${value.displayName}
<#else>
${value?xhtml}<#t>
</#if>
</#if><#t>
<#else><#t>
<#local temp=template?interpret><#t>
<@temp/><#t>
</#if><#t>
</td><#lt>
</#macro>

<#macro rttbodytrend entity buttons='' readonly=false celleditable=true deleteable=true>
<#if !readonly>
<td class="action">
<#if buttons!=''>
<#local temp=buttons?interpret>
<@temp/>
<#else>
<@button text=action.getText('edit') view='input'/>
<#if celleditable>
<@button text=action.getText('save') action='save'/>
</#if>
<#if deleteable>
<@button text=action.getText('delete') action='delete'/>
</#if>
</#if>
</td>
</#if>
</tr>
</#macro>

<#macro rtend buttons='' readonly=false createable=true celleditable=true deleteable=true searchable=false searchButtons=''>
</tbody>
</table>
<table style="margin-top:10px;">
<tr>
<td width="30%">
<#if resultPage??>
<div class="pagination">
<#if resultPage.first>
<span class="disabled" title="${action.getText('firstpage')}">&lt;&lt;</span>
<span class="disabled" title="${action.getText('previouspage')}">&lt;</span>
<#else>
<a class="firstPage" title="${action.getText('firstpage')}">&lt;&lt;</a>
<a class="prevPage" title="${action.getText('previouspage')}">&lt;</a>
</#if>
<#if resultPage.last>
<span class="disabled" title="${action.getText('nextpage')}">&gt;</span>
<span class="disabled" title="${action.getText('lastpage')}">&gt;&gt;</span>
<#else>
<a class="nextPage" title="${action.getText('nextpage')}">&gt;</a>
<a class="lastPage" title="${action.getText('lastpage')}">&gt;&gt;</a>
</#if>
<input type="text" name="resultPage.pageNo" value="${resultPage.pageNo}" class="inputPage"/>/<span class="totalPage">${resultPage.totalPage}</span>${action.getText('page')}
${action.getText('pagesize')}<select name="resultPage.pageSize" class="pageSize">
<#local array=[5,10,20,50,100,500]>
<#list array as ps>
<option value="${ps}" <#if resultPage.pageSize==ps>selected</#if>>${ps}</option>
</#list> 
<option value="${resultPage.totalRecord}">${action.getText('all')}</option>
</select>${action.getText('row')}
</div>
</#if>
</td>
<td class="action" width="35%" align="center">
<#if buttons!=''>
<#local temp=buttons?interpret>
<@temp/>
<#else>
<#if !readonly>
<#if createable><@button text=action.getText('create') view='input'/></#if>
<#if celleditable><@button text=action.getText('save') action='save'/></#if>
<#if deleteable><@button text=action.getText('delete') action='delete'/></#if>
</#if><@button text=action.getText('reload') action='reload'/></#if>
</td>
<td width="20%">
<#if searchable>
<@s.textfield theme="simple" name="keyword" cssClass="focus" size="15"/><@s.submit theme="simple" value="%{getText('search')}" />
</#if>
<#if searchButtons!=''>
<#local temp=searchButtons?interpret>
<@temp/>
</#if>
</td>
<td align="right">
<#if resultPage??>
${action.getText('total')}${resultPage.totalRecord}${action.getText('record')}<#if resultPage.totalRecord!=0>,${action.getText('display')}${resultPage.start+1}-${resultPage.start+resultPage.result?size}</#if>
<#else>
${action.getText('total')}${list?size}${action.getText('record')}<#if list?size!=0>,${action.getText('display')}1-${list?size}</#if>	
</#if>
</td>
</tr>
</table>
</form>
<#if !readonly&&celleditable>
<div style="display: none;">
<textarea id="rt_edit_template_input">
<input type="text" class="text" value="" onblur="Richtable.updateCell(this)" style="width: 100%;"/>
</textarea>
<textarea id="rt_edit_template_inputdate">
<input type="text" class="text date" value="" style="width: 100%;"/>
</textarea>
<textarea id="rt_select_template_boolean">
<select onblur="Richtable.updateCell(this)" style="width: 100%;">
<option value="true">${action.getText('true')}</option>
<option value="false">${action.getText('false')}</option>
</select>
</textarea>
</div>
</#if>
</#macro>