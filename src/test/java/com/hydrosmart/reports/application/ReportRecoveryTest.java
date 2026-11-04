package com.hydrosmart.reports.application;

import com.hydrosmart.reports.application.internal.commandservices.ReportCommandServiceImpl;
import com.hydrosmart.reports.application.internal.outboundservices.ConsumptionDirectory;
import com.hydrosmart.reports.domain.model.valueobjects.ReportRuleViolation;
import com.hydrosmart.reports.domain.services.ReportRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportRecoveryTest {
 private final ReportRepository repository=mock(ReportRepository.class);
 private final ConsumptionDirectory directory=mock(ConsumptionDirectory.class);
 private final ReportCommandServiceImpl service=new ReportCommandServiceImpl(repository,directory,mock(com.hydrosmart.reports.application.internal.outboundservices.PdfReportRenderer.class),Runnable::run);
 private final UUID id=UUID.randomUUID(),unit=UUID.randomUUID(),request=UUID.randomUUID();
 private final Instant start=Instant.parse("2026-09-01T05:00:00Z"),end=Instant.parse("2026-10-01T05:00:00Z");
 private Map<String,Object> job(String status){return Map.of("requested_by_user_id","owner","unit_id",unit,"period_start",start,"period_end",end,"status",status,"request_event_id",request);}

 @Test void retryRequiresOwnershipBeforeCallingOtherServices(){
  when(repository.get(id)).thenReturn(job("failed"));
  assertEquals(ReportRuleViolation.Kind.NOT_FOUND,assertThrows(ReportRuleViolation.class,()->service.retry("another-user",id,"token")).kind());
  verifyNoInteractions(directory);verify(repository,never()).retry(any());
 }
 @Test void retryCannotRecoverAReportFromAnEarlierTenancy(){
  when(repository.get(id)).thenReturn(job("failed"));
  when(directory.access(unit,"token")).thenReturn(Map.of("visibleFrom",end.toString()));
  assertEquals(ReportRuleViolation.Kind.NOT_FOUND,assertThrows(ReportRuleViolation.class,()->service.retry("owner",id,"token")).kind());
  verify(repository,never()).retry(any());
 }
 @Test void authorizedRetryUsesTheSameJobAndPublishesThroughItsRepository(){
  when(repository.get(id)).thenReturn(job("failed"));when(directory.access(unit,"token")).thenReturn(Map.of());when(repository.retry(id)).thenReturn(true);
  service.retry("owner",id,"token");verify(repository).retry(id);
 }
 @Test void retryRejectsACompletedOrStillRunningJob(){
  when(repository.get(id)).thenReturn(job("completed"));when(directory.access(unit,"token")).thenReturn(Map.of());
  assertEquals(ReportRuleViolation.Kind.CONFLICT,assertThrows(ReportRuleViolation.class,()->service.retry("owner",id,"token")).kind());
 }
 @Test void anOldDeliveryCannotGenerateANewAttempt(){
  when(repository.get(id)).thenReturn(job("pending"));service.generate(id,UUID.randomUUID());
  verifyNoInteractions(directory);verify(repository,never()).complete(any(),any(),any());
 }
 @Test void terminalFailureKeepsItsRequestIdentityAndASafeUserMessage(){
  service.failed(id,request);
  verify(repository).failed(eq(id),eq(request),argThat(message->message.contains("reintentarlo")&&message.length()<=300));
 }
 @Test void currentDeliveryCanCompleteAndFailedDeliveryCannotRunAgain(){
  when(repository.get(id)).thenReturn(job("pending"));
  when(directory.report(unit,start.toString(),end.toString())).thenReturn(Map.of("daily",List.of(),"estimate",Map.of("total","28.56"),"tariffName","Referencia"));
  service.generate(id,request);verify(repository).complete(eq(id),eq(request),argThat(csv->new String(csv,java.nio.charset.StandardCharsets.UTF_8).contains("28.56")));
  clearInvocations(directory,repository);when(repository.get(id)).thenReturn(job("failed"));service.generate(id,request);verifyNoInteractions(directory);
 }
}
