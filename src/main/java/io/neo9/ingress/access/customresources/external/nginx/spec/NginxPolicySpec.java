package io.neo9.ingress.access.customresources.external.nginx.spec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NginxPolicySpec {

	private AccessControlSpec accessControl;

}
