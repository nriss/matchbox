package ch.ahdis.matchbox.engine.tests;

/*
 * #%L
 * Matchbox Engine
 * %%
 * Copyright (C) 2022 ahdis
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import ch.ahdis.matchbox.engine.CdaMappingEngine;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureMap;
import org.hl7.fhir.r5.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.r5.utils.EOperationOutcome;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

class CdaToFhirTransformTests {

	static private CdaMappingEngine engine;
	static String cdaLabItaly;
    

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CdaToFhirTransformTests.class);

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		InputStream in = getResourceAsStream("cda-it.xml");
		cdaLabItaly = IOUtils.toString(in, StandardCharsets.UTF_8);
	}

	private CdaMappingEngine getEngine() {
		if (engine == null) {
			try {
				engine = new CdaMappingEngine.CdaMappingEngineBuilder().getEngine();
                StructureMap sm = engine.parseMap(getFileAsStringFromResources("datatypes.map"));
                assertTrue(sm != null);
                engine.addCanonicalResource(sm);
                sm = engine.parseMap(getFileAsStringFromResources("FullHeader.map"));
                assertTrue(sm != null);
                engine.addCanonicalResource(sm);
                sm = engine.parseMap(getFileAsStringFromResources("LabBody.map"));
                assertTrue(sm != null);
                engine.addCanonicalResource(sm);
                sm = getEngine().parseMap(getFileAsStringFromResources("cda-it-observation.map"));
                assertTrue(sm != null);
                engine.addCanonicalResource(sm);
			} catch (FHIRException | IOException | URISyntaxException e) {
				e.printStackTrace();
			}
			return engine;
		}
		return engine;
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void TestFhirPath() throws FHIRException, IOException {
		assertEquals("11502-2", getEngine().evaluateFhirPath(cdaLabItaly, false, "ClinicalDocument.code.code"));
		String attributeWithCdaWhiteSpace = cdaLabItaly.replaceAll("11502-2", " 11502-2");
		assertTrue(attributeWithCdaWhiteSpace.indexOf(" 11502-2") > 0);
		assertEquals("11502-2",
						 getEngine().evaluateFhirPath(attributeWithCdaWhiteSpace, false, "ClinicalDocument.code.code"));
		assertEquals("REFERTO DI LABORATORIO",
						 getEngine().evaluateFhirPath(cdaLabItaly, false, "ClinicalDocument.title.dataString"));
		assertEquals("IT", getEngine().evaluateFhirPath(cdaLabItaly, false, "ClinicalDocument.realmCode.code"));
		assertEquals("2.16.840.1.113883.1.3",
						 getEngine().evaluateFhirPath(cdaLabItaly, false, "ClinicalDocument.typeId.root"));
		assertEquals("POCD_MT000040UV02",
						 getEngine().evaluateFhirPath(cdaLabItaly, false, "ClinicalDocument.typeId.extension"));
		assertEquals("active", getEngine().evaluateFhirPath(cdaLabItaly, false, "ClinicalDocument.statusCode.code"));
// 	<effectiveTime value="20220330112426+0100"/>
		assertEquals("2022-03-30T11:24:26+01:00",
						 getEngine().evaluateFhirPath(cdaLabItaly, false, "ClinicalDocument.effectiveTime.value"));
		assertEquals("1993-06-19",
						 getEngine().evaluateFhirPath(cdaLabItaly,
																false,
																"ClinicalDocument.recordTarget.patientRole.patient.birthTime.value"));
		assertEquals("Verdi",
						 getEngine().evaluateFhirPath(cdaLabItaly,
																false,
																"ClinicalDocument.recordTarget.patientRole.patient.name.family.dataString"));
		assertEquals("Giuseppe",
						 getEngine().evaluateFhirPath(cdaLabItaly,
																false,
																"ClinicalDocument.recordTarget.patientRole.patient.name.given.dataString"));
	}

	@Test
	void TestInitial() throws FHIRException, IOException {
		String result = getEngine().transform(cdaLabItaly,
														  false,
														  "http://salute.gov.it/ig/cda-fhir-maps/StructureMap/RefertodilaboratorioFULLBODY",
														  true);
		assertNotNull(result);
	}

	@Test
	void TestObservation() throws FHIRException, IOException {
		InputStream in = getResourceAsStream("cda-it-observation.xml");

        StructureMap sm = getEngine().parseMap(getFileAsStringFromResources("cda-it-observation.map"));
        assertTrue(sm != null);
        engine.addCanonicalResource(sm);

		String cdaObservation = IOUtils.toString(in, StandardCharsets.UTF_8);
		Resource resource = getEngine().transformToFhir(cdaObservation,
																		false,
																		"http://salute.gov.it/ig/cda-fhir-maps/StructureMap/TestObservation");

        Observation obs = (Observation) resource;
        assertEquals("Tue Mar 01 00:00:00 CET 2022", obs.getValuePeriod().getEnd().toString(), "hight should be same");                                                                

		assertNotNull(resource);
	}

    @Test
	void TestObservationSt() throws FHIRException, IOException {
		InputStream in = getResourceAsStream("cda-it-observation-st.xml");

		String cdaObservation = IOUtils.toString(in, StandardCharsets.UTF_8);
		Resource resource = getEngine().transformToFhir(cdaObservation,
																		false,
																		"http://salute.gov.it/ig/cda-fhir-maps/StructureMap/TestObservation");
        Observation obs = (Observation) resource;
        assertEquals("Tue Mar 01 00:00:00 CET 2022", obs.getValuePeriod().getEnd().toString(), "hight should be same");                                                                

		assertNotNull(resource);
	}

	@Test
	void TestSecond() throws FHIRException, IOException {
		String result = getEngine().transform(cdaLabItaly,
														  false,
														  "http://salute.gov.it/ig/cda-fhir-maps/StructureMap/RefertodilaboratorioFULLBODY",
														  true);
		assertNotNull(result);
	}

	@Test
	void TestThird() throws FHIRException, IOException {
		String result = getEngine().transform(cdaLabItaly,
														  false,
														  "http://salute.gov.it/ig/cda-fhir-maps/StructureMap/RefertodilaboratorioFULLBODY",
														  true);
		assertNotNull(result);
	}


   // 14:56:32.193 [main] ERROR ch.ahdis.matchbox.engine.tests.CdaToFhirTransformTests - Wrong namespace - expected 'urn:oid:1.3.6.1.4.1.19376.1.3.2'
   // 14:56:32.193 [main] ERROR ch.ahdis.matchbox.engine.tests.CdaToFhirTransformTests - ServiceEvent.statusCode: max allowed = 1, but found 2 (from http://hl7.org/fhir/cda/StructureDefinition/ServiceEvent|2.1.0-draft1)
	@Test
	void TestValidateCdaIt() throws FHIRException, IOException, EOperationOutcome {
		InputStream in = getResourceAsStream("/cda-it.xml");
		OperationOutcome outcome = getEngine().validate(in,
																		FhirFormat.XML,
																		"http://hl7.org/fhir/cda/StructureDefinition/ClinicalDocument");
		assertNotNull(outcome);

        // FIXME
		assertEquals(2, errors(outcome));
	}

	private int errors(OperationOutcome op) {
		int i = 0;
		for (OperationOutcomeIssueComponent vm : op.getIssue()) {
			if (vm.getSeverity() == IssueSeverity.ERROR || vm.getSeverity() == IssueSeverity.FATAL) {
				// eg ('tel: 390 666 0581')
				if (vm.getDetails().getText().startsWith("URI values cannot have whitespace")) {
    				continue;
				}
				// https://terminology.hl7.org/5.0.0/ValueSet-v3-RoleClassAssignedEntity.json.html has a filter with an is-a concept to ASSIGEND and this cannot be evaluated by org.hl7.fhir.r5.terminologies.ValueSetCheckerSimple
				if (vm.getDetails().getText().startsWith(
					"The value provided ('ASSIGNED') is not in the value set 'RoleClassAssignedEntity' (http://terminology.hl7.org/ValueSet/v3-RoleClassAssignedEntity|2.0.0), and a code is required from this value set) (error message = Unable to resolve system - value set http://terminology.hl7.org/ValueSet/v3-RoleClassAssignedEntity|2.0.0 include #0 has no system)")) {
					continue;
				}
	    		log.error(vm.getDetails().getText());
				++i;
			}
		}
		return i;
	}

	private static InputStream getResourceAsStream(final String filename) {
		return CdaToFhirTransformTests.class.getResourceAsStream("/cda/" + filename);
	}

    public String getFileAsStringFromResources(String file) throws IOException {
		InputStream in = CdaToFhirTransformTests.class.getResourceAsStream("/cda/" + file);
		return IOUtils.toString(in, StandardCharsets.UTF_8);
	}

}
