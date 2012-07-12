<#if request.scheme!='https' && !request.servletPath?string?contains('cart')>
	<@s.action name="cart!facade" namespace="/" executeResult="true" />
<@cache key="product" timeToLive="300">
		<div><img src="<@url value="/product/${relatedProduct.code}.s.jpg"/>" alt="${relatedProduct.code}" class="product_list" /></div>
		<div><a href="<@url value="/product/view/${relatedProduct.code}"/>">${relatedProduct.name}</a></div>
		<div><a href="<@url value="/cart/add/${relatedProduct.code}"/>" class="ajax view" replacement="cart_items">放入购物车</a></div>
</@cache>
</#if>
