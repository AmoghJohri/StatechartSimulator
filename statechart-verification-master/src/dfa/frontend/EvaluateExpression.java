package frontend;

import ast.*;

public class EvaluateExpression{
    public static Expression evaluate(Expression e){
        try {
          if(e instanceof BinaryExpression)
            return evaluateBinaryExpression((BinaryExpression)e);
          else if(e instanceof Name)
            return FrontEnd.map.get(((Name)e).getDeclaration().getFullVName());
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

    public static Expression evaluateBinaryExpression(BinaryExpression e){
        Expression lhs = null;
        Expression rhs = null;
        if(FrontEnd.isConstantExpression(e.left))
          lhs = e.left;
        else if(e.left instanceof Name)
          lhs =  FrontEnd.map.get(((Name)e.left).getDeclaration().getFullVName());
        else
          lhs = evaluateBinaryExpression((BinaryExpression)e.left);
        if(FrontEnd.isConstantExpression(e.right))
          rhs = e.right;
        else if(e.right instanceof Name)
          rhs = FrontEnd.map.get(((Name)e.right).getDeclaration().getFullVName());
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
        // this should rarely ever occur as we need to make sure we are dealing with integers
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
}
