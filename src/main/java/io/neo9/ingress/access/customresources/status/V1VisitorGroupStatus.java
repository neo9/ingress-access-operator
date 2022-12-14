package io.neo9.ingress.access.customresources.status;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Data;

@Data
public class V1VisitorGroupStatus extends ObservedGenerationAwareStatus {

  private String errorMessage;

}
