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

  public FrontEnd(Statechart statechart)
  {
    this.parser = null; // for the time being
    this.statechart = statechart;
    simulation();

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
  private State getAtomicState(State state)
  {
    State init = state;
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
    // if the state is an atomic state itself
    return init;
  }

  // as of now only executes single-assignment-statement
  private void executeStatement(Statement statement)
  {
    if(statement instanceof AssignmentStatement)
      this.executeAssignmentStatement((AssignmentStatement)statement);
    else
    {
      return ;
    }
  }

  private void executeAssignmentStatement(AssignmentStatement assignment)
  {
    String variableName = assignment.lhs.getDeclaration().getFullVName();

    // if the rhs is a binary expression
    if(assignment.rhs instanceof BinaryExpression)  
      map.put(variableName, evaluateBinaryExpression((BinaryExpression)assignment.rhs));
    // if the rhs is a constant
    else if(assignment.rhs instanceof IntegerConstant || assignment.rhs instanceof StringLiteral || assignment.rhs instanceof BooleanConstant)
      map.put(variableName, assignment.rhs);
    else if(assignment.rhs instanceof Name)
      map.put(variableName, map.get(((Name)assignment.rhs).getDeclaration().getFullVName()));
    // still need to make provisions for unary expression
    else
      System.out.println("Nothing matched!\n");
  }

  private boolean isConstantExpression(Expression e) 
  {
    if(e instanceof BooleanConstant || e instanceof IntegerConstant || e instanceof StringLiteral)
      return true;
    return false;
  }

  private Expression evaluateBinaryExpression(BinaryExpression e) // takes in a binary expression and returns a constant basic type expression
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
  
  // displaying all variables - just to get an idea of the state
  private void displayMap()
  {
    for(Map.Entry<String, Expression> m : map.entrySet()){    
      System.out.println(m.getKey()+": "+m.getValue());    
     }  
  }
  
  public void simulation()
  {
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
          if(((BooleanConstant)t.guard).value) // this needs to be evaluated, as of now assuming it to be a BooleanConstant value
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
}
