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
  Map<String,Expression> map = new HashMap<String,Expression>();//Creating a HashMap for the variable bindings, every variable is identified with its fullVName which is unique to it


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
      map.put(d.getFullVName(), null);
    
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
        executeStatementList(statement);
    }
    // else, the statement is a single statement
    try {
    executeStatement(statement); }
    catch (Exception e)
    {
      System.out.println("Execute Statement inside executeStatementList failed!\n");
    }
  }
  // as of now only executes single-assignment-statement
  private void executeStatement(Statement statement) throws Exception 
  {
    try {
    if(statement instanceof AssignmentStatement)
      this.executeAssignmentStatement((AssignmentStatement)statement);
    else if(statement instanceof IfStatement)
      this.executeConditionalStatement((IfStatement)statement);
    else 
      return ;
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
    String variableName = assignment.lhs.getDeclaration().getFullVName();
    map.put(variableName, evaluateExpression(assignment.rhs));
    }
    catch (Exception exc)
    {
      System.out.println("Assignment Statement Failed!\n");
    }
  }
  // takes an epression and asserts whether it is of a constant type or not
  private boolean isConstantExpression(Expression e) 
  {
    if(e instanceof BooleanConstant || e instanceof IntegerConstant || e instanceof StringLiteral)
      return true;
    return false;
  }
  // evaluation for binary expressions, takes a binary expression and returns an Expression with a constant type (Integer, String or Bool)
  private Expression evaluateBinaryExpression(BinaryExpression e)
  {
    Expression lhs = null;
    Expression rhs = null;
    if(isConstantExpression(e.left))
      lhs = e.left;
    else if(e.left instanceof Name)
      lhs =  map.get(((Name)e.left).getDeclaration().getFullVName());
    else
      lhs = evaluateBinaryExpression((BinaryExpression)e.left);
    if(isConstantExpression(e.right))
      rhs = e.right;
    else if(e.right instanceof Name)
      rhs = map.get(((Name)e.right).getDeclaration().getFullVName());
    else
      rhs = evaluateBinaryExpression((BinaryExpression)e.right);
    
    // for boolean 
    if(e.operator.equals("&&") && lhs instanceof BooleanConstant)
      return (Expression)(new BooleanConstant(((BooleanConstant)(lhs)).value && ((BooleanConstant)(rhs)).value));
    else if(e.operator.equals("||") && lhs instanceof BooleanConstant)
      return (Expression)(new BooleanConstant(((BooleanConstant)(lhs)).value || ((BooleanConstant)(rhs)).value));
    // for string
    else if(e.operator.equals("+") && lhs instanceof StringLiteral)
      return (Expression)(new StringLiteral(((StringLiteral)(lhs)).value + ((StringLiteral)(rhs)).value));
    // for integers which return integers
    else if(e.operator.equals("+") && lhs instanceof IntegerConstant)
      return (Expression)(new IntegerConstant(((IntegerConstant)(lhs)).value + ((IntegerConstant)(rhs)).value));
    else if(e.operator.equals("-") && lhs instanceof IntegerConstant)
      return (Expression)(new IntegerConstant(((IntegerConstant)(lhs)).value - ((IntegerConstant)(rhs)).value));
    else if(e.operator.equals("/") && lhs instanceof IntegerConstant)
      return (Expression)(new IntegerConstant(((IntegerConstant)(lhs)).value / ((IntegerConstant)(rhs)).value));
    else if(e.operator.equals("*") && lhs instanceof IntegerConstant)
      return (Expression)(new IntegerConstant(((IntegerConstant)(lhs)).value * ((IntegerConstant)(rhs)).value));
    // for integers which return boolean
    else if(e.operator.equals(">=") && lhs instanceof IntegerConstant)
      return (Expression)(new BooleanConstant(((IntegerConstant)(lhs)).value >= ((IntegerConstant)(rhs)).value));
    else if(e.operator.equals(">") && lhs instanceof IntegerConstant)
      return (Expression)(new BooleanConstant(((IntegerConstant)(lhs)).value > ((IntegerConstant)(rhs)).value));
    else if(e.operator.equals("<=") && lhs instanceof IntegerConstant)
      return (Expression)(new BooleanConstant(((IntegerConstant)(lhs)).value <= ((IntegerConstant)(rhs)).value));
    else if(e.operator.equals("<") && lhs instanceof IntegerConstant)
      return (Expression)(new BooleanConstant(((IntegerConstant)(lhs)).value < ((IntegerConstant)(rhs)).value));
    else if(e.operator.equals("=") && lhs instanceof IntegerConstant)
      return (Expression)(new BooleanConstant(((IntegerConstant)(lhs)).value == ((IntegerConstant)(rhs)).value));
    else if(e.operator.equals("!=") && lhs instanceof IntegerConstant)
      return (Expression)(new BooleanConstant(((IntegerConstant)(lhs)).value != ((IntegerConstant)(rhs)).value));
    else  
      return null;
    
  }
  // adding conditional
  private void executeConditionalStatement(IfStatement c) throws Exception
  {
    try {
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
      Expression e = evaluateBinaryExpression((BinaryExpression)c.condition);
      if(((BooleanConstant)e).value)
        executeStatement(c.then_body);
      else
        executeStatement(c.else_body);
    }
    }
    catch (Exception e)
    {
      System.out.println("executeConditionalStatement Failed!\n");
    }
  }
  // evaluate expression - I am not sure on how much we need this
  private Expression evaluateExpression(Expression e) throws Exception
  {
    try {
      if(e instanceof BinaryExpression)
        return evaluateBinaryExpression((BinaryExpression)e);
      else if(e instanceof Name)
        return map.get(((Name)e).getDeclaration().getFullVName());
      else if(e instanceof BooleanConstant || e instanceof IntegerConstant || e instanceof StringLiteral)
        return e;
    }
    catch (Exception exc)
    {
      System.out.println("Unknown Type of Expression!\n");
      return null;
    }
    return null; // the execution should not get to here
  }

  // displaying all variables - just to get an idea of the state
  private void displayMap()
  {
    for(Map.Entry<String, Expression> m : map.entrySet()){    
      System.out.println(m.getKey()+": "+m.getValue());    
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

      input.nextLine();
      System.out.println("State: " + curr.getFullName());
      System.out.println("Map: ");
      displayMap();

      for(Transition t : transitions) // sequentially executing very transition (only considering the scenarion where the transition is present in the parent and transition occurs in its child states)
      {
        if(t.getSource() == curr)
        {
          if(((BooleanConstant)evaluateExpression(t.guard)).value) // this needs to be evaluated, as of now assuming it to be a BooleanConstant value
          {
            executeStatement(curr.exit);
            executeStatement(t.action);
            curr = t.getDestination();
            break;
          }
        }
      }
    }
   }
  catch (Exception e)
  {
    System.out.println("Simulation Failed!\n"); 
  }
}
}
