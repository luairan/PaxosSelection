package com.luairan;

import com.luairan.control.HelloControl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = HelloControl.class)
@WebAppConfiguration
public class SuperannaApplicationTests {

	@Test
	public void contextLoads() {
	}

}
