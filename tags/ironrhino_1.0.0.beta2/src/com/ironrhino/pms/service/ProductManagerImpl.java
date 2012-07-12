package com.ironrhino.pms.service;

import javax.inject.Named;
import javax.inject.Singleton;

import org.ironrhino.core.metadata.FlushCache;
import org.ironrhino.core.service.BaseManagerImpl;
import org.springframework.transaction.annotation.Transactional;

import com.ironrhino.pms.model.Product;

@Singleton
public class ProductManagerImpl extends BaseManagerImpl<Product> implements
		ProductManager {

	@Override
	@Transactional
	@FlushCache(key = "${args[0].code}", namespace = "product")
	public void save(Product product) {
		super.save(product);
	}

	@Override
	@Transactional
	@FlushCache(key = "${args[0].code}", namespace = "product")
	public void delete(Product product) {
		super.delete(product);
	}

}