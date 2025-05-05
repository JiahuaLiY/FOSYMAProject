package eu.su.mas.dedaleEtu.mas.agents.dummies;

import java.util.ArrayList;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.BroadcastBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.EndBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.ExploreBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.RandomSearchBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.RandomTerminationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.ShareReceiveBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.SolveDeadlockBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.TerminationInformBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge.AgentType;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;

public final class ExploreAgent extends AbstractDedaleAgent {

  private static final long serialVersionUID = 1585931133019386794L;

  protected void setup() {
    super.setup();
    
    final Object[] args = getArguments();
    var agentIdentifiers = new ArrayList<String>();
    if (args.length == 0) {
      System.err.println("Error while creating the agent, names of agent to contact expected");
      System.exit(-1);
    }
    for (var i = 2; i < args.length; i++) {
      agentIdentifiers.add((String)args[i]);
    }
    
    var behaviours = new ArrayList<Behaviour>();
    var fsm = new FSMBehaviour();
    var agentKnowledge = new AgentKnowledge(this, agentIdentifiers, AgentType.EXPLORE_AGENT);
    
    fsm.registerFirstState(new ExploreBehaviour(this, agentKnowledge), "MAP-EXPLORE");
    fsm.registerState(new BroadcastBehaviour(this, agentKnowledge), "BROADCAST");
    fsm.registerState(new ShareReceiveBehaviour(this, agentKnowledge), "SHARE-RECEIVE-MANAGEMENT");
    fsm.registerState(new SolveDeadlockBehaviour(this, agentKnowledge), "SOLVE-DEADLOCK");
    
    fsm.registerState(new TerminationInformBehaviour(this, agentKnowledge), "TERMINATION-INFORM");
    fsm.registerState(new RandomTerminationBehaviour(this, agentKnowledge), "RANDOM-TERMINATION");
    fsm.registerState(new RandomSearchBehaviour(this, agentKnowledge), "RANDOM-SEARCH");
    
    fsm.registerLastState(new EndBehaviour(this, agentKnowledge), "END");

    fsm.registerTransition("MAP-EXPLORE", "RANDOM-SEARCH", 0);
    fsm.registerTransition("MAP-EXPLORE", "BROADCAST", 1);
    fsm.registerTransition("MAP-EXPLORE", "SOLVE-DEADLOCK", 2);
    
    fsm.registerTransition("RANDOM-SEARCH", "TERMINATION-INFORM", 0);
    fsm.registerTransition("RANDOM-SEARCH", "BROADCAST", 1);
    fsm.registerTransition("RANDOM-SEARCH", "SOLVE-DEADLOCK", 2);
    
    //fsm.registerTransition("TERMINATION-INFORM", "RANDOM-TERMINATION", 0);
    fsm.registerTransition("TERMINATION-INFORM", "BROADCAST", 1);
    fsm.registerTransition("TERMINATION-INFORM", "SOLVE-DEADLOCK", 2);
    
    fsm.registerTransition("RANDOM-TERMINATION", "END", 0);
    fsm.registerTransition("RANDOM-TERMINATION", "BROADCAST", 1);
    fsm.registerTransition("RANDOM-TERMINATION", "SOLVE-DEADLOCK", 2);
    
    fsm.registerDefaultTransition("SOLVE-DEADLOCK", "BROADCAST");
    fsm.registerDefaultTransition("BROADCAST", "SHARE-RECEIVE-MANAGEMENT");
    
    fsm.registerTransition("SHARE-RECEIVE-MANAGEMENT", "MAP-EXPLORE", 1);
    //fsm.registerTransition("SHARE-RECEIVE-MANAGEMENT", "TREASURE-COLLECT", 2);
    //fsm.registerTransition("SHARE-RECEIVE-MANAGEMENT", "EMPTY-PACK", 3);
    fsm.registerTransition("SHARE-RECEIVE-MANAGEMENT", "RANDOM-SEARCH", 4);
    fsm.registerTransition("SHARE-RECEIVE-MANAGEMENT", "TERMINATION-INFORM", 5);
    fsm.registerTransition("SHARE-RECEIVE-MANAGEMENT", "RANDOM-TERMINATION", 6);
    
    behaviours.add(fsm);
    addBehaviour(new StartMyBehaviours(this, behaviours));
    System.out.println("the agent " + getLocalName() + " is started");
  }
  
  protected void takeDown() {
    super.takeDown();
  }

  protected void beforeMove( ){
    super.beforeMove();
  }

  protected void afterMove() {
    super.afterMove();
  }
}

