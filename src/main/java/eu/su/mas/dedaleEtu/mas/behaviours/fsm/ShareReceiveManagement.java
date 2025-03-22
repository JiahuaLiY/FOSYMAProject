package eu.su.mas.dedaleEtu.mas.behaviours.fsm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.utils.WaitingList;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ShareReceiveManagement extends OneShotBehaviour {

  private static final long serialVersionUID = 8602656376798731220L;

  @Override
  public void action() {
    nextStateTransition = 0;

    if (receivedTopos.isEmpty()) {
      processReceivedTopos(); // merge map
      processBroadcastAck(); // share map
      
      processTopoShareAck(); // nothing
      processBroadcast(); // nothing
    }
    else {
      nextStateTransition = 2;
    }
    /*
    processReceivedTopos();
    if (receivedTopos.isEmpty()) {
      processBroadcastAck();
    }
    processTopoShareAck(); // nothing
    processBroadcast(); // nothing*/
    
    if (!waitingList.allTasksCompleted()) {
      System.out.println("----------\n" + myAgent.getLocalName() + " waits for \n" + waitingList + "\n----------"); 
    }
    
    // if nothing to do, and need wait.
    if (sharedWith.isEmpty() &&
        receivedTopos.isEmpty() &&
        !waitingList.allTasksCompleted()) {
      
      var blockTime = Math.max(waitingList.getMaxRemainingTime(), 1L);

      block(blockTime);
    }
  }
  
  @Override
  public int onEnd() {
    return nextStateTransition;
  }

  private int nextStateTransition = 0;
  
  private final WaitingList waitingList;
  private final Map<String, SerializableSimpleGraph<String, MapAttribute>> receivedTopos;
  private final List<String> sharedWith;
  
  private final static long WAIT_DURATION_FOR_TOPOLOGY_SHARE_MS = 25;
  //private final static long WAIT_DURATION_FOR_TOPOLOGY_SHARE_ACK_MS = 10;
  
  public ShareReceiveManagement(
      AbstractDedaleAgent agent,
      WaitingList waitingList,
      Map<String, SerializableSimpleGraph<String, MapAttribute>> receivedTopos,
      List<String> sharedWith) {
    super(agent);
    
    Objects.requireNonNull(waitingList);
    Objects.requireNonNull(receivedTopos);
    Objects.requireNonNull(sharedWith);
    
    this.waitingList = waitingList;
    this.receivedTopos = receivedTopos;
    this.sharedWith = sharedWith;
  }
  
  @SuppressWarnings("unchecked")
  private void processReceivedTopos() {
    var msgTemplate = MessageTemplate.and(
        MessageTemplate.MatchProtocol("TOPO-SHARE"),
        MessageTemplate.MatchPerformative(ACLMessage.INFORM));
    ACLMessage msg = null;
    
    var senders = new ArrayList<String>();
    while ((msg = myAgent.receive(msgTemplate)) != null) {
      SerializableSimpleGraph<String, MapAttribute> receivedMap = null;
      
      try {
        receivedMap = (SerializableSimpleGraph<String, MapAttribute>)msg.getContentObject();
      } catch (Exception e) {
        e.printStackTrace();
      }
      
      var senderName = msg.getSender().getLocalName();
      receivedTopos.put(senderName, receivedMap);
      senders.add(senderName);
    }
    
    if (!senders.isEmpty()) {
      nextStateTransition = 2;
      sendAck(senders, "TOPO-SHARE-ACK");
    }
  }
  
  private void processBroadcast() {
    var msgTemplate = MessageTemplate.and(
        MessageTemplate.MatchProtocol("BROADCAST"),
        MessageTemplate.MatchPerformative(ACLMessage.INFORM));
    ACLMessage msg = null;
    
    var senders = new ArrayList<String>();
    while ((msg = myAgent.receive(msgTemplate)) != null) {
      var senderName = msg.getSender().getLocalName();
      
      waitingList.add(senderName, "BROADCAST-ACK", "TOPO-SHARE", WAIT_DURATION_FOR_TOPOLOGY_SHARE_MS);
      waitingList.remove(senderName, "BROADCAST");
      senders.add(senderName);
    }
    
    if (!senders.isEmpty()) {
      sendAck(senders, "BROADCAST-ACK");
    }
  }
  
  private void processBroadcastAck() {
    var msgTemplate = MessageTemplate.and(
        MessageTemplate.MatchProtocol("BROADCAST-ACK"),
        MessageTemplate.MatchPerformative(ACLMessage.INFORM));
    ACLMessage msg = null;
    
    while ((msg = myAgent.receive(msgTemplate)) != null) {
      var senderName = msg.getSender().getLocalName();
      
      // waitingList.add(senderName, "TOPO-SHARE-ACK", WAIT_DURATION_FOR_TOPOLOGY_SHARE_ACK_MS);
      waitingList.remove(senderName, "TOPO-SHARE");
      sharedWith.add(senderName);
    }
    
    if (!sharedWith.isEmpty()) {
      nextStateTransition = 1;
    }
  }
  
  private void processTopoShareAck() {
    // to do
  }
  
  private void sendAck(List<String> receivers, String ackProtocol) {
    var ackMsg = new ACLMessage(ACLMessage.INFORM);
    ackMsg.setProtocol(ackProtocol);
    ackMsg.setSender(myAgent.getAID());
    for (var receiverName : receivers) {
      ackMsg.addReceiver(new AID(receiverName, AID.ISLOCALNAME));
    }
    ((AbstractDedaleAgent)myAgent).sendMessage(ackMsg);
  }
}
