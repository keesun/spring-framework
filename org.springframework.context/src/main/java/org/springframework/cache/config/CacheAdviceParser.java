/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.config;

import java.util.List;

import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheUpdateOperation;
import org.springframework.cache.interceptor.NameMatchCacheOperationSource;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser
 * BeanDefinitionParser} for the {@code <tx:advice/>} tag.
 * 
 * @author Costin Leau
 *
 */
class CacheAdviceParser extends AbstractSingleBeanDefinitionParser {

	/**
	 * Simple, reusable class used for overriding defaults.
	 * 
	 * @author Costin Leau
	 */
	private static class Props {

		private String key, condition;
		private String[] caches = null;

		Props(Element root) {
			String defaultCache = root.getAttribute("cache");
			key = root.getAttribute("key");
			condition = root.getAttribute("condition");

			if (StringUtils.hasText(defaultCache)) {
				caches = StringUtils.commaDelimitedListToStringArray(defaultCache.trim());
			}
		}

		CacheOperation merge(Element element, ReaderContext readerCtx, CacheOperation op) {
			String cache = element.getAttribute("cache");
			String k = element.getAttribute("key");
			String c = element.getAttribute("condition");

			String[] localCaches = caches;
			String localKey = key, localCondition = condition;

			// sanity check
			if (StringUtils.hasText(cache)) {
				localCaches = StringUtils.commaDelimitedListToStringArray(cache.trim());
			} else {
				if (caches == null) {
					readerCtx.error("No cache specified specified for " + element.getNodeName(), element);
				}
			}

			if (StringUtils.hasText(k)) {
				localKey = k.trim();
			}

			if (StringUtils.hasText(c)) {
				localCondition = c.trim();
			}
			op.setCacheNames(localCaches);
			op.setKey(localKey);
			op.setCondition(localCondition);

			return op;
		}
	}

	private static final String CACHEABLE_ELEMENT = "cacheable";
	private static final String CACHE_EVICT_ELEMENT = "cache-evict";
	private static final String METHOD_ATTRIBUTE = "method";
	private static final String DEFS_ELEMENT = "definitions";

	@Override
	protected Class<?> getBeanClass(Element element) {
		return CacheInterceptor.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.addPropertyReference("cacheManager", CacheNamespaceHandler.extractCacheManager(element));
		CacheNamespaceHandler.parseKeyGenerator(element, builder.getBeanDefinition());

		List<Element> cacheDefs = DomUtils.getChildElementsByTagName(element, DEFS_ELEMENT);
		if (cacheDefs.size() >= 1) {
			// Using attributes source.
			List<RootBeanDefinition> attributeSourceDefinitions = parseDefinitionsSources(cacheDefs, parserContext);
			builder.addPropertyValue("cacheOperationSources", attributeSourceDefinitions);
		} else {
			// Assume annotations source.
			builder.addPropertyValue("cacheOperationSources", new RootBeanDefinition(
					AnnotationCacheOperationSource.class));
		}
	}

	private List<RootBeanDefinition> parseDefinitionsSources(List<Element> definitions, ParserContext parserContext) {
		ManagedList<RootBeanDefinition> defs = new ManagedList<RootBeanDefinition>(definitions.size());

		// extract default param for the definition
		for (Element element : definitions) {
			defs.add(parseDefinitionSource(element, parserContext));
		}

		return defs;
	}

	private RootBeanDefinition parseDefinitionSource(Element definition, ParserContext parserContext) {
		Props prop = new Props(definition);
		// add cacheable first

		ManagedMap<TypedStringValue, CacheOperation> cacheOpeMap = new ManagedMap<TypedStringValue, CacheOperation>();
		cacheOpeMap.setSource(parserContext.extractSource(definition));

		List<Element> updateCacheMethods = DomUtils.getChildElementsByTagName(definition, CACHEABLE_ELEMENT);

		for (Element opElement : updateCacheMethods) {
			String name = opElement.getAttribute(METHOD_ATTRIBUTE);
			TypedStringValue nameHolder = new TypedStringValue(name);
			nameHolder.setSource(parserContext.extractSource(opElement));
			CacheOperation op = prop.merge(opElement, parserContext.getReaderContext(), new CacheUpdateOperation());

			cacheOpeMap.put(nameHolder, op);
		}

		List<Element> evictCacheMethods = DomUtils.getChildElementsByTagName(definition, CACHE_EVICT_ELEMENT);

		for (Element opElement : evictCacheMethods) {
			String name = opElement.getAttribute(METHOD_ATTRIBUTE);
			TypedStringValue nameHolder = new TypedStringValue(name);
			nameHolder.setSource(parserContext.extractSource(opElement));
			CacheOperation op = prop.merge(opElement, parserContext.getReaderContext(), new CacheEvictOperation());

			cacheOpeMap.put(nameHolder, op);
		}

		RootBeanDefinition attributeSourceDefinition = new RootBeanDefinition(NameMatchCacheOperationSource.class);
		attributeSourceDefinition.setSource(parserContext.extractSource(definition));
		attributeSourceDefinition.getPropertyValues().add("nameMap", cacheOpeMap);
		return attributeSourceDefinition;
	}
}