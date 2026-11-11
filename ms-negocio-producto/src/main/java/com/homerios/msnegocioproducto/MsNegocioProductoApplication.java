package com.homerios.msnegocioproducto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.context.config.ContextRefreshedWithApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MsNegocioProductoApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(MsNegocioProductoApplication.class);
		ConfigurableApplicationContext context = application.run(args);
		context.publishEvent(new ContextRefreshedWithApplicationEvent(application, args, context));
	}

}
