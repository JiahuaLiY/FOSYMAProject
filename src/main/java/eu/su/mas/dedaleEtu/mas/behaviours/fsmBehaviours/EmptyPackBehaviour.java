package eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours;

import java.util.Objects;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge.AgentMode;
import jade.core.behaviours.OneShotBehaviour;

public final class EmptyPackBehaviour extends OneShotBehaviour {

  private static final long serialVersionUID = 2601992555243070970L;

  private final AgentKnowledge agentKnowledge;
  
  private int nextStateTransition;
  
  public EmptyPackBehaviour(AbstractDedaleAgent agent, AgentKnowledge agentKnowledge) {
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
    if (agentKnowledge.reachDestination()) {
      agentKnowledge.setAttemptPositionID(null);
      agentKnowledge.resetShortestPath();
      
      //System.out.println(currentPosition);
      System.out.println(agentKnowledge.getTankerAgentName());
      System.out.println(System.currentTimeMillis());
      
      if (agent.emptyMyBackPack(agentKnowledge.getTankerAgentName())) {
        System.err.println(agent.getLocalName() + " " + agentKnowledge.currentPackCapacity() + " " + agentKnowledge.initialPackCapacity());
        if (!agentKnowledge.isPackFree()) {
          nextStateTransition = 1;
        }
        else {
          nextStateTransition = 0;
          agentKnowledge.setAgentMode(AgentMode.TREASURE_COLLECT);
        }
      }
      else {
        nextStateTransition = 1;
      }
      System.out.println(System.currentTimeMillis());
      return;
    }
    
    
    nextStateTransition = 1;
    if (agentKnowledge.isDeadlock()) {
      nextStateTransition = 2;
      return;
    }
    else {
      var nextPositionID = agentKnowledge.getNextPositionID(observations);
      if (nextPositionID.isEmpty()) {
        if (agentKnowledge.reachDestination()) {
          agentKnowledge.setAttemptPositionID(null);
          //agentKnowledge.resetShortestPath();
          return;
        }
        nextStateTransition = 2;
        //agentKnowledge.setAgentMode(AgentMode.TREASURE_COLLECT);
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
