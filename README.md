# TBSaSMapper ![Java 16](https://img.shields.io/badge/java-16-brightgreen.svg)

The TBSaSMapper is a mapping tool within the AQL framework. The main objective of the mapper is to map source and sinks from application (apk) [findings](https://github.com/TaintBench/backflash/blob/master/backflash_findings.json) to an [AQL-Answer](https://github.com/FoelliX/AQL-System/wiki/Answers) or to map an [AQL-Answer](https://github.com/FoelliX/AQL-System/wiki/Answers) to a source and sinks file (text) which is required as an input to [Flowdroid](https://github.com/secure-software-engineering/FlowDroid) or [Amandroid](https://github.com/arguslab/Argus-SAF) tool.

## Launch Parameters 

| Parameters        | Meaning  |
| ------------- | ----- |
| `-tb "X"`, `-taintbench "X"`  | Generates an AQL-Answer with sources and sinks using the taintbench path provided in "X". "X" here represents path to taintbench |
| `-id "X"` |  With this parameter, the taintbench flow id "X" is used to generate the answer, "X" represents id in Numeric form  |
| `-o "X"` | With this parameter, the output directory is provided. "X" represents the directory in which output should be written, default is "data/answer" |
| `-from "X"`  | Generates an AQL-Answer with sources and sinks using "X" as the start taintbench flow id  |
| `-to "X"`  | Generates an AQL-Answer with sources and sinks using "X" as the end taintbench flow id  |
| `-c "X"`, `-conv "X"`, `-convert "X"`  | this parameter informs the mapper to convert the given AQL-Answer to X's format. Here "X" represent fd or ad for Flowdroid and Amandroid respectively |
  
## Query Execution

A couple of small examples :

1. Input command line :

`java -jar /path/to/TBSaSMapper-1.0.jar cajino_baidu.apk`

* Output 

`cajino_baidu.xml`

2. Input command line :

`java -jar /path/to/TBSaSMapper-1.0.jar cajino_baidu.xml -c fd`

* Output 

`SourcesAndSinks_fd_c7934977b4f936f47c71d4facd317b3fef6f48b3bfaac314e2059257a73a32c3.txt`

3. Input command line :

`java -jar /path/to/TBSaSMapper-1.0.jar -tb data/json/ -id 3 cajino_baidu.apk`

* Output 

`cajino_baidu.xml`  : xml only contains findings with given id.

The generated xml and text files can be viewed separately in a editor.
