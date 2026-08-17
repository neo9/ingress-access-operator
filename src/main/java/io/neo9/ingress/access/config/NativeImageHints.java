package io.neo9.ingress.access.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * GraalVM / Spring AOT hints for Fabric8, JOSDK, and Kubernetes model types that are
 * loaded or instantiated via reflection at runtime (native image).
 */
public class NativeImageHints implements RuntimeHintsRegistrar {

	private static final MemberCategory[] REFLECT_ALL = { MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
			MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS,
			MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.DECLARED_FIELDS, MemberCategory.PUBLIC_FIELDS };

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		registerFabric8Client(hints);
		registerFabric8Networking(hints);
		registerJosdk(hints);

		hints.resources().registerPattern("META-INF/services/io.fabric8.kubernetes.client.http.HttpClient$Factory");
		hints.resources().registerPattern("META-INF/services/io.fabric8.kubernetes.client.extension.ExtensionAdapter");
	}

	private static void registerFabric8Client(RuntimeHints hints) {
		registerType(hints, "io.fabric8.kubernetes.client.impl.KubernetesClientImpl");
		registerType(hints, "io.fabric8.kubernetes.client.impl.BaseClient");
		registerType(hints, "io.fabric8.kubernetes.client.Config");
		registerType(hints, "io.fabric8.kubernetes.client.ConfigBuilder");
		registerType(hints, "io.fabric8.kubernetes.client.utils.KubernetesSerialization");
		registerType(hints, "io.fabric8.kubernetes.client.okhttp.OkHttpClientFactory");
		registerType(hints, "io.fabric8.kubernetes.client.okhttp.OkHttpClientImpl");
		registerType(hints, "io.fabric8.kubernetes.client.okhttp.OkHttpClientBuilderImpl");
	}

	private static void registerFabric8Networking(RuntimeHints hints) {
		// Watch events deserialize Ingress.status.loadBalancer; missing from generated
		// reflect-config.
		registerType(hints, "io.fabric8.kubernetes.api.model.networking.v1.IngressLoadBalancerStatus");
		registerType(hints, "io.fabric8.kubernetes.api.model.networking.v1.IngressLoadBalancerIngress");
		registerType(hints, "io.fabric8.kubernetes.api.model.LoadBalancerIngress");
		registerType(hints, "io.fabric8.kubernetes.api.model.networking.v1.IngressStatus");
	}

	private static void registerJosdk(RuntimeHints hints) {
		// JOSDK instantiates Retry / RateLimiter from @ControllerConfiguration via
		// reflection.
		registerType(hints, "io.javaoperatorsdk.operator.processing.retry.GenericRetry");
		registerType(hints, "io.javaoperatorsdk.operator.processing.retry.GenericRetryExecution");
		registerType(hints, "io.javaoperatorsdk.operator.processing.event.rate.LinearRateLimiter");
	}

	private static void registerType(RuntimeHints hints, String typeName) {
		hints.reflection().registerType(TypeReference.of(typeName), REFLECT_ALL);
	}

}
