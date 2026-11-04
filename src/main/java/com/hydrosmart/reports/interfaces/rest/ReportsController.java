package com.hydrosmart.reports.interfaces.rest;
import java.util.*;
import java.time.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.*;
import com.hydrosmart.reports.domain.services.ReportService;
import com.hydrosmart.reports.domain.model.valueobjects.ReportRuleViolation;
import com.hydrosmart.reports.interfaces.rest.resources.CreateReportResource;
@RestController
public class ReportsController {
 private final ReportService service;public ReportsController(ReportService service){this.service=service;}
 @PostMapping("/api/v1/reports")public ResponseEntity<?> create(@AuthenticationPrincipal Jwt jwt,@RequestHeader("Authorization")String authorization,@RequestBody CreateReportResource body){
  UUID id=service.create(jwt.getSubject(),body.unitId(),body.from().toInstant(),body.to().toInstant(),authorization.substring(7),body.format());
  return ResponseEntity.accepted().body(Map.of("id",id,"status","pending"));
 }
 @GetMapping("/api/v1/reports")public List<Map<String,Object>> list(@AuthenticationPrincipal Jwt jwt,@RequestHeader("Authorization")String authorization,@RequestParam UUID unitId){return service.list(jwt.getSubject(),unitId,authorization.substring(7));}
 @PostMapping("/api/v1/reports/{id}/retry")public ResponseEntity<?> retry(@AuthenticationPrincipal Jwt jwt,@RequestHeader("Authorization")String authorization,@PathVariable UUID id){
  service.retry(jwt.getSubject(),id,authorization.substring(7));
  return ResponseEntity.accepted().body(Map.of("id",id,"status","pending"));
 }
 @GetMapping("/api/v1/reports/{id}/download")public ResponseEntity<byte[]> download(@AuthenticationPrincipal Jwt jwt,@RequestHeader("Authorization")String authorization,@PathVariable UUID id){
  var document=service.download(jwt.getSubject(),id,authorization.substring(7));
  return ResponseEntity.ok().header("Content-Disposition","attachment; filename=\"hydrosmart-"+id+"."+document.format()+"\"").contentType("pdf".equals(document.format())?MediaType.APPLICATION_PDF:MediaType.parseMediaType("text/csv;charset=utf-8")).body(document.content());
 }
 @ExceptionHandler(com.hydrosmart.reports.shared.infrastructure.messaging.RpcBus.BrokerStatus.class)public ResponseEntity<?> brokerStatus(com.hydrosmart.reports.shared.infrastructure.messaging.RpcBus.BrokerStatus error){return ResponseEntity.status(Set.of(400,403,404,409).contains(error.status)?error.status:503).body(Map.of("detail","No tienes acceso al recurso solicitado."));}
 @ExceptionHandler(IllegalStateException.class)public ResponseEntity<?> unavailable(){return ResponseEntity.status(503).body(Map.of("detail","El servicio no está disponible."));}
 @ExceptionHandler(ReportRuleViolation.class)public ResponseEntity<?> rule(ReportRuleViolation error){return ResponseEntity.status(error.kind()==ReportRuleViolation.Kind.NOT_FOUND?404:409).body(Map.of("detail",error.getMessage()));}
 @ExceptionHandler({IllegalArgumentException.class,java.time.DateTimeException.class})public ResponseEntity<?> invalid(RuntimeException error){return ResponseEntity.badRequest().body(Map.of("detail",error.getMessage()==null?"Datos inválidos.":error.getMessage()));}
}

