package com.hydrosmart.reports.infrastructure.documents;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import java.util.*;
import java.time.Instant;
import java.nio.file.*;
class PdfReportTest {
 @Test void reportIsPaginatedSearchableAndKeepsAccentsAndAllRows()throws Exception{
  var rows=new ArrayList<Map<String,Object>>();for(int i=0;i<120;i++)rows.add(Map.of("date","2026-10-"+String.format("%02d",i%30+1),"deviceId","device-"+i,"deviceName","Baño principal · Medidor "+i,"liters",10,"readingCount",1));
  var job=Map.<String,Object>of("unit_id",UUID.randomUUID(),"period_start",Instant.parse("2026-10-01T05:00:00Z"),"period_end",Instant.parse("2026-11-01T05:00:00Z"));
  var data=Map.<String,Object>of("daily",rows,"estimate",Map.of("total","20.00"),"tariffName","Tarifas del período","unitName","Departamento 101");
  var content=new PdfBoxReportRenderer().render(job,data);
  try(var document=Loader.loadPDF(content)){
   assertTrue(document.getNumberOfPages()>1);String text=new PDFTextStripper().getText(document);
   assertTrue(text.contains("Baño principal"));assertTrue(text.contains("Medidor 119"));assertTrue(text.contains("1200 L"));assertTrue(text.contains("S/ 20.00"));
  }
  Files.createDirectories(Path.of("target/report-previews"));Files.write(Path.of("target/report-previews/hydrosmart-demo.pdf"),content);
 }
}
