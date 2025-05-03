package eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge;
import eu.su.mas.dedaleEtu.mas.utils.DataContainer;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ShareReceiveBehaviour extends OneShotBehaviour {

  private static final long serialVersionUID = 8602656376798731220L;
  
  @Override
  public int onEnd() {
    agentKnowledge.waitingProtocolsList().clear();
    //while (myAgent.receive() != null);
    return switch (agentKnowledge.mode()) {
      case IMMOBILE -> 0;
      case MAP_EXPLORE -> 1;
      case TREASURE_COLLECT -> 2;
      case EMPTY_PACKAGE -> 3;
      case RANDOM_SEARCH -> 4;
      case TERMINATION_INFORM -> 5;
      case RANDOM_TERMINATION -> 6;
    };
  }
  
  @Override
  public void action() {
    processExploreBroadcast();
    processExploreBroadcastAck();
    processDataShare();
    processDataShareAck();
    if (agentKnowledge.isTankerAgent()) {
      processAgentTermination();
    }
    
    var waitingList = agentKnowledge.waitingProtocolsList();
    
    if (!waitingList.allCompleted()) {
      //System.out.println("--------------------\n" + myAgent.getLocalName() + ":\n" + waitingList + "\n--------------------");
      var blockedTime = Math.max(waitingList.getMaxRemainingTime(), 1L);
      block(blockedTime);
    }
  }

  private final AgentKnowledge agentKnowledge;
  
  public ShareReceiveBehaviour(AbstractDedaleAgent agent, AgentKnowledge agentKnowledge) {
    super(agent);  
    Objects.requireNonNull(agentKnowledge);
    this.agentKnowledge = agentKnowledge;
  }
  
  private void processExploreBroadcast() {
    var receivedProtocol = "BROADCAST";
    var sentProtocol = "BROADCAST-ACK";
    var waitForProtocol = "DATA-SHARE";
    
    var msgTemplate = MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
        MessageTemplate.MatchProtocol(receivedProtocol));
    ACLMessage msg = null;
    
    var waitingList = agentKnowledge.waitingProtocolsList();
    var senders = new HashSet<String>();
    while ((msg = myAgent.receive(msgTemplate)) != null) {
      var sender = msg.getSender().getLocalName();
      
      /*if (waitingList.remove(sender, receivedProtocol)) {
        senders.add(sender);
        //waitingList.remove(sender, receivedProtocol);
        waitingList.add(sender, sentProtocol, waitForProtocol, WAITING_DURATION_FOR_DATA_SHARE);
      }*/
      senders.add(sender);
      waitingList.remove(sender, receivedProtocol);
      waitingList.add(sender, sentProtocol, waitForProtocol, AgentKnowledge.DATA_SHARE_WAITING_DURATION);
    }
    
    if (!senders.isEmpty()) {
      sendAck(senders, sentProtocol);
    }
  }
  
  private void processExploreBroadcastAck() {
    var receivedProtocol = "BROADCAST-ACK";
    var sentProtocol = "DATA-SHARE";
    var waitForProtocol = "DATA-SHARE-ACK";
    
    var msgTemplate = MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
        MessageTemplate.MatchProtocol(receivedProtocol));
    ACLMessage msg = null;
    
    var waitingList = agentKnowledge.waitingProtocolsList();
    var senders = new HashSet<String>();
    while ((msg = myAgent.receive(msgTemplate)) != null) {
      var sender = msg.getSender().getLocalName();      
      if (waitingList.remove(sender, receivedProtocol)) {
        senders.add(sender);
        //waitingList.remove(sender, receivedProtocol);
        waitingList.add(sender, sentProtocol, waitForProtocol, AgentKnowledge.DATA_SHARE_ACK_WAITING_DURATION);
      }
    }
    
    if (!senders.isEmpty()) {
      shareData(senders);
    }
  }
  
  private void processDataShare() {
    var receivedProtocol = "DATA-SHARE";
    var sentProtocol = "DATA-SHARE-ACK";
    
    var msgTemplate = MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
        MessageTemplate.MatchProtocol(receivedProtocol));
    ACLMessage msg = null;
    
    var waitingList = agentKnowledge.waitingProtocolsList();
    var receivedDataMap = new HashMap<String, DataContainer>();
    while ((msg = myAgent.receive(msgTemplate)) != null) {
      DataContainer receivedData = null;
      
      try {
        receivedData = ((DataContainer) msg.getContentObject());
      } catch (Exception e) {
        e.printStackTrace();
      }
      
      var sender = msg.getSender().getLocalName();
      if (waitingList.remove(sender, receivedProtocol)) {
      //waitingList.remove(sender, receivedProtocol);
        receivedDataMap.put(sender, receivedData);
      }
    }
    if (!receivedDataMap.isEmpty()) {
      sendAck(receivedDataMap.keySet(), sentProtocol);
      mergeData(receivedDataMap);
    }
  }
  
  private void processDataShareAck() {
    var receivedProtocol = "DATA-SHARE-ACK";

    var msgTemplate = MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
        MessageTemplate.MatchProtocol(receivedProtocol));
    ACLMessage msg = null;
    
    var waitingList = agentKnowledge.waitingProtocolsList();
    while ((msg = myAgent.receive(msgTemplate)) != null) {
      var sender = msg.getSender().getLocalName();
      
      if (waitingList.remove(sender, receivedProtocol)) {
        agentKnowledge.cleanDataTracker(sender);
      }
    }
  }
  
  private void shareData(Collection<String> receivers) {
    for (var receiver: receivers) {
      var msg = new ACLMessage(ACLMessage.INFORM);
      msg.setProtocol("DATA-SHARE");
      msg.setSender(myAgent.getAID());
      msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
      
      var data = agentKnowledge.getDataToBeSent(receiver);
      try {
        msg.setContentObject(data);
      } catch (IOException e) {
        e.printStackTrace();
      }
      
      ((AbstractDedaleAgent)myAgent).sendMessage(msg);
    }
  }
  
  private void mergeData(Map<String, DataContainer> receivedDataMap) {
    for (var entry: receivedDataMap.entrySet()) {
      agentKnowledge.updateReceivedData(entry.getKey(), entry.getValue());
    }
  }
  
  private void sendAck(Collection<String> receivers, String ackProtocol) {
    var ackMsg = new ACLMessage(ACLMessage.INFORM);
    ackMsg.setProtocol(ackProtocol);
    ackMsg.setSender(myAgent.getAID());
    for (var receiverName: receivers) {
      ackMsg.addReceiver(new AID(receiverName, AID.ISLOCALNAME));
    }
    ((AbstractDedaleAgent)myAgent).sendMessage(ackMsg);
  }
  
  private void processAgentTermination() {
    var receivedProtocol = "TERMINATION-INFORM";
    var sentProtocol = "TERMINATION-INFORM-ACK";
    
    var msgTemplate = MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
        MessageTemplate.MatchProtocol(receivedProtocol));
    ACLMessage msg = null;
    
    var senders = new ArrayList<String>();
    while ((msg = myAgent.receive(msgTemplate)) != null) {
      var sender = msg.getSender().getLocalName();
      agentKnowledge.setAgentTermination(sender);
      senders.add(sender);
    }
    
    if (!senders.isEmpty()) {
      sendAck(senders, sentProtocol);
    }
  }
}
