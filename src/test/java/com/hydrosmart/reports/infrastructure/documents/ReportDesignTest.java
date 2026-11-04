package com.hydrosmart.reports.infrastructure.documents;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReportDesignTest {
 private static final Map<String,Object> JOB=Map.of("unit_id",UUID.randomUUID(),"period_start",Instant.parse("2026-10-29T05:00:00Z"),"period_end",Instant.parse("2026-11-04T05:00:00Z"));
 private static Map<String,Object> sample(){
  String[] dates={"2026-10-29","2026-10-30","2026-10-31","2026-11-01","2026-11-02","2026-11-03"};
  String[] liters={"203.35","164.8","131.2","161.65","174.4","206.078"};
  var rows=new ArrayList<Map<String,Object>>();for(int i=0;i<dates.length;i++)rows.add(Map.of("date",dates[i],"deviceId","demo-shower","deviceName","Ducha principal","liters",liters[i],"readingCount",i==5?1330:4));
  return Map.of("unitName","Departamento 101","daily",rows,"estimate",Map.of("total","10.87"),"tariffName","Tarifas del período");
 }
 private static Map<String,String> parts(byte[] bytes)throws Exception{
  var parts=new HashMap<String,String>();try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes),StandardCharsets.UTF_8)){
   for(var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry())parts.put(entry.getName(),new String(zip.readAllBytes(),StandardCharsets.UTF_8));
  }
  var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
  for(String xml:parts.values())factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  return parts;
 }
 @Test void excelIsARealWorkbookWithNumericValuesStylesAndSafeText()throws Exception{
  var content=new XlsxReportRenderer().render(JOB,sample());var parts=parts(content);
  assertTrue(parts.containsKey("[Content_Types].xml"));assertTrue(parts.containsKey("xl/styles.xml"));
  String sheet=parts.get("xl/worksheets/sheet1.xml");
  assertTrue(sheet.contains("showGridLines=\"0\""));assertTrue(sheet.contains("state=\"frozen\""));
  assertTrue(sheet.contains("<v>203.35</v>"));assertTrue(sheet.contains("<v>1041.478</v>"));assertTrue(sheet.contains("<v>10.87</v>"));
  assertTrue(sheet.contains("<autoFilter ref=\"B13:F19\""));
  Path preview=Path.of("target/report-previews");Files.createDirectories(preview);Files.write(preview.resolve("hydrosmart-consumo-ejemplo.xlsx"),content);
  var unsafe=new HashMap<>(sample());unsafe.put("unitName","=HYPERLINK(\"https://example.invalid\") <&>");
  String encoded=parts(new XlsxReportRenderer().render(JOB,unsafe)).get("xl/worksheets/sheet1.xml");
  assertTrue(encoded.contains("t=\"inlineStr\""));assertTrue(encoded.contains("&lt;&amp;&gt;"));assertFalse(encoded.contains("<f>=HYPERLINK"));
 }
 @Test void pdfSummaryKeepsDataAndAllSixDatesOnOnePage()throws Exception{
  byte[] pdf=new PdfBoxReportRenderer().render(JOB,sample());
  try(var document=Loader.loadPDF(pdf)){
   assertEquals(1,document.getNumberOfPages());String text=new PDFTextStripper().getText(document);
   assertTrue(text.contains("Departamento 101"));assertTrue(text.contains("1,041.478 L"));assertTrue(text.contains("S/ 10.87"));
   assertTrue(text.contains("29 oct"));assertTrue(text.contains("03 nov"));assertTrue(text.contains("Página 1 de 1"));
  }
  Path preview=Path.of("target/report-previews");Files.createDirectories(preview);Files.write(preview.resolve("hydrosmart-consumo-ejemplo.pdf"),pdf);
 }
 @Test void emptyExportsDoNotInventReadings()throws Exception{
  var data=Map.<String,Object>of("daily",List.of(),"estimate",Map.of("total","0.00"),"unitName","Unidad sin lecturas");
  String sheet=parts(new XlsxReportRenderer().render(JOB,data)).get("xl/worksheets/sheet1.xml");assertTrue(sheet.contains("Sin lecturas en este período."));
  try(var document=Loader.loadPDF(new PdfBoxReportRenderer().render(JOB,data))){assertTrue(new PDFTextStripper().getText(document).contains("No hay lecturas registradas"));}
 }
}
