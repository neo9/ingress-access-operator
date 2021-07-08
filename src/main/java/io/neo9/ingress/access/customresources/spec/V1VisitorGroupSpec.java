package io.neo9.ingress.access.customresources.spec;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class V1VisitorGroupSpec {

	private List<V1VisitorGroupSpecSources> sources;

}

