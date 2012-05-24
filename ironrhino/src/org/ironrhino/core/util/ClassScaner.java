package org.ironrhino.core.util;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.SystemPropertyUtils;

public class ClassScaner {

	private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	private final List<TypeFilter> includeFilters = new LinkedList<TypeFilter>();

	private final List<TypeFilter> excludeFilters = new LinkedList<TypeFilter>();

	private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(
			this.resourcePatternResolver);

	public ClassScaner() {

	}

	@Autowired(required = false)
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils
				.getResourcePatternResolver(resourceLoader);
		this.metadataReaderFactory = new CachingMetadataReaderFactory(
				resourceLoader);
	}

	public final ResourceLoader getResourceLoader() {
		return this.resourcePatternResolver;
	}

	public void addIncludeFilter(TypeFilter includeFilter) {
		this.includeFilters.add(includeFilter);
	}

	public void addExcludeFilter(TypeFilter excludeFilter) {
		this.excludeFilters.add(0, excludeFilter);
	}

	public void resetFilters(boolean useDefaultFilters) {
		this.includeFilters.clear();
		this.excludeFilters.clear();
	}

	@SafeVarargs
	public static Set<Class<?>> scanAnnotated(String basePackage,
			Class<? extends Annotation>... annotations) {
		ClassScaner cs = new ClassScaner();
		for (Class<? extends Annotation> anno : annotations)
			cs.addIncludeFilter(new AnnotationTypeFilter(anno));
		return cs.doScan(basePackage);
	}

	public static Set<Class<?>> scanAnnotated(String[] basePackages,
			Class<? extends Annotation> annotation) {
		ClassScaner cs = new ClassScaner();
		cs.addIncludeFilter(new AnnotationTypeFilter(annotation));
		Set<Class<?>> classes = new HashSet<Class<?>>();
		for (String s : basePackages)
			classes.addAll(cs.doScan(s));
		return classes;
	}

	@SafeVarargs
	public static Set<Class<?>> scanAnnotated(String[] basePackages,
			Class<? extends Annotation>... annotations) {
		ClassScaner cs = new ClassScaner();
		for (Class<? extends Annotation> anno : annotations)
			cs.addIncludeFilter(new AnnotationTypeFilter(anno));
		Set<Class<?>> classes = new HashSet<Class<?>>();
		for (String s : basePackages)
			classes.addAll(cs.doScan(s));
		return classes;
	}

	public static Set<Class<?>> scanAssignable(String basePackage,
			Class<?>... classes) {
		ClassScaner cs = new ClassScaner();
		for (Class<?> clz : classes)
			cs.addIncludeFilter(new AssignableTypeFilter(clz));
		Set<Class<?>> set = new HashSet<Class<?>>();
		set.addAll(cs.doScan(basePackage));
		return set;
	}

	public static Set<Class<?>> scanAssignable(String[] basePackages,
			Class<?>... classes) {
		ClassScaner cs = new ClassScaner();
		for (Class<?> clz : classes)
			cs.addIncludeFilter(new AssignableTypeFilter(clz));
		Set<Class<?>> set = new HashSet<Class<?>>();
		for (String s : basePackages)
			set.addAll(cs.doScan(s));
		return set;
	}

	public static Set<Class<?>> scanAnnotatedPackage(String basePackage,
			Class<? extends Annotation> annotation) {
		ClassScaner cs = new ClassScaner();
		cs.addIncludeFilter(new AnnotationTypeFilter(annotation));
		return cs.doScan(basePackage, "/**/*/package-info.class");
	}

	@SafeVarargs
	public static Set<Class<?>> scanAnnotatedPackage(String basePackage,
			Class<? extends Annotation>... annotations) {
		ClassScaner cs = new ClassScaner();
		for (Class<? extends Annotation> anno : annotations)
			cs.addIncludeFilter(new AnnotationTypeFilter(anno));
		return cs.doScan(basePackage, "/**/*/package-info.class");
	}

	public Set<Class<?>> doScan(String basePackage) {
		return doScan(basePackage, null);
	}

	public Set<Class<?>> doScan(String basePackage, String pattern) {
		if (org.apache.commons.lang3.StringUtils.isBlank(pattern))
			pattern = "/**/*.class";
		Set<Class<?>> classes = new HashSet<Class<?>>();
		Resource resource = null;
		try {
			String searchPath = new StringBuilder(
					ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX)
					.append(org.springframework.util.ClassUtils
							.convertClassNameToResourcePath(SystemPropertyUtils
									.resolvePlaceholders(basePackage)))
					.append(pattern).toString();
			Resource[] resources = this.resourcePatternResolver
					.getResources(searchPath);
			for (int i = 0; i < resources.length; i++) {
				resource = resources[i];
				if (resource.isReadable()) {
					MetadataReader metadataReader = this.metadataReaderFactory
							.getMetadataReader(resource);
					if ((includeFilters.size() == 0 && excludeFilters.size() == 0)
							|| matches(metadataReader)) {
						try {
							classes.add(Class.forName(metadataReader
									.getClassMetadata().getClassName()));
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}

					}
				}
			}
		} catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"I/O failure during classpath scanning " + resource, ex);
		}
		return classes;
	}

	protected boolean matches(MetadataReader metadataReader) throws IOException {
		for (TypeFilter tf : this.excludeFilters) {
			try {
				return !tf.match(metadataReader, this.metadataReaderFactory);
			} catch (Exception e) {
				return false;
			}
		}
		for (TypeFilter tf : this.includeFilters) {
			try {
				return tf.match(metadataReader, this.metadataReaderFactory);
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}

	public static String[] getAppPackages() {
		Set<String> packages = new TreeSet<String>();
		for (Package p : Package.getPackages()) {
			String name = p.getName();
			if (isExcludePackage(name))
				continue;
			int index = name.indexOf('.');
			if (index < 0) {
				packages.add(name);
			} else if (name.startsWith("org.ironrhino.")) {
				packages.add(name);
			} else {
				int index2 = name.indexOf('.', index + 1);
				if (index2 > 0)
					packages.add(name.substring(0, index2));
				else
					packages.add(name.substring(0, index));
			}
		}
		return packages.toArray(new String[0]);
	}

	private static boolean isExcludePackage(String name) {
		if (name.equals("org.ironrhino.core.model"))
			return false;
		if (name.equals("net") || name.equals("com") || name.equals("org")
				|| name.equals("org.ironrhino")) {
			return true;
		}
		for (String s : excludePackages) {
			if (name.equals(s) || name.startsWith(s + '.'))
				return true;
		}
		return false;
	}

	private static String[] excludePackages = new String[] { "java", "javax",
			"com.sun", "sun", "org.w3c", "org.xml", "antlr", "com.bea",
			"com.caucho", "com.chenlb", "com.fasterxml", "com.google",
			"com.ibm", "com.jolbox", "com.mongodb", "com.mysql",
			"com.opensymphony", "com.oracle", "freemarker", "javassist",
			"jsr166y", "net.sf", "net.sourceforge", "ognl", "org.antlr",
			"org.aopalliance", "org.apache", "org.aspectj", "org.bson",
			"org.codehaus", "org.compass", "org.dom4j", "org.eclipse",
			"org.hibernate", "org.ietf", "org.jcp", "org.mvel2", "org.slf4j",
			"org.springframework", "org.ironrhino.core", "weblogic" };

}