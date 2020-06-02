package frontend;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;

// My imports
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.util.Scanner;

import ast.*;
//

public class FrontEnd {

  private final Parser parser;

  // necessary for simulator
  private Statechart statechart = null;
  public static Map<Declaration, Expression> map = new HashMap<Declaration, Expression>(); //Creating a HashMap for the variable bindings, every variable is identified with its fullVName which is unique to it


  // constructors
  public FrontEnd(String input) throws FileNotFoundException {
    this.parser = new Parser(new Lexer(new FileReader(input)));
  }

  public FrontEnd(Statechart statechart) throws Exception
  {
    this.parser = null; // for the time being
    try {
    this.statechart = statechart;
    simulation();
    }
    catch (Exception e)
    {
      System.out.println("FrontEnd(Statechart statechart) failed!\n");
    }

  }
  // pasrser
  public Parser getParser() {
    return this.parser;
  }
  // populate map, all variables are by default bound to null
  private void populate()
  {
    for(Declaration d : this.statechart.declarations)
    {
      map.put(d, null);
    }
    
  }
  
  // gets the atomic-state (initial state) for any state up the heirarchy and executes all the entry statements in the process
  private State getAtomicState(State state) throws Exception
  {
    State init = state;
    try {
    ExecuteStatement.executeStatement(init.entry);
    for (State s : init.states)
    {  
      ExecuteStatement.executeStatement(s.entry);

      if(s.states.isEmpty())
      {
        return s;
      }
      else
        init = s;
    }
    }
  catch (Exception e)
  {
    System.out.println("One of the Execute Statements failed!\n");
  }
    // if the state is an atomic state itself
    return init;
  }
  

  // displaying all variables - just to get an idea of the state
  private void displayMap()
  {
    for(Map.Entry<Declaration, Expression> m : map.entrySet()){    
      System.out.println(m.getKey().getFullVName()+": "+m.getValue());    
     }  
  }
  
  public void simulation() throws Exception
  {
    try {
    System.out.println("\n\n\nSimulation...\n\n\n");
    // populating the map to contain all the variable names, as this is a statically typed langugae, no further additions are required in the map
    this.populate();

    // to check the sequential simulation
    Scanner input = new Scanner(System.in);  
    int counter = 0;

    State curr = (State)statechart;
    List<Transition> transitions = curr.transitions;

    //Main-loop
    while(true)
    {
      curr = getAtomicState(curr); // gets to the state where from where the execution begins
      System.out.println("+--------------------------------------------------+");
      System.out.println("Initial State Map: ");
      displayMap();
      System.out.println();

      // System.out.println("Static Analysis: ");
      // new ZonotopeAbstractDomain(curr, map); // - uncomment this to run the numerical static analyzer

      input.nextLine();
      System.out.println("After " + counter + " transition/transitions :-");
      System.out.println("State: " + curr.getFullName());
      System.out.println("Map: ");

      for(Transition t : transitions) // sequentially executing very transition (only considering the scenarion where the transition is present in the parent and transition occurs in its child states)
      {
        if(t.getSource() == curr)
        {
          //if(((BooleanConstant)evaluateExpression(t.guard)).value) // this needs to be evaluated, as of now assuming it to be a BooleanConstant value
          if(((BooleanConstant)EvaluateExpression.evaluate(t.guard)).value)
          {
            ExecuteStatement.executeStatement(curr.exit);
            System.out.println("Final State Map: ");
            displayMap();
            System.out.println("+--------------------------------------------------+");
            ExecuteStatement.executeStatement(t.action);
            curr = t.getDestination();
            break;
          }
        }
      }
      counter ++;
    }
   }
  catch (Exception e)
  {
    System.out.println("Simulation Failed!\n"); 
  }
}
}
//https://github.com/AmoghJohri/StatechartSimulator.git

