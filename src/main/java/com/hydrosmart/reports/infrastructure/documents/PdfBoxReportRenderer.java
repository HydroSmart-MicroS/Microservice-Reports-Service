package com.hydrosmart.reports.infrastructure.documents;

import java.util.*;
import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import com.hydrosmart.reports.application.internal.outboundservices.PdfReportRenderer;

@Component
public class PdfBoxReportRenderer implements PdfReportRenderer {
 private static final ZoneId LIMA=ZoneId.of("America/Lima");
 private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm",Locale.forLanguageTag("es-PE")).withZone(LIMA);
 @SuppressWarnings("unchecked") public byte[] render(Map<String,Object> job,Map<String,Object> data){
  try(var document=new PDDocument();var bytes=new ByteArrayOutputStream();var fontStream=getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf")){
   if(fontStream==null)throw new IOException("Report font missing");
   var font=PDType0Font.load(document,fontStream,true);var writer=new PageWriter(document,font);
   document.getDocumentInformation().setTitle("HydroSmart · Reporte de consumo");document.getDocumentInformation().setAuthor("HydroSmart");
   writer.page();writer.line("Reporte de consumo",23);writer.line(Objects.toString(data.get("unitName"),"Unidad "+job.get("unit_id")),11);
   writer.line("Desde "+DATE.format(Instant.parse(job.get("period_start").toString())),10);
   writer.line("Hasta "+DATE.format(Instant.parse(job.get("period_end").toString()).minusSeconds(1))+" · hora de Lima",10);writer.y-=12;
   var daily=(List<Map<String,Object>>)data.getOrDefault("daily",List.of());
   var estimate=(Map<String,Object>)data.getOrDefault("estimate",Map.of("total","0.00"));
   BigDecimal total=daily.stream().map(row->new BigDecimal(row.get("liters").toString())).reduce(BigDecimal.ZERO,BigDecimal::add);
   writer.metric(44,"CONSUMO DEL PERÍODO",total.stripTrailingZeros().toPlainString()+" L");
   writer.metric(218,"COSTO ESTIMADO","S/ "+estimate.get("total"));
   writer.metric(392,"REGISTROS DIARIOS",Integer.toString(daily.size()));writer.y-=76;
   writer.paragraph("Tarifa: "+Objects.toString(data.get("tariffName"),"Tarifa del período"),10,507);writer.y-=12;
   writer.tableHead();
   if(daily.isEmpty())writer.line("No hay lecturas registradas en este período.",11);
   for(var row:daily){
    var names=writer.wrap(Objects.toString(row.getOrDefault("deviceName",row.get("deviceId"))),9,205);
    float height=Math.max(25,names.size()*13+12);
    if(writer.y-height<70){writer.page();writer.tableHead();}
    writer.text(44,writer.y-15,9,Objects.toString(row.get("date")));
    for(int i=0;i<names.size();i++)writer.text(132,writer.y-15-i*13,9,names.get(i));
    writer.text(360,writer.y-15,9,Objects.toString(row.get("liters")));
    writer.text(477,writer.y-15,9,Objects.toString(row.getOrDefault("readingCount",0)));
    writer.stream.setStrokingColor(0.85f,0.90f,0.87f);writer.stream.setLineWidth(.4f);writer.stream.moveTo(44,writer.y-height);writer.stream.lineTo(551,writer.y-height);writer.stream.stroke();writer.y-=height;
   }
   if(writer.y<135)writer.page();writer.y-=22;
   writer.paragraph("Importes estimados a partir del consumo registrado y las tarifas del período. Consulta el estado de tus pagos en Plan y pagos.",9,507);
   writer.paragraph("HydroSmart local · Datos de consumo simulados.",9,507);writer.close();
   int pages=document.getNumberOfPages();
   for(int i=0;i<pages;i++)try(var footer=new PDPageContentStream(document,document.getPage(i),PDPageContentStream.AppendMode.APPEND,true,true)){
    footer.setNonStrokingColor(.36f,.46f,.43f);footer.beginText();footer.setFont(font,8);footer.newLineAtOffset(44,32);footer.showText("HydroSmart · "+(i+1)+" / "+pages);footer.endText();
   }
   document.save(bytes);return bytes.toByteArray();
  }catch(IOException error){throw new IllegalStateException("No se pudo generar el PDF.",error);}
 }
 private static final class PageWriter implements AutoCloseable {
  final PDDocument document;final PDType0Font font;PDPageContentStream stream;float y;
  PageWriter(PDDocument document,PDType0Font font){this.document=document;this.font=font;}
  void page()throws IOException{
   if(stream!=null)stream.close();var page=new PDPage(PDRectangle.A4);document.addPage(page);stream=new PDPageContentStream(document,page);
   stream.setNonStrokingColor(.06f,.25f,.26f);stream.addRect(0,766,595,76);stream.fill();stream.setNonStrokingColor(1f,1f,1f);raw(44,803,19,"HYDROSMART");raw(44,783,9,"Tu agua, bajo control.");y=736;
  }
  String safe(String value){var out=new StringBuilder();value.codePoints().forEach(cp->{String character=new String(Character.toChars(cp));try{font.encode(character);out.append(character);}catch(Exception error){out.append('?');}});return out.toString();}
  void raw(float x,float y,float size,String text)throws IOException{stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(x,y);stream.showText(safe(text));stream.endText();}
  void text(float x,float y,float size,String text)throws IOException{stream.setNonStrokingColor(.10f,.24f,.25f);raw(x,y,size,text);}
  void line(String text,float size)throws IOException{paragraph(text,size,507);}
  void paragraph(String text,float size,float width)throws IOException{for(String line:wrap(text,size,width)){if(y<75)page();text(44,y,size,line);y-=size+7;}}
  List<String> wrap(String value,float size,float width)throws IOException{
   var lines=new ArrayList<String>();var line=new StringBuilder();
   for(int cp:value.codePoints().toArray()){String next=new String(Character.toChars(cp));if(cp=='\n'||font.getStringWidth(safe(line+next))/1000*size>width){if(!line.isEmpty())lines.add(line.toString());line.setLength(0);if(cp=='\n')continue;}line.append(next);}
   if(!line.isEmpty())lines.add(line.toString());return lines.isEmpty()?List.of(""):lines;
  }
  void metric(float x,String label,String value)throws IOException{stream.setNonStrokingColor(.93f,.97f,.94f);stream.addRect(x,y-60,159,60);stream.fill();text(x+12,y-20,8,label);text(x+12,y-43,14,value);}
  void tableHead()throws IOException{stream.setNonStrokingColor(.92f,.96f,.94f);stream.addRect(44,y-25,507,25);stream.fill();text(50,y-16,9,"Fecha (Lima)");text(132,y-16,9,"Medidor");text(360,y-16,9,"Litros");text(477,y-16,9,"Lecturas");y-=25;}
  public void close()throws IOException{if(stream!=null)stream.close();}
 }
}
