<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<#escape x as x?html><html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh-CN" lang="zh-CN">
<head>
<title>List Setttings</title>
</head>
<body>
<#assign config={'key':{},'value':{'template':r'<input type="text" size="80" name="settings.${entity.key}" value="${value?xhtml}"/>'}}>
<#assign bottomButtons=btn(action.getText('save'),null,'submit')+btn(action.getText('reload'),'Richtable.reload()')>
<@richtable entityName="setting" config=config readonly=true includeParameters=false bottomButtons=bottomButtons/>
</body>
</html></#escape>
