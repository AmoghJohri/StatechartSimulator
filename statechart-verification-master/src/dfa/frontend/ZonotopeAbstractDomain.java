package frontend;

import ast.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class ZonotopeAbstractDomain 
{
    // takes an atomic state as an input, reads all the variables and the actions defined in it
    // execution takes place as follow: entry action --> exit actions : and the interval values are given out at the time of exit
    
    // all the instructions to be executed are stored here
    private List<Statement> listOfInstructions = new ArrayList<Statement>();
    
    // all the required variables are initialized here
    private Map<String, Expression> initVariables = new HashMap<String, Expression>();

    // maps each variable to its abstract value
    private Map<String, List<Float>> abstractDomain = new HashMap<String, List<Float>>();

    ZonotopeAbstractDomain(State s, Map<String, Expression> map)
    {
        for(Declaration d : s.getEnvironment().getAllDeclarations())
        {
            if(map.get(d.getFullVName()) != null)
            {
                // as right now the only numerical type is int
                if(("int").equals(d.typeName.toString()))
                {
                    ArrayList<Float> centralValue = new ArrayList<Float>();
                    initVariables.put(d.getFullVName(), map.get(d.getFullVName()));
                    if(map.get(d.getFullVName()) instanceof IntegerConstant)
                    {
                        centralValue.add(((Integer)(((IntegerConstant)(map.get(d.getFullVName()))).value)).floatValue());
                        abstractDomain.put(d.getFullVName(), centralValue);
                    }
                }
            }     
        }
        // there can be assignments, conditionals, loops and expressions
        listOfInstructions.add(s.entry);
        listOfInstructions.add(s.exit);
    }

    // takes the zonotope domain value and returns the interval
    private List<Float> get_interval(List<Float> l) 
    {
        float centralValue = l.get(0);
        float lhs = centralValue;
        float rhs = centralValue;
        for(Float f : l)
        {
            lhs = lhs - Math.abs(f);
            rhs = rhs + Math.abs(f);
        }
        List<Float> output = new ArrayList<Float>();
        output.add(lhs);
        output.add(rhs);
        return output;
    }

    // executing the instructions
    private void executeInstruction(Statement s)
    {
        if(s instanceof StatementList)
        {
            List<Statement> st_list = ((StatementList)s).getStatements();
            for(Statement st : st_list)
                executeInstruction(st);
        }
        else // here we have an atomic statement
        {

        }
    }

    private void executeAssignmentInstruction(AssignmentStatement s)
    {
        abstractDomain.put((s.lhs).getDeclaration().getFullVName(), evaluateExpression(s.rhs));
    }

    private List<Float> getAbstractValue(Expression e)
    {
        List<Float> val = new ArrayList<Float>();
        if(e instanceof IntegerConstant)
        {
            val.add((((Integer)((IntegerConstant)e).value).floatValue()));
        }
        else if(e instanceof Name)
        {
            val = abstractDomain.get(((Name)e).getDeclaration().getFullVName());
        }
        return val;
    }

    private List<Float> evaluateBinaryExpression(BinaryExpression e)
    {
        List<Float> lhs = null;
        List<Float> rhs = null;
        List<Float> val = new ArrayList<Float>();
        if(e.left instanceof IntegerConstant || e.left instanceof Name)
          lhs = getAbstractValue(e.left);
        else
          lhs = evaluateBinaryExpression((BinaryExpression)e.left);
        if(e.right instanceof IntegerConstant || e.right instanceof Name)
          rhs = getAbstractValue(e.right);
        else
          rhs = evaluateBinaryExpression((BinaryExpression)e.right);

        // operations
        if(e.operator.equals("+"))
        {
            int i = 0;
            while(i < lhs.size() && i < rhs.size())
            {
                val.add(lhs.get(i) + rhs.get(i));
            }
            for(;i < lhs.size();i++)
                val.add(lhs.get(i));
            for(;i < rhs.size();i++)
                val.add(rhs.get(i));
        }
        else if(e.operator.equals("-"))
        {
            int i = 0;
            while(i < lhs.size() && i < rhs.size())
            {
                val.add(lhs.get(i) - rhs.get(i));
            }
            for(;i < lhs.size();i++)
                val.add(lhs.get(i));
            for(;i < rhs.size();i++)
                val.add(-1*rhs.get(i));
        }
        else if(e.operator.equals("*"))
        {
            val.add(lhs.get(0) + rhs.get(0));
            float aux = 0;
            for(int i = 1; i < Integer.min(lhs.size(), rhs.size()); i++)
            {
                aux = aux + Math.abs(lhs.get(i) * rhs.get(i));
            }
            val.set(0, val.get(0) + aux/2);
            
            int i = 1;
            while(i < lhs.size() && i < rhs.size())
            {
                val.set(i, lhs.get(0)*rhs.get(i) + rhs.get(0)*lhs.get(i));
            }
            for(; i < lhs.size(); i++)
                val.set(i, lhs.get(i)*rhs.get(0));
            for(; i < rhs.size(); i++)
                val.set(i, rhs.get(i)*lhs.get(0));
            float aux2 = 0;
            for(i = lhs.size(); i < rhs.size(); i++)  
                lhs.add(((Integer)0).floatValue());
            for(i = rhs.size(); i < lhs.size(); i++)  
                rhs.add(((Integer)0).floatValue());
            for(i = 1; i < Integer.max(lhs.size(), rhs.size()); i++)
            {
                for(int j = i + 1; j < Integer.max(lhs.size(), rhs.size()); j++)
                {
                    aux2 = aux2 + Math.abs(lhs.get(i)* rhs.get(j) + lhs.get(j)*rhs.get(i));
                }
            }
            val.add(aux + aux2);     
        }
        // ignoring division for now
        return val;
    }

    private List<Float> evaluateExpression(Expression e)
    {
        List<Float> val = new ArrayList<Float>();
        if(e instanceof IntegerConstant)
        {   
            val.add(((Integer)((IntegerConstant)e).value).floatValue());
        }
        else if(e instanceof Name)
        {
            val = (ArrayList<Float>)abstractDomain.get(((Name)e).getDeclaration().getFullVName());
        }
        else // assuming the third type has to be a binary expression
            val = evaluateBinaryExpression((BinaryExpression) e);
        return val;
    }


    
}