<%@ page contentType="text/html; charset=utf-8" language="java"%>
<%@ taglib uri="/struts-tags" prefix="s"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="zh-CN" lang="zh-CN">
<head>
<title>ironrhino</title>
</head>
<body>
<div id="info">
<table>
	<tr>
		<td><s:property value="getText('order.code')" /></td>
		<td><s:property value="order.code" /></td>
	</tr>
	<tr>
		<td><s:property value="getText('order.orderDate')" /></td>
		<td><s:date name="order.orderDate" format="yyyy-MM-dd HH:mm:ss" /></td>
	</tr>
	<tr>
		<td><s:property value="getText('order.status')" /></td>
		<td><s:property value="order.status.displayName" /></td>
	</tr>
	<tr>
		<td><s:property value="getText('order.total')" /></td>
		<td><s:property value="order.total" /></td>
	</tr>
	<tr>
		<td><s:property value="getText('order.discount')" /></td>
		<td><s:property value="order.discount" /></td>
	</tr>
	<tr>
		<td><s:property value="getText('order.shipcost')" /></td>
		<td><s:property value="order.shipcost" /></td>
	</tr>
	<tr>
		<td><s:property value="getText('order.grandtotal')" /></td>
		<td><s:property value="order.grandtotal" /></td>
	</tr>
</table>
<table>
	<tr>
		<td><s:property value="getText('order.orderItems.productCode')" /></td>
		<td><s:property value="getText('order.orderItems.productName')" /></td>
		<td><s:property value="getText('order.orderItems.productPrice')" /></td>
		<td><s:property value="getText('order.orderItems.quantity')" /></td>
		<td><s:property value="getText('order.orderItems.subtotal')" /></td>
	</tr>
	<s:iterator value="order.items">
		<tr>
			<td><s:property value="productCode" /></td>
			<td><s:property value="productName" /></td>
			<td><s:property value="productPrice" /></td>
			<td><s:property value="quantity" /></td>
			<td><s:property value="subtotal" /></td>
		</tr>
	</s:iterator>
</table>
</div>

<div id="addressee">
<table>
	<tr>
		<td><s:property value="getText('order.addressee.name')" /></td>
		<td><s:property value="order.addressee.name" /></td>
	</tr>
	<tr>
		<td><s:property value="getText('order.addressee.address')" /></td>
		<td><s:property value="order.addressee.address" /></td>
	</tr>
	<tr>
		<td><s:property value="getText('order.addressee.zip')" /></td>
		<td><s:property value="order.addressee.zip" /></td>
	</tr>
	<tr>
		<td><s:property value="getText('order.addressee.telephone')" /></td>
		<td><s:property value="order.addressee.telephone" /></td>
	</tr>
	<tr>
		<td><s:property value="getText('order.description')" /></td>
		<td><s:property value="order.description" /></td>
	</tr>
	<s:if test="%{order.isNew()}">
		<tr>
			<td colspan="2"><a
				href="<s:url value="/account/order/addressee"/>">edit</a></td>
		</tr>
	</s:if>
</table>
</div>

<div id="operation"><s:if test="%{order.isNew()}">
	<div><a href="<s:url value="/account/order/confirm"/>">confirm</a>&nbsp;<a
		href="<s:url value="/"/>">cancel</a></div>
</s:if> <s:else>
	<s:if test="%{order.status.name=='INITIAL'}">
		<div id="payment"><s:iterator value="paymentManager.payments">
			<s:if test="!disabled">
				<div><img
					src="<s:url value="%{'/images/'+code+'.jpg'}"/>"
					alt="<s:property value="code" />" /><s:property value="name" /><s:property
					value="getPayForm([1].order)" escape="false" /></div>
			</s:if>
		</s:iterator></div>
		<div><a
			href="<s:url value="%{'/account/order/cancel/'+order.code}"/>">cancel</a></div>
	</s:if>
	<s:elseif test="%{order.status.name=='CANCELLED'}">
		<div><a
			href="<s:url value="%{'/account/order/delete/'+order.code}"/>">delete</a></div>
	</s:elseif>
</s:else></div>
</body>
</html>
