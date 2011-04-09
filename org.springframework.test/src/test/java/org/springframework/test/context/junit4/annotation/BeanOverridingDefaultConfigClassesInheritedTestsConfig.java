/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.test.context.junit4.annotation;

import org.springframework.beans.Employee;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ApplicationContext configuration class for
 * {@link BeanOverridingDefaultConfigClassesInheritedTests} and
 * {@link BeanOverridingExplicitConfigClassesInheritedTests}.
 * 
 * @author Sam Brannen
 * @since 3.1
 */
@Configuration
public class BeanOverridingDefaultConfigClassesInheritedTestsConfig {

	@Bean
	public Employee employee() {
		Employee employee = new Employee();
		employee.setName("Yoda");
		employee.setAge(900);
		employee.setCompany("The Force");
		return employee;
	}

}
