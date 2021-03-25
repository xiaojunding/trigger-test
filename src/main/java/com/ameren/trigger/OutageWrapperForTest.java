package com.ameren.trigger;

import com.ameren.outage.core.model.Outage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OutageWrapperForTest {
	@JsonProperty(value = "data")
	private Outage outage;

	public Outage getOutage() {
		return outage;
	}

	public void setOutage(Outage outage) {
		this.outage = outage;
	}
	
}