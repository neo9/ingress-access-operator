package io.neo9.gatekeeper.customresources.spec;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class V1VisitorGroupSpecSources {

	private String cidr;

	private String name;

}

