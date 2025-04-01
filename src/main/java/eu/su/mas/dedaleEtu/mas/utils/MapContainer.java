package eu.su.mas.dedaleEtu.mas.utils;

import java.io.Serializable;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

public class MapContainer implements Serializable {
  private static final long serialVersionUID = 7570552087036405768L;
  private MapRepresentation map;
  
  public MapContainer() {
    this.map = null;
  }
  
  public MapRepresentation map() {
    if (map == null) {
      this.map = new MapRepresentation();
    }
    return map;
  }
}
