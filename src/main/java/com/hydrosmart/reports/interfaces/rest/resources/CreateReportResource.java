package com.hydrosmart.reports.interfaces.rest.resources;
import java.time.OffsetDateTime;
import java.util.UUID;
public record CreateReportResource(UUID unitId,OffsetDateTime from,OffsetDateTime to,String format) {
 public CreateReportResource {
  if(unitId==null||from==null||to==null)throw new IllegalArgumentException("Unidad y fechas son obligatorias.");
  if(format==null)format="csv";
  if(!java.util.Set.of("csv","pdf","xlsx").contains(format))throw new IllegalArgumentException("Elige Excel, PDF o CSV.");
 }
}
