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
  static Map<Declaration, Expression> map = new HashMap<Declaration, Expression>(); //Creating a HashMap for the variable bindings, every variable is identified with its fullVName which is unique to it


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
    executeStatement(init.entry);
    for (State s : init.states)
    {  
      executeStatement(s.entry);

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
  // execution for a list-of-statements
  private void executeStatementList(Statement statement) throws Exception
  {
    if(statement instanceof StatementList)
    {
      List<Statement> st_list = ((StatementList)statement).getStatements();
      for(Statement st : st_list)
        executeStatementList(st);
    }
    else 
    {
      try 
      {
        executeStatement(statement); 
      }
      catch (Exception e)
      {
        System.out.println("Execute Statement inside executeStatementList failed!\n");
      }
    }
  }
  // as of now only executes single-assignment-statement
  private void executeStatement(Statement statement) throws Exception 
  {
    try 
    {
      if(statement instanceof StatementList)
        this.executeStatementList(statement);
      else if(statement instanceof AssignmentStatement)
        this.executeAssignmentStatement((AssignmentStatement)statement);
      else if(statement instanceof IfStatement)
        this.executeConditionalStatement((IfStatement)statement);
      else if(statement instanceof WhileStatement)
        this.executeWhileStatement((WhileStatement)statement);
      else if(statement instanceof ExpressionStatement){
        //this.evaluateExpression(((ExpressionStatement)statement).expression);
        EvaluateExpression.evaluate(((ExpressionStatement)statement).expression);
      }
      // I still do not completely understand what the next 2 statements do, right now the behavior is defined by my rudimentary understanding
      else if(statement instanceof SkipStatement) 
        return ;
      else if(statement instanceof HaltStatement)
        System.exit(0);
      else
      {
        System.out.println("The Statement Type Could Not Be Identified!\n");
        System.out.println(statement.getClass());
        return ;
      }
    }
    catch (Exception e)
    {
      System.out.println("Execute Statement Failed!\n");
    }
  }
  // executes an assignment statement
  private void executeAssignmentStatement(AssignmentStatement assignment) throws Exception
  {
    try {
    Declaration variableDeclaration = assignment.lhs.getDeclaration();
    //map.put(variableName, evaluateExpression(assignment.rhs));
    map.put(variableDeclaration, EvaluateExpression.evaluate(assignment.rhs));

    }
    catch (Exception exc)
    {
      System.out.println("Assignment Statement Failed!\n");
    }
  }
  // takes an epression and asserts whether it is of a constant type or not
  static boolean isConstantExpression(Expression e) 
  {
    if(e instanceof BooleanConstant || e instanceof IntegerConstant || e instanceof StringLiteral)
      return true;
    return false;
  }
  
  // adding conditional
  private void executeConditionalStatement(IfStatement c) throws Exception
  {
    try 
    {
    // the condition would either be a straight-forward constant (although it would not make sense to have it at all then) or a binary expression
      if(c.condition instanceof BooleanConstant)
      {
        if(((BooleanConstant)c.condition).value)
          executeStatement(c.then_body);
        else
          executeStatement(c.else_body);
      }
      else if(c.condition instanceof BinaryExpression)
      {
        //Expression e = evaluateBinaryExpression((BinaryExpression)c.condition);
        Expression e = EvaluateExpression.evaluate((BinaryExpression)c.condition);
        if(((BooleanConstant)e).value)
          executeStatement(c.then_body);
        else
          executeStatement(c.else_body);
      }
      
    }
    catch (Exception e)
    {
      System.out.println("Execute Statement Failed Inside ExecuteConditionalStatement\n");
    }
  }
  // evaluate expression - I am not sure on how much we need this
  private void executeWhileStatement(WhileStatement w) throws Exception
  {
    // the condition would either be a straight-forward constant (in an infinite loop like scenarios) or a binary expression
    try
    {
      if(w.condition instanceof BooleanConstant)
      {
        if(((BooleanConstant)w.condition).value)
        {
          executeStatement(w.body);
          executeStatement(w);
        }
      }
      else if(w.condition instanceof BinaryExpression)
      {
        //Expression e = evaluateBinaryExpression((BinaryExpression)w.condition);
        Expression e = EvaluateExpression.evaluate((BinaryExpression)w.condition);
        if(((BooleanConstant)e).value)
        {
          executeStatement(w.body);
          executeStatement(w);
        }
      }
          return ;
    }
    catch (Exception e)
    {
      System.out.println("Execute Statement Failed Inside ExecuteWhileStatement\n");
    }
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
            executeStatement(curr.exit);
            System.out.println("Final State Map: ");
            displayMap();
            System.out.println("+--------------------------------------------------+");
            executeStatement(t.action);
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

