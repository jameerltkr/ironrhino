<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<#escape x as x?html><html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh-CN" lang="zh-CN">
<head>
<title><#if page.new>${action.getText('create')}<#else>${action.getText('edit')}</#if>${action.getText('page')}</title>
<meta name="cms_path" content="${cmsPath}" />
<script type="text/javascript" src="<@url value="/assets/components/tiny_mce/jquery.tinymce.js"/>"></script>
<script type="text/javascript">
$(function() {
		$('#display_page_head').click(function(){$('#page_head').toggle()});
		$('#upload').click();

		var cmsPath= $('meta[name="cms_path"]').attr('content') || '';
		var options = {
			language : MessageBundle.shortLang(),
			script_url : '<@url value="/assets/components/tiny_mce/tiny_mce.js"/>',
			theme : "advanced",
			plugins : "safari,pagebreak,layer,table,advimage,advlink,emotions,inlinepopups,preview,media,searchreplace,print,contextmenu,paste,fullscreen,noneditable,visualchars,xhtmlxtras,autosave",
			theme_advanced_buttons1 : "cut,copy,paste,pastetext,pasteword,|,search,replace,|,bold,italic,underline,strikethrough,|,justifyleft,justifycenter,justifyright,justifyfull,formatselect,fontselect,fontsizeselect,|,fullscreen,preview,code",
			theme_advanced_buttons2 : "blockquote,|,undo,redo,|,link,unlink,anchor,image,cleanup,|,forecolor,backcolor,tablecontrols,|,hr,removeformat,visualaid,|,sub,sup,|,charmap,emotions,media,|,print",
			theme_advanced_buttons3 : "",
        	theme_advanced_buttons4 : "",
			theme_advanced_toolbar_location : "top",
			theme_advanced_toolbar_align : "left",
			theme_advanced_statusbar_location : "bottom",
			theme_advanced_resizing : true,
			remove_script_host : false,
        	convert_urls : false,
			mode : "textareas"
		};
		$('#page_content').tinymce(options);
		setTimeout(function(){
		var ed = tinymce.EditorManager.get('page_content');
		$('#draft').click(function(){
			if(!$('#form').attr('dirty')&&!ed.isDirty()) return false;
			$('#form').attr('action','draft');
			ed.save();
		});
		$('#save').click(function(){
			$('#form').attr('action','save');
			ed.save();
		});
		ed.onKeyUp.add(function(ed, e) {
          ed.isNotDirty = 0;
      	});
		var form = $('#form');
		form[0].onsuccess = function(){	
		$('#form').removeAttr('dirty')
		ed.isNotDirty = 1;
		var page = Ajax.jsonResult.page;
		if(page){
			var date = page.draftDate;
			var path = page.path;
			$('#page_id').val(page.id);
			$('#page_path').val(path);
			path = CONTEXT_PATH+cmsPath+path;
			if(date){
				//save draft 
				$('.draft').show();
				$('.draftDate').text(date);
				$('#preview').attr('href',path+'?preview=true');
				$('#upload').attr('href',CONTEXT_PATH+'/common/upload?folder=/page/'+page.id);
			}else{
				//save
				$('.draft').hide();
				$('#view').attr('href',path);
				$('#upload').attr('href',CONTEXT_PATH+'/common/upload?folder=/page/'+page.id);
			}
			}else{
				document.location.href=document.location.href;
			}
		};
		setInterval(function(){
			if(($('#form').attr('dirty')||ed.isDirty())&&$('#page_path').val()){
				$('#form').attr('action','draft');
				ed.save();
				form.submit();
				}
		},60000);
		},1500);
		$('input[name="page.path"],input[name="page.title"]').keyup(function(){
			$('#form').attr('dirty',true);
		});
		$('#drop').click(function(){
			var form = $('#form');
			form.attr('action','drop');
			$('#page_content').text('');
			form[0].onsuccess=function(){
				document.location.href = document.location.href;
			};
			}
		);
		
		//input tags
		function split(val) {
			return val.split(/,\s*/);
		}
		function extractLast(term) {
			return split(term).pop();
		}
		$('input[name="page.tagsAsString"]').autocomplete({
			source: function(request, response) {
				$.getJSON("suggest", {
					term: extractLast(request.term)
				}, response);
			},
			search: function() {
				// custom minLength
				var term = extractLast(this.value);
				if (term.length < 1) {
					return false;
				}
			},
			focus: function() {
				// prevent value inserted on focus
				return false;
			},
			select: function(event, ui) {
				var terms = split( this.value );
				// remove the current input
				terms.pop();
				// add the selected item
				terms.push( ui.item.value );
				// add placeholder to get the comma-and-space at the end
				terms.push("");
				this.value = terms.join(",");
				return false;
			}
		});
});

</script>
</head>
<body>
<@s.form id="form" action="draft" method="post" cssClass="ajax">
	<@s.hidden id="page_id" name="page.id" />
	<@s.textfield id="page_path" label="%{getText('path')}" name="page.path" cssClass="required" size="50"/>
	<@s.textfield label="%{getText('displayOrder')}" name="page.displayOrder" cssClass="integer"/>
	<@s.textfield label="%{getText('tag')}" name="page.tagsAsString" size="50"/>
	<@s.textfield label="%{getText('title')}" name="page.title" size="50"/>
	<@s.textarea id="page_content" label="%{getText('content')}" labelposition="top" name="page.content" cols="50" rows="12"/>
	<@s.textarea id="page_head" name="page.head" cols="100" rows="3" cssStyle="display:none;"/>
	<div class="field">
		<@button id="display_page_head" text="${action.getText('edit')}${action.getText('head')}"/>
		<#if page.id??>
		<@button id="upload" type="link" text="${action.getText('upload')}" href="${getUrl('/common/upload?folder=/page/'+page.id)}" target="_blank"/>
		<#else>
		<@button id="upload" type="link" text="${action.getText('upload')}" target="_blank"/>
		</#if>
	</div>
	<div class="field">
	<@s.submit id="draft" value="%{getText('draft')}" theme="simple"/>
	<span class="draft" <#if !draft>style="display: none;"</#if>>
	${action.getText('draftDate')}:<span class="draftDate"><#if page.draftDate??>${page.draftDate?datetime}</#if></span>
	<#if page.id??>
	<@button id="preview" type="link" text="${action.getText('preview')}" href="${getUrl(cmsPath+page.path)}?preview=true" target="_blank"/>
	<#else>
	<@button id="preview" type="link" text="${action.getText('preview')}" target="_blank"/>
	</#if>
	<@s.submit id="drop" value="%{getText('drop')}" theme="simple"/>
	</span>
	</div>
	<div class="field">
	<@s.submit id="save" value="%{getText('save')}" theme="simple"/>
	<#if page.id??>
	<@button id="view" type="link" text="${action.getText('view')}" href="${getUrl(cmsPath+page.path)}" target="_blank"/>
	<#else>
	<@button id="view" type="link" text="${action.getText('view')}" target="_blank"/>
	</#if>
	</div>
</@s.form>
</body>
</html></#escape>


