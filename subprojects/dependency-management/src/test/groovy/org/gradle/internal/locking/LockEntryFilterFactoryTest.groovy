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

package org.gradle.internal.locking

import spock.lang.Specification
import spock.lang.Unroll

class LockEntryFilterFactoryTest extends Specification {

    @Unroll
    def "filters #filteredValues and accept #acceptedValues for filter with #filters"() {
        when:
        def filter = LockEntryFilterFactory.forParameter(filters)

        then:
        filteredValues.each {
            assert filter.filters(it)
        }
        if (!acceptedValues.empty) {
            acceptedValues.each {
                assert !filter.filters(it)
            }
        }

        where:
        filters                 | filteredValues                    | acceptedValues
        ['org:foo,com*:bar']    | ['org:foo:2.1', 'com:bar:1.1']    | ['org:baz:1.1']
        ['org:foo']             | ['org:foo:2.1']                   | ['com:bar:1.1']
        ['org:foo,']            | ['org:foo:2.1']                   | ['com:bar:1.1'] // Simply shows a trailing comma is ignored
        ['co*:ba*']             | ['com:bar:2.1']                   | ['org:foo:1.1']
        ['*:ba*']               | ['org:bar:2.1']                   | ['com:foo:1.1']
        ['*:bar']               | ['org:bar:2.1']                   | ['com:foo:1.1']
        ['org:f*']              | ['org:foo:1.1']                   | ['com:bar:2.1']
        ['org:*']               | ['org:foo:1.1']                   | ['com:bar:2.1']
        ['or*:*']               | ['org:foo:1.1']                   | ['com:bar:2.1']
        ['*:*']                 | ['org:foo:1.1', 'com:bar:2.1']    | []
    }

    @Unroll
    def "fails for invalid filter #filters"() {
        when:
        LockEntryFilterFactory.forParameter(filters)

        then:
        thrown(IllegalArgumentException)

        where:
        filters << [['*org:foo'], ['org:*foo'], ['org'], [',org:foo'], [',']]
    }
}
