package de.foellix.aql.taintbench.sasmapper;

import java.io.*;
import java.util.*;

import de.foellix.aql.datastructure.*;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.helper.JawaHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


public class Mapper {
	private static final String pathJson = "data/json/";
    private static final String pathXML = "data/answer/";
    private static final String sourceString = "_SOURCE_";
    private static final String sinkString = "_SINK_";
    private static final String flowDroid = "fd";
    private static final String amanDroid = "ad";
    private static String answerFile = "data/answer/";
    private static String appName = "";

	public static void main(String[] args) {
		final long startTime = System.currentTimeMillis();
		if (args.length == 1 && args[0].contains(".apk")) {
		    //export AQL answer
			exportAQLAnswer(args[0]);
		} else if (args.length == 3 && args[0].equals("-c")
				&& (args[1].toLowerCase().equals(flowDroid) || args[1].toLowerCase().equals(amanDroid))
				&& args[2].contains(".xml")) {
		    //export Source and Sinks
			exportSourceAndSinks(args);
		} else {
			System.out.println("Invalid Command Option! Either input .apk or -c Fd answer.xml");
		}
		System.out.println(System.currentTimeMillis() - startTime);
	}

    private static void exportAQLAnswer(String app) {
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
        App appObj = Helper.createApp(app);
        Sources sources = new Sources();
        Sinks sinks = new Sinks();
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

                        Sink sink = new Sink();
                        Reference reference = new Reference();
                        Statement statement = new Statement();
                        reference.setApp(appObj);
                        if (((JSONObject) object.get("sink")).get("className") != null){
                            //Extracting ClassName
                            reference.setClassname(((JSONObject) object.get("sink")).get("className").toString());
                        }
                        if (((JSONObject) object.get("sink")).get("jimpleStmt") != null && ((JSONObject) object.get("sink")).get("lineNo") != null){
                            //Extracting JimpleStmt and lineNo
                            statement = Helper.createStatement(((JSONObject) object.get("sink")).get("jimpleStmt").toString(),Integer.parseInt(((JSONObject) object.get("sink")).get("lineNo").toString()));
                        }else if (((JSONObject) object.get("sink")).get("jimpleStmt") != null){
                            //Extracting JimpleStmt
                            statement = Helper.createStatement(((JSONObject) object.get("sink")).get("jimpleStmt").toString());
                        }
                        if (((JSONObject) object.get("sink")).get("methodName") != null){
                            //Extracting methodName
                            reference.setMethod(((JSONObject) object.get("sink")).get("methodName").toString());
                        }
                        if (((JSONObject) object.get("sink")).get("targetName") != null){
                            //Extracting targetName
                            reference.setType(((JSONObject) object.get("sink")).get("targetName").toString());
                        }
                        reference.setStatement(statement);
                        sink.getReference().add(reference);
                        sinks.getSink().add(sink);

                    }
                    if (object.get("source").toString() == null || object.get("source").toString().equals("")) {
                        System.out.println("No Intent Source found");
                    } else {

                        Source source = new Source();
                        Reference reference = new Reference();
                        Statement statement = new Statement();
                        reference.setApp(appObj);
                        if (((JSONObject) object.get("source")).get("className") != null){
                            //Extracting ClassName
                            reference.setClassname(((JSONObject) object.get("source")).get("className").toString());
                        }
                        if (((JSONObject) object.get("source")).get("jimpleStmt") != null && ((JSONObject) object.get("source")).get("lineNo") != null){
                            //Extracting JimpleStmt and lineNo
                            statement = Helper.createStatement(((JSONObject) object.get("source")).get("jimpleStmt").toString(),Integer.parseInt(((JSONObject) object.get("source")).get("lineNo").toString()));
                        }else if (((JSONObject) object.get("source")).get("jimpleStmt") != null){
                            //Extracting JimpleStmt
                            statement = Helper.createStatement(((JSONObject) object.get("source")).get("jimpleStmt").toString());
                        }
                        if (((JSONObject) object.get("source")).get("methodName") != null){
                            //Extracting methodName
                            reference.setMethod(((JSONObject) object.get("source")).get("methodName").toString());
                        }
                        if (((JSONObject) object.get("source")).get("targetName") != null){
                            //Extracting targetName
                            reference.setType(((JSONObject) object.get("source")).get("targetName").toString());
                        }
                        reference.setStatement(statement);
                        source.getReference().add(reference);
                        sources.getSource().add(source);
                    }
                }
                aqlAnswer.setSinks(sinks);
                aqlAnswer.setSources(sources);
                File file = new File(answerFile);
                AnswerHandler.createXML(aqlAnswer,file);
            }
        } catch (final JSONException e) {
            e.printStackTrace();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

	private static void exportSourceAndSinks(String[] args) {
		appName = args[2].substring(0, args[2].indexOf(".xml"));
		answerFile = answerFile + "SourcesAndSinks[" + args[1].toLowerCase() + "]_" + appName + ".txt";
		try {
			if (appName.equals("") || appName.isEmpty()) {
			    System.out.println("Invalid XML file");
				return;
			}
			final File answerXml = new File(pathXML + appName + ".xml");

			// TODO: Instead of Dom please use the AQL Parser (from AQL-System project - see next line)
			final Answer aqlAnswer = AnswerHandler.parseXML(answerXml);
			// TODO: Do the same as you did but with aqlAnswer

			// FIXME: The output contain unwanted linebreaks etc.
			// Try to adapt to the implementation in the BREW project (https://git.cs.upb.de/fpauck/BREW -> de.foellix.aql.ggwiz.Exporter -> exportSourcesAndSinks -- Jawa is the intermediate representation (IR) language of Amandroid and Jimple is the IR of FlowDroid)
            if (aqlAnswer == null){
                System.out.println("Invalid AQL Answer");
                return;
            }
			if (args[1].toLowerCase().equals(flowDroid)){
			    //FlowDroid IR generation
				exportSourceAndSinksJimple(aqlAnswer);
			}else if (args[1].toLowerCase().equals(amanDroid)){
			    //AmanDroid IR generation
				exportSourceAndSinksJawa(aqlAnswer);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

    private static void exportSourceAndSinksJimple(Answer aqlAnswer){
		final Set<String> sourcesAndSinksToExport = new HashSet<>();
		for (Source ss : aqlAnswer.getSources().getSource()){
			for	(Reference ref : ss.getReference()){
				sourcesAndSinksToExport.add("<"+ref.getStatement().getStatementgeneric()+"> -> "+ sourceString);
			}
		}
		for (Sink sk : aqlAnswer.getSinks().getSink()){
			for (Reference ref : sk.getReference()){
				sourcesAndSinksToExport.add("<"+ref.getStatement().getStatementgeneric()+"> -> "+ sinkString);
			}
		}
		exportToFile(sourcesAndSinksToExport, flowDroid);

	}

    private static void exportSourceAndSinksJawa(Answer aqlAnswer){
		final Set<String> sourcesAndSinksToExport = new HashSet<>();
		for (Source source : aqlAnswer.getSources().getSource()){
			for (Reference reference : source.getReference()){
				sourcesAndSinksToExport.add(JawaHelper.toJawa(reference.getStatement()) + " SENSITIVE_INFO -> "
						+ sourceString);
			}
		}
		buildSinkJawa(aqlAnswer.getSinks(),sourcesAndSinksToExport);
	}


    private static void buildSinkJawa(Sinks sinks, Set<String> sourcesAndSinksToExport){
		final StringBuilder attachment = new StringBuilder();
		final Set<String> sinksToExport = new HashSet<>();
		for (Sink sink : sinks.getSink()) {
			for (Reference reference : sink.getReference()) {
				if (reference.getStatement().getParameters() != null && !reference.getStatement().getParameters().getParameter().isEmpty()) {
					attachment.append(" ");
					for (int i = 0; i < reference.getStatement().getParameters().getParameter().size(); i++) {
						attachment.append((attachment.length() == 1 ? "" : "|") + i);
					}
				}
				sourcesAndSinksToExport.add(JawaHelper.toJawa(reference.getStatement()) + " -> "
						+ sinkString + attachment.toString());
			}
		}
		exportToFile(sourcesAndSinksToExport, amanDroid);
	}

    private static void exportToFile(Set<String> sourcesAndSinksToExport, String format){
		final List<String> sourcesAndSinksToExportSorted = new ArrayList<String>(sourcesAndSinksToExport);
		Collections.sort(sourcesAndSinksToExportSorted);
		try {
			FileWriter fileWriter = new FileWriter(answerFile);
            //Source
			for (final String s : sourcesAndSinksToExportSorted) {
				if (s.endsWith(sourceString)
						|| s.substring(0, s.lastIndexOf(' ')).endsWith(sourceString)) {
					fileWriter.write(s + "\n");
				}
			}

			fileWriter.write("\n");

			// Sinks
			for (final String s : sourcesAndSinksToExportSorted) {
				if (s.endsWith(sinkString)
						|| s.substring(0, s.lastIndexOf(' ')).endsWith(sinkString)) {
					fileWriter.write(s + "\n");
				}
			}
			fileWriter.close();

		}catch (final IOException e){
			e.printStackTrace();
		}
	}
}
