package io.neo9.ingress.access.customresources.external.istio.spec;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EgressSpec {

	private List<String> hosts;

}
