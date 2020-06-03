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
  public static List<Transition> transitions = new ArrayList<Transition>();
  public static Map<State, Integer>history_map = new HashMap<State, Integer>();

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
  private void populate(State s)
  {
    populate_state(s);
    populate_transition(s);
    if(!s.states.isEmpty())
    {
      for(State st : s.states)
        populate(st);
    }
  }
  
  private void populate_state(State s)
  {
    for(Declaration d : s.declarations)
    {
      map.put(d, null);
    }
  }

  private void populate_transition(State s)
  {
    for(Transition t : s.transitions)
    {
      transitions.add(t);
    }
  }

  // gets the atomic-state (initial state) for any state up the heirarchy and executes all the entry statements in the process
  private State getAtomicState(State state) throws Exception
  {
    State init = state;
    try 
    {
      ExecuteStatement.executeStatement(init.entry);
      if(init.states.isEmpty())
        return init;
      int index = 0;
      if(history_map.containsKey(init))
        index = history_map.get(init);
      else
        index = 0;
      return getAtomicState(state.states.get(index));
    }
    catch (Exception e)
    {
      System.out.println("One of the Execute Statements failed!\n");
    }
    return init; // this should not happen
  }
  
  // displaying all variables - just to get an idea of the state
  private void displayMap()
  {
    for(Map.Entry<Declaration, Expression> m : map.entrySet()){    
      System.out.println(m.getKey().getFullVName()+": "+m.getValue());    
     }  
  }
  
  // 

  // checking whether a state maintains history
  private boolean hasHistory(State s)
  {
    for(Declaration d : s.declarations)
    {
      if(d.getFullVName().equals(s.getFullName() + ".HISTORY")) // if the HISTORY variable exists
      {
        if(map.get(d) instanceof BooleanConstant) // if the history variable is a boolean constant
        {
          if(((BooleanConstant)map.get(d)).value) // if the history variable is true
            return true;
        }
      }
    }
    return false;
  }

  // setting the current state 
  private void setCurrent(State parent, State current)
  {
    int i = 0;
    for(State s : parent.states) // getting the index of the current state
    {
      if(s.equals(current))
        break;
      else
        i ++;
    }
    history_map.put(parent, i);
  }


  // bubbling-up to the lower upper bound
  private void performTransition(Transition t) throws Exception
  {
    try
    {
      State source = t.getSource();
      State destination = t.getDestination();
      State lub = statechart.lub(source, destination);
      State current = source;

      while(!current.equals(lub))
      {
        ExecuteStatement.executeStatement(current.exit);
        State currParent = current.getSuperstate();
        if(hasHistory(currParent))
        {
          setCurrent(currParent, current);
        }
        current = currParent;
      }
      
      ExecuteStatement.executeStatement(t.action);
      ArrayList<State> path = new ArrayList<State>();
      current = destination;
      while(!current.equals(lub))
      {
        path.add(current);
        current = current.getSuperstate();
      }
      int i = path.size() - 1;
      while(i > 1)
      {
        ExecuteStatement.executeStatement(path.get(i).entry);
        i ++;
      }
    }
    catch (Exception e)
    {
      System.out.println("Something wrong with execute statement!\n");
    }
  }

  public void simulation() throws Exception
  {
    try 
    {
      System.out.println("\n\n\nSimulation...\n\n\n");
      // populating the map to contain all the variable names, as this is a statically typed langugae, no further additions are required in the map
      this.populate(statechart);
      System.out.println("Initial Statechart Map: ");
      displayMap();

      // to check the sequential simulation
      Scanner input = new Scanner(System.in);  
      int counter = 0;

      State curr = (State)statechart;

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

        input.nextLine(); // to break the flow into key-strokes

        System.out.println("After " + counter + " transition/transitions :-");
        System.out.println("State: " + curr.getFullName());


        for(Transition t : transitions)
        {
          if(t.getSource().equals(curr))
          {
            if(((BooleanConstant)EvaluateExpression.evaluate(t.guard)).value)
            {
              performTransition(t);
              System.out.println("Final State Map: ");
              displayMap();
              curr = t.getDestination();
              System.out.println("+--------------------------------------------------+");
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

