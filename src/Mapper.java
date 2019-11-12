import org.json.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Mapper {
    public static final String pathJson = "data/json/";
    public static final String pathXML = "data/answer/";
    public static final String sourceString   = "_SOURCE_";
    public static final String sinkString   = "_SINK_";
    public static String answerFile = "data/answer/";
    public static String appName = "";

    public static void main(String[] args){
        long startTime = System.currentTimeMillis();
        if(args.length == 1 && args[0].contains(".apk")) {
            generateXML(args[0]);
        }else if(args.length == 3 && args[0].equals("-c") && args[1].toLowerCase().equals("fd") && args[2].contains(".xml")){
            generateText(args);
        }else {
            System.out.println("Invalid Command Option! Either input .apk or -c Fd answer.xml");
        }
        System.out.println(System.currentTimeMillis()-startTime);
    }

    public static void generateText(String[] args){
        appName = args[2].substring(0,args[2].indexOf(".xml"));
        answerFile = answerFile+ "SourcesAndSinks_" + appName + ".txt";
        String answer = "";
        String stmt;
        try {
            if (appName == null || appName == ""){
                return;
            }
            File answerXml = new File(pathXML+appName+".xml");
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(answerXml);
            doc.getDocumentElement().normalize();
            NodeList list = doc.getElementsByTagName("intentsource");
            for (int i = 0; i < list.getLength(); i++){
                Node n = list.item(i);
                stmt = extractStmt(n);
                stmt = stmt.concat(" -> "+sourceString);
                answer = answer.concat(stmt).concat("\n");
            }
            list = doc.getElementsByTagName("intentsink");
            for (int i = 0; i < list.getLength(); i++){
                Node n = list.item(i);
                stmt = extractStmt(n);
                stmt = stmt.concat(" -> "+sinkString);
                answer = answer.concat(stmt).concat("\n");
            }
            if (answer != "" || answer != null){
                FileWriter writer = new FileWriter(answerFile);
                writer.write(answer);
                writer.close();
            }

        }catch (IOException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String extractStmt(Node n){
        String jimpleStmt;
        int firstIndex;
        int lastIndex;
        String stmt = "";
        if(n.getNodeType() == Node.ELEMENT_NODE){
            Element eElement = (Element) n;
            jimpleStmt = eElement.getElementsByTagName("jimpleStmt").item(0).getTextContent();
            firstIndex = jimpleStmt.indexOf("<");
            lastIndex = jimpleStmt.lastIndexOf(">");
            stmt = jimpleStmt.substring(firstIndex,lastIndex+1);

        }
        return stmt;
    }

    public static void generateXML(String app){
        appName = app.substring(0, app.indexOf(".apk"));
        answerFile = answerFile + appName + ".xml";
        String findings = app.trim().replace(".apk", "").concat("_findings.json");
        String answer = "";
        String sinks = "";
        String sources = "";

        try {
            FileReader findingsJson = new FileReader(pathJson.concat(findings));
            JSONTokener token = new JSONTokener(findingsJson);
            JSONObject obj = new JSONObject(token);
            if (obj.get("findings").toString() == null || obj.get("findings").toString().equals("")) {
                System.out.println("No findings found!");
            } else {
                JSONArray allFindings = (JSONArray) obj.get("findings");
                for (int i = 0; i < allFindings.length(); i++) {
                    JSONObject object = (JSONObject) allFindings.get(i);
                    if (object.get("sink").toString() == null || object.get("sink").toString().equals("")) {
                        System.out.println("No Intent Sink found");
                    } else {
                        sinks = sinks.concat(XML.toString(object.get("sink"), "intentsink"));
                    }
                    if (object.get("source").toString() == null || object.get("source").toString().equals("")) {
                        System.out.println("No Intent Source found");
                    } else {
                        sources = sources.concat(XML.toString(object.get("source"), "intentsource"));
                    }
                }
                sources = "<intentsources>" + sources.concat("</intentsources>");
                sinks = "<intentsinks>" + sinks.concat("</intentsinks>");
                answer = answer.concat(sources).concat(sinks);
                answer = "<answer>" + answer.concat("</answer>");
                FileWriter writer = new FileWriter(answerFile);
                writer.write(answer);
                writer.close();

            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
