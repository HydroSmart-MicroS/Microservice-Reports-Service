package com.hydrosmart.reports.domain.model.valueobjects;
public final class ReportRuleViolation extends RuntimeException {
 public enum Kind { NOT_FOUND, CONFLICT }
 private final Kind kind;
 public ReportRuleViolation(Kind kind){this(kind,"Reporte no disponible.");}
 public ReportRuleViolation(Kind kind,String message){super(message);this.kind=kind;}
 public Kind kind(){return kind;}
}
