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

package org.gradle.api.internal.artifacts.repositories.resolver

import org.gradle.api.Action
import org.gradle.api.attributes.Attribute
import org.gradle.api.internal.artifacts.DefaultImmutableModuleIdentifierFactory
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser.GradlePomModuleDescriptorBuilder
import org.gradle.api.internal.artifacts.repositories.metadata.IvyMutableModuleMetadataFactory
import org.gradle.api.internal.artifacts.repositories.metadata.MavenMutableModuleMetadataFactory
import org.gradle.api.internal.attributes.DefaultAttributesSchema
import org.gradle.api.internal.attributes.ImmutableAttributes
import org.gradle.api.internal.notations.DependencyMetadataNotationParser
import org.gradle.internal.component.external.descriptor.Configuration
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.internal.component.external.model.MutableMavenModuleResolveMetadata
import org.gradle.internal.component.external.model.MutableModuleComponentResolveMetadata
import org.gradle.internal.component.model.ComponentAttributeMatcher
import org.gradle.internal.component.model.LocalComponentDependencyMetadata
import org.gradle.internal.reflect.DirectInstantiator
import org.gradle.util.TestUtil
import spock.lang.Specification

import static org.gradle.internal.component.external.model.DefaultModuleComponentSelector.newSelector

class ComponentMetadataDetailsAdapterTest extends Specification {
    private instantiator = DirectInstantiator.INSTANCE
    private dependencyMetadataNotationParser = DependencyMetadataNotationParser.parser(instantiator, DirectDependencyMetadataImpl.class)
    private dependencyConstraintMetadataNotationParser = DependencyMetadataNotationParser.parser(instantiator, DependencyConstraintMetadataImpl.class)

    def versionIdentifier = new DefaultModuleVersionIdentifier("org.test", "producer", "1.0")
    def componentIdentifier = DefaultModuleComponentIdentifier.newId(versionIdentifier)
    def testAttribute = Attribute.of("someAttribute", String)
    def attributes = TestUtil.attributesFactory().of(testAttribute, "someValue")
    def schema = new DefaultAttributesSchema(new ComponentAttributeMatcher(), TestUtil.instantiatorFactory())
    def ivyMetadataFactory = new IvyMutableModuleMetadataFactory(new DefaultImmutableModuleIdentifierFactory(), TestUtil.attributesFactory())
    def mavenMetadataFactory = new MavenMutableModuleMetadataFactory(new DefaultImmutableModuleIdentifierFactory(), TestUtil.attributesFactory(), TestUtil.objectInstantiator(), TestUtil.featurePreviews())

    def gradleMetadata
    def adapterOnMavenMetadata = new ComponentMetadataDetailsAdapter(mavenComponentMetadata(), instantiator, dependencyMetadataNotationParser, dependencyConstraintMetadataNotationParser)
    def adapterOnIvyMetadata = new ComponentMetadataDetailsAdapter(ivyComponentMetadata(), instantiator, dependencyMetadataNotationParser, dependencyConstraintMetadataNotationParser)
    def adapterOnGradleMetadata = new ComponentMetadataDetailsAdapter(gradleComponentMetadata(), instantiator, dependencyMetadataNotationParser, dependencyConstraintMetadataNotationParser)

    private ivyComponentMetadata() {
        ivyMetadataFactory.create(componentIdentifier, [], [new Configuration("configurationDefinedInIvyMetadata", true, true, [])], [], [])
    }
    private gradleComponentMetadata() {
        def metadata = mavenMetadataFactory.create(componentIdentifier)
        metadata.addVariant("variantDefinedInGradleMetadata1", attributes) //gradle metadata is distinguished from maven POM metadata by explicitly defining variants
        metadata.addVariant("variantDefinedInGradleMetadata2", TestUtil.attributesFactory().of(testAttribute, "other")) //gradle metadata is distinguished from maven POM metadata by explicitly defining variants
        gradleMetadata = metadata
        metadata
    }

    private MutableMavenModuleResolveMetadata mavenComponentMetadata() {
        mavenMetadataFactory.create(componentIdentifier)
    }

    def setup() {
        schema.attribute(testAttribute)
    }

    def "sees variants defined in Gradle metadata"() {
        given:
        def rule = Mock(Action)

        when:
        adapterOnGradleMetadata.withVariant("variantDefinedInGradleMetadata1", rule)

        then:
        noExceptionThrown()
        1 * rule.execute(_)
    }

    def "can execute rule on all variants"() {
        given:
        def adapterRule = Mock(Action)
        def dependenciesRule = Mock(Action)
        def constraintsRule = Mock(Action)
        def attributesRule = Mock(Action)
        when:
        adapterOnGradleMetadata.allVariants(adapterRule)

        then: "the adapter rule is called once"
        noExceptionThrown()
        1 * adapterRule.execute(_) >> {
            def adapter = it[0]
            adapter.withDependencies(dependenciesRule)
            adapter.withDependencyConstraints(constraintsRule)
            adapter.attributes(attributesRule)
        }
        0 * _

        when:
        resolve(gradleMetadata)

        then: "attributes are used during matching, the rule is applied on all variants"
        2 * attributesRule.execute(_)

        and: " we only apply the dependencies rule to the selected variant"
        1 * dependenciesRule.execute(_)
        1 * constraintsRule.execute(_)
        0 * _
    }

    def "treats ivy configurations as variants"() {
        given:
        def rule = Mock(Action)

        when:
        adapterOnIvyMetadata.withVariant("configurationDefinedInIvyMetadata", rule)

        then:
        noExceptionThrown()
        1 * rule.execute(_)
    }

    def "treats maven scopes as variants"() {
        given:
        //historically, we defined default MAVEN2_CONFIGURATIONS which eventually should become MAVEN2_VARIANTS
        def mavenVariants = GradlePomModuleDescriptorBuilder.MAVEN2_CONFIGURATIONS.keySet()
        def variantCount = mavenVariants.size()
        def rule = Mock(Action)

        when:
        mavenVariants.each {
            adapterOnMavenMetadata.withVariant(it, rule)
        }

        then:
        noExceptionThrown()
        variantCount * rule.execute(_)
    }

    void resolve(MutableModuleComponentResolveMetadata component) {
        def immutable = component.asImmutable()
        def componentIdentifier = DefaultModuleComponentIdentifier.newId("org.test", "consumer", "1.0")
        def consumerIdentifier = DefaultModuleVersionIdentifier.newId(componentIdentifier)
        def componentSelector = newSelector(consumerIdentifier.group, consumerIdentifier.name, new DefaultMutableVersionConstraint(consumerIdentifier.version))
        def consumer = new LocalComponentDependencyMetadata(componentIdentifier, componentSelector, "default", attributes, ImmutableAttributes.EMPTY, null, [] as List, [], false, false, true, false, null)

        def configuration = consumer.selectConfigurations(attributes, immutable, schema)[0]
        configuration.dependencies
    }
}
