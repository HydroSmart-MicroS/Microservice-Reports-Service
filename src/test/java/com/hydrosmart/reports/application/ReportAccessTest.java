package com.hydrosmart.reports.application;

import java.util.*;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.hydrosmart.reports.domain.model.valueobjects.ReportRuleViolation;
import com.hydrosmart.reports.application.internal.commandservices.ReportCommandServiceImpl;
import com.hydrosmart.reports.application.internal.outboundservices.ConsumptionDirectory;
import com.hydrosmart.reports.domain.services.ReportRepository;

class ReportAccessTest {
 final ReportRepository repository=mock(ReportRepository.class);
 final ConsumptionDirectory directory=mock(ConsumptionDirectory.class);
 final ReportCommandServiceImpl service=new ReportCommandServiceImpl(repository,directory,mock(com.hydrosmart.reports.application.internal.outboundservices.PdfReportRenderer.class),Runnable::run);
 final UUID unit=UUID.randomUUID(),id=UUID.randomUUID();
 final Instant start=Instant.parse("2026-10-20T05:00:00Z");

 @Test void newTenancyDoesNotRestoreDownloadAccessToPreviousTenancy(){
  when(repository.get(id)).thenReturn(Map.of("requested_by_user_id","tenant","unit_id",unit,"period_start",start.minusSeconds(86400),"status","completed"));
  when(directory.access(unit,"token")).thenReturn(Map.of("visibleFrom",start.toString()));
  assertEquals(ReportRuleViolation.Kind.NOT_FOUND,assertThrows(ReportRuleViolation.class,()->service.download("tenant",id,"token")).kind());
  verify(repository,never()).content(any());
 }
 @Test void tenantListOnlyIncludesReportsFromCurrentTenancy(){
  var old=Map.<String,Object>of("id",id,"period_start",start.minusSeconds(86400).toString());
  var current=Map.<String,Object>of("id",UUID.randomUUID(),"period_start",start.toString());
  when(directory.access(unit,"token")).thenReturn(Map.of("visibleFrom",start.toString()));
  when(repository.list("tenant",unit)).thenReturn(List.of(old,current));
  assertEquals(List.of(current),service.list("tenant",unit,"token"));
 }
 @Test void ownerRetainsAccessToFullHistory(){
  when(repository.get(id)).thenReturn(Map.of("requested_by_user_id","owner","unit_id",unit,"period_start",start.minusSeconds(86400),"status","completed"));
  when(directory.access(unit,"token")).thenReturn(Map.of("ownerUserId","owner"));
  when(repository.content(id)).thenReturn(new byte[]{1,2});
  assertArrayEquals(new byte[]{1,2},service.download("owner",id,"token").content());
 }
}
