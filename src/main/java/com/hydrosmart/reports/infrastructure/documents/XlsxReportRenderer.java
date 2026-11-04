package com.hydrosmart.reports.infrastructure.documents;

import com.hydrosmart.reports.application.internal.outboundservices.SpreadsheetReportRenderer;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.zip.*;
import org.springframework.stereotype.Component;

@Component
public class XlsxReportRenderer implements SpreadsheetReportRenderer {
 private static final String NS="http://schemas.openxmlformats.org/spreadsheetml/2006/main";
 private static final String REL="http://schemas.openxmlformats.org/officeDocument/2006/relationships";
 private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd MMM uuuu",Locale.forLanguageTag("es-PE")).withZone(ZoneId.of("America/Lima"));

 @SuppressWarnings("unchecked") public byte[] render(Map<String,Object> job,Map<String,Object> data){
  var daily=(List<Map<String,Object>>)data.getOrDefault("daily",List.of());
  var estimate=(Map<String,Object>)data.getOrDefault("estimate",Map.of("total","0"));
  BigDecimal total=daily.stream().map(row->decimal(row.get("liters"))).reduce(BigDecimal.ZERO,BigDecimal::add);
  BigDecimal readings=daily.stream().map(row->decimal(row.getOrDefault("readingCount",0))).reduce(BigDecimal.ZERO,BigDecimal::add);
  long meters=daily.stream().map(row->Objects.toString(row.getOrDefault("deviceId",row.get("deviceName")),"")).distinct().count();
  int last=13+Math.max(1,daily.size()),totals=last+1;
  var sheet=new StringBuilder("<worksheet xmlns=\""+NS+"\" xmlns:r=\""+REL+"\"><sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>");
  sheet.append("<dimension ref=\"A1:G").append(totals+5).append("\"/><sheetViews><sheetView showGridLines=\"0\" zoomScale=\"90\" workbookViewId=\"0\"><pane ySplit=\"13\" topLeftCell=\"B14\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><sheetFormatPr defaultRowHeight=\"20\"/>");
  sheet.append("<cols><col min=\"1\" max=\"1\" width=\"3\" customWidth=\"1\"/><col min=\"2\" max=\"2\" width=\"17\" customWidth=\"1\"/><col min=\"3\" max=\"3\" width=\"35\" customWidth=\"1\"/><col min=\"4\" max=\"5\" width=\"18\" customWidth=\"1\"/><col min=\"6\" max=\"6\" width=\"14\" customWidth=\"1\"/><col min=\"7\" max=\"7\" width=\"3\" customWidth=\"1\"/></cols><sheetData>");
  row(sheet,2,27,text("B2","HYDROSMART / TU AGUA, EN CLARO",1)+blanks(2,1));
  row(sheet,3,38,text("B3","Reporte de consumo de agua",2)+blanks(3,2));
  row(sheet,5,34,text("B5",Objects.toString(data.get("unitName"),"Unidad seleccionada"),4));
  row(sheet,6,24,text("B6","Del "+DATE.format(Instant.parse(job.get("period_start").toString()))+" al "+DATE.format(Instant.parse(job.get("period_end").toString()).minusSeconds(1))+" · hora de Lima",3));
  row(sheet,8,25,text("B8","CONSUMO DEL PERÍODO",5)+blank("C8",5)+text("D8","MEDIDORES",5)+text("E8","COSTO ESTIMADO",5)+blank("F8",5));
  row(sheet,9,31,formula("B9","SUM(D14:D"+last+")",total,6)+blank("C9",6)+number("D9",BigDecimal.valueOf(meters),8)+number("E9",decimal(estimate.get("total")),7)+blank("F9",7));
  row(sheet,10,12,blank("B10",6)+blank("C10",6)+blank("D10",8)+blank("E10",7)+blank("F10",7));
  row(sheet,12,28,text("B12","Detalle diario por medidor",4));
  String[] headers={"Fecha (Lima)","Medidor","Consumo (L)","Consumo (m³)","Lecturas"};
  StringBuilder heading=new StringBuilder();for(int i=0;i<headers.length;i++)heading.append(text(column(i)+"13",headers[i],9));row(sheet,13,30,heading.toString());
  for(int i=0;i<daily.size();i++){
   var item=daily.get(i);int r=14+i,offset=i%2==0?0:5;var liters=decimal(item.get("liters"));
   long date=ChronoUnit.DAYS.between(LocalDate.of(1899,12,30),LocalDate.parse(item.get("date").toString()));
   String name=Objects.toString(item.getOrDefault("deviceName",item.get("deviceId")),"Medidor");
   row(sheet,r,name.length()>35?40:25,number("B"+r,BigDecimal.valueOf(date),10+offset)+text("C"+r,name,11+offset)+number("D"+r,liters,12+offset)+number("E"+r,liters.movePointLeft(3),13+offset)+number("F"+r,decimal(item.getOrDefault("readingCount",0)),14+offset));
  }
  if(daily.isEmpty())row(sheet,14,30,text("C14","Sin lecturas en este período.",11));
  row(sheet,totals,30,text("B"+totals,"TOTAL",21)+text("C"+totals,daily.size()+" registros diarios",21)+formula("D"+totals,"SUM(D14:D"+last+")",total,20)+formula("E"+totals,"SUM(E14:E"+last+")",total.movePointLeft(3),22)+formula("F"+totals,"SUM(F14:F"+last+")",readings,23));
  row(sheet,totals+2,32,text("B"+(totals+2),"Tarifa: "+Objects.toString(data.get("tariffName"),"Tarifas del período"),3));
  row(sheet,totals+3,36,text("B"+(totals+3),"El importe es una estimación del período seleccionado; no es un recibo ni confirma pagos. Consulta Plan y pagos para ver su estado.",3));
  row(sheet,totals+4,25,text("B"+(totals+4),"Fuente: registros de HydroSmart · datos de consumo simulados · moneda PEN.",3));
  sheet.append("</sheetData><autoFilter ref=\"B13:F").append(last).append("\"/><mergeCells count=\"13\">");
  for(String range:List.of("B2:F2","B3:F3","B5:F5","B6:F6","B8:C8","E8:F8","B9:C10","D9:D10","E9:F10","B12:F12","B"+(totals+2)+":F"+(totals+2),"B"+(totals+3)+":F"+(totals+3),"B"+(totals+4)+":F"+(totals+4)))sheet.append("<mergeCell ref=\"").append(range).append("\"/>");
  sheet.append("</mergeCells><printOptions horizontalCentered=\"1\"/><pageMargins left=\"0.3\" right=\"0.3\" top=\"0.4\" bottom=\"0.4\" header=\"0.2\" footer=\"0.2\"/><pageSetup paperSize=\"9\" orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/><headerFooter><oddFooter>&amp;LHydroSmart · consumo de agua&amp;RPágina &amp;P de &amp;N</oddFooter></headerFooter></worksheet>");
  try(var output=new ByteArrayOutputStream();var zip=new ZipOutputStream(output,StandardCharsets.UTF_8)){
   part(zip,"[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>");
   part(zip,"_rels/.rels",relationships("<Relationship Id=\"rId1\" Type=\""+REL+"/officeDocument\" Target=\"xl/workbook.xml\"/>"));
   part(zip,"xl/workbook.xml","<workbook xmlns=\""+NS+"\" xmlns:r=\""+REL+"\"><bookViews><workbookView/></bookViews><sheets><sheet name=\"Consumo\" sheetId=\"1\" r:id=\"rId1\"/></sheets><definedNames><definedName name=\"_xlnm.Print_Area\" localSheetId=\"0\">Consumo!$B$2:$F$"+(totals+4)+"</definedName><definedName name=\"_xlnm.Print_Titles\" localSheetId=\"0\">Consumo!$13:$13</definedName></definedNames><calcPr calcId=\"191029\" fullCalcOnLoad=\"1\"/></workbook>");
   part(zip,"xl/_rels/workbook.xml.rels",relationships("<Relationship Id=\"rId1\" Type=\""+REL+"/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\""+REL+"/styles\" Target=\"styles.xml\"/>"));
   part(zip,"xl/worksheets/sheet1.xml",sheet.toString());part(zip,"xl/styles.xml",styles());
   zip.finish();return output.toByteArray();
  }catch(IOException error){throw new IllegalStateException("No se pudo generar el archivo Excel.",error);}
 }
 private static String styles(){
  var fonts=new StringBuilder();String[][] specs={{"10","163B39",""},{"11","FFFFFF","b"},{"18","FFFFFF","b"},{"10","61766E",""},{"11","163B39","b"},{"20","163B39","b"},{"10","FFFFFF","b"},{"10","163B39","b"}};
  for(var spec:specs)fonts.append("<font><sz val=\"").append(spec[0]).append("\"/><color rgb=\"FF").append(spec[1]).append("\"/><name val=\"Arial\"/>").append(spec[2].isEmpty()?"":"<b/>").append("</font>");
  var formats=new StringBuilder();int[][] styles={{0,0,0},{1,2,0},{2,2,0},{3,0,0},{4,0,0},{3,3,0},{5,3,168},{5,3,166},{5,3,1},{6,2,0},{0,0,164},{0,0,0},{0,0,165},{0,0,167},{0,0,1},{0,4,164},{0,4,0},{0,4,165},{0,4,167},{0,4,1},{7,3,165},{7,3,0},{7,3,167},{7,3,1}};
  for(int i=0;i<styles.length;i++){var s=styles[i];String align=i>=5&&i<=9?"center":s[2]!=0&&s[2]!=164?"right":"left";formats.append("<xf numFmtId=\"").append(s[2]).append("\" fontId=\"").append(s[0]).append("\" fillId=\"").append(s[1]).append("\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyNumberFormat=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\" horizontal=\"").append(align).append("\"").append(align.equals("left")?" indent=\"1\"":"").append(" wrapText=\"1\"/></xf>");}
  return "<styleSheet xmlns=\""+NS+"\"><numFmts count=\"5\"><numFmt numFmtId=\"164\" formatCode=\"dd mmm yyyy\"/><numFmt numFmtId=\"165\" formatCode=\"#,##0.000\"/><numFmt numFmtId=\"166\" formatCode=\"&quot;S/ &quot;#,##0.00\"/><numFmt numFmtId=\"167\" formatCode=\"#,##0.000000\"/><numFmt numFmtId=\"168\" formatCode=\"#,##0.000&quot; L&quot;\"/></numFmts><fonts count=\"8\">"+fonts+"</fonts><fills count=\"5\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF10413C\"/></patternFill></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE7F1E9\"/></patternFill></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF4F8F5\"/></patternFill></fill></fills><borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"24\">"+formats+"</cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>";
 }
 private static String column(int index){return Character.toString((char)('B'+index));}
 private static BigDecimal decimal(Object value){return new BigDecimal(Objects.toString(value,"0"));}
 private static String blanks(int row,int style){StringBuilder out=new StringBuilder();for(char c='C';c<='F';c++)out.append(blank(c+Integer.toString(row),style));return out.toString();}
 private static String blank(String address,int style){return "<c r=\""+address+"\" s=\""+style+"\"/>";}
 private static String text(String address,String value,int style){return "<c r=\""+address+"\" s=\""+style+"\" t=\"inlineStr\"><is><t xml:space=\"preserve\">"+xml(value)+"</t></is></c>";}
 private static String number(String address,BigDecimal value,int style){return "<c r=\""+address+"\" s=\""+style+"\"><v>"+value.toPlainString()+"</v></c>";}
 private static String formula(String address,String formula,BigDecimal value,int style){return "<c r=\""+address+"\" s=\""+style+"\"><f>"+xml(formula)+"</f><v>"+value.toPlainString()+"</v></c>";}
 private static void row(StringBuilder out,int index,int height,String cells){out.append("<row r=\"").append(index).append("\" ht=\"").append(height).append("\" customHeight=\"1\">").append(cells).append("</row>");}
 private static String relationships(String content){return "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"+content+"</Relationships>";}
 private static void part(ZipOutputStream zip,String name,String content)throws IOException{zip.putNextEntry(new ZipEntry(name));zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"+content).getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
 private static String xml(String value){var out=new StringBuilder();value.codePoints().filter(cp->cp==9||cp==10||cp==13||cp>=32&&cp<=0xD7FF||cp>=0xE000&&cp<=0xFFFD||cp>=0x10000&&cp<=0x10FFFF).forEach(out::appendCodePoint);return out.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
}
