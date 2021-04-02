/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint.http;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.OperationArgumentResolver;
import org.springframework.boot.actuate.endpoint.annotation.Producible;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * An {@link OperationArgumentResolver} for {@link Producible producible enums}.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public class ProducibleOperationArgumentResolver implements OperationArgumentResolver {

	private final Map<String, List<String>> headers;

	public ProducibleOperationArgumentResolver(Map<String, List<String>> headers) {
		this.headers = headers;
	}

	@Override
	public boolean canResolve(Class<?> type) {
		return Producible.class.isAssignableFrom(type) && Enum.class.isAssignableFrom(type);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T resolve(Class<T> type) {
		return (T) resolveProducible((Class<Enum<? extends Producible<?>>>) type);
	}

	private Enum<? extends Producible<?>> resolveProducible(Class<Enum<? extends Producible<?>>> type) {
		List<String> accepts = this.headers.get("Accept");
		List<Enum<? extends Producible<?>>> values = Arrays.asList(type.getEnumConstants());
		Collections.reverse(values);
		if (CollectionUtils.isEmpty(accepts)) {
			return values.get(0);
		}
		Enum<? extends Producible<?>> result = null;
		for (String accept : accepts) {
			for (String mimeType : MimeTypeUtils.tokenize(accept)) {
				result = mostRecent(result, forType(values, MimeTypeUtils.parseMimeType(mimeType)));
			}
		}
		return result;
	}

	private static Enum<? extends Producible<?>> mostRecent(Enum<? extends Producible<?>> existing,
			Enum<? extends Producible<?>> candidate) {
		int existingOrdinal = (existing != null) ? existing.ordinal() : -1;
		int candidateOrdinal = (candidate != null) ? candidate.ordinal() : -1;
		return (candidateOrdinal > existingOrdinal) ? candidate : existing;
	}

	private static Enum<? extends Producible<?>> forType(List<Enum<? extends Producible<?>>> candidates,
			MimeType mimeType) {
		for (Enum<? extends Producible<?>> candidate : candidates) {
			if (mimeType.isCompatibleWith(((Producible<?>) candidate).getProducedMimeType())) {
				return candidate;
			}
		}
		return null;
	}

}
