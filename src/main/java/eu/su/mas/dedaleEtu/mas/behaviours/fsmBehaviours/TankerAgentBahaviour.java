package eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours;

import java.util.Objects;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge;
import jade.core.behaviours.OneShotBehaviour;

public class TankerAgentBahaviour extends OneShotBehaviour {

  private static final long serialVersionUID = -9218639616621586525L;
  
  private final AgentKnowledge agentKnowledge;
  
  private boolean initialExplorationIsDone = false;
  
  private int nextStateTransition;
  
  public TankerAgentBahaviour(AbstractDedaleAgent agent, AgentKnowledge agentKnowledge) {
    super(agent);
    Objects.requireNonNull(agentKnowledge);
    this.agentKnowledge = agentKnowledge;
  }
  
  @Override
  public void action() {
    var agent = (AbstractDedaleAgent)myAgent;
    var currentPosition = agent.getCurrentPosition();
    if (currentPosition == null) {
      System.out.println("The agent " + myAgent.getLocalName() + " does not exist in the environment");
      return;
    }
    
    try {
      agent.doWait(AgentKnowledge.MOVEMENT_WAITING_DURATION);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    nextStateTransition = 1;
    if (initialExplorationIsDone) {
      if (agentKnowledge.allAgentsTerminate()) {
        nextStateTransition = 0;
        //agentKnowledge.a();
      }
      return;
    }
    var observations = agent.observe();
    agentKnowledge.updateAgentKnowledge(currentPosition, observations);
    var edges = agentKnowledge.map().getAllEdges();
    agentKnowledge.initializeTankerAgentKnowledge(agent.getLocalName(),
                                                  currentPosition.getLocationId(),
                                                  edges.stream()
                                                  .map(e -> e.left().equals(currentPosition.getLocationId()) ? e.right() : e.left())
                                                  .toList());
    initialExplorationIsDone = true;
  }
  
  @Override
  public int onEnd() {
    return nextStateTransition;
  }
}
