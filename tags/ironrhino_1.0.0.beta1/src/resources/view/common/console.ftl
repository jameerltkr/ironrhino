<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<#escape x as x?html><html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh-CN" lang="zh-CN">
<head>
<title>Console</title>
</head>
<body>
<@s.form action="console" method="post" cssClass="ajax">
	<@s.textfield theme="simple" id="cmd" name="cmd" size="50"/>
	<@s.submit id="submit" theme="simple" value="%{getText('confirm')}" />
	<@button text="rebuild index" onclick="$('#cmd').val('compassGps.index()');$('#submit').click()"/>
</@s.form>
</body>
</html></#escape>