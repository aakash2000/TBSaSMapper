# TBSaSMapper

The TBSaSMapper is a mapping tool within the AQL framework. The main objective of the mapper is to map source and sinks from application (apk) [findings](https://github.com/TaintBench/backflash/blob/master/backflash_findings.json) to an [AQL-Answer](https://github.com/FoelliX/AQL-System/wiki/Answers) or to map an [AQL-Answer](https://github.com/FoelliX/AQL-System/wiki/Answers) to a source and sinks file (text) which is required as an input to [Flowdroid](https://github.com/secure-software-engineering/FlowDroid) or [Amandroid](https://github.com/arguslab/Argus-SAF) tool.

## Launch Parameters 

| Parameters        | Meaning  |
| ------------- | ----- |
| A.apk  | Generates an AQL-Answer with sources and sinks for A.apk  |
| A.apk -id "X" |  With this parameter, the taintbench flow id "X" is used to generate the answer, "X" represents id in Numeric form  |
| A.apk -o "X" | With this parameter, the output directory is provided. "X" represents the directory in which output should be written, default is "data/answer" |
| -c "X" A.xml, -conv "X" A.xml, -convert "X" A.xml  | this parameter informs the mapper to convert the given AQL-Answer(A.xml) to X's format. Here "X" represent fd or ad for Flowdroid and Amandroid respectively |
| -tb "X" A.apk, -taintbench "X" A.apk  | Generates an AQL-Answer with sources and sinks for A.apk using the taintbench path provided in "X". "X" here represents path to taintbench |
| A.apk -from "X"  | Generates an AQL-Answer with sources and sinks for A.apk using "X" as the start taintbench flow id  |
| A.apk -to "X"  | Generates an AQL-Answer with sources and sinks for A.apk using "X" as the end taintbench flow id  |

  
## Query Execution

A couple of small examples :

1. Input command line :

`java -jar /path/to/TBSaSMapper-1.0.jar godwon_samp.apk`

* Output 

`godwon_samp.xml`

2. Input command line :

`java -jar /path/to/TBSaSMapper-1.0.jar godwon_samp.xml -c fd`

* Output 

`'SourcesAndSinks[fd]_godwon_samp.txt'`

The generated xml and text files can be viewed separately in a editor.
