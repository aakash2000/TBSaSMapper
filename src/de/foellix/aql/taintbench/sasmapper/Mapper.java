package de.foellix.aql.taintbench.sasmapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.App;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.datastructure.Sink;
import de.foellix.aql.datastructure.Sinks;
import de.foellix.aql.datastructure.Source;
import de.foellix.aql.datastructure.Sources;
import de.foellix.aql.datastructure.Statement;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.ggwiz.taintbench.datastructure.Finding;
import de.foellix.aql.ggwiz.taintbench.datastructure.TaintBenchCase;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.helper.JawaHelper;

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
			// export AQL answer
			exportAQLAnswer(args[0]);
		} else if (args.length == 3 && args[0].equals("-c")
				&& (args[1].toLowerCase().equals(flowDroid) || args[1].toLowerCase().equals(amanDroid))
				&& args[2].contains(".xml")) {
			// export Source and Sinks
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
		final App appObj = Helper.createApp(app);
		final Sources sources = new Sources();
		final Sinks sinks = new Sinks();
		try {
			final File jsonfile = new File(pathJson.concat(findings));
			final ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			final TaintBenchCase taintBenchCase = mapper.readValue(jsonfile, TaintBenchCase.class);

			if (taintBenchCase.getFindings().isEmpty() || taintBenchCase.getFindings().size() < 1) {
				System.out.println("No findings found!");
			} else {
				final List<Finding> findingsList = taintBenchCase.getFindings();
				for (int i = 0; i < findingsList.size(); i++) {
					final Finding finding = findingsList.get(i);

					// Sink
					if (finding.getSink() == null) {
						System.out.println("No Intent Sink found");
					} else {
						final Sink sink = new Sink();
						final Reference reference = new Reference();
						Statement statement = new Statement();
						reference.setApp(appObj);
						if (!finding.getSink().getClassName().isEmpty() && finding.getSink().getClassName() != null) {
							// Extracting ClassName
							reference.setClassname(finding.getSink().getClassName());
						}
						if (!finding.getSink().getJimpleStmt().isEmpty() && finding.getSink().getJimpleStmt() != null
								&& finding.getSink().getLineNo() != null) {
							// Extracting JimpleStmt and lineNo
							statement = Helper.createStatement(finding.getSink().getJimpleStmt(),
									finding.getSink().getLineNo());
						} else if (!finding.getSink().getJimpleStmt().isEmpty()
								&& finding.getSink().getJimpleStmt() != null) {
							// Extracting JimpleStmt
							statement = Helper.createStatement(finding.getSink().getJimpleStmt());
						}
						if (finding.getSink().getMethodName() != null && !finding.getSink().getMethodName().isEmpty()) {
							// Extracting methodName
							reference.setMethod(finding.getSink().getMethodName());
						}
						reference.setStatement(statement);
						sink.setReference(reference);
						sinks.getSink().add(sink);

					}

					// Source
					if (finding.getSource() == null) {
						System.out.println("No Intent Source found");
					} else {

						final Source source = new Source();
						final Reference reference = new Reference();
						Statement statement = new Statement();
						reference.setApp(appObj);
						if (finding.getSource().getClassName() != null
								&& !finding.getSource().getClassName().isEmpty()) {
							// Extracting ClassName
							reference.setClassname(finding.getSource().getClassName());
						}
						if (!finding.getSource().getJimpleStmt().isEmpty()
								&& finding.getSource().getJimpleStmt() != null
								&& finding.getSource().getLineNo() != null) {
							// Extracting JimpleStmt and lineNo
							statement = Helper.createStatement(finding.getSource().getJimpleStmt(),
									finding.getSource().getLineNo());
						} else if (!finding.getSource().getJimpleStmt().isEmpty()
								&& finding.getSource().getJimpleStmt() != null) {
							// Extracting JimpleStmt
							statement = Helper.createStatement(finding.getSource().getJimpleStmt());
						}
						if (finding.getSource().getMethodName() != null
								&& !finding.getSource().getMethodName().isEmpty()) {
							// Extracting methodName
							reference.setMethod(finding.getSource().getMethodName());
						}
						reference.setStatement(statement);
						source.setReference(reference);
						sources.getSource().add(source);
					}
				}
				aqlAnswer.setSinks(sinks);
				aqlAnswer.setSources(sources);
				final File file = new File(answerFile);
				AnswerHandler.createXML(aqlAnswer, file);
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
			if (aqlAnswer == null) {
				System.out.println("Invalid AQL Answer");
				return;
			}
			if (args[1].toLowerCase().equals(flowDroid)) {
				// FlowDroid IR generation
				exportSourceAndSinksJimple(aqlAnswer);
			} else if (args[1].toLowerCase().equals(amanDroid)) {
				// AmanDroid IR generation
				exportSourceAndSinksJawa(aqlAnswer);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private static void exportSourceAndSinksJimple(Answer aqlAnswer) {
		final Set<String> sourcesAndSinksToExport = new HashSet<>();
		for (final Source ss : aqlAnswer.getSources().getSource()) {
			sourcesAndSinksToExport
					.add("<" + ss.getReference().getStatement().getStatementgeneric() + "> -> " + sourceString);
		}
		for (final Sink sk : aqlAnswer.getSinks().getSink()) {
			sourcesAndSinksToExport
					.add("<" + sk.getReference().getStatement().getStatementgeneric() + "> -> " + sinkString);
		}
		exportToFile(sourcesAndSinksToExport);
	}

	private static void exportSourceAndSinksJawa(Answer aqlAnswer) {
		final Set<String> sourcesAndSinksToExport = new HashSet<>();
		for (final Source source : aqlAnswer.getSources().getSource()) {
			sourcesAndSinksToExport.add(
					JawaHelper.toJawa(source.getReference().getStatement()) + " SENSITIVE_INFO -> " + sourceString);
		}
		buildSinkJawa(aqlAnswer, sourcesAndSinksToExport);
	}

	private static void buildSinkJawa(Answer answer, Set<String> sourcesAndSinksToExport) {
		final Sinks sinks = answer.getSinks();
		StringBuilder attachment;
		for (final Sink sink : sinks.getSink()) {
			attachment = new StringBuilder();
			if (sink.getReference().getStatement().getParameters() != null
					&& !sink.getReference().getStatement().getParameters().getParameter().isEmpty()) {
				attachment.append(" ");
				for (int i = 0; i < sink.getReference().getStatement().getParameters().getParameter().size(); i++) {
					attachment.append(attachment.length() == 1 ? "" : "|").append(i);
				}
			}
			sourcesAndSinksToExport.add(JawaHelper.toJawa(sink.getReference().getStatement()) + " -> " + sinkString
					+ attachment.toString());
		}
		exportToFile(sourcesAndSinksToExport);
	}

	private static void exportToFile(Set<String> sourcesAndSinksToExport) {
		final List<String> sourcesAndSinksToExportSorted = new ArrayList<>(sourcesAndSinksToExport);
		Collections.sort(sourcesAndSinksToExportSorted);
		try {
			final FileWriter fileWriter = new FileWriter(answerFile);
			// Sources
			for (final String s : sourcesAndSinksToExportSorted) {
				if (s.endsWith(sourceString) || s.substring(0, s.lastIndexOf(' ')).endsWith(sourceString)) {
					fileWriter.write(s + "\n");
				}
			}

			fileWriter.write("\n");

			// Sinks
			for (final String s : sourcesAndSinksToExportSorted) {
				if (s.endsWith(sinkString) || s.substring(0, s.lastIndexOf(' ')).endsWith(sinkString)) {
					fileWriter.write(s + "\n");
				}
			}
			fileWriter.close();

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
