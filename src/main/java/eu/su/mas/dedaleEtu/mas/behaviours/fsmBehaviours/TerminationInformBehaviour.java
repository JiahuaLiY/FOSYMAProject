package eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours;

import java.util.List;
import java.util.Objects;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge.AgentMode;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public final class TerminationInformBehaviour extends OneShotBehaviour {

  private static final long serialVersionUID = 4337320812442557488L;
  
  private final AgentKnowledge agentKnowledge;
  
  private int nextStateTransition;
  
  private boolean isSentTerminationInform = false;
  
  public TerminationInformBehaviour(AbstractDedaleAgent agent, AgentKnowledge agentKnowledge) {
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
    //System.out.println(agent.getLocalName() + " in sen" + agentKnowledge.reachDestination());
    if (agentKnowledge.reachDestination()) {
      //System.out.println(agentKnowledge.destinationPositionID());
      //System.out.println(agentKnowledge.getAdjacentPositionsToTankerPosition());
      var tankerAgentName = getTankerAgentNameToSentTerminationInform(currentPosition, observations);
      if (!isSentTerminationInform) {
        var sentProtocol = "TERMINATION-INFORM";
        var msg = new ACLMessage(ACLMessage.INFORM);
        msg.setProtocol(sentProtocol);
        msg.setSender(myAgent.getAID());
        msg.addReceiver(new AID(tankerAgentName, AID.ISLOCALNAME));
        ((AbstractDedaleAgent)myAgent).sendMessage(msg);
        System.out.println(agent.getLocalName() +  " send termination inform to the tanker agent " + tankerAgentName + " " + System.currentTimeMillis());
        isSentTerminationInform = true;
      }
      var msgTemplate = MessageTemplate.and(
          MessageTemplate.MatchPerformative(ACLMessage.INFORM),
          MessageTemplate.MatchProtocol("TERMINATION-INFORM-ACK"));
      //ACLMessage msg = myAgent.receive(msgTemplate);
      ACLMessage msg = null;
      if ((msg = myAgent.receive(msgTemplate)) == null) {
        block();
      }
      else {
        //System.out.println("receive " + agent.getLocalName() + msg.getSender().getLocalName());
        tankerAgentName = msg.getSender().getLocalName();
        System.out.println(agent.getLocalName() +  " successfully informs it termination to the tanker agent " + tankerAgentName + " " + System.currentTimeMillis());
        agentKnowledge.setAttemptPositionID(null);
        agentKnowledge.resetShortestPath();
        agentKnowledge.receiveTerminationInformAckFrom(tankerAgentName);
        agentKnowledge.showAgentKnowledge();
        if (agentKnowledge.receiveAllTerminationInformAcks()) {
          agentKnowledge.setAgentMode(AgentMode.RANDOM_TERMINATION);
        }
        else {
          isSentTerminationInform = false;
        }
      }
      nextStateTransition = 1;
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
          nextStateTransition = 1;
        }
        else {
          nextStateTransition = 2;
        }
        return;
      }
      agent.moveTo(new GsLocation(nextPositionID.get()));
    }
  }
  
  private String getTankerAgentNameToSentTerminationInform(Location currentPosition,
                                                           List<Couple<Location, List<Couple<Observation, String>>>> observations) {
    for (var couple: observations) {
      if (couple.getLeft().equals(currentPosition)) {
        continue;
      }
      for (var observation: couple.getRight()) {
        switch (observation.getLeft()) {
          case AGENTNAME:
            if (!agentKnowledge.isTankerAgentID(observation.getRight())
                || agentKnowledge.isTerminationInformAckDone(observation.getRight())) {
              break;
            }
            return observation.getRight();
          default:
            break;
        }
      }
    }
    return null;
  }
  
  @Override
  public int onEnd() {
    return nextStateTransition;
  }
}
