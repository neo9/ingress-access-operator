package io.neo9.ingress.access.utils.common;

import jakarta.annotation.Nullable;
import java.util.Map;

import io.fabric8.kubernetes.api.model.HasMetadata;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import static io.neo9.ingress.access.config.MutationLabels.MANAGED_BY_OPERATOR_KEY;
import static io.neo9.ingress.access.config.MutationLabels.MANAGED_BY_OPERATOR_VALUE;
import static org.springframework.util.StringUtils.uncapitalize;

@UtilityClass
public class KubernetesUtils {

	public static String getAnnotationValue(HasMetadata hasMetadata, String key, String defaultValue) {
		Map<String, String> annotations = hasMetadata.getMetadata().getAnnotations();
		if (annotations == null) {
			return defaultValue;
		}
		return annotations.getOrDefault(key, defaultValue);
	}

	@Nullable
	public static String getAnnotationValue(HasMetadata hasMetadata, String key) {
		Map<String, String> annotations = hasMetadata.getMetadata().getAnnotations();
		if (annotations == null) {
			return null;
		}
		return annotations.get(key);
	}

	@Nullable
	public static String getLabelValue(HasMetadata hasMetadata, String key) {
		Map<String, String> labels = hasMetadata.getMetadata().getLabels();
		if (labels == null) {
			return null;
		}
		return labels.get(key);
	}

	public static String getResourceNamespaceAndName(HasMetadata hasMetadata) {
		String namespace = hasMetadata.getMetadata().getNamespace();
		String name = hasMetadata.getMetadata().getName();
		return String.format("%s/%s/%s", uncapitalize(hasMetadata.getKind()), namespace, name);
	}

	public static boolean isManagedByOperator(HasMetadata hasMetadata) {
		return MANAGED_BY_OPERATOR_VALUE.equals(getLabelValue(hasMetadata, MANAGED_BY_OPERATOR_KEY));
	}

	public static boolean hasLabel(HasMetadata hasMetadata, String key, String value) {
		return value.equalsIgnoreCase(getLabelValue(hasMetadata, key));
	}

	public static boolean hasLabel(HasMetadata hasMetadata, String key) {
		return StringUtils.isNotBlank(getLabelValue(hasMetadata, key));
	}

	public static boolean hasAnnotation(HasMetadata hasMetadata, String key, String value) {
		return value.equalsIgnoreCase(getAnnotationValue(hasMetadata, key));
	}

	public static boolean hasAnnotation(HasMetadata hasMetadata, String key) {
		return StringUtils.isNotBlank(getAnnotationValue(hasMetadata, key));
	}

}
