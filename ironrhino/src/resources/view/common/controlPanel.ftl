<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<#escape x as x?html><html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh-CN" lang="zh-CN">
<head>
<title>Control Panel</title>
<script type="text/javascript" src="${base}/dwr/engine.js"></script>
<script type="text/javascript" src="${base}/dwr/interface/ApplicationContextConsole.js"></script>
<script>
Initialization.init = function(){
$('#execute').click(function(){ApplicationContextConsole.execute($('#cmd').val(),function(result){alert(result);});});
}

</script>
</head>
<body>
<div><input id="cmd" type="text" name="cmd" size="80" />
<@button id="execute"/>
</div>
</body>
</html></#escape>
