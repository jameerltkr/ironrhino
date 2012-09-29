var MODERN_BROWSER = !$.browser.msie || $.browser.version > 8;
(function() {
	var d = document.domain;
	if (!d.match(/^(\d+\.){3}\d+$/)) {
		d = d.split('.');
		try {
			if (d.length > 2)
				document.domain = d[d.length - 2] + '.' + d[d.length - 1];
		} catch (e) {
			if (d.length > 3)
				document.domain = d[d.length - 3] + '.' + d[d.length - 2] + '.'
						+ d[d.length - 1];
		}
	}
	$.ajaxSettings.traditional = true;
	var $ajax = $.ajax;
	if (MODERN_BROWSER)
		$.ajax = function(options) {
			options.url = UrlUtils.makeSameOrigin(options.url);
			options.xhrFields = {
				withCredentials : true
			};
			return $ajax(options);
		}

	if (typeof $.rc4EncryptStr != 'undefined'
			&& ($('meta[name="pe"]').attr('content') != 'false')) {
		var temp = $.param;
		$.param = function(a, traditional) {
			if (jQuery.isArray(a) || a.jquery) {
				jQuery.each(a, function() {
							if (/password$/.test(this.name.toLowerCase())) {
								try {
									var key = $.cookie('T');
									key = key.substring(15, 25);
									this.value = $
											.rc4EncryptStr(
													encodeURIComponent(this.value
															+ key), key);
								} catch (e) {
								}
							}
						});

			}
			return temp(a, traditional);
		}
	}

})();

Indicator = {
	text : '',
	show : function(iserror) {
		if (!$('#indicator').length)
			$('<div id="indicator"></div>').appendTo(document.body);
		var ind = $('#indicator');
		if (iserror && ind.hasClass('loading'))
			ind.removeClass('loading');
		if (!iserror && !ind.hasClass('loading'))
			ind.addClass('loading');
		ind.html(Indicator.text || MessageBundle.get('ajax.loading'));
		ind.show();
		Indicator.text = '';
	},
	showError : function() {
		Indicator.text = MessageBundle.get('ajax.error');
		Indicator.show(true);
	},
	hide : function() {
		Indicator.text = '';
		if ($('#indicator'))
			$('#indicator').hide()
	}
};

UrlUtils = {
	extractDomain : function(a) {
		if (UrlUtils.isAbsolute(a)) {
			a = a.replace(/:\d+/, '');
			a = a.substring(a.indexOf('://') + 3);
			var i = a.indexOf('/');
			if (i > 0)
				a = a.substring(0, i);
			return a;
		} else {
			return document.location.hostname;
		}
	},
	isSameDomain : function(a, b) {
		b = b || document.location.href;
		var ad = UrlUtils.extractDomain(a);
		var bd = UrlUtils.extractDomain(b);
		return ad == bd;
	},
	isSameOrigin : function(a, b) {
		b = b || document.location.href;
		var ad = UrlUtils.extractDomain(a);
		var bd = UrlUtils.extractDomain(b);
		if ($.browser.msie && ad != bd)
			return false;
		var arra = ad.split('.');
		var arrb = bd.split('.');
		return (arra[arra.length - 1] == arrb[arrb.length - 1] && arra[arra.length
				- 2] == arrb[arrb.length - 2]);
	},
	makeSameOrigin : function(url, referrer) {
		referrer = referrer || document.location.href;
		if (!UrlUtils.isSameOrigin(url, referrer))
			return referrer.substring(0, referrer.indexOf('/', referrer
									.indexOf('://')
									+ 3))
					+ CONTEXT_PATH + '/webproxy/' + url;
		else
			return url;
	},
	isAbsolute : function(a) {
		if (!a)
			return false;
		var index = a.indexOf('://');
		return (index == 4 || index == 5);
	},
	absolutize : function(url) {
		if (UrlUtils.isAbsolute(url))
			return url;
		var a = document.location.href;
		var index = a.indexOf('://');
		if (url.length == 0 || url.indexOf('/') == 0) {
			return a.substring(0, a.indexOf('/', index + 3)) + CONTEXT_PATH
					+ url;
		} else {
			return a.substring(0, a.lastIndexOf('/') + 1) + url;
		}
	}
}

Message = {
	compose : function(message, className) {
		return '<div class="'
				+ className
				+ '"><span class="close" onclick="$(this.parentNode).remove()"></span>'
				+ message + '</div>';
	},
	showMessage : function() {
		Message.showActionMessage(MessageBundle.get.apply(this, arguments));
	},
	showActionMessage : function(messages, target) {
		if (!messages)
			return;
		if (typeof messages == 'string') {
			var a = [];
			a.push(messages);
			messages = a;
		}
		if (!$('#message').length)
			$('<div id="message"></div>').prependTo($('#content'));
		if (typeof $.fn.jnotifyInizialize != 'undefined') {
			if (!$('#notification').length)
				$('<div id="notification"><div>').prependTo(document.body)
						.jnotifyInizialize({
									oneAtTime : false,
									appendType : 'append'
								}).css({
									'position' : 'fixed',
									'top' : '40px',
									'right' : '40px',
									'width' : '250px',
									'min-height' : '50px',
									'z-index' : '9999'
								});
			for (var i = 0; i < messages.length; i++) {
				$('#notification').jnotifyAddMessage({
							text : messages[i],
							permanent : false
						});
			}
			return;
		}
		var html = '';
		for (var i = 0; i < messages.length; i++)
			html += Message.compose(messages[i], 'action-message');
		if (target && target.tagName == 'FORM') {
			if ($('#' + target.id + '_message').length == 0)
				$(target).before('<div id="' + target.id + '_message"></div>');
			$('#' + target.id + '_message').html(html);
		} else {
			if (!$('#message').length)
				$('<div id="message"></div>').prependTo($('#content'));
			$('#message').html(html);
		}
	},
	showError : function() {
		Message.showActionError(MessageBundle.get.apply(this, arguments));
	},
	showActionError : function(messages, target) {
		if (!messages)
			return;
		if (typeof messages == 'string') {
			var a = [];
			a.push(messages);
			messages = a;
		}
		var parent = $('#content');
		var msg;
		if ($('#_window_').parents('.ui-dialog').length)
			parent = $('#_window_');
		if (!$('#message', parent).length)
			$('<div id="message"></div>').prependTo(parent);
		msg = $('#message', parent);
		if (typeof $.fn.jnotifyInizialize != 'undefined') {
			msg.jnotifyInizialize({
						oneAtTime : false
					});
			for (var i = 0; i < messages.length; i++)
				msg.jnotifyAddMessage({
							text : messages[i],
							disappearTime : 60000,
							permanent : false,
							type : 'error'
						});
			$('html,body').animate({
						scrollTop : $('#message').offset().top - 20
					}, 100);
			return;
		}
		if ($.alerts) {
			$.alerts.alert(messages.join('\n'), MessageBundle.get('error'));
			return;
		}
		var html = '';
		for (var i = 0; i < messages.length; i++)
			html += Message.compose(messages[i], 'action-error');
		if (html)
			if (target && target.tagName == 'FORM') {
				if ($('#' + target.id + '_message').length == 0)
					$(target).before('<div id="' + target.id
							+ '_message"></div>');
				$('#' + target.id + '_message').html(html);
			} else {
				if (!$('#message').length)
					$('<div id="message"></div>').prependTo($('#content'));

				$('#message').html(html);
			}
	},
	showFieldError : function(field, msg, msgKey) {
		var msg = msg || MessageBundle.get(msgKey);
		if (field && $(field).length) {
			field = $(field);
			// field.parent().append(Message.compose(msg, 'field-error'));
			$('.fieldError', field.closest('.field')).remove();
			var prompt = $('<div class="fieldError removeonclick"><div class="fieldErrorContent">'
					+ msg + '</div><div>').insertAfter(field);
			var arrow = $('<div class="fieldErrorArrow"/>')
					.html('<div class="line10"><!-- --></div><div class="line9"><!-- --></div><div class="line8"><!-- --></div><div class="line7"><!-- --></div><div class="line6"><!-- --></div><div class="line5"><!-- --></div><div class="line4"><!-- --></div><div class="line3"><!-- --></div><div class="line2"><!-- --></div><div class="line1"><!-- --></div>')
					.appendTo(prompt);
			var promptTopPosition, promptleftPosition, marginTopSize;
			var fieldWidth = field.width();
			var promptHeight = prompt.height();
			promptTopPosition = field.position().top;
			promptleftPosition = field.position().left + fieldWidth - 30;
			marginTopSize = -promptHeight;
			prompt.css({
						"top" : promptTopPosition + "px",
						"left" : promptleftPosition + "px",
						"marginTop" : marginTopSize + "px",
						"opacity" : 0
					});
			prompt.animate({
						"opacity" : 0.8
					});
		} else
			Message.showActionError(msg);
	}
};

Form = {
	focus : function(form) {
		var arr = $(':input:visible', form).get();
		for (var i = 0; i < arr.length; i++) {
			if ($('.fieldError,.field-error', $(arr[i]).parent()).length) {
				setTimeout(function() {
							$(arr[i]).focus();
						}, 50);
				break;
			}
		}
	},
	validate : function(target) {
		if ($(target).prop('tagName') != 'FORM') {
			$('.fieldError,.field-error', $(target).parent()).fadeIn().remove();
			if ($(target).is(':visible') && !$(target).prop('disabled')) {
				if ($(target).hasClass('required') && !$(target).val()) {
					if ($(target).prop('tagName') == 'SELECT')
						Message.showFieldError(target, null,
								'selection.required');
					else
						Message.showFieldError(target, null, 'required');
					return false;
				} else if ($(target).hasClass('email')
						&& $(target).val()
						&& !$(target)
								.val()
								.match(/^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/)) {
					Message.showFieldError(target, null, 'email');
					return false;
				} else if ($(target).hasClass('phone') && $(target).val()
						&& !$(target).val().match(/^[\d-]+$/)) {
					Message.showFieldError(target, null, 'phone');
					return false;
				} else if ($(target).hasClass('integer') && $(target).val()) {
					if ($(target).hasClass('positive')
							&& !$(target).val().match(/^[+]?\d*$/)) {
						Message
								.showFieldError(target, null,
										'integer.positive');
						return false;
					}
					if (!$(target).hasClass('positive')
							&& !$(target).val().match(/^[-+]?\d*$/)) {
						Message.showFieldError(target, null, 'integer');
						return false;
					}
					return true;
				} else if ($(target).hasClass('double') && $(target).val()) {
					if ($(target).hasClass('positive')
							&& !$(target).val().match(/^[+]?\d+(\.\d+)?$/)) {
						Message.showFieldError(target, null, 'double.positive');
						return false;
					}
					if (!$(target).hasClass('positive')
							&& !$(target).val().match(/^[-+]?\d+(\.\d+)?$/)) {
						Message.showFieldError(target, null, 'double');
						return false;
					}
				}
				return true;
			} else {
				return true;
			}
		} else {
			var valid = true;
			$(':input', target).each(function() {
						if (!Form.validate(this))
							valid = false;
					});
			if (!valid)
				Form.focus(target);
			return valid;
		}
	}
};

Ajax = {
	defaultRepacement : 'content',
	fire : function(target, funcName) {
		if (!target)
			return true;
		var func = target[funcName];
		if (typeof func == 'undefined')
			func = $(target).attr(funcName);
		if (typeof func == 'undefined' || !func)
			return true;
		var args = [];
		if (arguments.length > 2)
			for (var i = 2; i < arguments.length; i++)
				args[i - 2] = arguments[i];
		var ret;
		if (typeof(func) == 'function') {
			ret = func.apply(target, args);
		} else {
			if (func.indexOf('return') > -1)
				func = func.replace('return', '');
			target._temp = function() {
				return eval(func)
			};
			try {
				ret = target._temp();
			} catch (e) {
				alert(e);
			}
		}
		if (false == ret)
			return false;
		return true;
	},
	handleResponse : function(data, options) {
		if (!data)
			return;
		var hasError = false;
		var target = options.target;
		if (target && $(target).parents('div.ui-dialog').length)
			options.quiet = true;
		if ((typeof data == 'string')
				&& (data.indexOf('{') == 0 || data.indexOf('[') == 0))
			data = $.parseJSON(data);
		if (typeof data == 'string') {
			if (data.indexOf('<title>') >= 0 && data.indexOf('</title>') > 0) {
				Ajax.title = data.substring(data.indexOf('<title>') + 7, data
								.indexOf('</title>'));
				if (options.replaceTitle)
					document.title = Ajax.title;
			}
			var replacement = {};
			var entries = (options.replacement
					|| $(target).attr('replacement')
					|| ($(target).prop('tagName') == 'FORM' ? $(target)
							.attr('id') : null) || Ajax.defaultRepacement)
					.split(',');
			for (var i = 0; i < entries.length; i++) {
				var entry = entries[i];
				var ss = entry.split(':', 2);
				replacement[ss[0]] = (ss.length == 2 ? ss[1] : ss[0]);
			}
			var html = data.replace(/<script(.|\s)*?\/script>/g, '');
			var div = $('<div/>').html(html);
			// others
			for (var key in replacement) {
				var r = $('#' + key);
				if (key == Ajax.defaultRepacement && !r.length)
					r = $('body');
				if (!options.quiet && r.length)
					$('html,body').animate({
								scrollTop : r.offset().top - 50
							}, 100);
				var rep = div.find('#' + replacement[key]);
				if (rep.length) {
					r.html(rep.html());
				} else {
					if (div.find('#content').length)
						r.html(div.find('#content').html());
					else if (div.find('body').length)
						r.html(div.find('body').html());
					else
						r.html(html);
				}
				if (!options.quiet && (typeof $.effects != 'undefined'))
					r.effect('highlight');
				_observe(r);
			}
			div.remove();
			Ajax.fire(target, 'onsuccess', data);
		} else {
			Ajax.jsonResult = data;
			if (data.fieldErrors || data.actionErrors) {
				hasError = true;
				if (options.onerror)
					options.onerror.apply(window);
				Ajax.fire(target, 'onerror', data);
			} else {
				if (options.onsuccess)
					options.onsuccess.apply(window);
				Ajax.fire(target, 'onsuccess', data);
			}
			Message.showActionError(data.actionErrors, target);
			Message.showActionMessage(data.actionMessages, target);

			if (data.fieldErrors) {
				if (target) {
					for (key in data.fieldErrors)
						Message.showFieldError(target[key],
								data.fieldErrors[key]);
					Form.focus(target);
				} else {
					for (key in data.fieldErrors)
						Message.showActionError(data.fieldErrors[key]);
				}
			}
		}
		if (target && target.tagName == 'FORM') {
			setTimeout(function() {
						$('button[type="submit"]', target).prop('disabled',
								false);
						Captcha.refresh()
					}, 100);
			if (!hasError && $(target).hasClass('reset')) {
				if (typeof target.reset == 'function'
						|| (typeof target.reset == 'object' && !target.reset.nodeType))
					target.reset();
			}
		}
		Indicator.text = '';
		Ajax.fire(target, 'oncomplete', data);
	},
	jsonResult : null,
	title : ''
};

function ajaxOptions(options) {
	options = options || {};
	if (!options.dataType)
		options.dataType = 'text';
	if (!options.headers)
		options.headers = {};
	$.extend(options.headers, {
				'X-Data-Type' : options.dataType
			});
	var beforeSend = options.beforeSend;
	options.beforeSend = function(xhr) {
		if (beforeSend)
			beforeSend(xhr);
		Indicator.text = options.indicator;
		Ajax.fire(null, options.onloading);
	}
	var success = options.success;
	options.success = function(data, textStatus, XMLHttpRequest) {
		Ajax.handleResponse(data, options);
		if (success && !(data.fieldErrors || data.actionErrors))
			success(data, textStatus, XMLHttpRequest);
	};
	return options;
}

function ajax(options) {
	$.ajax(ajaxOptions(options));
}

var CONTEXT_PATH = $('meta[name="context_path"]').attr('content') || '';

if (typeof(Initialization) == 'undefined')
	Initialization = {};
if (typeof(Observation) == 'undefined')
	Observation = {};
function _init() {
	var array = [];
	for (var key in Initialization) {
		if (typeof(Initialization[key]) == 'function')
			array.push(key);
	}
	array.sort();
	for (var i = 0; i < array.length; i++)
		Initialization[array[i]].call(this);
	_observe();
}
function _observe(container) {
	if (!container)
		container = document;
	$('.chart,form.ajax,.ajaxpanel', container).each(function(i) {
		if (!this.id)
			this.id = ('a' + (i + Math.random())).replace('.', '').substring(0,
					5);
	});
	var array = [];
	for (var key in Observation) {
		if (typeof(Observation[key]) == 'function')
			array.push(key);
	}
	array.sort();
	for (var i = 0; i < array.length; i++)
		Observation[array[i]].call(this, container);
}
$(_init);

Initialization.common = function() {
	$(document).ajaxStart(function() {
				Indicator.show()
			});
	$(document).ajaxError(function() {
				Indicator.showError()
			});
	$(document).ajaxSuccess(function(ev, xhr) {
				Indicator.hide();
				var url = xhr.getResponseHeader('X-Redirect-To');
				if (url) {
					var url = UrlUtils.absolutize(url);
					try {
						var href = top.location.href;
						if (href && UrlUtils.isSameDomain(href, url))
							top.location.href = url;
						else
							document.location.href = url;
					} catch (e) {
						document.location.href = url;
					}
					return;
				}
			});
	$('.removeonclick').live('click', function() {
				$(this).remove()
			});
	$.alerts.okButton = MessageBundle.get('confirm');
	$.alerts.cancelButton = MessageBundle.get('cancel');
	$('.menu li a').each(function() {
				if ($(this).attr('href') == document.location.pathname)
					$(this).closest('li').addClass('active');
			});
	$('.menu li a').click(function() {
				$('li', $(this).closest('.menu')).removeClass('active');
				$(this).closest('li').addClass('active');
			});
};

var HISTORY_ENABLED = MODERN_BROWSER
		&& (typeof history.pushState != 'undefined' || typeof $.history != 'undefined')
		&& ($('meta[name="history_enabled"]').attr('content') != 'false');
if (HISTORY_ENABLED) {
	var SESSION_HISTORY_SUPPORT = typeof history.pushState != 'undefined'
			&& document.location.hash.indexOf('#!/') != 0;
	var _historied_ = false;
	Initialization.history = function() {

		if (SESSION_HISTORY_SUPPORT) {
			var url = document.location.href;
			history.replaceState({
						url : url
					}, '', url);
			window.onpopstate = function(event) {
				var url = document.location.href;
				if (event.state) {
					ajax({
								url : url,
								replaceTitle : true,
								replacement : event.state.replacement,
								cache : false,
								success : function() {
									$('.menu li a').each(function() {
										if (this.href == url) {
											$('li', $(this).closest('.menu'))
													.removeClass('active');
											$(this).closest('li')
													.addClass('active');
										}
									});
								},
								headers : {
									'X-Fragment' : '_'
								}
							});
				}
			};
			return;
		}
		$.history.init(function(hash) {
					if ((!hash && !_historied_)
							|| (hash && hash.indexOf('!') < 0))
						return;
					var url = document.location.href;
					if (url.indexOf('#') > 0)
						url = url.substring(0, url.indexOf('#'));
					if (hash.length) {
						hash = hash.substring(1);
						if (UrlUtils.isSameDomain(hash)) {
							if (CONTEXT_PATH)
								hash = CONTEXT_PATH + hash;
						}
						url = hash;
					}
					_historied_ = true;
					ajax({
								url : url,
								cache : true,
								replaceTitle : true,
								success : function() {
									$('.menu li a').each(function() {
										if ($(this).attr('href') == url) {
											$('li', $(this).closest('.menu'))
													.removeClass('active');
											$(this).closest('li')
													.addClass('active');
										}
									});
								},
								headers : {
									'X-Fragment' : '_'
								}
							});
				}, {
					unescape : true
				});
	}
}

Observation.common = function(container) {
	$(
			'div.action-error,div.action-message,ul.action-error li,ul.action-message li',
			container).each(function() {
		var t = $(this);
		if (!$('div.close', t).length)
			t
					.prepend('<div class="close" onclick="$(this.parentNode).remove()"></div>');
	});

	$('div.field-error', container).each(function() {
				var text = $(this).text();
				var field = $(':input', $(this).parent());
				$(this).remove();
				Message.showFieldError(field, text);
			});
	var ele = ($(container).prop('tagName') == 'FORM' && $(container)
			.hasClass('focus')) ? container : $('.focus:eq(0)', container);
	if (ele.prop('tagName') != 'FORM') {
		ele.focus();
	} else {
		var arr = $(':input:visible', ele).toArray();
		for (var i = 0; i < arr.length; i++) {
			if (!$(arr[i]).val()) {
				$(arr[i]).focus();
				break;
			}
		}
	}
	$('form', container).each(function() {
				if (!$(this).hasClass('ajax'))
					$(this).submit(function() {
								$('.action-message,.action-error').remove();
								return Form.validate(this)
							});
			});
	$('input[type="text"]', container).each(function() {
				if (!$(this).attr('autocomplete'))
					$(this).attr('autocomplete', 'off');
				var maxlength = $(this).attr('maxlength');
				if (!maxlength || maxlength > 3000) {
					if ($(this).hasClass('date'))
						$(this).attr('maxlength', '10');
					else if ($(this).hasClass('integer'))
						$(this).attr('maxlength', '11');
					else if ($(this).hasClass('double'))
						$(this).attr('maxlength', '22');
					else
						$(this).attr('maxlength', '255');
				}
			});
	$('.highlightrow tbody tr', container).hover(function() {
				$(this).addClass('highlight');
			}, function() {
				$(this).removeClass('highlight');
			});
	$('.linkage', container).each(function() {
		var c = $(this);
		c.data('originalclass', c.attr('class'));
		var sw = $('.linkage_switch', c);
		$('.linkage_component', c).show();
		$('.linkage_component', c).not('.' + sw.val()).hide().filter(':input')
				.val('');
		c.attr('class', c.data('originalclass') + ' ' + sw.val());
		sw.change(function() {
					var c = $(this).closest('.linkage');
					var sw = $(this);
					$('.linkage_component', c).show();
					$('.linkage_component', c).not('.' + sw.val()).hide()
							.filter(':input').val('');
					c.attr('class', c.data('originalclass') + ' ' + sw.val());
				});
	});
	$(':input.conjunct', container).change(function() {
				var t = $(this);
				var f = $(this).closest('form');
				var hid = $(':input[type=hidden][name$=".id"]', f);
				var url = f.attr('action');
				if (url.indexOf('/') > -1)
					url = url.substring(0, url.lastIndexOf('/')) + '/input';
				else
					url = 'input';
				var data = {};
				if (hid.val())
					data['id'] = hid.val();
				data[t.attr('name')] = t.val();
				ajax({
							global : false,
							quiet : true,
							method : 'GET',
							url : url,
							data : data,
							replacement : t.attr('replacement')
						});
			});
	if (!$.browser.msie && typeof $.fn.elastic != 'undefined')
		$('textarea.elastic', container).elastic();
	if (typeof $.fn.tabs != 'undefined')
		$('div.tabs', container).each(function() {
					$(this).tabs({
								select : function(event, ui) {
									$(ui.panel).trigger('load');
								}
							}).tabs('select', $(this).data('tab'));

				});
	if (typeof $.fn.corner != 'undefined' && $.browser.msie
			&& $.browser.version <= 8)
		$('.rounded', container).each(function() {
					$(this).corner($(this).data('corner'));
				});
	if (typeof $.fn.datepicker != 'undefined')
		$('input.date', container).datepicker({
					dateFormat : 'yy-mm-dd',
					zIndex : 2000
				});
	$('input.captcha', container).focus(function() {
				if ($(this).data('_captcha_'))
					return;
				$(this).after('<img class="captcha" src="' + this.id + '"/>');
				$('img.captcha', container).click(Captcha.refresh);
				$(this).data('_captcha_', true);
			});
	if (typeof $.fn.treeTable != 'undefined')
		$('.treeTable', container).each(function() {
			$(this).treeTable({
				initialState : $(this).hasClass('expanded')
						? 'expanded'
						: 'collapsed'
			});
		});
	if (typeof $.fn.truncatable != 'undefined')
		$('.truncatable', container).each(function() {
					$(this).truncatable({
								limit : $(this).data('limit') || 100
							});
				});
	if (typeof $.fn.tipsy != 'undefined') {
		$('div.tipsy').live('mouseout', function() {
					$(this).remove();
				});
		$('.tiped,:input[title]', container).each(function() {
					var t = $(this);
					var options = {
						html : true,
						fade : true,
						gravity : t.data('gravity') || 'w'
					};
					if (!t.attr('title') && t.data('tipurl'))
						t.attr('title', MessageBundle.get('ajax.loading'));
					t.hover(function() {
								if (t.data('tipurl'))
									$.ajax({
												url : t.data('tipurl'),
												global : false,
												dataType : 'html',
												success : function(data) {
													t.attr('title', data);
													t.tipsy(true).show();
												}
											});
							}, function() {
								t.removeAttr('tipurl');
							});
					if (t.is(':input')) {
						options.trigger = 'focus';
						options.gravity = 'w';
					}
					t.tipsy(options);
				});
	}
	$('.switch', container).each(function() {
				var t = $(this);
				t.children().css('cursor', 'pointer').click(function() {
							t.children().removeClass('selected').css({
										'font-weight' : 'normal'
									});
							$(this).addClass('selected').css({
										'font-weight' : 'bold'
									});
						}).filter('.selected').css({
							'font-weight' : 'bold',
							'font-size' : '1.1em'
						});
			});
	if (typeof swfobject != 'undefined') {
		$('.chart', container).each(function() {
			var id = this.id;
			var width = $(this).width();
			var height = $(this).height();
			var data = $(this).attr('data');
			if (data.indexOf('/') == 0)
				data = document.location.protocol + '//'
						+ document.location.host + data;
			data = encodeURIComponent(data);
			if (!id || !width || !height || !data)
				alert('id,width,height,data all required');
			swfobject.embedSWF(CONTEXT_PATH
							+ '/assets/images/open-flash-chart.swf', id, width,
					height, '9.0.0', CONTEXT_PATH
							+ '/assets/images/expressInstall.swf', {
						'data-file' : data
					}, {
						wmode : 'transparent'
					});
		});
		window.save_image = function() {
			var content = [];
			content
					.push('<html><head><title>Charts: Export as Image<\/title><\/head><body>');
			$('object[data]').each(function() {
				content.push('<img src="data:image/png;base64,'
						+ this.get_img_binary() + '"/>');
			});
			content.push('<\/body><\/html>');
			var img_win = window.open('', '_blank');
			with (img_win.document) {
				write(content.join(''));
				img_win.document.close();
			}
		}
	}
	if (typeof $.fn.cycle != 'undefined')
		$('.cycle').each(function() {
			var options = {
				fx : 'fade',
				pause : 1
			};
			$.extend(options, (new Function("return "
							+ ($(this).data('cycleoptions') || '{}')))());
			$(this).cycle(options);
		});
	$('a.ajax,form.ajax', container).each(function() {
		var target = this;
		var ids = [];
		var targetId = $(target).attr('id');
		if (typeof targetId != 'string')
			targetId = '';
		var entries = ($(target).attr('replacement') || ($(target)
				.prop('tagName') == 'FORM' ? targetId : '')).split(',');
		for (var i = 0; i < entries.length; i++) {
			var entry = entries[i];
			var ss = entry.split(':', 2);
			var id = ss.length == 2 ? ss[1] : ss[0];
			if (id)
				ids.push(id);
		}
		if (this.tagName == 'FORM') {
			var options = {
				beforeSubmit : function() {
					if (!Ajax.fire(target, 'onprepare'))
						return false;
					$('.action-message,.action-error').remove();
					if (!Form.validate(target))
						return false;
					Indicator.text = $(target).data('indicator');
					$('button[type="submit"]', target).prop('disabled', true);
					Ajax.fire(target, 'onloading');
				},
				error : function() {
					Form.focus(target);
					if (target && target.tagName == 'FORM')
						setTimeout(function() {
									$('button[type="submit"]', target).prop(
											'disabled', false);
								}, 100);
					Ajax.fire(target, 'onerror');
				},
				success : function(data) {
					Ajax.handleResponse(data, {
								'target' : target
							});
				},
				headers : {}
			};
			if (!$(this).hasClass('view'))
				$.extend(options.headers, {
							'X-Data-Type' : 'json'
						});
			if (ids.length > 0)
				$.extend(options.headers, {
							'X-Fragment' : ids.join(',')
						});
			$(this).bind('submit', function() {
						$(this).ajaxSubmit(options);
						return false;
					});
			$('input', this).keyup($.debounce(500, function(ev) {
						if (!$(this).hasClass('email') && ev.keyCode != 13)
							Form.validate(this);
						return true;
					})).blur(function(ev) {
						if (this.value != this.defaultValue)
							Form.validate(this);
						return true;
					});
			$('select', this).change(function() {
						Form.validate(this);
						return true;
					});
			return;
		} else {
			$(this).click(function() {
				if (!Ajax.fire(target, 'onprepare'))
					return false;
				if (HISTORY_ENABLED
						&& $(this).hasClass('view')
						&& ($(this).hasClass('history') || !$(this)
								.attr('replacement'))) {
					var hash = this.href;
					if (UrlUtils.isSameDomain(hash)) {
						hash = hash.substring(hash.indexOf('//') + 2);
						hash = hash.substring(hash.indexOf('/'));
						if (SESSION_HISTORY_SUPPORT) {
							history.pushState({
										replacement : $(this)
												.attr('replacement'),
										url : hash
									}, '', hash);
						} else {
							if (CONTEXT_PATH)
								hash = hash.substring(CONTEXT_PATH.length);
							hash = hash.replace(/^.*#/, '');
							$.history.load('!' + hash);
							return false;
						}
					}

				}
				var options = {
					url : this.href,
					type : $(this).attr('method') || 'GET',
					cache : $(this).hasClass('cache'),
					beforeSend : function() {
						$('.action-message,.action-error').remove();
						Indicator.text = $(target).data('indicator');
						Ajax.fire(target, 'onloading');
					},
					error : function() {
						Ajax.fire(target, 'onerror');
					},
					headers : {}
				};
				var _opt = {
					'target' : target
				};
				if (!$(this).hasClass('view'))
					$.extend(options.headers, {
								'X-Data-Type' : 'json'
							});
				if (ids.length > 0) {
					$.extend(options.headers, {
								'X-Fragment' : ids.join(',')
							});
				} else {
					$.extend(options.headers, {
								'X-Fragment' : '_'
							});
					_opt.replaceTitle = true;
				}

				options.success = function(data) {
					Ajax.handleResponse(data, _opt);
				};
				$.ajax(options);
				return false;
			});
		}
	});
};

var Dialog = {
	adapt : function(d, iframe) {
		var useiframe = iframe != null;
		if (!iframe) {
			$(d).dialog('option', 'title', Ajax.title);
		} else {
			var doc = iframe.document;
			if (iframe.contentDocument) {
				doc = iframe.contentDocument;
			} else if (iframe.contentWindow) {
				doc = iframe.contentWindow.document;
			}
			$(d).dialog('option', 'title', doc.title);
			$(d).dialog('option', 'minHeight', height);
			var height = $(doc).height() + 20;
			$(iframe).height(height);
		}
		d.dialog('option', 'position', 'center');
		var height = d.height();
		if (height > 600)
			d.dialog('option', 'position', 'top');

	}
}

Captcha = {
	refresh : function() {
		$('img.captcha').each(function() {
					var src = this.src;
					var i = src.lastIndexOf('&');
					if (i > 0)
						src = src.substring(0, i);
					this.src = src + '&' + Math.random();
				});
		$('input.captcha').val('');
	}
};
ArrayUtils = {
	unique : function(arr) {
		if (arr) {
			var arr2 = [];
			var provisionalTable = {};
			for (var i = 0, item; (item = arr[i]) != null; i++) {
				if (!provisionalTable[item]) {
					arr2.push(item);
					provisionalTable[item] = true;
				}
			}
			return arr2;
		}
	}
};