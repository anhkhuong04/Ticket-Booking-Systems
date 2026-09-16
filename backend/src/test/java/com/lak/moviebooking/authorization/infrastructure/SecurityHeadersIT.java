package com.lak.moviebooking.authorization.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lak.moviebooking.support.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SecurityHeadersIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void sendsBrowserHardeningHeadersForApiResponses() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Security-Policy",
						"default-src 'self'; frame-ancestors 'none'; base-uri 'self'; object-src 'none'"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("Referrer-Policy", "no-referrer"));
	}
}
