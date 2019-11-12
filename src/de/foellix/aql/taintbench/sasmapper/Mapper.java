package de.foellix.aql.taintbench.sasmapper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.handler.AnswerHandler;

public class Mapper {
	public static final String pathJson = "data/json/";
	public static final String pathXML = "data/answer/";
	public static final String sourceString = "_SOURCE_";
	public static final String sinkString = "_SINK_";
	public static String answerFile = "data/answer/";
	public static String appName = "";

	public static void main(String[] args) {
		final long startTime = System.currentTimeMillis();
		if (args.length == 1 && args[0].contains(".apk")) {
			generateXML(args[0]);
		} else if (args.length == 3 && args[0].equals("-c") && args[1].toLowerCase().equals("fd")
				&& args[2].contains(".xml")) {
			generateText(args);
		} else {
			System.out.println("Invalid Command Option! Either input .apk or -c Fd answer.xml");
		}
		System.out.println(System.currentTimeMillis() - startTime);
	}

	public static void generateText(String[] args) {
		appName = args[2].substring(0, args[2].indexOf(".xml"));
		answerFile = answerFile + "SourcesAndSinks_" + appName + ".txt";
		String answer = "";
		String stmt;
		try {
			if (appName == null || appName == "") {
				return;
			}
			final File answerXml = new File(pathXML + appName + ".xml");

			// TODO: Instead of Dom please use the AQL Parser (from AQL-System project - see next line)
			final Answer aqlAnswer = AnswerHandler.parseXML(answerXml);
			// TODO: Do the same as you did but with aqlAnswer

			// FIXME: The output contain unwanted linebreaks etc.
			// Try to adapt to the implementation in the BREW project (https://git.cs.upb.de/fpauck/BREW -> de.foellix.aql.ggwiz.Exporter -> exportSourcesAndSinks -- Jawa is the intermediate representation (IR) language of Amandroid and Jimple is the IR of FlowDroid)

			final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			final Document doc = documentBuilder.parse(answerXml);
			doc.getDocumentElement().normalize();
			NodeList list = doc.getElementsByTagName("intentsource"); // FIXME: Should be "source"
			for (int i = 0; i < list.getLength(); i++) {
				final Node n = list.item(i);
				stmt = extractStmt(n);
				stmt = stmt.concat(" -> " + sourceString);
				answer = answer.concat(stmt).concat("\n");
			}
			list = doc.getElementsByTagName("intentsink"); // FIXME: Should be "sink"
			for (int i = 0; i < list.getLength(); i++) {
				final Node n = list.item(i);
				stmt = extractStmt(n);
				stmt = stmt.concat(" -> " + sinkString);
				answer = answer.concat(stmt).concat("\n");
			}
			if (answer != "" || answer != null) {
				final FileWriter writer = new FileWriter(answerFile);
				writer.write(answer);
				writer.close();
			}

		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static String extractStmt(Node n) {
		String jimpleStmt;
		int firstIndex;
		int lastIndex;
		String stmt = "";
		if (n.getNodeType() == Node.ELEMENT_NODE) {
			final Element eElement = (Element) n;
			jimpleStmt = eElement.getElementsByTagName("jimpleStmt").item(0).getTextContent();
			firstIndex = jimpleStmt.indexOf("<");
			lastIndex = jimpleStmt.lastIndexOf(">");
			stmt = jimpleStmt.substring(firstIndex, lastIndex + 1);

		}
		return stmt;
	}

	public static void generateXML(String app) {
		// TODO: Use the AQL-System project classes here as well:
		final Answer aqlAnswer = new Answer();
		// And then continue like this: aqlAnswer.setSources(...);
		// Also consider using: Helper.createApp(...) and Helper.createStatement(...) etc.
		// To write the actual XML file, please use AnswerHandler: AnswerHandler.createXML(aqlAnswer, xmlFile);

		// FIXME: Additionally Sources and Sinks have to be represented in AQL-Format: https://github.com/FoelliX/AQL-System/wiki/Answers
		// However, this should become clear once you use the actual AQL-System classes
		// FIXME: Use Sources and Sinks instead of IntentSource and IntentSinks
		// Therefore you have to use the up-to-date version of the AQL project from gitlab (develop branch)

		appName = app.substring(0, app.indexOf(".apk"));
		answerFile = answerFile + appName + ".xml";
		final String findings = app.trim().replace(".apk", "").concat("_findings.json");
		String answer = "";
		String sinks = "";
		String sources = "";

		try {
			final FileReader findingsJson = new FileReader(pathJson.concat(findings));
			final JSONTokener token = new JSONTokener(findingsJson);
			final JSONObject obj = new JSONObject(token);
			if (obj.get("findings").toString() == null || obj.get("findings").toString().equals("")) {
				System.out.println("No findings found!");
			} else {
				final JSONArray allFindings = (JSONArray) obj.get("findings");
				for (int i = 0; i < allFindings.length(); i++) {
					final JSONObject object = (JSONObject) allFindings.get(i);
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
				final FileWriter writer = new FileWriter(answerFile);
				writer.write(answer);
				writer.close();

			}
		} catch (final JSONException e) {
			e.printStackTrace();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
