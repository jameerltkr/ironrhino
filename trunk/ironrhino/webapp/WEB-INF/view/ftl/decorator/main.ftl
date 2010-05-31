<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<#compress><#escape x as x?html><html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh-CN" lang="zh-CN">
<head>
<title><#noescape>${title}</#noescape></title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<meta http-equiv="X-UA-Compatible" content="chrome=1" />
<meta name="context_path" content="${request.contextPath}" />
<link rel="shortcut icon" href="<@url value="/assets/images/favicon.ico"/>" />
<link href="<@url value="/assets/styles/ironrhino-min.css"/>" media="screen" rel="stylesheet" type="text/css" />
<link href="<@url value="/assets/styles/app-min.css"/>" media="screen" rel="stylesheet" type="text/css" />
<!--[if IE]>
	<link href="<@url value="/assets/styles/ie.css"/>" media="all" rel="stylesheet" type="text/css" />
<![endif]-->
<script src="<@url value="/assets/scripts/ironrhino-min.js"/>" type="text/javascript"></script>
<#noescape>${head}</#noescape>
</head>

<body>
<div id="wrapper">
<div id="header">
<@authorize ifAnyGranted="ROLE_BUILTIN_USER">
<ul class="menu">
	<li><a href="<@url value="/index"/>">${action.getText('index')}</a></li>
	<@authorize ifAnyGranted="ROLE_ADMINISTRATOR">
		<li><a href="<@url value="/user"/>">${action.getText('user')}</a></li>
	</@authorize>
	<li><a href="<@url value="/user/password"/>">${action.getText('change')}${action.getText('password')}</a></li>
	<li><a href="<@url value="/logout"/>">${action.getText('logout')}</a></li>
</ul>
</@authorize>
<@authorize ifNotGranted="ROLE_BUILTIN_USER">
<div class="menu" style="text-align:center;font-size:1.2em;font-weight:bold;">
${action.getText('login')}
</div>
</@authorize>
</div>

<div id="content">
<div id="message">
<@s.actionerror cssClass="action_error" />
<@s.actionmessage cssClass="action_message" />
</div>
<#noescape>${body}</#noescape>
</div>

<!--
<div id="footer">
	<ul>
		<li><a href="<@url value="/index"/>">${action.getText('index')}</a></li>
	</ul>
	<p class="copyright">Copyright 2010</p>
</div>
-->

</div>
</body>
</html></#escape></#compress>
