package mx.taller.report;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import java.util.UUID;
import mx.taller.config.SecurityConfig;
import mx.taller.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers=TestReportController.class,properties="app.cors.allowed-origins=http://localhost")
@Import(SecurityConfig.class)
class TestReportAuthorizationTest {
  static final String BASE="/api/v1/test-reports", ID="00000000-0000-0000-0000-000000000001";
  @Autowired MockMvc mvc;
  @MockitoBean TestReportService service;
  @MockitoBean JwtService jwt;

  @ParameterizedTest @CsvSource({"ADMINISTRADOR,true","MANAGER,true","AUDITOR,true","RECEPCIONISTA,false","CUSTOMER,false","SECRETARIA,false","MECHANIC,false"})
  void reportListDetailsAndFilesShareTheSameReadPermission(String role,boolean allowed) throws Exception {
    when(service.list()).thenReturn(List.of());
    when(service.artifact(any())).thenReturn(new byte[]{1,2});
    for(String path:List.of(BASE,BASE+"/"+ID,BASE+"/"+ID+"/archive"))
      mvc.perform(get(path).with(user(ID).roles(role))).andExpect(status().is(allowed?200:403));
    if(!allowed)verifyNoInteractions(service);
  }
  @ParameterizedTest @CsvSource({"ADMINISTRADOR,201","MANAGER,403","AUDITOR,403","RECEPCIONISTA,403","CUSTOMER,403"})
  void onlyAdministrationCanUpload(String role,int expected) throws Exception {
    mvc.perform(multipart(BASE).file(new MockMultipartFile("file","report.zip","application/zip",new byte[]{1}))
      .param("title","Ejecución").param("tool","KARATE").with(user(ID).roles(role))).andExpect(status().is(expected));
    if(expected==403)verifyNoInteractions(service);
  }
  @Test void anonymousCannotReadOrUploadEvenWithDirectUrls() throws Exception {
    for(String path:List.of(BASE,BASE+"/"+ID,BASE+"/"+ID+"/archive"))mvc.perform(get(path)).andExpect(status().isUnauthorized());
    mvc.perform(multipart(BASE).file(new MockMultipartFile("file",new byte[]{1})).param("title","Test").param("tool","KARATE"))
      .andExpect(status().isUnauthorized());
    verifyNoInteractions(service);
  }
  @Test void artifactsAreAttachmentsAndNeverCached() throws Exception {
    when(service.artifact(UUID.fromString(ID))).thenReturn(new byte[]{1,2});
    mvc.perform(get(BASE+"/"+ID+"/archive").with(user(ID).roles("AUDITOR")))
      .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
      .andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.startsWith("attachment;")))
      .andExpect(content().contentType("application/zip"));
  }
}
