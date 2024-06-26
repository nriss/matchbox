package ch.ahdis.matchbox.terminology;

import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.util.StopWatch;
import ch.ahdis.fhir.hapi.jpa.validation.ValidationProvider;
import ch.ahdis.matchbox.CliContext;
import ch.ahdis.matchbox.MatchboxEngineSupport;
import ch.ahdis.matchbox.StructureDefinitionResourceProvider;
import ch.ahdis.matchbox.engine.MatchboxEngine;
import ch.ahdis.matchbox.engine.cli.VersionUtil;
import ch.ahdis.matchbox.engine.exception.MatchboxEngineCreationException;
import ch.ahdis.matchbox.gazelle.models.metadata.Interface;
import ch.ahdis.matchbox.gazelle.models.metadata.RestBinding;
import ch.ahdis.matchbox.gazelle.models.metadata.Service;
import ch.ahdis.matchbox.gazelle.models.validation.*;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The WebService for the registry API
 *
 * @author Oliver Egger
 **/
@RestController
@RequestMapping(path = "/tx-reg")
public class RegistryWs {
	private static final Logger log = LoggerFactory.getLogger(RegistryWs.class);

	/**
	 * HTTP paths.
	 */
	private static final String RESOLVE_PATH = "/resolve";

	// The base CLI context, with the default parameters
	private final CliContext baseCliContext;

	public RegistryWs(final CliContext baseCliContext) {
		this.baseCliContext = Objects.requireNonNull(baseCliContext);
	}

	/**
	 * Returns the metadata of the validation service.
	 */
	@GetMapping(path = RESOLVE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public RegistryResponse resolve(final HttpServletRequest request) {
		final var registryResponse = new RegistryResponse();
		registryResponse.setFormatVersion("1");
		registryResponse.setRegistryUrl("https://raw.githubusercontent.com/FHIR/ig-registry/master/tx-servers.json");

		Candidate candidate = new Candidate("Matchbox Internal FHIR Terminology Server", this.baseCliContext.getTxServer(),
				true);
		registryResponse.setCandidates(List.of(candidate));
		return registryResponse;
	}

}