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
import java.util.Set;
import java.util.HashSet;

import ast.*;
//

public class FrontEnd {
  private Statechart statechart = null;
  private final Parser parser;

  public FrontEnd(String input) throws FileNotFoundException {
    this.parser = new Parser(new Lexer(new FileReader(input)));
  }

  public FrontEnd(Statechart statechart)
  {
    this.parser = null; // for the time being
    this.statechart = statechart;
    simulation();

  }

  public Parser getParser() {
    return this.parser;
  }


  // gets the start state (atomic state) for the given state
  public State get_Start_State(State state)
  {
    while(!state.states.isEmpty()) // entering the first state of every statechart until we reach an atomic state. We declare that as the start state
      {
        state = state.states.get(0);
      }
      return state;
  }
  
  public void simulation()
  {
    System.out.println("Need to write this!\n");
  }
}
