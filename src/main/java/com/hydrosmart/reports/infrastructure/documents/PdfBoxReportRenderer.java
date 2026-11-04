package com.hydrosmart.reports.infrastructure.documents;

import com.hydrosmart.reports.application.internal.outboundservices.PdfReportRenderer;
import java.io.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

@Component
public class PdfBoxReportRenderer implements PdfReportRenderer {
 private static final ZoneId LIMA=ZoneId.of("America/Lima");
 private static final Locale LOCALE=Locale.forLanguageTag("es-PE");
 private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd MMM uuuu",LOCALE).withZone(LIMA);
 private static final float[] INK={.07f,.24f,.22f},MUTED={.37f,.46f,.43f},GREEN={.03f,.49f,.44f},PALE={.91f,.95f,.92f};

 @SuppressWarnings("unchecked") public byte[] render(Map<String,Object> job,Map<String,Object> data){
  try(var document=new PDDocument();var bytes=new ByteArrayOutputStream();var source=getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf")){
   if(source==null)throw new IOException("Report font missing");
   var font=PDType0Font.load(document,source,true);
   document.getDocumentInformation().setTitle("HydroSmart · Reporte de consumo de agua");
   document.getDocumentInformation().setAuthor("HydroSmart");
   var daily=(List<Map<String,Object>>)data.getOrDefault("daily",List.of());
   var estimate=(Map<String,Object>)data.getOrDefault("estimate",Map.of("total","0"));
   BigDecimal total=daily.stream().map(row->new BigDecimal(row.get("liters").toString())).reduce(BigDecimal.ZERO,BigDecimal::add);
   long meters=daily.stream().map(row->Objects.toString(row.getOrDefault("deviceId",row.get("deviceName")),"")).distinct().count();
   var writer=new PageWriter(document,font);
   writer.page();
   writer.paragraph("Tu consumo, en detalle.",25,INK,507);
   writer.y-=7;
   writer.paragraph(Objects.toString(data.get("unitName"),"Unidad seleccionada"),13,INK,507);
   writer.paragraph("Del "+DATE.format(Instant.parse(job.get("period_start").toString()))+" al "+DATE.format(Instant.parse(job.get("period_end").toString()).minusSeconds(1))+" · hora de Lima",9,MUTED,507);
   writer.y-=18;
   writer.metric(44,"CONSUMO DEL PERÍODO",number(total,3)+" L");
   writer.metric(218,"COSTO ESTIMADO","S/ "+money(estimate.get("total")));
   writer.metric(392,"MEDIDORES CON DATOS",Long.toString(meters));
   writer.y-=96;
   writer.chart(daily);
   writer.paragraph("Detalle diario por medidor",12,INK,507);writer.y-=4;writer.tableHead();
   if(daily.isEmpty())writer.paragraph("No hay lecturas registradas en este período.",10,MUTED,507);
   int index=0;
   for(var row:daily){
    var lines=writer.wrap(Objects.toString(row.getOrDefault("deviceName",row.get("deviceId")),"Medidor"),9,205);
    float height=Math.max(26,lines.size()*13+13);
    if(writer.y-height<85){writer.page();writer.paragraph("Detalle de consumo · continuación",13,INK,507);writer.y-=9;writer.tableHead();}
    if(index++%2==0)writer.rect(44,writer.y-height,507,height,new float[]{.96f,.98f,.96f});
    writer.text(52,writer.y-18,8.5f,DateTimeFormatter.ofPattern("dd MMM uuuu",LOCALE).format(LocalDate.parse(row.get("date").toString())),INK);
    for(int i=0;i<lines.size();i++)writer.text(142,writer.y-18-i*13,9,lines.get(i),INK);
    writer.right(463,writer.y-18,9,number(new BigDecimal(row.get("liters").toString()),3),INK);
    writer.right(542,writer.y-18,9,number(new BigDecimal(Objects.toString(row.get("readingCount"),"0")),0),INK);
    writer.y-=height;
   }
   if(writer.y<198)writer.page();
   writer.rect(44,writer.y-32,507,32,PALE);
   writer.text(53,writer.y-20,9,"TOTAL DEL PERÍODO",INK);writer.right(463,writer.y-20,10,number(total,3)+" L",INK);writer.y-=56;
   String tariff="Tarifa: "+Objects.toString(data.get("tariffName"),"Tarifas del período");
   if(writer.y-writer.wrap(tariff,9,507).size()*15<115)writer.page();
   writer.paragraph(tariff,9,MUTED,507);writer.y-=5;
   writer.paragraph("El importe es una estimación del período; no es un recibo ni confirma pagos. Consulta Plan y pagos para revisar su estado.",8,MUTED,507);
   writer.paragraph("Fuente: registros de HydroSmart · datos de consumo simulados · moneda PEN.",8,MUTED,507);
   writer.close();
   int pages=document.getNumberOfPages();
   for(int i=0;i<pages;i++)try(var footer=new PDPageContentStream(document,document.getPage(i),PDPageContentStream.AppendMode.APPEND,true,true)){
    footer.setStrokingColor(.83f,.88f,.85f);footer.setLineWidth(.5f);footer.moveTo(44,53);footer.lineTo(551,53);footer.stroke();
    footer.setNonStrokingColor(MUTED[0],MUTED[1],MUTED[2]);footer.beginText();footer.setFont(font,8);footer.newLineAtOffset(44,33);footer.showText("HydroSmart · Reporte de consumo");footer.endText();
    String page="Página "+(i+1)+" de "+pages;footer.beginText();footer.setFont(font,8);footer.newLineAtOffset(551-font.getStringWidth(page)/1000*8,33);footer.showText(page);footer.endText();
   }
   document.save(bytes);return bytes.toByteArray();
  }catch(IOException error){throw new IllegalStateException("No se pudo generar el PDF.",error);}
 }
 private static String number(BigDecimal value,int decimals){var format=NumberFormat.getNumberInstance(LOCALE);format.setMaximumFractionDigits(decimals);return format.format(value);}
 private static String money(Object value){var format=NumberFormat.getNumberInstance(LOCALE);format.setMinimumFractionDigits(2);format.setMaximumFractionDigits(2);return format.format(new BigDecimal(value.toString()));}

 private static final class PageWriter implements AutoCloseable {
  final PDDocument document;final PDType0Font font;PDPageContentStream stream;float y;
  PageWriter(PDDocument document,PDType0Font font){this.document=document;this.font=font;}
  void page()throws IOException{
   if(stream!=null)stream.close();var page=new PDPage(PDRectangle.A4);document.addPage(page);stream=new PDPageContentStream(document,page);
   text(44,798,17,"hydrosmart.",INK);right(551,800,8,"INFORME DE CONSUMO",MUTED);right(551,786,7,"DEMOSTRACIÓN LOCAL",MUTED);
   rect(44,768,507,2,GREEN);y=735;
  }
  String safe(String value){var out=new StringBuilder();value.codePoints().forEach(cp->{String character=new String(Character.toChars(cp));try{font.encode(character);out.append(character);}catch(Exception error){out.append('?');}});return out.toString();}
  void text(float x,float y,float size,String value,float[] color)throws IOException{stream.setNonStrokingColor(color[0],color[1],color[2]);stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(x,y);stream.showText(safe(value));stream.endText();}
  void right(float x,float y,float size,String value,float[] color)throws IOException{text(x-font.getStringWidth(safe(value))/1000*size,y,size,value,color);}
  void rect(float x,float y,float w,float h,float[] color)throws IOException{stream.setNonStrokingColor(color[0],color[1],color[2]);stream.addRect(x,y,w,h);stream.fill();}
  void paragraph(String value,float size,float[] color,float width)throws IOException{for(String line:wrap(value,size,width)){if(y<80)page();text(44,y,size,line,color);y-=size+6;}}
  List<String> wrap(String value,float size,float width)throws IOException{
   var lines=new ArrayList<String>();String line="";
   for(String word:value.replaceAll("[\\r\\n]+"," ").split("\\s+")){
    if(!line.isEmpty()&&font.getStringWidth(safe(line+" "+word))/1000*size>width){lines.add(line.stripTrailing());line="";}
    for(int cp:word.codePoints().toArray()){String next=new String(Character.toChars(cp));if(font.getStringWidth(safe(line+next))/1000*size>width&&!line.isEmpty()){lines.add(line);line="";}line+=next;}
    line+=" ";
   }
   if(!line.isBlank())lines.add(line.stripTrailing());return lines.isEmpty()?List.of(""):lines;
  }
  void metric(float x,String label,String value)throws IOException{
   rect(x,y-77,159,77,PALE);text(x+12,y-19,7,label,MUTED);
   float size=23;while(size>10&&font.getStringWidth(safe(value))/1000*size>135)size-=1;
   text(x+12,y-54,size,value,INK);
  }
  void tableHead()throws IOException{rect(44,y-26,507,26,INK);float[] white={1f,1f,1f};text(52,y-17,8,"FECHA (LIMA)",white);text(142,y-17,8,"MEDIDOR",white);right(463,y-17,8,"CONSUMO (L)",white);right(542,y-17,8,"LECTURAS",white);y-=26;}
  void chart(List<Map<String,Object>> rows)throws IOException{
   if(rows.isEmpty())return;
   var dates=new TreeMap<LocalDate,BigDecimal>();for(var row:rows)dates.merge(LocalDate.parse(row.get("date").toString()),new BigDecimal(row.get("liters").toString()),BigDecimal::add);
   boolean monthly=dates.firstKey().until(dates.lastKey(),java.time.temporal.ChronoUnit.DAYS)>31;
   var series=new TreeMap<LocalDate,BigDecimal>();dates.forEach((date,value)->series.merge(monthly?date.withDayOfMonth(1):date,value,BigDecimal::add));
   if(y<270)page();paragraph(monthly?"Evolución del consumo por mes":"Evolución del consumo por día",12,INK,507);
   float top=y-11,base=top-64,step=507f/series.size(),width=Math.min(44,step*.66f);BigDecimal max=series.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
   for(int tick=0;tick<3;tick++){float line=base+tick*30;rect(44,line,507,.5f,new float[]{.88f,.92f,.89f});}
   int index=0;for(var item:series.entrySet()){
    float h=max.signum()>0?item.getValue().divide(max,8,java.math.RoundingMode.HALF_UP).floatValue()*52:0;
    float x=44+index*step+(step-width)/2;rect(x,base,width,h,GREEN);
    if(series.size()<=8){String value=number(item.getValue(),1)+" L";float size=7;float center=x+width/2;text(center-font.getStringWidth(value)/1000*size/2,base+h+6,size,value,INK);}
    if(series.size()<=8||index==0||index==series.size()/2||index==series.size()-1){String label=DateTimeFormatter.ofPattern(monthly?"MMM yy":"dd MMM",LOCALE).format(item.getKey());float size=7;float center=x+width/2;text(center-font.getStringWidth(label)/1000*size/2,base-17,size,label,MUTED);}
    index++;
   }
   y=base-28;paragraph(monthly?"Cada barra suma el consumo registrado dentro de cada mes del período.":"Cada barra suma las lecturas de todos los medidores en esa fecha.",8,MUTED,507);y-=9;
  }
  public void close()throws IOException{if(stream!=null)stream.close();}
 }
}
