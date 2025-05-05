package eu.su.mas.dedaleEtu.mas.agents.dummies;

import java.util.ArrayList;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.BroadcastBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.EndBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.ShareReceiveBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.TankerAgentBahaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge.AgentType;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;

public final class TankerAgent extends AbstractDedaleAgent {

  private static final long serialVersionUID = -2242538629089665353L;

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
    var agentKnowledge = new AgentKnowledge(this, agentIdentifiers, AgentType.TANKER_AGENT);
    
    fsm.registerFirstState(new TankerAgentBahaviour(this, agentKnowledge), "TANKER");
    fsm.registerState(new BroadcastBehaviour(this, agentKnowledge), "BROADCAST");
    fsm.registerState(new ShareReceiveBehaviour(this, agentKnowledge), "SHARE-RECEIVE-MANAGEMENT");
    fsm.registerLastState(new EndBehaviour(this, agentKnowledge), "END");

    fsm.registerTransition("TANKER", "END", 0);
    fsm.registerTransition("TANKER", "BROADCAST", 1);
    fsm.registerDefaultTransition("BROADCAST", "SHARE-RECEIVE-MANAGEMENT");
    fsm.registerDefaultTransition("SHARE-RECEIVE-MANAGEMENT", "TANKER");
    
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
