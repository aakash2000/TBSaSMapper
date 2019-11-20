package de.foellix.aql.taintbench.sasmapper;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.foellix.aql.datastructure.*;
import de.foellix.aql.datastructure.Sink;
import de.foellix.aql.datastructure.Source;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.helper.JawaHelper;
import de.foellix.aql.ggwiz.taintbench.datastructure.*;

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
        final Answer aqlAnswer = new Answer();
        appName = app.substring(0, app.indexOf(".apk"));
        answerFile = answerFile + appName + ".xml";
        final String findings = app.trim().replace(".apk", "").concat("_findings.json");
        App appObj = Helper.createApp(app);
        Sources sources = new Sources();
        Sinks sinks = new Sinks();
        try {
            final File jsonfile = new File(pathJson.concat(findings));
            final ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            final TaintBenchCase taintBenchCase = mapper.readValue(jsonfile,TaintBenchCase.class);

            if (taintBenchCase.getFindings().isEmpty() || taintBenchCase.getFindings().size() < 1) {
                System.out.println("No findings found!");
            } else {
                final List<Finding> findingsList = taintBenchCase.getFindings();
                for (int i = 0; i < findingsList.size(); i++) {
                    Finding finding = findingsList.get(i);
                    if (finding.getSink() == null) {
                        System.out.println("No Intent Sink found");
                    } else {
                        Sink sink = new Sink();
                        Reference reference = new Reference();
                        Statement statement = new Statement();
                        reference.setApp(appObj);
                        if (!finding.getSink().getClassName().isEmpty() && finding.getSink().getClassName() != null){
                            //Extracting ClassName
                            reference.setClassname(finding.getSink().getClassName());
                        }
                        if (!finding.getSink().getJimpleStmt().isEmpty() && finding.getSink().getJimpleStmt() != null && finding.getSink().getLineNo() != null){
                            //Extracting JimpleStmt and lineNo
                            statement = Helper.createStatement(finding.getSink().getJimpleStmt(),finding.getSink().getLineNo());
                        }else if (!finding.getSink().getJimpleStmt().isEmpty() && finding.getSink().getJimpleStmt() != null){
                            //Extracting JimpleStmt
                            statement = Helper.createStatement(finding.getSink().getJimpleStmt());
                        }
                        if (finding.getSink().getMethodName() != null && !finding.getSink().getMethodName().isEmpty()){
                            //Extracting methodName
                            reference.setMethod(finding.getSink().getMethodName());
                        }
                        reference.setStatement(statement);
                        sink.getReference().add(reference);
                        sinks.getSink().add(sink);

                    }
                    if (finding.getSource() == null) {
                        System.out.println("No Intent Source found");
                    } else {

                        Source source = new Source();
                        Reference reference = new Reference();
                        Statement statement = new Statement();
                        reference.setApp(appObj);
                        if (finding.getSource().getClassName() != null && !finding.getSource().getClassName().isEmpty()){
                            //Extracting ClassName
                            reference.setClassname(finding.getSource().getClassName());
                        }
                        if (!finding.getSource().getJimpleStmt().isEmpty() && finding.getSource().getJimpleStmt() != null && finding.getSource().getLineNo() != null){
                            //Extracting JimpleStmt and lineNo
                            statement = Helper.createStatement(finding.getSource().getJimpleStmt(),finding.getSource().getLineNo());
                        }else if (!finding.getSource().getJimpleStmt().isEmpty() && finding.getSource().getJimpleStmt() != null){
                            //Extracting JimpleStmt
                            statement = Helper.createStatement(finding.getSource().getJimpleStmt());
                        }
                        if (finding.getSource().getMethodName() != null && !finding.getSource().getMethodName().isEmpty()){
                            //Extracting methodName
                            reference.setMethod(finding.getSource().getMethodName());
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
			final Answer aqlAnswer = AnswerHandler.parseXML(answerXml);
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
		exportToFile(sourcesAndSinksToExport);

	}

    private static void exportSourceAndSinksJawa(Answer aqlAnswer){
		final Set<String> sourcesAndSinksToExport = new HashSet<>();
		for (Source source : aqlAnswer.getSources().getSource()){
			for (Reference reference : source.getReference()){
				sourcesAndSinksToExport.add(JawaHelper.toJawa(reference.getStatement()) + " SENSITIVE_INFO -> "
						+ sourceString);
			}
		}
		buildSinkJawa(aqlAnswer,sourcesAndSinksToExport);
	}


    private static void buildSinkJawa(Answer answer, Set<String> sourcesAndSinksToExport){
		final Sinks sinks = answer.getSinks();
		StringBuilder attachment;
		for (Sink sink : sinks.getSink()) {
			attachment = new StringBuilder();
			for (Reference reference : sink.getReference()) {
				if (reference.getStatement().getParameters() != null && !reference.getStatement().getParameters().getParameter().isEmpty()) {
					attachment.append(" ");
					for (int i = 0; i < reference.getStatement().getParameters().getParameter().size(); i++) {
						attachment.append(attachment.length() == 1 ? "" : "|").append(i);
					}
				}
				sourcesAndSinksToExport.add(JawaHelper.toJawa(reference.getStatement()) + " -> "
						+ sinkString + attachment.toString());
			}
		}
		exportToFile(sourcesAndSinksToExport);
	}

    private static void exportToFile(Set<String> sourcesAndSinksToExport){
		final List<String> sourcesAndSinksToExportSorted = new ArrayList<>(sourcesAndSinksToExport);
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
