package eu.su.mas.dedaleEtu.mas.behaviours.fsm;

import java.util.List;
import java.util.Objects;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.utils.WaitingList;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;


public class Broadcast extends OneShotBehaviour {

  private static final long serialVersionUID = 2975061924452119586L;
  
  @Override
  public void action() {
    if (++cycleCnt < 10) {
      return;
    }
    cycleCnt = 0;
    
    var msg = new ACLMessage(ACLMessage.INFORM);
    msg.setProtocol("BROADCAST");
    msg.setSender(myAgent.getAID());
    
    for (var agentName : agentNames) {
      msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
      waitingList.add(agentName, "BROADCAST", "BROADCAST-ACK", WAIT_DURATION_FOR_BROADCAST_MS);
    }
    ((AbstractDedaleAgent)myAgent).sendMessage(msg);
  }
  
  private final List<String> agentNames;
  private final WaitingList waitingList;
  private int cycleCnt = 0;
  
  private final static long WAIT_DURATION_FOR_BROADCAST_MS = 10;
  
  public Broadcast(
      AbstractDedaleAgent agent,
      List<String> agentNames,
      WaitingList waitingList) {
    super(agent);
    Objects.requireNonNull(agentNames);
    Objects.requireNonNull(waitingList);
    
    this.agentNames = agentNames;
    this.waitingList = waitingList;
  }
}
