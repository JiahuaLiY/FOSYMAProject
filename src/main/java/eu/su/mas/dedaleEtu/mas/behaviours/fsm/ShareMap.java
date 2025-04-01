package eu.su.mas.dedaleEtu.mas.behaviours.fsm;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.utils.MapContainer;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class ShareMap extends OneShotBehaviour {

  private static final long serialVersionUID = 2159021860296918621L;

  @Override
  public void action() {
    var map = mapContainer.map();
    
    for (var agentName : shareWith) {
      System.out.println(myAgent.getLocalName() + " sends map to " + agentName);
      topoShare(map, agentName);
    }
    shareWith.clear();
  }

  private final MapContainer mapContainer;
  private final List<String> shareWith;
  
  public ShareMap(
      AbstractDedaleAgent agent,
      MapContainer mapContainer,
      List<String> shareWith) {
    super(agent);
    
    Objects.requireNonNull(mapContainer);
    Objects.requireNonNull(shareWith);
    
    this.mapContainer = mapContainer;
    this.shareWith = shareWith;
  }
  
  private void topoShare(MapRepresentation map, String agentName) {
    var msg = new ACLMessage(ACLMessage.INFORM);
    msg.setProtocol("TOPO-SHARE");
    msg.setSender(myAgent.getAID());
    msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
    
    var graph = map.getSerializableGraph(agentName);
    
    try {
      msg.setContentObject(graph);
    } catch (IOException e) {
        e.printStackTrace();
    }
    ((AbstractDedaleAgent)myAgent).sendMessage(msg);
    //map.cleanSet(agentName); // to do.
    System.out.println("ok for " + agentName);
  }
}
