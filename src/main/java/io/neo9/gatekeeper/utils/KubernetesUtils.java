package io.neo9.gatekeeper.utils;

import javax.annotation.Nullable;

import io.fabric8.kubernetes.api.model.HasMetadata;
import lombok.experimental.UtilityClass;

@UtilityClass
public class KubernetesUtils {

	public static String getAnnotationValue(String key, HasMetadata hasMetadata, String defaultValue) {
		return hasMetadata.getMetadata().getAnnotations().getOrDefault(key, defaultValue);
	}

	@Nullable
	public static String getAnnotationValue(String key, HasMetadata hasMetadata) {
		return hasMetadata.getMetadata().getAnnotations().get(key);
	}

}
