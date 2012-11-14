<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<#escape x as x?html><html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh-CN" lang="zh-CN">
<head>
<title><#if user.new>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText('user')}</title>
</head>
<body>
<@s.form action="save" method="post" cssClass="ajax">
	<#if !user.new>
		<@s.hidden name="user.id" />
		<@s.textfield label="%{getText('username')}" name="user.username" readonly="true"/>
		<@s.password label="%{getText('password')}" name="password"/>
		<@s.password label="%{getText('confirmPassword')}" name="confirmPassword"/>
	<#else>
		<@s.textfield label="%{getText('username')}" name="user.username" cssClass="required"/>
		<@s.password label="%{getText('password')}" name="password" cssClass="required"/>
		<@s.password label="%{getText('confirmPassword')}" name="confirmPassword" cssClass="required"/>
	</#if>
	<@s.textfield label="%{getText('name')}" name="user.name" cssClass="required"/>
	<@s.textfield label="%{getText('email')}" name="user.email" cssClass="email"/>
	<@s.textfield label="%{getText('phone')}" name="user.phone"/>
	<@s.checkbox label="%{getText('enabled')}" name="user.enabled" />
	<@s.checkboxlist label="%{getText('role')}" name="roleId" list="roles" listKey="key" listValue="value"/>
	<@s.submit value="%{getText('save')}" />
</@s.form>
</body>
</html></#escape>

