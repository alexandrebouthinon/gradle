/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.plugin.devel.plugins;

import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.model.Model;
import org.gradle.model.RuleSource;
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension;

class JavaGradlePluginPluginRules extends RuleSource {
    @Model
    public GradlePluginDevelopmentExtension gradlePluginDevelopmentExtension(ExtensionContainer extensionContainer) {
        return extensionContainer.getByType(GradlePluginDevelopmentExtension.class);
    }
}
