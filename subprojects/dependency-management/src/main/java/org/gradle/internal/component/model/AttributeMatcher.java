/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.internal.component.model;

import org.gradle.api.Nullable;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.HasAttributes;
import org.gradle.api.internal.attributes.AttributeContainerInternal;

import java.util.Collection;
import java.util.List;

public interface AttributeMatcher {
    boolean isMatching(AttributeContainer candidate, AttributeContainer requested);

    <T> boolean isMatching(Attribute<T> attribute, T candidate, T requested);

    <T extends HasAttributes> List<T> matches(Collection<? extends T> candidates, AttributeContainerInternal requested);

    <T extends HasAttributes> List<T> matches(Collection<? extends T> candidates, AttributeContainerInternal requested, @Nullable T fallback);
}