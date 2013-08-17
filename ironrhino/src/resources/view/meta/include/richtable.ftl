<#macro richtable columns entityName formid='' action='' actionColumnWidth='50px' actionColumnButtons='' bottomButtons='' rowid='' resizable=true sortable=true readonly=false readonlyExpression="" createable=true viewable=false celleditable=true deletable=true enableable=false searchable=false searchButtons='' includeParameters=true showPageSize=true showCheckColumn=true multipleCheck=true columnfilterable=true rowDynamicAttributes='' formHeader='' formFooter=''>
<@rtstart formid=formid action=action entityName=entityName resizable=resizable sortable=sortable includeParameters=includeParameters showCheckColumn=showCheckColumn multipleCheck=multipleCheck columnfilterable=columnfilterable formHeader=formHeader>
<#nested/>
</@rtstart>
<#local index = 0>
<#local size = columns?keys?size>
<#list columns?keys as name>
<#local index = index+1>
<#local cellName=((columns[name]['trimPrefix']??)?string('',entityName+'.'))+name>
<@rttheadtd name=name alias=columns[name]['alias']! title=columns[name]['title']! class=columns[name]['cssClass']! width=columns[name]['width']! cellName=cellName cellEdit=columns[name]['cellEdit'] readonly=readonly resizable=(readonly&&index!=size||!readonly)&&resizable excludeIfNotEdited=columns[name]['excludeIfNotEdited']!false/>
</#list>
<@rtmiddle width=actionColumnWidth showActionColumn=actionColumnButtons?has_content||!readonly||viewable/>
<#if resultPage??><#local list=resultPage.result></#if>
<#list list as entity>
<#local entityReadonly = !readonly && readonlyExpression?has_content && readonlyExpression?eval />
<#local _rowDynamicAttributes={}>
<#if rowDynamicAttributes?has_content>
<#local _rowDynamicAttributes><@rowDynamicAttributes?interpret /></#local>
<#if _rowDynamicAttributes?has_content>
<#local _rowDynamicAttributes=_rowDynamicAttributes?eval>
<#else>
<#local _rowDynamicAttributes={}>
</#if>
</#if>
<#if celleditable&&!readonly&&entityReadonly>
<#local _rowDynamicAttributes=_rowDynamicAttributes+{"data-readonly":"true"}>
</#if>
<@rttbodytrstart entity=entity showCheckColumn=showCheckColumn multipleCheck=multipleCheck rowid=rowid dynamicAttributes=_rowDynamicAttributes/>
<#list columns?keys as name>
	<#if columns[name]['value']??>
	<#local value=columns[name]['value']>
	<#else>
	<#if !name?contains('.')>
		<#local value=entity[name]!>
	<#else>
		<#local value=('entity.'+name)?eval!>
	</#if>
	</#if>
	<#local dynamicAttributes={}>
	<#if columns[name]['readonlyExpression']?has_content && columns[name]['readonlyExpression']?eval>
		<#local dynamicAttributes=dynamicAttributes+{'data-readonly':'true'}/>
	</#if>
	<@rttbodytd entity=entity value=value celleditable=columns[name]['cellEdit']?? template=columns[name]['template']! cellDynamicAttributes=columns[name]['dynamicAttributes'] dynamicAttributes=dynamicAttributes/>
</#list>
<@rttbodytrend entity=entity buttons=actionColumnButtons editable=!readonly viewable=viewable entityReadonly=entityReadonly/>
</#list>
<@rtend buttons=bottomButtons readonly=readonly createable=createable celleditable=celleditable deletable=deletable enableable=enableable searchable=searchable searchButtons=searchButtons showPageSize=showPageSize formFooter=formFooter/>
</#macro>

<#macro rtstart formid='',action='',entityName='',resizable=true,sortable=true,includeParameters=true showCheckColumn=true multipleCheck=true columnfilterable=true formHeader=''>
<#local action=action?has_content?string(action,request.requestURI)>
<form id="<#if formid?has_content>${formid}<#else>${entityName}_form</#if>" action="${getUrl(action)}" method="post" class="richtable ajax view history"<#if actionBaseUrl!=action> data-actionbaseurl="${actionBaseUrl}"</#if><#if entityName!=action&&entityName!=''> data-entity="${entityName}"</#if>>
<#nested/>
<#if includeParameters>
<#list Parameters?keys as name>
<#if name!='_'&&name!='pn'&&name!='ps'&&!name?starts_with('resultPage.')&&name!='keyword'&&name!='check'>
<input type="hidden" name="${name}" value="${Parameters[name]}" />
</#if>
</#list>
</#if>
<#if formHeader?has_content>
<@formHeader?interpret/>
</#if>
<table class="table table-hover table-striped table-bordered richtable<#if sortable> sortable</#if><#if columnfilterable> filtercolumn</#if><#if resizable> resizable</#if>">
<thead>
<tr>
<#if showCheckColumn>
<th class="nosort <#if multipleCheck>checkbox<#else>radio</#if>" style="width:40px;"><#if multipleCheck><input type="checkbox" class="checkbox custom"/></#if></th>
</#if>
</#macro>

<#macro rttheadtd name,alias='',title='',cellName='',cellEdit='',class='',width='',readonly=false,resizable=true,excludeIfNotEdited=false>
<th<#if title!=''> title="${action.getText(title)}"</#if><#if excludeIfNotEdited||class!=''> class="<#if excludeIfNotEdited> excludeIfNotEdited</#if><#if class!=''> ${class}</#if>"</#if><#if width!=''> style="width:${width};"</#if><#if !readonly> data-cellName="${cellName}"</#if><#if cellEdit!=''> data-cellEdit="${cellEdit}"</#if>>
<#if resizable><span class="resizeTitle"></#if><#if alias!=''>${action.getText(alias)}<#else>${action.getText(name)}</#if><#if resizable></span><span class="resizeBar visible-desktop"></span></#if>
</th>
</#macro>
<#macro rtmiddle width='50px' showActionColumn=true>
<#if showActionColumn>
<th class="nosort" style="width:${width};"></th>
</#if>
</tr>
</thead>
<tbody>
</#macro>

<#macro rttbodytrstart entity showCheckColumn=true multipleCheck=true rowid='' dynamicAttributes...>
<#if rowid==''>
	<#local id=entity.id?string/>
<#else>
	<#local id><@rowid?interpret/></#local>
</#if>
<tr<#if entity.enabled??> data-enabled="${entity.enabled?string}"</#if><#if !showCheckColumn&&id?has_content> data-rowid="${id}"</#if><#list dynamicAttributes?keys as attr><#if attr=='dynamicAttributes'><#list dynamicAttributes['dynamicAttributes']?keys as attr> ${attr}="${dynamicAttributes['dynamicAttributes'][attr]?string}"</#list><#else> ${attr}="${dynamicAttributes[attr]?string}"</#if></#list>>
<#if showCheckColumn><td class="<#if multipleCheck>checkbox<#else>radio</#if>"><input type="<#if multipleCheck>checkbox<#else>radio</#if>" name="check"<#if id?has_content> value="${id}"</#if> class="custom"/></td></#if>
</#macro>

<#macro rttbodytd value,entity,celleditable=true,template='',cellDynamicAttributes='',dynamicAttributes...>
<#if cellDynamicAttributes?is_string>
	<#local _cellDynamicAttributes={}>
	<#if cellDynamicAttributes?has_content>
		<#local _cellDynamicAttributes><@cellDynamicAttributes?interpret /></#local>
		<#if _cellDynamicAttributes?has_content>
			<#local _cellDynamicAttributes=_cellDynamicAttributes?eval>
		<#else>
			<#local _cellDynamicAttributes={}>
		</#if>
	</#if>
	<#local cellDynamicAttributes=_cellDynamicAttributes>
</#if>
<#if dynamicAttributes['dynamicAttributes']??>
<#local dynamicAttributes=dynamicAttributes+dynamicAttributes['dynamicAttributes']>
</#if>
<#if cellDynamicAttributes['class']?? && dynamicAttributes['class']??>
<#local cellDynamicAttributes=cellDynamicAttributes+{'class':dynamicAttributes['class']+' '+cellDynamicAttributes['class']}>
</#if>
<#local dynamicAttributes=dynamicAttributes+cellDynamicAttributes>
<td<#if celleditable><#if value??><#if value?is_boolean> data-cellvalue="${value?string}"</#if><#if value?is_hash&&value.displayName??> data-cellvalue="${value.name()}"</#if></#if></#if><#list dynamicAttributes?keys as attr><#if attr!='dynamicAttributes'> ${attr}="${dynamicAttributes[attr]?html}"</#if></#list>><#rt>
<#if template==''>
	<#if value??>
		<#if value?is_boolean>
		${action.getText(value?string)}<#t>
		<#elseif value?is_hash&&value.displayName??>
		${value.displayName}<#t>
		<#else>
		${value?xhtml}<#t>
		</#if>
	</#if>
<#else>
	<@template?interpret/><#t>
</#if>
</td>
</#macro>

<#macro rttbodytrend entity buttons='' editable=true viewable=false entityReadonly=false>
<#if buttons?has_content || editable || viewable>
<td class="action">
<#if buttons!=''>
<@buttons?interpret/>
<#else>
<#if viewable>
<button type="button" class="btn" data-view="view">${action.getText("view")}</button>
</#if>
<#if editable && !entityReadonly>
<button type="button" class="btn" data-view="input">${action.getText("edit")}</button>
</#if>
</#if>
</td>
</#if>
</tr>
</#macro>

<#macro rtend buttons='' readonly=false createable=true celleditable=true deletable=true enableable=false searchable=false searchButtons='' showPageSize=true formFooter=''>
</tbody>
</table>
<div class="toolbar row-fluid">
<div class="pagination span4">
<#if resultPage?? && resultPage.paginating>
<ul>
<#if resultPage.first>
<li class="disabled firstPage"><a title="${action.getText('firstpage')}"><i class="icon-fast-backward"></i></a></li>
<li class="disabled"><a title="${action.getText('previouspage')}"><i class="icon-step-backward"></i></a></li>
<#else>
<li class="firstPage"><a title="${action.getText('firstpage')}" href="${resultPage.renderUrl(1)}"><i class="icon-fast-backward"></i></a></li>
<li class="prevPage"><a title="${action.getText('previouspage')}" href="${resultPage.renderUrl(resultPage.previousPage)}"><i class="icon-step-backward"></i></a></li>
</#if>
<#if resultPage.last>
<li class="disabled"><a title="${action.getText('nextpage')}"><i class="icon-step-forward"></i></a></li>
<li class="disabled lastPage"><a title="${action.getText('lastpage')}"><i class="icon-fast-forward"></i></a></li>
<#else>
<li class="nextPage"><a title="${action.getText('nextpage')}" href="${resultPage.renderUrl(resultPage.nextPage)}"><i class="icon-step-forward"></i></a></li>
<li class="lastPage"><a title="${action.getText('lastpage')}" href="${resultPage.renderUrl(resultPage.totalPage)}"><i class="icon-fast-forward"></i></a></li>
</#if>
<li>
<span class="input-append">
    <input type="text" name="resultPage.pageNo" value="${resultPage.pageNo}" class="inputPage integer positive" title="${action.getText('currentpage')}"/><span class="add-on totalPage"><span class="divider">/</span><strong title="${action.getText('totalpage')}">${resultPage.totalPage}</strong></span>
</span>
<#if showPageSize>
<li class="visible-desktop">
<select name="resultPage.pageSize" class="pageSize" title="${action.getText('pagesize')}">
<#local array=[5,10,20,50,100,500]>
<#local selected=false>
<#list array as ps>
<option value="${ps}"<#if resultPage.pageSize==ps><#local selected=true> selected</#if>>${ps}</option>
</#list>
<#if resultPage.canListAll>
<option value="${resultPage.totalResults}"<#if !selected && resultPage.pageSize==resultPage.totalResults><#local selected=true> selected</#if>>${action.getText('all')}</option>
</#if>
<#if !selected !array?seq_contains(resultPage.pageSize)>
<option value="${resultPage.pageSize}" selected>${resultPage.pageSize}</option>
</#if>
</select>
</li>
</#if>
</ul>
</#if>
</div>
<div class="action span4">
<#if buttons!=''>
<@buttons?interpret/>
<#else>
<#if !readonly>
<#if createable><button type="button" class="btn" data-view="input">${action.getText("create")}</button></#if>
<#if celleditable><button type="button" class="btn confirm" data-action="save">${action.getText("save")}</button></#if>
<#if enableable>
<button type="button" class="btn confirm hidden-pad" data-action="enable" data-shown="selected" data-filterselector="[data-enabled='false']:not([data-readonly='true'])">${action.getText("enable")}</button>
<button type="button" class="btn confirm hidden-pad" data-action="disable" data-shown="selected" data-filterselector="[data-enabled='true']:not([data-readonly='true'])">${action.getText("disable")}</button>
</#if>
</#if>
<#if !readonly||deletable><button type="button" class="btn" data-action="delete" data-shown="selected" data-filterselector="<#if enableable>[data-enabled='false']</#if>:not([data-deletable='false'])">${action.getText("delete")}</button></#if>
<button type="button" class="btn" data-action="reload">${action.getText("reload")}</button>
</#if>
</div>
<div class="search span2">
<#if searchable>
<span class="input-append">
    <input type="text" name="keyword" value="${keyword!?html}" placeholder="${action.getText('search')}"/><span class="add-on hidden-tablet hidden-phone"><i class="icon-search"></i></span>
</span>
</#if>
<#if searchButtons!=''>
<@searchButtons?interpret/>
<#else>
</#if>
</div>
<div class="status span2">
<span>
<#if resultPage??>
${resultPage.totalResults} ${action.getText('record')}
<#else>
${list?size}${action.getText('record')}
</#if>
</span>
</div>
</div>
<#if formFooter?has_content>
<@formFooter?interpret/>
</#if>
</form>
</#macro>