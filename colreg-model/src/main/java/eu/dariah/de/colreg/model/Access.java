package eu.dariah.de.colreg.model;

import java.util.List;

import org.hibernate.validator.constraints.NotBlank;

public class Access {
	@NotBlank
	private String type;
	private String subtype;
	
	@NotBlank(message="{~eu.dariah.de.colreg.validation.access.uri}")
	private String uri;
	private List<String> schemeIds;
	private String description;
	private String oaiSet;
	private String metadataPrefix;
	
	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	
	public String getSubtype() { return subtype; }
	public void setSubtype(String subtype) { this.subtype = subtype; }
	
	public String getUri() { return uri; }
	public void setUri(String uri) { this.uri = uri; }
	
	public List<String> getSchemeIds() { return schemeIds; }
	public void setSchemeIds(List<String> schemeIds) { this.schemeIds = schemeIds; }
	
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	
	public String getOaiSet() { return oaiSet; }
	public void setOaiSet(String oaiSet) { this.oaiSet = oaiSet; }
	
	public String getMetadataPrefix() { return metadataPrefix; }
	public void setMetadataPrefix(String metadataPrefix) {  this.metadataPrefix = metadataPrefix; }
}
