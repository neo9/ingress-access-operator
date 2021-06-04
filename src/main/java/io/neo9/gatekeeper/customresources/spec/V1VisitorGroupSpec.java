package io.neo9.gatekeeper.customresources.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class V1VisitorGroupSpec {

	private List<V1VisitorGroupSpecSources> sources;

}

