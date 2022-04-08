package io.neo9.ingress.access.customresources.spec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class V1VisitorGroupSpecSources {

	private String cidr;

	private String name;

}
