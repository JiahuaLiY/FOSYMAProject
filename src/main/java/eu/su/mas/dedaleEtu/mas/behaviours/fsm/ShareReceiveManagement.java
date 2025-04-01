package eu.su.mas.dedaleEtu.mas.behaviours.fsm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.DataShare;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.utils.MapContainer;
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

    /*if (receivedTopos.isEmpty()) {
      processReceivedTopos(); // merge map
      processBroadcastAck(); // share map
      
      processTopoShareAck(); // nothing
      processBroadcast(); // nothing
    }
    else {
      nextStateTransition = 2;
    }*/
    if (sharedWith.isEmpty()) {
      processReceivedTopos(); // merge map
      if (receivedTopos.isEmpty()) {
        processBroadcastAck(); // share map
      }
      
      processTopoShareAck();
      processBroadcast();
    }
    else {
      nextStateTransition = SHARE_TRANSITION;
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
    if (nextStateTransition == END_TRANSITION) {
      waitingList.clearAllTasks();
    }
    return nextStateTransition;
  }

  private int nextStateTransition = END_TRANSITION;
  
  private final MapContainer mapContainer;
  private final WaitingList waitingList;
  private final Map<String, DataShare> receivedTopos;
  private final List<String> sharedWith;
  
  public final static long WAIT_DURATION_FOR_TOPOLOGY_SHARE_MS = 100;
  public final static long WAIT_DURATION_FOR_TOPOLOGY_SHARE_ACK_MS = 50;
  
  public final static int END_TRANSITION = 0;
  public final static int SHARE_TRANSITION = 1;
  public final static int MERGE_TRANSITION = 2;
  
  public ShareReceiveManagement(
      AbstractDedaleAgent agent,
      WaitingList waitingList,
      Map<String, DataShare> receivedTopos,
      List<String> sharedWith,
      MapContainer mapContainer) {
    super(agent);
    
    Objects.requireNonNull(mapContainer);
    Objects.requireNonNull(waitingList);
    Objects.requireNonNull(receivedTopos);
    Objects.requireNonNull(sharedWith);
    
    this.mapContainer = mapContainer;
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
      DataShare receivedMap = null;
      
      try {
        receivedMap = (DataShare)msg.getContentObject();
      } catch (Exception e) {
        e.printStackTrace();
      }
      
      var senderName = msg.getSender().getLocalName();
      receivedTopos.put(senderName, receivedMap);
      senders.add(senderName);
      waitingList.remove(senderName, "TOPO-SHARE");
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
      
      //if (waitingList.remove(senderName, "BROADCAST-ACK")) {
      waitingList.remove(senderName, "BROADCAST-ACK");
      waitingList.add(senderName, "TOPO-SHARE", "TOPO-SHARE-ACK", WAIT_DURATION_FOR_TOPOLOGY_SHARE_ACK_MS);
      sharedWith.add(senderName);
      //}
    }
    
    if (!sharedWith.isEmpty()) {
      nextStateTransition = 1;
    }
  }
  
  private void processTopoShareAck() {
    var msgTemplate = MessageTemplate.and(
        MessageTemplate.MatchProtocol("TOPO-SHARE-ACK"),
        MessageTemplate.MatchPerformative(ACLMessage.INFORM));
    ACLMessage msg = null;
    
    while ((msg = myAgent.receive(msgTemplate)) != null) {
      var senderName = msg.getSender().getLocalName();
      
      if (waitingList.remove(senderName, "TOPO-SHARE-ACK")) {
        // clean map;
        mapContainer.map().cleanSet(senderName);
      }
    }
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
