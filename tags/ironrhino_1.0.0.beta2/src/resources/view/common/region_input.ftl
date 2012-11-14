<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<#escape x as x?html><html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh-CN" lang="zh-CN">
<head>
<title><#if region.new>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText('region')}</title>
</head>
<body>
<@s.form action="save" method="post" cssClass="ajax">
	<@s.hidden name="parentId" />
	<@s.hidden name="region.id" />
	<@s.textfield label="%{getText('name')}" name="region.name" />
	<@s.textfield label="%{getText('displayOrder')}" name="region.displayOrder" cssClass="integer"/>
	<@s.submit value="%{getText('save')}" />
</@s.form>
</body>
</html></#escape>

