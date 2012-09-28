<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<#escape x as x?html><html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh-CN" lang="zh-CN">
<head>
<title><#if setting.new>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText('setting')}</title>
</head>
<body>
<@s.form action="${getUrl(actionBaseUrl+'/save')}" method="post" cssClass="ajax">
	<#if !setting.new>
		<@s.hidden name="setting.id" />
	</#if>
	<#if Parameters.brief??>
		<@s.hidden name="setting.key"/>
		<@s.textarea label="%{getText('description')}" name="setting.description" cols="50" rows="1" tabindex="-1" readonly=true cssStyle="border:none;resize:none;outline:none;"/>
	<#else>
		<@s.textfield label="%{getText('key')}" name="setting.key" cssClass="required checkavailable" size="50"/>
	</#if>
	<@s.textarea label="%{getText('value')}" name="setting.value" cols="50" rows="5" />
	<#if !Parameters.brief??>
		<@s.textarea label="%{getText('description')}" name="setting.description" cols="50" rows="5" />
	</#if>
	<@s.submit value="%{getText('save')}" />
</@s.form>
</body>
</html></#escape>

