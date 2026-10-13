/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.samples.petclinic.customers.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Tags every response with the running application version and pod name.
 * <p>
 * This is particularly useful when multiple versions of this service are
 * deployed side by side behind the same Kubernetes {@code Service} (e.g. for
 * canary / A-B testing): callers can tell exactly which version handled a
 * given request, which otherwise would be opaque behind load-balanced,
 * identical-looking responses.
 * <p>
 * The version is controlled by the {@code APP_VERSION} environment variable
 * (bound to the {@code app.version} property), so two deployments of the
 * exact same image can self-report different version numbers.
 */
@Component
public class VersionHeaderFilter extends OncePerRequestFilter {

    @Value("${app.version:unspecified}")
    private String appVersion;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        response.setHeader("X-App-Version", appVersion);
        response.setHeader("X-Pod-Name", System.getenv().getOrDefault("HOSTNAME", "unknown"));
        filterChain.doFilter(request, response);
    }
}
