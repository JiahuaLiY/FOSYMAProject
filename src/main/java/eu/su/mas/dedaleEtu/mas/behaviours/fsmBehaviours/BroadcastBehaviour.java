package eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours;

import java.util.Objects;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;


public final class BroadcastBehaviour extends OneShotBehaviour {

  private static final long serialVersionUID = 2975061924452119586L;
  
  private final AgentKnowledge agentKnowledge;
  
  public BroadcastBehaviour(AbstractDedaleAgent agent, AgentKnowledge agentKnowledge) {
    super(agent);
    Objects.requireNonNull(agentKnowledge);
    this.agentKnowledge = agentKnowledge;
  }

  @Override
  public void action() {
    var sentProtocol = "BROADCAST";
    var waitForProtocol = "BROADCAST-ACK";
    var msg = new ACLMessage(ACLMessage.INFORM);
    msg.setProtocol(sentProtocol);
    msg.setSender(myAgent.getAID());
    var waitingProtocolsList = agentKnowledge.waitingProtocolsList();
    for (var agentID: agentKnowledge.agentIdentifiers()) {
      if (!agentKnowledge.sendingRequired(agentID)) {
        continue;
      }
      msg.addReceiver(new AID(agentID, AID.ISLOCALNAME));
      waitingProtocolsList.add(agentID, sentProtocol, waitForProtocol, AgentKnowledge.BROADCAST_WAITING_DURATION);
    }
    ((AbstractDedaleAgent)myAgent).sendMessage(msg);
  }
}
