# TBSaSMapper

The TBSasMapper is a mapping tool within the AQL framework. The main objective of the mapper is to map source and sinks from application (apk) findings to an AQL answer (XML) or to map an AQL answer to a source and sinks file (text) which is required as an input to Flowdroid or Amandroid tool.

The mapper accepts below mentioned formats as an input to the command line :-
1) Input : .apk file, Output : .xml (AQL answer), Example : mapper android.apk answer.xml

  Internally the mapper fetches the findings JSON for the provided apk and generates an AQL answer with sources and sinks extracted from the findings JSON.

2) Input : .xml file (AQL Answer), Output : .txt (source and sink file), options :- -c Fd (or Ad),
   Example : mapper -c fd answer.xml sas.txt

  Here the -c option represents conversion which can also be provided as -conv or -convert.
  Fd or Ad represents Flowdroid or Amandroid respectively. This input is case insensitive.

  The mapper here simply fetches the sources and sinks from provided AQL answer and represents it in Jawa or Jimple text file    depending the tool provided at input. Flowdroid requires Jimple while Amandroid required Jawa format.
  
