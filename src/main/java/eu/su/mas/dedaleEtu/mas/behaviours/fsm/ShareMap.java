package eu.su.mas.dedaleEtu.mas.behaviours.fsm;

import java.io.IOException;
import java.util.stream.Collectors;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class ShareMap extends OneShotBehaviour {

  private static final long serialVersionUID = -5643714182000770788L;
  private final MapContainer mapContainer;
  
  public ShareMap(AbstractDedaleAgent agent, MapContainer mapContainer) {
    super(agent);
    this.mapContainer = mapContainer;
  }
  
  @Override
  public void action() {
    var obsevations = ((AbstractDedaleAgent)myAgent).observe();
    var agentNames = obsevations.stream()
        .flatMap(couple -> couple.getRight().stream())
        .filter(couple -> couple.getLeft().equals(Observation.AGENTNAME))
        .map(Couple::getRight)
        .collect(Collectors.toList());
    
    var map = mapContainer.map();
    
    var msg = new ACLMessage(ACLMessage.INFORM);
    msg.setProtocol("SHARE-TOPO");
    msg.setSender(this.myAgent.getAID());

    for (var agentName : agentNames) {
      msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
      var partialGraph = map.getSerializableGraph(agentName);
      
      try {
        msg.setContentObject(partialGraph);
      } catch (IOException e) {
          e.printStackTrace();
          continue;
      }
      ((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
      map.cleanSet(agentName);
      System.out.println(partialGraph);
      System.out.println(myAgent.getLocalName() + " -- send --> " + agentName);
    }
  }
}
