package io.neo9.ingress.access.customresources.external.nginx.spec;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessControlSpec {

	private List<String> allow;

	private List<String> deny;

}
