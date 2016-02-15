package eu.dariah.de.colreg.controller.api;

import java.io.StringReader;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import eu.dariah.de.colreg.model.Access;
import eu.dariah.de.colreg.model.Collection;
import eu.dariah.de.colreg.model.LocalizedDescription;
import eu.dariah.de.colreg.model.PersistedUserDetails;
import eu.dariah.de.colreg.model.api.repository.RepositoryDraft;
import eu.dariah.de.colreg.model.api.repository.RepositoryDraftContainer;
import eu.dariah.de.colreg.model.api.repository.RepositoryResponse;
import eu.dariah.de.colreg.model.validation.CollectionValidator;
import eu.dariah.de.colreg.service.CollectionService;
import eu.dariah.de.colreg.service.PersistedUserDetailsService;
import eu.dariah.de.colreg.service.VocabularyService;

@Controller
@RequestMapping("/colreg/collection")
public class CrLegacyApiController {
	
	protected Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired private Jaxb2Marshaller jaxb2Marshaller;

	@Autowired private PersistedUserDetailsService userDetailsService;
	@Autowired private CollectionValidator collectionValidator;
	@Autowired private CollectionService collectionService;
	@Autowired private VocabularyService vocabularyService;
	
	@RequestMapping(value={"/submitDraft", "/submitDraft/"}, method = RequestMethod.POST, produces="application/xml")
	public @ResponseBody RepositoryResponse postRepositoryCollection(@RequestBody String xml, HttpServletResponse response, HttpServletRequest request) {
		RepositoryResponse resp = new RepositoryResponse();
		resp.setStatus("Error");
		try {
			logger.debug("Received draft from repository: " + xml);
			
			if (xml==null || xml.trim().isEmpty()) {
				resp.setError("No content provided in request body.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
			
			RepositoryDraftContainer container = null;
			try {
				Source source = new StreamSource(new StringReader(xml));
				container = (RepositoryDraftContainer)jaxb2Marshaller.unmarshal(source);
			} catch (Exception e) {
				resp.setError("Failed to parse provided payload: " + e.getMessage());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
			
			if (container!=null) {
				RepositoryDraft draft = container.getRepositoryDraft();
				if (draft==null) {
					resp.setError("No content provided in request body.");
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					return resp;
				}
				Collection c = new Collection();
				
				// Titles, descriptions -> localized descriptions
				if (draft.getTitles()!=null) {
					c.setLocalizedDescriptions(new ArrayList<LocalizedDescription>());
					LocalizedDescription desc;
					for (int i=0; i<draft.getTitles().size(); i++) {
						desc = new LocalizedDescription();
						desc.setLanguageId("und");
						desc.setTitle(draft.getTitles().get(i));
						if (draft.getDescriptions()!=null && draft.getDescriptions().size()>i) {
							desc.setDescription(draft.getDescriptions().get(i));
						}
						c.getLocalizedDescriptions().add(desc);
					}
				}
				
				Access a = new Access();
				a.setSchemeIds(new ArrayList<String>());
				a.getSchemeIds().add("oai_dc");
				a.setType(vocabularyService.findAccessTypeByIdentfier("oaipmh").getId());
				a.setUri(this.getOaiUrl(draft));
				a.setOaiSet(this.getOaiSet(draft));
				
				c.setAccessMethods(new ArrayList<Access>(1));
				c.getAccessMethods().add(a);
				
				// Identifiers -> Identifiers, Access
				c.setProvidedIdentifier(draft.getIdentifiers());
				
				
				c.setCollectionDescriptionRights("CC0");
				c.setAccessRights("?");
				c.setCollectionType("DARIAH-DE Repository");
				c.setWebPage(draft.getAboutAttribute()!=null ? draft.getAboutAttribute() : a.getUri());
				
				// Creator and draft user
				String username = null;
				String endpoint = "https://ldap-dariah.esc.rzg.mpg.de/idp/shibboleth";
				if (draft.getContributors()!=null) {
					for (String contr : draft.getContributors()) {
						if (contr.toLowerCase().endsWith("@dariah.eu")) {
							username = contr.toLowerCase();
							break;
						}
					}
				}
				PersistedUserDetails ud = userDetailsService.loadUserByUsername(endpoint, username);
				if (ud==null) {
					ud = new PersistedUserDetails();
					ud.setEndpointId(endpoint);
					ud.setEndpointName(endpoint);
					ud.setUsername(username);
					userDetailsService.saveUser(ud);
				}
				c.setDraftUserId(ud.getId());

				DataBinder binder = new DataBinder(c);
				collectionValidator.validate(c, binder.getBindingResult());
				if (binder.getBindingResult().hasErrors()) {
					resp.setError("Validation errors occurred: " + binder.getBindingResult().toString());
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					return resp;
				}
				
				collectionService.save(c, ud.getId());
				
				String url = ServletUriComponentsBuilder.fromCurrentContextPath().path("/drafts/" + c.getEntityId()).build().toUriString();
				
				resp.setStatus("OK");
				resp.setUrl(url);
				response.setStatus(HttpServletResponse.SC_OK);
			}
		} catch (Exception e) {
			resp.setStatus("Error");
			resp.setError("An unhandled exception occurred. Check server log.");
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			logger.error("Failed to consume draft from repository", e);
		}
		return resp;
	}
	
	private String getOaiUrl(RepositoryDraft draft) {
		if (draft==null || draft.getIdentifiers()==null || draft.getIdentifiers().size()==0) {
			return null;
		}
		for (String id : draft.getIdentifiers()) {
			if (id.startsWith("http") && id.contains("oai")) {
				return id.substring(0, id.lastIndexOf("/")+1);
			}
		}
		return null;
	}
	
	private String getOaiSet(RepositoryDraft draft) {
		if (draft==null || draft.getIdentifiers()==null || draft.getIdentifiers().size()==0) {
			return null;
		}
		for (String id : draft.getIdentifiers()) {
			if (id.startsWith("http") && id.contains("oai")) {
				return id.substring(id.lastIndexOf("/")+1);
			}
		}
		return null;
	}
}