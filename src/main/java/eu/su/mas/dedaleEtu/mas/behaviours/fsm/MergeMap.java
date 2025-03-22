package eu.su.mas.dedaleEtu.mas.behaviours.fsm;

import java.util.Map;
import java.util.Objects;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.utils.MapContainer;
import jade.core.behaviours.OneShotBehaviour;

public class MergeMap extends OneShotBehaviour {

  private static final long serialVersionUID = -549026987950737349L;

  @Override
  public void action() {
    var map = mapContainer.map();
    for (var entry : receivedTopos.entrySet()) {
      System.out.println(myAgent.getLocalName() + " merge the map from " + entry.getKey());
      map.mergeMap(entry.getValue());
    }
    receivedTopos.clear();
  }

  private final MapContainer mapContainer;
  private final Map<String, SerializableSimpleGraph<String, MapAttribute>> receivedTopos;
  
  public MergeMap(
      AbstractDedaleAgent agent,
      MapContainer mapContainer,
      Map<String, SerializableSimpleGraph<String, MapAttribute>> receivedTopos) {
    super(agent);
    
    Objects.requireNonNull(mapContainer);
    Objects.requireNonNull(receivedTopos);
    
    this.mapContainer = mapContainer;
    this.receivedTopos = receivedTopos;
  }
}
