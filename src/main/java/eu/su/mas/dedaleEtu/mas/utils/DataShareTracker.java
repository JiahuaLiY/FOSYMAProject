package eu.su.mas.dedaleEtu.mas.utils;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DataShareTracker implements Serializable {

  private static final long serialVersionUID = 4635837824576813821L;
  
  private final Set<String> agentIdentifiers;
  
  private final Map<String, Set<String>> graphNodes;
  
  private final Map<String, Set<String>> treasureNodes;
  
  private final Map<String, Boolean> treasureCollectionKnowledgeSendingStatus;
  
  private final Map<String, Boolean> agentPreferencesSendingStatus;
  
  private final Map<String, Boolean> tankerAgentKnowledgeSendingStatus;
  
  public DataShareTracker(Collection<String> agentIdentifiers) {
    Objects.requireNonNull(agentIdentifiers);
    
    this.agentIdentifiers = Set.copyOf(agentIdentifiers);
    this.graphNodes = new HashMap<>();
    this.treasureNodes = new HashMap<>();
    this.treasureCollectionKnowledgeSendingStatus = new HashMap<>();
    this.agentPreferencesSendingStatus = new HashMap<>();
    this.tankerAgentKnowledgeSendingStatus = new HashMap<>();
    
    for (var agentID: agentIdentifiers) {
      graphNodes.put(agentID, new HashSet<>());
      treasureNodes.put(agentID, new HashSet<>());
      treasureCollectionKnowledgeSendingStatus.put(agentID, true);
      agentPreferencesSendingStatus.put(agentID, true);
      tankerAgentKnowledgeSendingStatus.put(agentID, true);
    }
  }
  
  private void validateAgentIdentifier(String agentID) {
    if (!agentIdentifiers.contains(agentID)) {
      throw new IllegalArgumentException("Unknow agent identifier: " + agentID);
    }
  }
  
  public boolean sendingRequired(String agentID) {
    validateAgentIdentifier(agentID);
    return !graphNodes.get(agentID).isEmpty()
        || !treasureNodes.get(agentID).isEmpty()
        || !treasureCollectionKnowledgeSendingStatus.get(agentID)
        || !agentPreferencesSendingStatus.get(agentID)
        || !tankerAgentKnowledgeSendingStatus.get(agentID);
  }
  
  public void setTreasureCollectionKnowledgeSendingStatus(String agentID, boolean sent) {
    Objects.requireNonNull(agentID);
    validateAgentIdentifier(agentID);
    
    for (var innerAgentID: treasureCollectionKnowledgeSendingStatus.keySet()) {
      if (innerAgentID.equals(agentID)) {
        continue;
      }
      treasureCollectionKnowledgeSendingStatus.put(innerAgentID, sent);
    }
  }
  
  public void setTreasureCollectionKnowledgeSendingStatus(boolean sent) {
    for (var agentID: treasureCollectionKnowledgeSendingStatus.keySet()) {
      treasureCollectionKnowledgeSendingStatus.put(agentID, sent);
    }
  }
  
  public boolean getTreasureCollectionKnowledgeSendingStatus(String agentID) {
    Objects.requireNonNull(agentID);
    validateAgentIdentifier(agentID);
    
    return treasureCollectionKnowledgeSendingStatus.get(agentID);
  }
  
  public void setAgentPreferencesSendingStatus(String agentID, boolean sent) {
    Objects.requireNonNull(agentID);
    validateAgentIdentifier(agentID);
    
    for (var innerAgentID: agentPreferencesSendingStatus.keySet()) {
      if (innerAgentID.equals(agentID)) {
        continue;
      }
      agentPreferencesSendingStatus.put(innerAgentID, sent);
    }
  }
  
  public void setAgentPreferencesSendingStatus(boolean sent) {
    for (var agentID: agentPreferencesSendingStatus.keySet()) {
      agentPreferencesSendingStatus.put(agentID, sent);
    }
  }
  
  public boolean getAgentPreferencesSendingStatus(String agentID) {
    Objects.requireNonNull(agentID);
    validateAgentIdentifier(agentID);
    
    return agentPreferencesSendingStatus.get(agentID);
  }
  
  public void setTankerAgentKnowledgeSendingStatus(String agentID, boolean sent) {
    Objects.requireNonNull(agentID);
    validateAgentIdentifier(agentID);
    
    for (var innerAgentID: tankerAgentKnowledgeSendingStatus.keySet()) {
      if (innerAgentID.equals(agentID)) {
        continue;
      }
      tankerAgentKnowledgeSendingStatus.put(innerAgentID, sent);
    }
  }
  
  public void setTankerAgentKnowledgeSendingStatus(boolean sent) {
    for (var agentID: tankerAgentKnowledgeSendingStatus.keySet()) {
      tankerAgentKnowledgeSendingStatus.put(agentID, sent);
    }
  }
  
  public boolean getTankerAgentKnowledgeSendingStatus(String agentID) {
    Objects.requireNonNull(agentID);
    validateAgentIdentifier(agentID);
    
    return tankerAgentKnowledgeSendingStatus.get(agentID);
  }
  
  public void addGraphNode(String agentID, String nodeID) {
    Objects.requireNonNull(agentID);
    Objects.requireNonNull(nodeID);
    validateAgentIdentifier(agentID);
    
    for (var entry: graphNodes.entrySet()) {
      if (entry.getKey().equals(agentID)) {
        continue;
      }
      entry.getValue().add(nodeID);
    }
  }
  
  public void addGraphNode(String nodeID) {
    Objects.requireNonNull(nodeID);
    
    for (var entry: graphNodes.entrySet()) {
      entry.getValue().add(nodeID);
    }
  }
  
  public Set<String> getGraphNodes(String agentID) {
    Objects.requireNonNull(agentID);
    validateAgentIdentifier(agentID);
    
    return Set.copyOf(graphNodes.get(agentID));
  }
  
  public void addTreasureNode(String agentID, String nodeID) {
    Objects.requireNonNull(agentID);
    Objects.requireNonNull(nodeID);
    validateAgentIdentifier(agentID); 
    
    for (var entry: treasureNodes.entrySet()) {
      if (entry.getKey().equals(agentID)) {
        continue;
      }
      entry.getValue().add(nodeID);
    }
  }
  
  public void addTreasureNode(String nodeID) {
    Objects.requireNonNull(nodeID);
    
    for (var entry: treasureNodes.entrySet()) {
      entry.getValue().add(nodeID);
    }
  }
  
  public Set<String> getTreasureNodes(String agentID) {
    Objects.requireNonNull(agentID);
    validateAgentIdentifier(agentID);
    
    return Set.copyOf(treasureNodes.get(agentID));
  }
  
  public void cleanTracker(String agentID) {
    Objects.requireNonNull(agentID);
    validateAgentIdentifier(agentID);
    
    graphNodes.get(agentID).clear();
    treasureNodes.get(agentID).clear();
    treasureCollectionKnowledgeSendingStatus.put(agentID, true);
    agentPreferencesSendingStatus.put(agentID, true);
    tankerAgentKnowledgeSendingStatus.put(agentID, true);
  }
}
