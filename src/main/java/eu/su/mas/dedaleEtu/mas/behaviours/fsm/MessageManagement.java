package eu.su.mas.dedaleEtu.mas.behaviours.fsm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class MessageManagement extends OneShotBehaviour {

  private static final long serialVersionUID = 1273842784020317797L;
  
  private final Map<String, SerializableSimpleGraph<String, MapAttribute>> bufferOfReceivedMaps;
  
  public MessageManagement(
      AbstractDedaleAgent agent,
      Map<String, SerializableSimpleGraph<String, MapAttribute>> bufferOfReceivedMaps) {
    super(agent);
    Objects.requireNonNull(bufferOfReceivedMaps);
    this.bufferOfReceivedMaps = bufferOfReceivedMaps;
  }
  
  private void sendAck(List<String> receivers) {
    var ack = new ACLMessage(ACLMessage.INFORM);
    ack.setProtocol("ACK");
    ack.setSender(myAgent.getAID());
    for (var receiver : receivers) {
      ack.addReceiver(new AID(receiver, AID.ISLOCALNAME));
    }
    ((AbstractDedaleAgent)myAgent).sendMessage(ack);
  }
  
  @SuppressWarnings("unchecked")
  private void handleShareMaps() {
    var msgTemplate = MessageTemplate.and(
          MessageTemplate.MatchProtocol("SHARE-TOPO"),
          MessageTemplate.MatchPerformative(ACLMessage.INFORM));
     ACLMessage msg = null;
     var receivers = new ArrayList<String>();
     
     while ((msg = myAgent.receive(msgTemplate)) != null) {
       SerializableSimpleGraph<String, MapAttribute> mapReceived = null;
       try {
         mapReceived = (SerializableSimpleGraph<String, MapAttribute>)msg.getContentObject();
       } catch (Exception e) {
         e.printStackTrace();
       }
       
       var name = msg.getSender().getLocalName();
       bufferOfReceivedMaps.put(name, mapReceived);
       receivers.add(name);
     }
     
     if (!receivers.isEmpty()) {
       // sendAck(receivers); 
     }
  }
  
  @Override
  public void action() {
    handleShareMaps();
  }
  
  @Override
  public int onEnd() {
    return bufferOfReceivedMaps.isEmpty() ? 1 : 0;
  }
}
