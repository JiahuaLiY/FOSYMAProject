package eu.su.mas.dedaleEtu.mas.behaviours.fsm;

import java.util.Objects;

import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.OneShotBehaviour;

public class Explore extends OneShotBehaviour {

  private static final long serialVersionUID = 7749048525683865672L;
  private final MapContainer mapContainer;
  
  public Explore(AbstractDedaleAgent agent, MapContainer mapContainer) {
    super(agent);
    Objects.requireNonNull(mapContainer);
    this.mapContainer = mapContainer;
  }
  
  @Override
  public void action() {
    var agent = (AbstractDedaleAgent)myAgent;
    var map = mapContainer.map();
    
    var position = agent.getCurrentPosition();
    if (position == null) {
      System.out.println("The agent " + myAgent.getLocalName() + " does not exist in the environment");
      return;
    }
    var observations = agent.observe();
    
    try {
      agent.doWait(100);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    map.addNode(position.getLocationId(), MapAttribute.closed);
    
    String nextNodeID = null;
    for (var obsevation : observations) {
      var accessibleNode = obsevation.getLeft();
      var isNewNode = map.addNewNode(accessibleNode.getLocationId());
      
      if (position.getLocationId() != accessibleNode.getLocationId()) {
        map.addEdge(position.getLocationId(), accessibleNode.getLocationId());
        if (nextNodeID == null && isNewNode) {
          nextNodeID = accessibleNode.getLocationId();
        }
      }
    }
    
    if (map.hasOpenNode()) {
      if (nextNodeID == null) {
        nextNodeID = map.getShortestPathToClosestOpenNode(position.getLocationId()).getFirst();
        
        if (!agent.moveTo(new GsLocation(nextNodeID))) {
          System.out.println(myAgent.getLocalName() + " interblocking !");
        }
      }
    }
  }
  
  @Override
  public int onEnd() {
    return mapContainer.map().hasOpenNode() ? 1 : 0;
  }
}
