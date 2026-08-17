package io.neo9.ingress.access.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Override JOSDK executor bean so Spring does not infer destroyMethod=close.
 * ExecutorService on Java 17 has shutdown()/shutdownNow(), not close() (Java 19+).
 * AOT/native bean definitions were failing startup with
 * BeanDefinitionValidationException.
 */
@Configuration
@ImportRuntimeHints(NativeImageHints.class)
public class OperatorRuntimeConfig {

	@Bean(name = "reconciliationExecutorService", destroyMethod = "shutdown")
	public ExecutorService reconciliationExecutorService(
			@Value("${javaoperatorsdk.concurrent-reconciliation-threads:10}") int threads) {
		return Executors.newFixedThreadPool(Math.max(1, threads));
	}

}
