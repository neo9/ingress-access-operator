package io.neo9.gatekeeper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KubernetesGateKeeperApplication {

	public static void main(String[] args) {
		SpringApplication.run(KubernetesGateKeeperApplication.class, args);
	}

}
