package com.hydrosmart.reports.interfaces.messaging;
import java.util.*;
import org.springframework.stereotype.Component;
import com.hydrosmart.reports.domain.services.ReportService;
@Component
public class ReportEventsController {
 private final ReportService service;public ReportEventsController(ReportService service){this.service=service;}
 @SuppressWarnings("unchecked")public void receive(Map<String,Object> event){
  if(!"reports.report.requested.v1".equals(event.get("event_type")))throw new IllegalArgumentException();
  var data=(Map<String,Object>)event.get("data");service.generate(UUID.fromString(data.get("report_id").toString()),UUID.fromString(event.get("event_id").toString()));
 }
 @SuppressWarnings("unchecked")public void failed(Map<String,Object> event){
  if(!"reports.report.requested.v1".equals(event.get("event_type")))return;
  UUID id,request;
  try{var data=(Map<String,Object>)event.get("data");id=UUID.fromString(data.get("report_id").toString());request=UUID.fromString(event.get("event_id").toString());}
  catch(IllegalArgumentException|NullPointerException|ClassCastException invalid){return;}
  service.failed(id,request);
 }
}

