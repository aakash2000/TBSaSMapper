package de.foellix.aql.taintbench.sasmapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

import de.foellix.aql.Log;
import de.foellix.aql.brew.Exporter;
import de.foellix.aql.brew.sourceandsinkselector.SourceOrSink;
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
import de.foellix.aql.helper.EqualsHelper;
import de.foellix.aql.helper.HashHelper;
import de.foellix.aql.helper.Helper;

public class Mapper {
	private static final File DEFAULT_TAINTBENCH_PATH = new File("data/json");
	private static final File DEFAULT_RESULT_PATH = new File("data/answer");
	private static final int MODE_EXPORT_AQL_ANSWER = 1;
	private static final int MODE_FLOWDROID = 2;
	private static final int MODE_AMANDROID = 3;
	private static final String FLOWDROID = "FlowDroid";
	private static final String FLOWDROID_SHORT = "fd";
	private static final String AMANDROID = "Amandroid";
	private static final String AMANDROID_SHORT = "ad";

	public static boolean success;
	private static int mode;
	private static File apkFileInput;
	private static File aqlAnswerInput;
	private static File pathTaintBench;
	private static File pathOutput;
	private static int idFrom;
	private static int idTo;

	public static void main(String[] args) {
		final long startTime = System.currentTimeMillis();

		// Init
		success = false;
		mode = -1;
		apkFileInput = null;
		aqlAnswerInput = null;
		pathTaintBench = DEFAULT_TAINTBENCH_PATH;
		pathOutput = DEFAULT_RESULT_PATH;
		idFrom = -1;
		idTo = Integer.MAX_VALUE;

		// Start
		int invalidArgument = -1;
		for (int i = 0; i < args.length; i++) {
			if (args[i].endsWith(".apk")) {
				// Apk to map
				apkFileInput = new File(args[i]);
				if (apkFileInput.exists()) {
					mode = MODE_EXPORT_AQL_ANSWER;
				} else {
					Log.error("Input .apk-file does not exist: " + apkFileInput.getAbsolutePath());
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
							Log.error("Input AQL-Answer does not exist: " + aqlAnswerInput.getAbsolutePath());
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
				} else if (args[i].equalsIgnoreCase("-id")) {
					// TaintBench Flow ID
					final int id = Integer.valueOf(args[i + 1]);
					if (id > 0) {
						idFrom = id;
						idTo = id;
					}
				} else if (args[i].equalsIgnoreCase("-from")) {
					// TaintBench Flow ID
					idFrom = Integer.valueOf(args[i + 1]);
				} else if (args[i].equalsIgnoreCase("-to")) {
					// TaintBench Flow ID
					idTo = Integer.valueOf(args[i + 1]);
				} else {
					invalidArgument = i;
					break;
				}
				i++;
			}
		}

		if (mode > 0) {
			if (invalidArgument < 0) {
				Log.msg("Input [\n\tMode: "
						+ (mode == MODE_EXPORT_AQL_ANSWER ? "Export AQL-Answer"
								: "Convert to " + (mode == MODE_FLOWDROID ? FLOWDROID : AMANDROID))
						+ ",\n\tApk input: " + (apkFileInput != null ? apkFileInput.getAbsolutePath() : "-")
						+ ",\n\tAQL-Answer input: " + (aqlAnswerInput != null ? aqlAnswerInput.getAbsolutePath() : "-")
						+ ",\n\tTaintBench location: " + pathTaintBench.getAbsolutePath() + ",\n\tOutput path: "
						+ pathOutput.getAbsolutePath() + "\n]", Log.NORMAL);
				if (mode == MODE_EXPORT_AQL_ANSWER) {
					// export AQL answer
					exportAQLAnswer();
					success = true;
				} else if (aqlAnswerInput != null) {
					// export Source and Sinks
					exportSourceAndSinks();
					success = true;
				} else {
					Log.error("No input AQL-Answer specified!");
				}
			} else {
				Log.error("Invalid argument! (Argument: " + args[invalidArgument] + " unknown)");
			}
		} else {
			Log.error(
					"Invalid input!\nAt least provide an app to map:\n\tpath/to/apkFile.apk\nor an answer to convert:\n\t-convert FlowDroid/Amandroid answer.xml");
		}

		Log.msg("\nExecution " + (success ? "successfull" : " failed") + "! (Time consumed: "
				+ ((System.currentTimeMillis() - startTime) / 1000d) + "s)", Log.NORMAL);
	}

	private static void exportAQLAnswer() {
		final Answer aqlAnswer = new Answer();
		final Sources sources = new Sources();
		final Sinks sinks = new Sinks();
		aqlAnswer.setSinks(sinks);
		aqlAnswer.setSources(sources);

		final String appName = apkFileInput.getName().substring(0, apkFileInput.getName().indexOf(".apk"));
		final File answerFile = new File(pathOutput, appName + ".xml");
		final String findings = apkFileInput.getName().trim().replace(".apk", "").concat("_findings.json");
		final App appObj = Helper.createApp(apkFileInput);
		try {
			final File jsonfile = new File(pathTaintBench, findings);
			final ObjectMapper mapper = new ObjectMapper();
			// mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			final TaintBenchCase taintBenchCase = mapper.readValue(jsonfile, TaintBenchCase.class);

			if (taintBenchCase.getFindings().isEmpty() || taintBenchCase.getFindings().size() < 1) {
				Log.error("No findings found!");
			} else {
				final List<Finding> findingsList = taintBenchCase.getFindings();
				for (int i = 0; i < findingsList.size(); i++) {
					final Finding finding = findingsList.get(i);

					if (finding.getID() >= idFrom && finding.getID() <= idTo) {
						// Sink
						if (finding.getSink() == null) {
							Log.error("No Intent Sink found");
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
							} else {
								Log.warning("Incomplete sink defined for ID: " + finding.getID());
							}
						}

						// Source
						if (finding.getSource() == null) {
							Log.error("No Intent Source found");
						} else {
							final String jimpleStmt = TaintBenchHelper.getJimpleStmt(finding.getSource().getIRs());
							if (finding.getSource().getClassName() != null
									&& !finding.getSource().getClassName().isEmpty() && jimpleStmt != null
									&& !jimpleStmt.isEmpty() && finding.getSource().getMethodName() != null
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
							} else {
								Log.warning("Incomplete source defined for ID: " + finding.getID());
							}
						}
					}
				}
				AnswerHandler.createXML(aqlAnswer, answerFile);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private static void exportSourceAndSinks() {
		final Answer aqlAnswer = AnswerHandler.parseXML(aqlAnswerInput);
		if (aqlAnswer == null) {
			Log.error("Invalid AQL Answer");
			return;
		}

		// Convert list of source and list of sinks to list of sources or sinks
		final List<SourceOrSink> sourcesAndSinks = new ArrayList<>();
		for (final Source source : aqlAnswer.getSources().getSource()) {
			for (final SourceOrSink check : sourcesAndSinks) {
				if (EqualsHelper.equals(source.getReference(), check.getReference())) {
					continue;
				}
			}
			sourcesAndSinks.add(new SourceOrSink(true, false, source.getReference()));
		}
		for (final Sink sink : aqlAnswer.getSinks().getSink()) {
			for (final SourceOrSink check : sourcesAndSinks) {
				if (EqualsHelper.equals(sink.getReference(), check.getReference())) {
					check.setSink(true);
					continue;
				}
			}
			sourcesAndSinks.add(new SourceOrSink(false, true, sink.getReference()));
		}

		// Export
		final Exporter exporter = new Exporter(pathOutput);
		if (mode == MODE_FLOWDROID) {
			exporter.exportSourcesAndSinks(sourcesAndSinks, Exporter.EXPORT_FORMAT_FLOWDROID_JIMPLE);
		} else if (mode == MODE_AMANDROID) {
			exporter.exportSourcesAndSinks(sourcesAndSinks, Exporter.EXPORT_FORMAT_AMANDROID_JAWA);
		}

		// Copy to output
		try {
			if (mode == MODE_FLOWDROID) {
				final File resultFile = new File(pathOutput, "SourcesAndSinks_" + FLOWDROID_SHORT + "_"
						+ HashHelper.sha256Hash(aqlAnswerInput.getAbsolutePath(), true) + ".txt");
				Files.copy(exporter.getOutputFile(Exporter.EXPORT_FORMAT_FLOWDROID_JIMPLE), resultFile);
			} else {
				final File resultFile = new File(pathOutput, "SourcesAndSinks_" + AMANDROID_SHORT + "_"
						+ HashHelper.sha256Hash(aqlAnswerInput.getAbsolutePath(), true) + ".txt");
				Files.copy(exporter.getOutputFile(Exporter.EXPORT_FORMAT_AMANDROID_JAWA), resultFile);
			}
		} catch (final IOException e) {
			Log.error("Could not write result to file!" + Log.getExceptionAppendix(e));
		}
	}
}