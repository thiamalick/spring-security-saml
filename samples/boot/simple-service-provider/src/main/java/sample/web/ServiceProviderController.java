/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package sample.web;

import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.saml.provider.config.ExternalProviderConfiguration;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.ServiceProvider;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml.util.Network;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sample.config.AppConfig;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

@Controller
public class ServiceProviderController {

	private AppConfig configuration;
	private Network network;
	private static final Log logger =LogFactory.getLog(ServiceProviderController.class);
	private SamlProviderProvisioning<ServiceProvider> provisioning;

	@Autowired
	public void setNetwork(Network network) {
		this.network = network;
	}

	@Autowired
	public void setAppConfig(AppConfig config) {
		this.configuration = config;
	}

	@Autowired
	public void setSamlService(SamlProviderProvisioning<ServiceProvider> provisioning) {
		this.provisioning = provisioning;
	}

	@RequestMapping(value = {"/", "/index", "logged-in"})
	public String home() {
		return "logged-in";
	}

	@RequestMapping("/saml/sp/select")
	public String selectProvider(HttpServletRequest request, Model model) {
		List<ModelProvider> providers = new LinkedList<>();
		configuration.getServiceProvider().getProviders().stream().forEach(
			p -> {
				try {
					ModelProvider mp = new ModelProvider()
						.setLinkText(p.getLinktext())
						.setRedirect(getDiscoveryRedirect(request, p));
					providers.add(mp);
				} catch (Exception x) {
					logger.debug(format(
						"Unable to retrieve metadata for provider:%s with message:",
						p.getMetadata(),
						x.getMessage())
					);
				}
			}
		);
		model.addAttribute("idps", providers);
		return "select-provider";
	}

	protected String getDiscoveryRedirect(HttpServletRequest request, ExternalProviderConfiguration p) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(network.getBasePath(request));
		builder.pathSegment("saml/sp/discovery");
		ServiceProvider provider = provisioning.getHostedProvider(request);
		IdentityProviderMetadata metadata = provider.getRemoteProvider(p);
		builder.queryParam("idp", UriUtils.encode(metadata.getEntityId(), UTF_8));
		return builder.build().toUriString();
	}
}
