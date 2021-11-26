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
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.helper.FileWithHash;
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

	private int mode;
	private File apkFileInput;
	private File aqlAnswerInput;
	private File pathTaintBench;
	private File pathOutput;
	private int idFrom;
	private int idTo;

	public static void main(String[] args) {
		new Mapper().map(args);
	}

	public boolean map(String[] args) {
		final long startTime = System.currentTimeMillis();

		boolean success = false;
		this.pathTaintBench = DEFAULT_TAINTBENCH_PATH;
		this.pathOutput = DEFAULT_RESULT_PATH;
		this.idFrom = -1;
		this.idTo = Integer.MAX_VALUE;

		int invalidArgument = -1;
		for (int i = 0; i < args.length; i++) {
			if (args[i].endsWith(".apk")) {
				// Apk to map
				this.apkFileInput = new File(args[i]);
				if (this.apkFileInput.exists()) {
					this.mode = MODE_EXPORT_AQL_ANSWER;
				} else {
					Log.error("Input .apk-file does not exist: " + this.apkFileInput.getAbsolutePath());
					invalidArgument = i;
					break;
				}
			} else {
				if (args[i].equals("-c") || args[i].equals("-conv") || args[i].equals("-convert")) {
					// Targeted tool
					if (args[i + 1].equalsIgnoreCase(FLOWDROID) || args[i + 1].equalsIgnoreCase(FLOWDROID_SHORT)) {
						this.mode = MODE_FLOWDROID;
					} else if (args[i + 1].equalsIgnoreCase(AMANDROID)
							|| args[i + 1].equalsIgnoreCase(AMANDROID_SHORT)) {
						this.mode = MODE_AMANDROID;
					} else {
						invalidArgument = i + 1;
						break;
					}

					// Input AQL-Answer
					if (args[i + 2].endsWith(".xml")) {
						this.aqlAnswerInput = new File(args[i + 2]);
						if (!this.aqlAnswerInput.exists()) {
							Log.error("Input AQL-Answer does not exist: " + this.aqlAnswerInput.getAbsolutePath());
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
					this.pathOutput = new File(args[i + 1]);
					this.pathOutput.mkdirs();
				} else if (args[i].equalsIgnoreCase("-tb") || args[i].equalsIgnoreCase("-taintbench")) {
					// TaintBench Directory
					this.pathTaintBench = new File(args[i + 1]);
				} else if (args[i].equalsIgnoreCase("-id")) {
					// TaintBench Flow ID
					final int id = Integer.valueOf(args[i + 1]);
					if (id > 0) {
						this.idFrom = id;
						this.idTo = id;
					}
				} else if (args[i].equalsIgnoreCase("-from")) {
					// TaintBench Flow ID-From
					this.idFrom = Integer.valueOf(args[i + 1]);
				} else if (args[i].equalsIgnoreCase("-to")) {
					// TaintBench Flow ID-To
					this.idTo = Integer.valueOf(args[i + 1]);
				} else {
					invalidArgument = i;
					break;
				}
				i++;
			}
		}

		if (this.mode > 0) {
			if (invalidArgument < 0) {
				Log.msg("Input [\n\tMode: "
						+ (this.mode == MODE_EXPORT_AQL_ANSWER ? "Export AQL-Answer"
								: "Convert to " + (this.mode == MODE_FLOWDROID ? FLOWDROID : AMANDROID))
						+ ",\n\tApk input: " + (this.apkFileInput != null ? this.apkFileInput.getAbsolutePath() : "-")
						+ ",\n\tAQL-Answer input: "
						+ (this.aqlAnswerInput != null ? this.aqlAnswerInput.getAbsolutePath() : "-")
						+ ",\n\tTaintBench location: " + this.pathTaintBench.getAbsolutePath() + ",\n\tOutput path: "
						+ this.pathOutput.getAbsolutePath() + "\n]", Log.NORMAL);
				if (this.mode == MODE_EXPORT_AQL_ANSWER) {
					// Export AQL-Answer
					success = exportAQLAnswer();
				} else if (this.aqlAnswerInput != null) {
					// Export Sources & Sinks
					success = exportSourceAndSinks();
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

		return success;
	}

	private boolean exportAQLAnswer() {
		final Answer aqlAnswer = new Answer();
		final Sources sources = new Sources();
		final Sinks sinks = new Sinks();
		aqlAnswer.setSinks(sinks);
		aqlAnswer.setSources(sources);

		final String apkFileName = this.apkFileInput.getName().substring(0,
				this.apkFileInput.getName().indexOf(".apk"));
		final File aqlAnswerFile = new File(this.pathOutput, apkFileName + ".xml");
		final File tbFindingsFile = new File(this.pathTaintBench,
				this.apkFileInput.getName().replace(".apk", "_findings.json"));
		final App app = Helper.createApp(this.apkFileInput);
		try {
			// Read findings
			final ObjectMapper mapper = new ObjectMapper();
			final TaintBenchCase taintBenchCase = mapper.readValue(tbFindingsFile, TaintBenchCase.class);

			// Convert to AQL-Answer
			if (taintBenchCase == null || taintBenchCase.getFindings().isEmpty()) {
				Log.error("No findings found!");
			} else {
				for (final Finding finding : taintBenchCase.getFindings()) {
					if (finding.getID() >= this.idFrom && finding.getID() <= this.idTo) {
						// Sink
						if (finding.getSink() == null) {
							Log.error("No Intent Sink found");
						} else {
							final String jimpleStmt = TaintBenchHelper.getJimpleStmt(finding.getSink().getIRs());
							if (finding.getSink().getClassName() != null && !finding.getSink().getClassName().isEmpty()
									&& jimpleStmt != null && !jimpleStmt.isEmpty()
									&& finding.getSink().getMethodName() != null
									&& !finding.getSink().getMethodName().isEmpty()) {
								final Reference reference = new Reference();
								reference.setApp(app);
								reference.setClassname(finding.getSink().getClassName());
								reference.setMethod(finding.getSink().getMethodName());
								reference.setStatement(
										Helper.createStatement(jimpleStmt, finding.getSink().getLineNo()));
								final Sink sink = new Sink();
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
								final Reference reference = new Reference();
								reference.setApp(app);
								reference.setClassname(finding.getSource().getClassName());
								reference.setMethod(finding.getSource().getMethodName());
								reference.setStatement(
										Helper.createStatement(jimpleStmt, finding.getSource().getLineNo()));
								final Source source = new Source();
								source.setReference(reference);
								sources.getSource().add(source);
							} else {
								Log.warning("Incomplete source defined for ID: " + finding.getID());
							}
						}
					}
				}
				AnswerHandler.createXML(aqlAnswer, aqlAnswerFile);
				return true;
			}
		} catch (final Exception e) {
			Log.error("Error while mapping sources and sinks." + Log.getExceptionAppendix(e));
		}
		return false;
	}

	private boolean exportSourceAndSinks() {
		final Answer aqlAnswer = AnswerHandler.parseXML(this.aqlAnswerInput);
		if (aqlAnswer == null) {
			Log.error("Invalid AQL Answer");
			return false;
		}

		// Convert list of sources and list of sinks to list of sources and sinks
		final List<SourceOrSink> sourcesAndSinks = new ArrayList<>();
		for (final Source source : aqlAnswer.getSources().getSource()) {
			sourcesAndSinks.add(new SourceOrSink(true, false, source.getReference()));
		}
		for (final Sink sink : aqlAnswer.getSinks().getSink()) {
			sourcesAndSinks.add(new SourceOrSink(false, true, sink.getReference()));
		}

		// Export
		final Exporter exporter = new Exporter(this.pathOutput);
		if (this.mode == MODE_FLOWDROID) {
			exporter.exportSourcesAndSinks(sourcesAndSinks, Exporter.EXPORT_FORMAT_FLOWDROID_JIMPLE);
		} else if (this.mode == MODE_AMANDROID) {
			exporter.exportSourcesAndSinks(sourcesAndSinks, Exporter.EXPORT_FORMAT_AMANDROID_JAWA);
		}

		// Copy to output
		try {
			final File resultFile;
			if (this.mode == MODE_FLOWDROID) {
				resultFile = new File(this.pathOutput, "SourcesAndSinks_" + FLOWDROID_SHORT + "_"
						+ Helper.getAnswerFilesAsHash(new FileWithHash(this.aqlAnswerInput)) + ".txt");
				Files.copy(exporter.getOutputFile(Exporter.EXPORT_FORMAT_FLOWDROID_JIMPLE), resultFile);
			} else {
				resultFile = new File(this.pathOutput, "SourcesAndSinks_" + AMANDROID_SHORT + "_"
						+ Helper.getAnswerFilesAsHash(new FileWithHash(this.aqlAnswerInput)) + ".txt");
				Files.copy(exporter.getOutputFile(Exporter.EXPORT_FORMAT_AMANDROID_JAWA), resultFile);
			}
			Log.msg("(Copied result to unique file: " + resultFile.getAbsolutePath() + ")", Log.NORMAL);
		} catch (final IOException e) {
			Log.error("Could not write result to file!" + Log.getExceptionAppendix(e));
			return false;
		}
		return true;
	}
}