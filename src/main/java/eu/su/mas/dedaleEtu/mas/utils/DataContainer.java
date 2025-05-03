package eu.su.mas.dedaleEtu.mas.utils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.Treasure;

public final class DataContainer implements Serializable {

  private static final long serialVersionUID = 1756609969617841325L;
  
  private final SerializableSimpleGraph<String, MapAttribute> graph;
  
  private final Map<String, Treasure> treasures;
  
  private final Couple<String, Couple<String, List<String>>> tankerAgentKnowledge;
  
  private final Map<String, Map<Observation, Integer>> treasureCollectionKnowledge;
  
  private final Map<String, Observation> agentPreferences;
  
  public DataContainer(SerializableSimpleGraph<String, MapAttribute> graph,
                       Map<String, Treasure> treasures,
                       Couple<String, Couple<String, List<String>>> tankerAgentKnowledge,
                       Map<String, Map<Observation, Integer>> treasureCollectionKnowledge,
                       Map<String, Observation> agentPreferences) {
    this.graph = graph;
    this.treasures = treasures;
    this.tankerAgentKnowledge = tankerAgentKnowledge;
    this.treasureCollectionKnowledge = treasureCollectionKnowledge;
    this.agentPreferences = agentPreferences;
  }
  
  public Optional<SerializableSimpleGraph<String, MapAttribute>> graph() {
    return graph != null ? Optional.of(graph) : Optional.empty();
  }
  
  public Optional<Map<String, Treasure>> treasures() {
    return treasures != null ? Optional.of(treasures) : Optional.empty();
  }
  
  public Optional<Couple<String, Couple<String, List<String>>>> tankerAgentKnowledge() {
    return tankerAgentKnowledge != null ? Optional.of(tankerAgentKnowledge) : Optional.empty();
  }
  
  public Optional<Map<String, Map<Observation, Integer>>> treasureCollectionKnowledge() {
    return treasureCollectionKnowledge != null ? Optional.of(treasureCollectionKnowledge) : Optional.empty();
  }
  
  public Optional<Map<String, Observation>> agentPreferences() {
    return agentPreferences != null ? Optional.of(agentPreferences) : Optional.empty();
  }
}
