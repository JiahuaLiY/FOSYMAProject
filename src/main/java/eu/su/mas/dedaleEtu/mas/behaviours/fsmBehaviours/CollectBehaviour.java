package eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours;

import java.util.Objects;

import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge.AgentMode;
import jade.core.behaviours.OneShotBehaviour;

public final class CollectBehaviour extends OneShotBehaviour {

  private static final long serialVersionUID = -2479689428950965278L;
  
  private final AgentKnowledge agentKnowledge;
  
  private int nextStateTransition;
  
  public CollectBehaviour(AbstractDedaleAgent agent, AgentKnowledge agentKnowledge) {
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

      var treasureKnowledge = agentKnowledge.getTreasure(agentKnowledge.currentPositionID());
      if (treasureKnowledge.amount() <= 0 || !treasureKnowledge.type().equals(agent.getMyTreasureType())) {
        return;
      }
      
      if (treasureKnowledge.locked()) {
      //System.out.println(agent.getLocalName() + " " + observations);
      //System.out.println(agent.getLocalName() + " before " + treasureKnowledge.locked());
      //System.out.println(agent.getLocalName() + " open ? " + agent.openLock(agent.getMyTreasureType()));
        agent.openLock(agent.getMyTreasureType());
      }
      agent.pick();
      
      observations = agent.observe();
      //System.out.println(observations);
      var currentPositionObservation = observations.stream()
          .filter(couple -> couple.getLeft().equals(currentPosition))
          .toList()
          .getFirst()
          .getRight();
      agentKnowledge.updateTreasureAfterPicking(agentKnowledge.currentPositionID(), currentPositionObservation);
      treasureKnowledge = agentKnowledge.getTreasure(agentKnowledge.currentPositionID());
      //System.out.println(agent.getLocalName() + " " + currentPositionObservation);
      //System.out.println(agent.getLocalName() + " after " + treasureKnowledge.locked());
      if (treasureKnowledge.amount() == 0) {
        agentKnowledge.increaseMyTreasureCollectionCounter();
        //System.out.println(agent.getLocalName());
      }
      
      //if (agentKnowledge.currentPackCapacity() > 0 && !agentKnowledge.pickableTreasurePositions().isEmpty()) {
      
      
      if (agentKnowledge.canContinueToCollect()) {  
        nextStateTransition = 1;
        return;
      }
      nextStateTransition = 0; // Go to empty pack phase.
      agentKnowledge.setAgentMode(AgentMode.EMPTY_PACKAGE);
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
        }
        else if (agentKnowledge.isTreasureCollectionTaskCompleted()) {
          agentKnowledge.showAgentKnowledge();
          agentKnowledge.setAttemptPositionID(null);
          agentKnowledge.resetShortestPath();
          nextStateTransition = 4; // Go to termination inform phase.
          agentKnowledge.setAgentMode(AgentMode.TERMINATION_INFORM);
        }
        else if (!agentKnowledge.pickableTreasurePositions().isEmpty()) {
          System.out.println(agentKnowledge.pickableTreasurePositions());
          nextStateTransition = 2; // Go to solve deadlock phase.
        }
        else {
          System.out.println(agent.getLocalName() + " go to random search");
          agentKnowledge.showAgentKnowledge();
          agentKnowledge.setAttemptPositionID(null);
          agentKnowledge.resetShortestPath();
          nextStateTransition = 3;
          agentKnowledge.setAgentMode(AgentMode.RANDOM_SEARCH);
        }
        return;
        /*
        if (agentKnowledge.reachDestination()) {
          agentKnowledge.setAttemptPositionID(null);
        }
        else if (!agentKnowledge.isPackFree()) {
          System.out.println(agent.getBackPackFreeSpace());
          nextStateTransition = 0;
          agentKnowledge.setAgentMode(AgentMode.EMPTY_PACKAGE);
        }
        else if (agentKnowledge.isTreasureCollectionTaskCompleted()){
          nextStateTransition = 4; // Go to random end explore (i.e. move to a random position and end the game).
          agentKnowledge.setAgentMode(AgentMode.TERMINATION_INFORM);
          agentKnowledge.print();
        }
        else if (!agentKnowledge.pickableTreasurePositions().isEmpty()){
          agentKnowledge.setAttemptPositionID(null);
          agentKnowledge.resetShortestPath();
        }
        else {
          // TODO
          System.out.println("oups");
          agentKnowledge.print();
          agentKnowledge.setAgentMode(AgentMode.TERMINATION_INFORM);
          nextStateTransition = 4; // Go to find missing treasure explore.
        }
        return;*/
      }
      agent.moveTo(new GsLocation(nextPositionID.get()));
    }
  }
  
  @Override
  public int onEnd() {
    return nextStateTransition;
  }

}
