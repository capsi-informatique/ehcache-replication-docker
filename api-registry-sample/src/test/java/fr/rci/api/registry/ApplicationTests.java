package fr.rci.api.registry;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
@Disabled
public class ApplicationTests {
	@Autowired
	MockMvc mockMvc;

	@Test
	@Order(0)
	public void contextLoad() throws Exception {
	}

	@Test
	@Order(1)
	public void list() throws Exception {
		String response = this.mockMvc.perform(get("/api/services"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString() ;
		System.out.println(response);
		Assertions.assertTrue(response.contains("["));
	}

	
}
