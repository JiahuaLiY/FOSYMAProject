package eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge.AgentMode;
import jade.core.behaviours.OneShotBehaviour;

public final class SolveDeadlockBehaviour extends OneShotBehaviour {

  private static final long serialVersionUID = 3307251473223249221L;

  private final AgentKnowledge agentKnowledge;
  
  private final Random randomGenerator = new Random();
  
  public SolveDeadlockBehaviour(AbstractDedaleAgent agent, AgentKnowledge agentKnowledge) {
    super(agent);
    
    Objects.requireNonNull(agentKnowledge);
    this.agentKnowledge = agentKnowledge;
  }
  
  @Override
  public void action() {
    var agent = (AbstractDedaleAgent)myAgent;
    try {
      agent.doWait(randomGenerator.nextLong(AgentKnowledge.MAX_DEADLOCK_WAITING_DURATION) + 50);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    var observations = agent.observe();
    
    //agentKnowledge.resetShortestPath();
    
    var currentPositionID = agentKnowledge.currentPositionID();
    var attemptPositionID = agentKnowledge.attemptPositionID();
    if (attemptPositionID != null) {
      // Check whether there is an agent in the attempt position.
      var canMoveTo = observations.stream()
          .filter(couple -> !couple.getLeft().getLocationId().equals(currentPositionID) && couple.getLeft().getLocationId().equals(attemptPositionID))
          .allMatch(couple -> couple.getRight().stream()
              .allMatch(observation -> !observation.getLeft().equals(Observation.AGENTNAME)));
      if (canMoveTo) {
        agentKnowledge.setAttemptPositionID(attemptPositionID);
        System.out.println("Agent " + agent.getLocalName() + " moves from " + currentPositionID + " to " + attemptPositionID);
        agent.moveTo(new GsLocation(attemptPositionID));
        return;
      }
    }
    
    observations = agent.observe();
    if (agentKnowledge.computeShortestPathToDestination(observations)) {
      agentKnowledge.setAttemptPositionID(null);
      System.out.println("Agent " + myAgent.getLocalName() + " uses another shortest path from " + currentPositionID);
      return;
    }
    
    if (agentKnowledge.mode().equals(AgentMode.EMPTY_PACKAGE)) {
      agentKnowledge.resetShortestPath();
      agentKnowledge.setAttemptPositionID(null);
      return;
    }
    
    observations = agent.observe();
    
    agentKnowledge.resetShortestPath();
    agentKnowledge.setAttemptPositionID(null);
    
    var accessibleNodes = observations.stream()
        .filter(couple -> !couple.getLeft().getLocationId().equals(currentPositionID) && couple.getRight().stream()
            .allMatch(observation -> !observation.getLeft().equals(Observation.AGENTNAME)))
        .map(couple -> couple.getLeft().getLocationId())
        .collect(Collectors.toCollection(ArrayList::new));
    
    if (accessibleNodes.isEmpty()) {
      return;
    }
    var index = randomGenerator.nextInt(accessibleNodes.size());
    var nextPositionID = accessibleNodes.get(index);
    System.out.println("Agent " + myAgent.getLocalName() + " uses random move from " + currentPositionID + " to " + nextPositionID);
    agentKnowledge.setAttemptPositionID(nextPositionID);
    agent.moveTo(new GsLocation(nextPositionID));
  }
}
