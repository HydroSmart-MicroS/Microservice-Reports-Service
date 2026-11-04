package com.hydrosmart.reports.application.internal.commandservices;
import java.util.*;
import com.hydrosmart.reports.domain.model.valueobjects.ReportRuleViolation;
import java.time.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import com.hydrosmart.reports.domain.services.ReportService;
import com.hydrosmart.reports.domain.services.ReportRepository;
import com.hydrosmart.reports.application.internal.outboundservices.ConsumptionDirectory;
import com.hydrosmart.reports.application.internal.outboundservices.PdfReportRenderer;
@Service
public class ReportCommandServiceImpl implements ReportService {
 private final ReportRepository repository;private final ConsumptionDirectory directory;private final PdfReportRenderer pdf;private final com.hydrosmart.reports.application.internal.outboundservices.AccountGuard guard;
 public ReportCommandServiceImpl(ReportRepository repository,ConsumptionDirectory directory,PdfReportRenderer pdf,com.hydrosmart.reports.application.internal.outboundservices.AccountGuard guard){this.repository=repository;this.directory=directory;this.pdf=pdf;this.guard=guard;}
 public UUID create(String user,UUID unit,Instant from,Instant to,String token,String format){
  if(!Set.of("csv","pdf").contains(format))throw new IllegalArgumentException("Elige PDF o CSV.");
  if(!from.isBefore(to)||Duration.between(from,to).compareTo(Duration.ofDays(366))>0)throw new IllegalArgumentException("Período inválido.");
  var access=directory.access(unit,token);
  if(access.get("visibleFrom")!=null){var start=Instant.parse(access.get("visibleFrom").toString());if(from.isBefore(start))from=start;}
  if(!from.isBefore(to))throw new IllegalArgumentException("El período es anterior a tu alquiler.");
  UUID id=UUID.randomUUID();repository.create(id,user,unit,from,to,format);return id;
 }
 public List<Map<String,Object>> list(String user,UUID unit,String token){
  var access=directory.access(unit,token);var reports=repository.list(user,unit);
  if(access.get("visibleFrom")==null)return reports;
  var start=Instant.parse(access.get("visibleFrom").toString());
  return reports.stream().filter(row->!Instant.parse(row.get("period_start").toString()).isBefore(start)).toList();
 }
 private Map<String,Object> accessible(String user,UUID id,String token){
  var row=repository.get(id);if(row.isEmpty()||!user.equals(row.get("requested_by_user_id")))throw new ReportRuleViolation(ReportRuleViolation.Kind.NOT_FOUND);
  var access=directory.access((UUID)row.get("unit_id"),token);
  if(access.get("visibleFrom")!=null&&((Instant)row.get("period_start")).isBefore(Instant.parse(access.get("visibleFrom").toString())))throw new ReportRuleViolation(ReportRuleViolation.Kind.NOT_FOUND);
  return row;
 }
 public Document download(String user,UUID id,String token){
  var row=accessible(user,id,token);
  if(!"completed".equals(row.get("status")))throw new ReportRuleViolation(ReportRuleViolation.Kind.CONFLICT,"El reporte se está preparando.");
  return new Document(repository.content(id),Objects.toString(row.get("format"),"csv"));
 }
 public void retry(String user,UUID id,String token){
  accessible(user,id,token);
  if(!repository.retry(id))throw new ReportRuleViolation(ReportRuleViolation.Kind.CONFLICT,"Solo puedes reintentar un reporte fallido o pendiente desde hace más de cinco minutos.");
 }
 public void failed(UUID id,UUID request){repository.failed(id,request,"No se pudo generar el reporte tras varios intentos. Puedes reintentarlo.");}
 public static String cell(Object value){String text=String.valueOf(value);if(!text.isEmpty()&&"=+-@\t\r".indexOf(text.charAt(0))>=0)text="'"+text;return "\""+text.replace("\"","\"\"")+"\"";}
 public void generate(UUID id,UUID request){
  guard.read(()->generateLocked(id,request));
 }
 @SuppressWarnings("unchecked")private void generateLocked(UUID id,UUID request){
  var job=repository.get(id);if(job.isEmpty()||!"pending".equals(job.get("status"))||!Objects.equals(request,job.get("request_event_id")))return;
  var data=directory.report((UUID)job.get("unit_id"),((Instant)job.get("period_start")).toString(),((Instant)job.get("period_end")).toString());
  if("pdf".equals(job.get("format"))){repository.complete(id,request,pdf.render(job,data));return;}
  var csv=new StringBuilder("\uFEFFFecha (Lima),Medidor,Litros,Lecturas\r\n");
  for(var row:(List<Map<String,Object>>)data.get("daily"))csv.append(cell(row.get("date"))).append(',').append(cell(row.getOrDefault("deviceName",row.get("deviceId")))).append(',').append(cell(row.get("liters"))).append(',').append(cell(row.get("readingCount"))).append("\r\n");
  var estimate=(Map<String,Object>)data.get("estimate");
  csv.append("\r\nCosto estimado (PEN),").append(cell(estimate.get("total"))).append("\r\nTarifa,").append(cell(data.get("tariffName"))).append("\r\n");
  repository.complete(id,request,csv.toString().getBytes(StandardCharsets.UTF_8));
 }
}

