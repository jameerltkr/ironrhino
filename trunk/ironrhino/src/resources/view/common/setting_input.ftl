<!DOCTYPE html>
<#escape x as x?html><html>
<head>
<title><#if setting.new>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText('setting')}</title>
</head>
<body>
<@s.form action="${actionBaseUrl}/save" method="post" cssClass="ajax form-horizontal">
	<#if !setting.new>
		<@s.hidden name="setting.id" />
	</#if>
	<#if Parameters.embedded??>
		<@s.hidden name="setting.key"/>
		<@s.hidden name="setting.description"/>
	<#elseif Parameters.brief??>
		<@s.hidden name="setting.key"/>
		<@s.textarea label="%{getText('description')}" name="setting.description" tabindex="-1" readonly=true cssClass="input-xxlarge" cssStyle="height:20px;border:none;resize:none;outline:none;"/>
	<#else>
		<@s.textfield label="%{getText('key')}" name="setting.key" cssClass="required checkavailable input-xxlarge"/>
	</#if>
	<#if Parameters.embedded??>
	<@s.textarea label="%{getText('value')}" theme="simple" name="setting.value" cssStyle="width:95%;" maxlength="4000"/>
	<#else>
	<@s.textarea label="%{getText('value')}" name="setting.value" cssClass="input-xxlarge" maxlength="4000"/>
	</#if>
	<#if !Parameters.brief??&&!Parameters.embedded??>
		<@s.textarea label="%{getText('description')}" name="setting.description" cssClass="input-xxlarge" maxlength="4000"/>
	</#if>
	<@s.submit value="%{getText('save')}" />
</@s.form>
</body>
</html></#escape>


