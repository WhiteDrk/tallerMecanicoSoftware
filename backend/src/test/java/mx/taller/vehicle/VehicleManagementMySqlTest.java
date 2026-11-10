package mx.taller.vehicle;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import mx.taller.auth.ConflictException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Opt-in integration against a disposable MySQL database, never the application's data. */
@EnabledIfEnvironmentVariable(named = "QA_MYSQL_URL", matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/taller_vehicle_qa(?:\\?.*)?")
@SpringBootTest(properties = {
  "spring.datasource.url=${QA_MYSQL_URL}", "spring.datasource.username=${QA_MYSQL_USER}",
  "spring.datasource.password=${QA_MYSQL_PASSWORD}",
  "app.jwt.secret=cWEtb25seS1zeW50aGV0aWMta2V5LWZvci1sb2NhbC12ZWhpY2xlLWludGVncmF0aW9uLTIwMjYtbm90LWZvci1wcm9kdWN0aW9u"
})
@AutoConfigureMockMvc
class VehicleManagementMySqlTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired VehicleManagementFacade facade;
  @Autowired EntityManagerFactory emf;
  String actor, otherActor, workshop, client, vehicle;
  Long brand, model;

  @BeforeEach void fixtures() {
    actor = UUID.randomUUID().toString(); otherActor = UUID.randomUUID().toString();
    workshop = UUID.randomUUID().toString(); client = UUID.randomUUID().toString(); vehicle = UUID.randomUUID().toString();
    for (String id : new String[]{actor, otherActor}) {
      jdbc.update("INSERT INTO users(id,email,full_name,password_hash,enabled) VALUES(UUID_TO_BIN(?),?,'QA OPERADOR','not-a-login-hash',true)", id, id + "@example.invalid");
      jdbc.update("INSERT INTO user_roles SELECT UUID_TO_BIN(?),id FROM roles WHERE code='RECEPCIONISTA'", id);
    }
    jdbc.update("INSERT INTO workshops(id,name,legal_name,phone,manager_user_id,rfc,email,street,neighborhood,municipality,state,postal_code) VALUES(UUID_TO_BIN(?),'Taller QA','QA','5555555555',UUID_TO_BIN(?),'AAA010101AAA','qa@example.invalid','Calle QA','Centro','Municipio','Estado','01000')", workshop, actor);
    jdbc.update("INSERT INTO user_workshops VALUES(UUID_TO_BIN(?),UUID_TO_BIN(?))", actor, workshop);
    jdbc.update("INSERT INTO clients(id,full_name,alternate_contact_name,birth_date,phone,phone_normalized,work_phone,email,email_normalized,street,neighborhood,municipality,state,postal_code,workshop_id,status_id) SELECT UUID_TO_BIN(?),'Cliente QA','Contacto QA','1990-01-01','5555555555','5555555555','5555555556','qa@example.invalid','qa@example.invalid','Calle QA','Centro','Municipio','Estado','01000',UUID_TO_BIN(?),id FROM status_catalog WHERE str_valor='ACTIVO'", client, workshop);
    brand = jdbc.queryForObject("SELECT id FROM vehicle_brands WHERE name='KIA'", Long.class);
    model = jdbc.queryForObject("SELECT id FROM vehicle_models WHERE brand_id=? AND name='RIO'", Long.class, brand);
    jdbc.update("INSERT INTO vehicles(id,client_id,model_id,vin,plate,plate_normalized,vehicle_year,version,mileage,serial_number,status_id) SELECT UUID_TO_BIN(?),UUID_TO_BIN(?),?,'3KPF24AD3R2610131','QA-1234','QA-1234',2024,'EX',0,'SERIE-QA',id FROM status_catalog WHERE str_valor='ACTIVO'", vehicle, client, model);
  }

  @AfterEach void cleanupOwnFixtures() {
    jdbc.update("DELETE FROM audit_events WHERE entity_id=?", vehicle);
    jdbc.update("DELETE FROM vehicles WHERE id=UUID_TO_BIN(?)", vehicle);
    jdbc.update("DELETE FROM clients WHERE id=UUID_TO_BIN(?)", client);
    jdbc.update("DELETE FROM user_workshops WHERE workshop_id=UUID_TO_BIN(?)", workshop);
    jdbc.update("DELETE FROM workshops WHERE id=UUID_TO_BIN(?)", workshop);
    for (String id : new String[]{actor, otherActor}) {
      jdbc.update("DELETE FROM user_roles WHERE user_id=UUID_TO_BIN(?)", id);
      jdbc.update("DELETE FROM users WHERE id=UUID_TO_BIN(?)", id);
    }
  }
  VehicleUpdateRequest updateRequest(long revision) {
    return new VehicleUpdateRequest("3KPF24AD3R2610131", "qa - 5678", brand, model, null, null, 2024, "EX PLUS", 42L, "SERIE-QA", revision);
  }
  String updateBody(long revision) throws Exception { return json.writeValueAsString(updateRequest(revision)); }
  JsonNode getCard(String roleActor, String criterion, String value, int expected) throws Exception {
    String body = mvc.perform(get("/api/v1/vehicles").param("criterion", criterion).param("value", value).with(user(roleActor).roles("RECEPCIONISTA")))
      .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
    return json.readTree(body);
  }

  @Test void realHttpUpdateThenConfirmedSoftDeleteKeepsDataAndAudit() throws Exception {
    mvc.perform(put("/api/v1/vehicles/" + vehicle).with(user(actor).roles("RECEPCIONISTA")).contentType(MediaType.APPLICATION_JSON).content(updateBody(0))).andExpect(status().isOk());
    JsonNode card = getCard(actor, "VIN", "3KPF24AD3R2610131", 200);
    assertEquals("QA-5678", card.path("plate").asText()); assertEquals(1, card.path("revision").asInt());
    assertEquals(vehicle, getCard(actor, "PLATE", "QA-5678", 200).path("id").asText());
    getCard(actor, "PLATE", "QA-1234", 404);
    mvc.perform(put("/api/v1/vehicles/" + vehicle).with(user(actor).roles("RECEPCIONISTA")).contentType(MediaType.APPLICATION_JSON).content(updateBody(0))).andExpect(status().isConflict());
    mvc.perform(post("/api/v1/vehicles/" + vehicle + "/deactivation").with(user(actor).roles("RECEPCIONISTA")).contentType(MediaType.APPLICATION_JSON)
      .content("{\"confirmationVin\":\"3KPF24AD3R2610132\",\"confirmed\":true,\"revision\":1}")).andExpect(status().isBadRequest());
    mvc.perform(post("/api/v1/vehicles/" + vehicle + "/deactivation").with(user(actor).roles("RECEPCIONISTA")).contentType(MediaType.APPLICATION_JSON)
      .content("{\"confirmationVin\":\"3KPF24AD3R2610131\",\"confirmed\":true,\"revision\":1}")).andExpect(status().isOk());
    card = getCard(actor, "ID", vehicle, 200);
    assertEquals("INACTIVO", card.path("status").asText()); assertEquals("QA-5678", card.path("plate").asText()); assertEquals(2, card.path("revision").asInt());
    assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE entity_id=?", Integer.class, vehicle));
    String details = jdbc.queryForObject("SELECT details FROM audit_events WHERE entity_id=? AND action='VEHICLE_UPDATED'", String.class, vehicle);
    assertEquals("QA-1234", json.readTree(details).at("/before/vehicle/plate").asText());
    assertEquals("QA-5678", json.readTree(details).at("/after/vehicle/plate").asText());
    assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE entity_id=? AND actor_id=UUID_TO_BIN(?) AND LENGTH(ip_hash)=64", Integer.class, vehicle, actor));
    assertThrows(ConflictException.class, () -> facade.deactivate(UUID.fromString(vehicle), new VehicleDeactivationRequest("3KPF24AD3R2610131", true, 2L), actor, "ip"));
    assertThrows(ConflictException.class, () -> facade.update(UUID.fromString(vehicle), updateRequest(2), actor, "ip"));
    assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM vehicles WHERE id=UUID_TO_BIN(?)", Integer.class, vehicle));
  }

  @Test void foreignAndMissingObjectsHaveSame404AndNoWrites() throws Exception {
    for (String id : new String[]{vehicle, UUID.randomUUID().toString()}) {
      mvc.perform(put("/api/v1/vehicles/" + id).with(user(otherActor).roles("RECEPCIONISTA")).contentType(MediaType.APPLICATION_JSON).content(updateBody(0))).andExpect(status().isNotFound());
      mvc.perform(post("/api/v1/vehicles/" + id + "/deactivation").with(user(otherActor).roles("RECEPCIONISTA")).contentType(MediaType.APPLICATION_JSON)
        .content("{\"confirmationVin\":\"3KPF24AD3R2610131\",\"confirmed\":true,\"revision\":0}")).andExpect(status().isNotFound());
    }
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE entity_id=?", Integer.class, vehicle));
    assertEquals("QA-1234", getCard(actor, "ID", vehicle, 200).path("plate").asText());
  }

  @Test void auditFailureRollsBackAlreadyFlushedUpdate() throws Exception {
    assertThrows(NullPointerException.class, () -> facade.update(UUID.fromString(vehicle), updateRequest(0), actor, null));
    JsonNode card = getCard(actor, "ID", vehicle, 200);
    assertEquals("QA-1234", card.path("plate").asText()); assertEquals(0, card.path("revision").asInt());
    assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE entity_id=?", Integer.class, vehicle));
  }

  @Test void duplicatePlateOrSerialReturns409WithoutPartialChanges() throws Exception {
    String second = UUID.randomUUID().toString();
    jdbc.update("INSERT INTO vehicles(id,client_id,model_id,vin,plate,plate_normalized,vehicle_year,version,serial_number,status_id) SELECT UUID_TO_BIN(?),UUID_TO_BIN(?),?,'3KPF24AD3R2610132','QA-5678','QA-5678',2024,'EX','SERIE-OTHER',id FROM status_catalog WHERE str_valor='ACTIVO'", second, client, model);
    try {
      for (String body : new String[]{updateBody(0), updateBody(0).replace("qa - 5678", "QA-8765").replace("SERIE-QA", "SERIE-OTHER")}) {
        mvc.perform(put("/api/v1/vehicles/" + vehicle).with(user(actor).roles("RECEPCIONISTA")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
      }
      assertEquals("QA-1234", getCard(actor, "ID", vehicle, 200).path("plate").asText());
      assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE entity_id=?", Integer.class, vehicle));
      assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
        jdbc.update("UPDATE vehicles SET plate_normalized='QA-5678' WHERE id=UUID_TO_BIN(?)", vehicle));
    } finally { jdbc.update("DELETE FROM vehicles WHERE id=UUID_TO_BIN(?)", second); }
  }

  @Test void actualJpaVersionRejectsConcurrentEditAfterDeactivation() {
    var first = emf.createEntityManager(); var second = emf.createEntityManager();
    try {
      first.getTransaction().begin(); second.getTransaction().begin();
      Vehicle old = first.find(Vehicle.class, UUID.fromString(vehicle));
      Vehicle current = second.find(Vehicle.class, UUID.fromString(vehicle));
      current.deactivate(second.createQuery("select s from StatusCatalog s where s.value='INACTIVO'", mx.taller.status.StatusCatalog.class).getSingleResult());
      second.getTransaction().commit();
      old.updateDetails(old.getModel(), updateRequest(0), "QA-5678", "SERIE-QA");
      assertThrows(jakarta.persistence.OptimisticLockException.class, first::flush);
    } finally {
      if (first.getTransaction().isActive()) first.getTransaction().rollback();
      if (second.getTransaction().isActive()) second.getTransaction().rollback();
      first.close(); second.close();
    }
    assertEquals("QA-1234", jdbc.queryForObject("SELECT plate FROM vehicles WHERE id=UUID_TO_BIN(?)", String.class, vehicle));
  }
}
