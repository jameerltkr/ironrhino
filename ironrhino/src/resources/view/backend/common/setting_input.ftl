<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh-CN" lang="zh-CN">
<head>
<title>Create/Edit Setting</title>

</head>
<body>
<@s.form action="save" method="post" cssClass="ajax">
	<@s.hidden name="setting.id" />
	<@s.textfield label="%{getText('key')}" name="setting.key" />
	<@s.textfield label="%{getText('value')}" name="setting.value" />
	<@s.submit value="%{getText('save')}" />
</@s.form>
</body>
</html>


