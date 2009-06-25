﻿<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/security/tags"
	prefix="authz"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh-CN" lang="zh-CN">
<head>
<title>Login</title>
<script>
	Initialization.initForm = function() {
		$('#login_form')[0].onsuccess = redirect;
		$('#targetUrl')[0].value = self.location.href;
	}

	function redirect() {
		if ('<s:url value="%{targetUrl}" escapeAmp="false"/>')
			top.location.href = '<s:url value="%{targetUrl}" escapeAmp="false"/>';
		else
			top.location.href = '<s:url value="/"/>';
	}
</script>
</head>

<body>
<div class="tabs" tab="#${param['tab']}">
<ul>
	<li><a href="#login"><span>登录</span></a></li>
	<li><a href="#openid"><span>Openid登录</span></a></li>
	<li><a href="#forgot"><span>忘记密码</span></a></li>
	<li><a href="#resend"><span>手动激活</span></a></li>
</ul>
<div id="login"><s:form id="login_form" action="check"
	method="post" cssClass="ajax">
	<s:hidden id="targetUrl" name="targetUrl" />
	<s:textfield label="%{getText('username')}" name="username"
		labelposition="left" />
	<s:password label="%{getText('password')}" name="password"
		labelposition="left" />
	<s:select label="%{getText('rememberme')}" name="rememberme"
		list="#{'':'dont rememberme','604800':'1week','2592000':'1month','31536000':'1year','-1':'forever'}"
		listKey="key" listValue="value" labelposition="left" />
	<s:submit value="登录" />
</s:form></div>
<div id="openid"><s:form action="/account/openid" method="post"
	cssClass="ajax">
	<s:hidden name="targetUrl" />
	<s:textfield label="%{getText('openid')}" name="openid" size="30"
		cssClass="openid" labelposition="left" />
	<s:submit value="登录" />
</s:form></div>
<div id="forgot"><s:form action="forgot" method="post"
	cssClass="ajax reset">
	<s:textfield label="%{getText('email')}" name="account.email"
		cssClass="required email" labelposition="left" />
	<s:textfield label="%{getText('captcha')}" name="captcha" size="6"
		cssClass="autocomplete_off required captcha" labelposition="left" />
	<s:submit value="确认" />
</s:form></div>
<div id="resend"><s:form action="resend" method="post"
	cssClass="ajax reset">
	<s:textfield label="%{getText('username')}" name="account.username"
		cssClass="required" labelposition="left" />
	<s:textfield label="%{getText('email')}" name="account.email"
		cssClass="required email" labelposition="left" />
	<s:textfield label="%{getText('captcha')}" name="captcha" size="6"
		cssClass="autocomplete_off required captcha" labelposition="left" />
	<s:submit value="确认" />
</s:form></div>
</div>
</body>
</html>
