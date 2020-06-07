# StatechartSimulator

## Objective
We aim to build a simulator for the statechart builder already in place. The statechart builder uses the language StaBL to define statecharts. 

## Basis
The simulator that we build will assume that the statechart designed is syntactically correct, passes the Parser, Lexical Analyzer and the Type Checker. After all these stages, our simulator will come into play.

## Structure
- The simulator that we build will exist in the directory `statechart-verification-master/src/dfa/frontend`. 
- The simulator is basically what the user will see when its statechart is getting executed/simulated, so it's a logical choice to place it in the `frontend` directory. 

## Deliverable
- All the relevant code is present in the /statechart-verification-master/src/dfa/frontend directory.
- This contains EvaluateExpression.java, ExecuteStatement.java, FrontEnd.java and ZonotopeAbstractDomain.java.
- All the test-cases for the simulator are in the /statechart-verification-master/src/dfa/data directory.
- The above directory contains a TestCases-Diagram directory where we have the diagramatic representation of the Statecharts corresponding to the provided test-cases.
- Apart from these, we have also submitted the Project Report, Project Presentation Slides and the Review Report as our complete project output.


