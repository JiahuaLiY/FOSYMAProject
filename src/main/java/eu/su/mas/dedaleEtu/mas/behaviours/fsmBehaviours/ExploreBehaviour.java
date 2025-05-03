package eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours;

import java.util.Objects;

import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge.AgentMode;
import jade.core.behaviours.OneShotBehaviour;

public class ExploreBehaviour extends OneShotBehaviour {

  private static final long serialVersionUID = 7749048525683865672L;
  
  private final AgentKnowledge agentKnowledge;
  
  private int nextStateTransition;
  
  public ExploreBehaviour(AbstractDedaleAgent agent, AgentKnowledge agentKnowledge) {
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
    
    var observations = agent.observe();
    agentKnowledge.updateAgentKnowledge(currentPosition, observations);
    nextStateTransition = 1;
    if (agentKnowledge.isDeadlock()) {
      nextStateTransition = 2;
      return;
    }
    else {
      var nextPositionID = agentKnowledge.getNextPositionID(observations);
      if (nextPositionID.isEmpty()) {
        if (!agentKnowledge.map().hasOpenNode()) {
          agentKnowledge.setAttemptPositionID(null);
          agentKnowledge.resetShortestPath();
          nextStateTransition = 0; // Go to collect phase.
          agentKnowledge.setAgentMode(AgentMode.TREASURE_COLLECT);
          agentKnowledge.computeTreasureNumberRegister();
          agentKnowledge.print();
        }
        else {
          nextStateTransition = 2;
        }
        return;
      }
      agent.moveTo(new GsLocation(nextPositionID.get()));
    }
  }
  
  @Override
  public int onEnd() {
    return nextStateTransition;
  }
}
