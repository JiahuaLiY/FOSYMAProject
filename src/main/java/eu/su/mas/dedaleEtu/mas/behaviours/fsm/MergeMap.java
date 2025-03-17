package eu.su.mas.dedaleEtu.mas.behaviours.fsm;

import java.util.Map;
import java.util.Objects;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.OneShotBehaviour;

public class MergeMap extends OneShotBehaviour {

  private static final long serialVersionUID = -5135676196541799987L;
  private final MapContainer mapContainer;
  private final Map<String, SerializableSimpleGraph<String, MapAttribute>> bufferOfReceivedMaps;
  
  public MergeMap(
      AbstractDedaleAgent agent,
      MapContainer mapContainer,
      Map<String, SerializableSimpleGraph<String, MapAttribute>> bufferOfReceivedMaps) {
    super(agent);
    Objects.requireNonNull(mapContainer);
    Objects.requireNonNull(bufferOfReceivedMaps);
    
    this.mapContainer = mapContainer;
    this.bufferOfReceivedMaps = bufferOfReceivedMaps;
  }
  
  @Override
  public void action() {
    var map = mapContainer.map();
    for (var entry : bufferOfReceivedMaps.entrySet()) {
      System.out.println(myAgent.getLocalName() + " merge the map <" + entry.getValue() + "> from " + entry.getKey());
      map.mergeMap(entry.getValue());
    }
    
    bufferOfReceivedMaps.clear();
  }
}
