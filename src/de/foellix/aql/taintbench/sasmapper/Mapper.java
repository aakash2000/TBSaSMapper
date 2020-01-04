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

import de.foellix.aql.brew.taintbench.TaintBenchHelper;
import de.foellix.aql.brew.taintbench.datastructure.Finding;
import de.foellix.aql.brew.taintbench.datastructure.TaintBenchCase;
import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.App;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.datastructure.Sink;
import de.foellix.aql.datastructure.Sinks;
import de.foellix.aql.datastructure.Source;
import de.foellix.aql.datastructure.Sources;
import de.foellix.aql.datastructure.Statement;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.helper.HashHelper;
import de.foellix.aql.helper.Helper;
import de.foellix.aql.helper.JawaHelper;

public class Mapper {
	private static final File DEFAULT_TAINTBENCH_PATH = new File("data/json");
	private static final File DEFAULT_RESULT_PATH = new File("data/answer");
	private static final String SOURCE_STRING = "_SOURCE_";
	private static final String SINK_STRING = "_SINK_";
	private static final int MODE_EXPORT_AQL_ANSWER = 1;
	private static final int MODE_FLOWDROID = 2;
	private static final int MODE_AMANDROID = 3;
	private static final String FLOWDROID = "FlowDroid";
	private static final String FLOWDROID_SHORT = "fd";
	private static final String AMANDROID = "Amandroid";
	private static final String AMANDROID_SHORT = "ad";

	public static boolean success;

	private static int mode = -1;
	private static File apkFileInput;
	private static File aqlAnswerInput;
	private static File pathTaintBench = DEFAULT_TAINTBENCH_PATH;
	private static File pathOutput = DEFAULT_RESULT_PATH;

	public static void main(String[] args) {
		final long startTime = System.currentTimeMillis();

		success = false;
		int invalidArgument = -1;
		for (int i = 0; i < args.length; i++) {
			if (args[i].endsWith(".apk")) {
				// Apk to map
				apkFileInput = new File(args[i]);
				if (apkFileInput.exists()) {
					mode = MODE_EXPORT_AQL_ANSWER;
				} else {
					System.err.println("Input .apk-file does not exist: " + apkFileInput.getAbsolutePath());
					invalidArgument = i;
					break;
				}
			} else {
				if (args[i].equals("-c") || args[i].equals("-conv") || args[i].equals("-convert")) {
					// Target tool
					if (args[i + 1].equalsIgnoreCase(FLOWDROID) || args[i + 1].equalsIgnoreCase(FLOWDROID_SHORT)) {
						mode = MODE_FLOWDROID;
					} else if (args[i + 1].equalsIgnoreCase(AMANDROID)
							|| args[i + 1].equalsIgnoreCase(AMANDROID_SHORT)) {
						mode = MODE_AMANDROID;
					} else {
						invalidArgument = i + 1;
						break;
					}

					// Input AQL-Answer
					if (args[i + 2].endsWith(".xml")) {
						aqlAnswerInput = new File(args[i + 2]);
						if (!aqlAnswerInput.exists()) {
							System.err.println("Input AQL-Answer does not exist: " + aqlAnswerInput.getAbsolutePath());
							invalidArgument = i + 2;
							break;
						}
					} else {
						invalidArgument = i + 2;
						break;
					}

					i++;
				} else if (args[i].equalsIgnoreCase("-o") || args[i].equalsIgnoreCase("-out")
						|| args[i].equalsIgnoreCase("-output")) {
					// Output path
					pathOutput = new File(args[i + 1]);
					pathOutput.mkdirs();
				} else if (args[i].equalsIgnoreCase("-tb") || args[i].equalsIgnoreCase("-taintbench")) {
					// TaintBench Location
					pathTaintBench = new File(args[i + 1]);
				} else {
					invalidArgument = i;
					break;
				}
				i++;
			}
		}

		if (mode > 0) {
			if (invalidArgument < 0) {
				System.out.println("Input [\n\tMode: "
						+ (mode == MODE_EXPORT_AQL_ANSWER ? "Export AQL-Answer"
								: "Convert to " + (mode == MODE_FLOWDROID ? FLOWDROID : AMANDROID))
						+ ",\n\tApk input: " + (apkFileInput != null ? apkFileInput.getAbsolutePath() : "-")
						+ ",\n\tAQL-Answer input: " + (aqlAnswerInput != null ? aqlAnswerInput.getAbsolutePath() : "-")
						+ ",\n\tTaintBench location: " + pathOutput.getAbsolutePath() + ",\n\tOutput path: "
						+ pathOutput.getAbsolutePath() + "\n]");
				if (mode == MODE_EXPORT_AQL_ANSWER) {
					// export AQL answer
					exportAQLAnswer();
					success = true;
				} else if (aqlAnswerInput != null) {
					// export Source and Sinks
					exportSourceAndSinks();
					success = true;
				} else {
					System.err.println("No input AQL-Answer specified!");
				}
			} else {
				System.err.println("Invalid argument! (Argument: " + args[invalidArgument] + " unknown)");
			}
		} else {
			System.err.println(
					"Invalid input!\nAt least provide an app to map:\n\tpath/to/apkFile.apk\nor an answer to convert:\n\t-convert FlowDroid/Amandroid answer.xml");
		}

		System.out.println("\nExecution " + (success ? "successfull" : " failed") + "! (Time consumed: "
				+ ((System.currentTimeMillis() - startTime) / 1000d) + "s)");
	}

	private static void exportAQLAnswer() {
		final Answer aqlAnswer = new Answer();
		final String appName = apkFileInput.getName().substring(0, apkFileInput.getName().indexOf(".apk"));
		final File answerFile = new File(pathOutput, appName + ".xml");
		final String findings = apkFileInput.getName().trim().replace(".apk", "").concat("_findings.json");
		final App appObj = Helper.createApp(apkFileInput);
		final Sources sources = new Sources();
		final Sinks sinks = new Sinks();
		try {
			final File jsonfile = new File(pathTaintBench, findings);
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
						final String jimpleStmt = TaintBenchHelper.getJimpleStmt(finding.getSink().getIRs());
						if (finding.getSink().getClassName() != null && !finding.getSink().getClassName().isEmpty()
								&& jimpleStmt != null && !jimpleStmt.isEmpty()
								&& finding.getSink().getMethodName() != null
								&& !finding.getSink().getMethodName().isEmpty()) {
							final Sink sink = new Sink();
							final Reference reference = new Reference();
							Statement statement = new Statement();
							reference.setApp(appObj);

							// Extracting ClassName
							reference.setClassname(finding.getSink().getClassName());
							// Extracting JimpleStmt and lineNo
							statement = Helper.createStatement(jimpleStmt, finding.getSink().getLineNo());
							reference.setStatement(statement);
							// Extracting methodName
							reference.setMethod(finding.getSink().getMethodName());

							sink.setReference(reference);
							sinks.getSink().add(sink);
						}
					}

					// Source
					if (finding.getSource() == null) {
						System.out.println("No Intent Source found");
					} else {
						final String jimpleStmt = TaintBenchHelper.getJimpleStmt(finding.getSource().getIRs());
						if (finding.getSource().getClassName() != null && !finding.getSource().getClassName().isEmpty()
								&& jimpleStmt != null && !jimpleStmt.isEmpty()
								&& finding.getSource().getMethodName() != null
								&& !finding.getSource().getMethodName().isEmpty()) {
							final Source source = new Source();
							final Reference reference = new Reference();
							Statement statement = new Statement();
							reference.setApp(appObj);

							// Extracting ClassName
							reference.setClassname(finding.getSource().getClassName());
							// Extracting JimpleStmt and lineNo
							statement = Helper.createStatement(jimpleStmt, finding.getSource().getLineNo());
							reference.setStatement(statement);
							// Extracting methodName
							reference.setMethod(finding.getSource().getMethodName());

							source.setReference(reference);
							sources.getSource().add(source);
						}
					}
				}
				aqlAnswer.setSinks(sinks);
				aqlAnswer.setSources(sources);
				AnswerHandler.createXML(aqlAnswer, answerFile);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private static void exportSourceAndSinks() {
		final Answer aqlAnswer = AnswerHandler.parseXML(aqlAnswerInput);
		if (aqlAnswer == null) {
			System.out.println("Invalid AQL Answer");
			return;
		}
		if (mode == MODE_FLOWDROID) {
			// FlowDroid IR generation
			exportSourceAndSinksJimple(aqlAnswer);
		} else if (mode == MODE_AMANDROID) {
			// AmanDroid IR generation
			exportSourceAndSinksJawa(aqlAnswer);
		}
	}

	private static void exportSourceAndSinksJimple(Answer aqlAnswer) {
		final Set<String> sourcesAndSinksToExport = new HashSet<>();
		for (final Source ss : aqlAnswer.getSources().getSource()) {
			sourcesAndSinksToExport
					.add("<" + ss.getReference().getStatement().getStatementgeneric() + "> -> " + SOURCE_STRING);
		}
		for (final Sink sk : aqlAnswer.getSinks().getSink()) {
			sourcesAndSinksToExport
					.add("<" + sk.getReference().getStatement().getStatementgeneric() + "> -> " + SINK_STRING);
		}
		exportToFile(sourcesAndSinksToExport);
	}

	private static void exportSourceAndSinksJawa(Answer aqlAnswer) {
		final Set<String> sourcesAndSinksToExport = new HashSet<>();
		for (final Source source : aqlAnswer.getSources().getSource()) {
			sourcesAndSinksToExport.add(
					JawaHelper.toJawa(source.getReference().getStatement()) + " SENSITIVE_INFO -> " + SOURCE_STRING);
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
			sourcesAndSinksToExport.add(JawaHelper.toJawa(sink.getReference().getStatement()) + " -> " + SINK_STRING
					+ attachment.toString());
		}
		exportToFile(sourcesAndSinksToExport);
	}

	private static void exportToFile(Set<String> sourcesAndSinksToExport) {
		final File resultFile = new File(pathOutput,
				"SourcesAndSinks_" + (mode == MODE_FLOWDROID ? FLOWDROID_SHORT : AMANDROID_SHORT) + "_"
						+ HashHelper.sha256Hash(aqlAnswerInput.getAbsolutePath(), true) + ".txt");

		final List<String> sourcesAndSinksToExportSorted = new ArrayList<>(sourcesAndSinksToExport);
		Collections.sort(sourcesAndSinksToExportSorted);
		try {
			final FileWriter fileWriter = new FileWriter(resultFile);
			// Sources
			for (final String s : sourcesAndSinksToExportSorted) {
				if (s.endsWith(SOURCE_STRING) || s.substring(0, s.lastIndexOf(' ')).endsWith(SOURCE_STRING)) {
					fileWriter.write(s + "\n");
				}
			}

			fileWriter.write("\n");

			// Sinks
			for (final String s : sourcesAndSinksToExportSorted) {
				if (s.endsWith(SINK_STRING) || s.substring(0, s.lastIndexOf(' ')).endsWith(SINK_STRING)) {
					fileWriter.write(s + "\n");
				}
			}
			fileWriter.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
