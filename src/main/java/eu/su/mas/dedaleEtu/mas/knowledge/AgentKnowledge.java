package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.utils.DataContainer;
import eu.su.mas.dedaleEtu.mas.utils.DataShareTracker;
import eu.su.mas.dedaleEtu.mas.utils.UnorderedCouple;
import eu.su.mas.dedaleEtu.mas.utils.WaitingProtocolsList;

public final class AgentKnowledge implements Serializable {

  public enum AgentMode {
    IMMOBILE,
    MAP_EXPLORE,
    TREASURE_COLLECT,
    EMPTY_PACKAGE,
    RANDOM_SEARCH,
    RANDOM_TERMINATION,
    TERMINATION_INFORM
  }
  
  private static final long serialVersionUID = -5688359185607177514L;

  public static final long MOVEMENT_WAITING_DURATION = 1000;
  
  public static final long BROADCAST_WAITING_DURATION = 250;
  
  public static final long DATA_SHARE_WAITING_DURATION = 450;
  
  public static final long DATA_SHARE_ACK_WAITING_DURATION = 450;
  
  public static final long MAX_DEADLOCK_WAITING_DURATION = 1000;
  
  public static final List<Observation> TREASURE_TYPES = List.of(Observation.GOLD, Observation.DIAMOND);
  
  private final AbstractDedaleAgent agent;
  
  private final Set<String> agentIdentifiers;
  
  private final boolean isTankerAgent;
  
  private MapRepresentation map;
  
  private final Map<String, Treasure> treasures;
  
  private final DataShareTracker dataShareTracker;
  
  private final WaitingProtocolsList waitingProtocolsList;
  
  private Couple<String, Couple<String, List<String>>> tankerAgentKnowledge;
  
  private Map<String, Map<Observation, Integer>> treasureCollectionKnowledge;
  
  private Map<Observation, Integer> treasureNumberRegister;
  
  private Map<String, Observation> agentPreferences;
  
  private AgentMode mode;
  
  private String currentPositionID;
  
  private String attemptPositionID;
  
  private String destinationPositionID;
  
  private Iterator<String> shortestPathIter;
  
  private final Map<String, Boolean> agentTerminationsRegister;
  
  private Set<String> randomSelectedPositions;
  
  private final int initialPackCapacity;
  
  public AgentKnowledge(AbstractDedaleAgent agent,
                        Collection<String> agentIdentifiers,
                        boolean isTankerAgent) {
    Objects.requireNonNull(agent);
    Objects.requireNonNull(agentIdentifiers);
    
    this.agent = agent;
    this.agentIdentifiers = Set.copyOf(agentIdentifiers);
    this.isTankerAgent = isTankerAgent;
    
    this.treasures = new HashMap<>();
    this.dataShareTracker = new DataShareTracker(agentIdentifiers);
    this.waitingProtocolsList = new WaitingProtocolsList();
    
    this.treasureCollectionKnowledge = Stream.concat(agentIdentifiers.stream(), Stream.of(agent.getLocalName()))
        .collect(Collectors.toMap(Function.identity(), agentID -> TREASURE_TYPES.stream()
            .collect(Collectors.toMap(Function.identity(), type -> 0))));
    this.treasureNumberRegister = TREASURE_TYPES.stream()
        .collect(Collectors.toMap(Function.identity(), type -> -1));
    
    this.agentPreferences = new HashMap<>();
    this.agentPreferences.put(agent.getLocalName(), agent.getMyTreasureType());
    dataShareTracker.setAgentPreferencesSendingStatus(false);
    
    this.mode = isTankerAgent ? AgentMode.IMMOBILE : AgentMode.MAP_EXPLORE;
    
    if (isTankerAgent) {
      this.agentTerminationsRegister = agentIdentifiers.stream()
          .collect(Collectors.toMap(Function.identity(), agentID -> false));
    }
    else {
      this.agentTerminationsRegister = null;
    }
    
    if (!isTankerAgent) {
      this.initialPackCapacity = currentPackCapacity();
    }
    else {
      this.initialPackCapacity = 0;
    }
  }
  
  private void validateAgentIdentifier(String agentID) {
    Objects.requireNonNull(agentID);
    if (!agentIdentifiers.contains(agentID)) {
      throw new IllegalArgumentException("Unknow agent identifier: " + agentID);
    }
  }
  
  private boolean updateTreasureFromObservation(Treasure treasure,
                                                String treasureNodeID,
                                                List<Couple<Observation, String>> observation) {
    Objects.requireNonNull(treasure);
    Objects.requireNonNull(treasureNodeID);
    Objects.requireNonNull(observation);
    
    var isUpdated = false;
    for (var couple: observation) {
      switch (couple.getLeft()) {
        case GOLD:
          isUpdated = true;
          treasure.setTreasureType(Observation.GOLD);
          treasure.setTreasureAmount(Integer.valueOf(couple.getRight()));
          break;
          
        case DIAMOND:
          isUpdated = true;
          treasure.setTreasureType(Observation.DIAMOND);
          treasure.setTreasureAmount(Integer.valueOf(couple.getRight()));
          break;
          
        case LOCKPICKING:
          isUpdated = true;
          treasure.setTreasureLockPicking(Integer.valueOf(couple.getRight()));
          break;
          
        case STRENGH:
          isUpdated = true;
          treasure.setTreasureStrength(Integer.valueOf(couple.getRight()));
          break;
          
        case LOCKSTATUS:
          isUpdated = true;
          treasure.setTreasureLocked(!Boolean.valueOf(couple.getRight()));
          break;
          
        default:
          throw new IllegalArgumentException("Unsupported kind: " + couple.getLeft());
      }
    }
    return isUpdated;
  }
  
  public void updateAgentKnowledge(Location currentPosition,
                                   List<Couple<Location, List<Couple<Observation, String>>>> observations) {
    Objects.requireNonNull(currentPosition);
    Objects.requireNonNull(observations);
    
    var map = map();
    this.currentPositionID = currentPosition.getLocationId();
    if (map.containsNode(currentPositionID) && map.getNodeAttribute(currentPositionID).equals(MapAttribute.open)) {
      dataShareTracker.addGraphNode(currentPositionID);
    }
    map.addNode(currentPositionID, MapAttribute.closed);
    
    for (var couple: observations) {
      var adjacentNodeID = couple.getLeft().getLocationId();
      if (map.addNewNode(adjacentNodeID)) {
        dataShareTracker.addGraphNode(adjacentNodeID);
      }
      
      if (adjacentNodeID.equals(currentPositionID)) {
        if (treasures.containsKey(adjacentNodeID)) {
          var currentTreasure = treasures.get(adjacentNodeID);
          currentTreasure.setTreasureAmount(0);
          updateTreasureFromObservation(currentTreasure, adjacentNodeID, couple.getRight());
          currentTreasure.updatTimeStamp();
        }
        else {
          var emptyTreasure = Treasure.createEmptyTreasure();
          if (updateTreasureFromObservation(emptyTreasure, adjacentNodeID, couple.getRight())) {
            emptyTreasure.updatTimeStamp();
            treasures.put(adjacentNodeID, emptyTreasure);
            dataShareTracker.addTreasureNode(adjacentNodeID);
          }
        }
      }
      else {
        map.addEdge(currentPositionID, adjacentNodeID);
      }
    }
  }
  
  public void updateTreasureAfterPicking(String treasureNodeID,
                                         List<Couple<Observation, String>> observation) {
    Objects.requireNonNull(treasureNodeID);
    Objects.requireNonNull(observation);
    
    var currentTreasure = treasures.get(treasureNodeID);
    currentTreasure.setTreasureAmount(0);
    updateTreasureFromObservation(currentTreasure, treasureNodeID, observation);
    currentTreasure.updatTimeStamp();
  }
  
  public Treasure getTreasure(String positionID) {
    Objects.requireNonNull(positionID);
    return treasures.get(positionID);
  }
  

  public Set<UnorderedCouple<String>> getForbiddenEdgesForShortestPath(List<Couple<Location, List<Couple<Observation, String>>>> observations) {
    var forbiddenEdges = new HashSet<UnorderedCouple<String>>();
    if (knowsTankerAgent()) {
      var tankerAgentPositionID = tankerAgentKnowledge.getRight().getLeft();
      forbiddenEdges.addAll(tankerAgentKnowledge.getRight().getRight().stream()
          .map(e -> new UnorderedCouple<>(tankerAgentPositionID, e))
          .toList());
    }
    for (var couple: observations) {
      var nodeID = couple.getLeft().getLocationId();
      for (var observation: couple.getRight()) {
        if (observation.getLeft().equals(Observation.AGENTNAME)) {
          forbiddenEdges.add(new UnorderedCouple<>(currentPositionID, nodeID));
          break;
        }
      }
    }
    return forbiddenEdges;
  }
  
  public List<String> pickableTreasurePositions() {
    return treasures.entrySet().stream()
        .filter(e -> e.getValue().amount() > 0 && e.getValue().type().equals(agent.getMyTreasureType()))
        .map(Entry::getKey)
        .toList();
  }
  
  public Set<String> getKRandomElements(List<String> sources, int k) {
    Objects.requireNonNull(sources);
    if (k <= 0) {
      throw new IllegalArgumentException("The number of k cannot be negative or zero");
    }
    if (k > sources.size()) {
      throw new IllegalArgumentException("The number of k cannot be greater than the size of the sources");
    }
    if (k == sources.size()) {
      return new HashSet<>(sources);
    }
    var kRandomElements = new HashSet<String>();
    var rg = new Random();
    var sourcesSize = sources.size();
    while (kRandomElements.size() != k) {
      var idx = rg.nextInt(sourcesSize);
      kRandomElements.add(sources.get(idx));
    }
    return kRandomElements;
  }

  public boolean computeShortestPathToDestination(List<Couple<Location, List<Couple<Observation, String>>>> observations) {
    Objects.requireNonNull(observations);
    
    setAttemptPositionID(null);
    
    Collection<String> candidates = null;
    switch (mode) {
      case MAP_EXPLORE:
        candidates = map().getOpenNodeIdentifiers();
        break;
        
      case TREASURE_COLLECT:
        candidates = pickableTreasurePositions();
        break;
        
      case EMPTY_PACKAGE:
      case TERMINATION_INFORM:
        if (!knowsTankerAgent()) {
          throw new IllegalStateException("Agent " + agent.getLocalName() + " is not aware of the existence of the tanker agent");
        }
        candidates = tankerAgentKnowledge.getRight().getRight();
        break;
        
      case RANDOM_SEARCH:
        if (randomSelectedPositions == null || randomSelectedPositions.isEmpty()) {
          var sources = map().getAllNodes();
          this.randomSelectedPositions = getKRandomElements(map().getAllNodes(),
                                                            Math.min(sources.size() - 2, 5));
         randomSelectedPositions.remove(tankerAgentKnowledge.getRight().getLeft());
         var rg = new Random();
         var idx = rg.nextInt(tankerAgentKnowledge.getRight().getRight().size());
         randomSelectedPositions.add(tankerAgentKnowledge.getRight().getRight().get(idx));
        }
        candidates = randomSelectedPositions;
        System.out.println(agent.getLocalName() + " " + candidates + " " + mode);
        break;
        
      case RANDOM_TERMINATION:
        if (randomSelectedPositions == null || randomSelectedPositions.isEmpty()) {
          var sources = map().getAllNodes();
          this.randomSelectedPositions = getKRandomElements(map().getAllNodes(),
                                                            Math.min(sources.size(), agentIdentifiers.size() + 5));
        }
        candidates = randomSelectedPositions;
        System.out.println(agent.getLocalName() + " " + candidates + " " + mode);
        break;
        
      case IMMOBILE:
        throw new IllegalStateException("The tanker agent cannot be moved");
    }
    var shortestPath = map().getShortestPathFromCondidates(currentPositionID,
                                                           getForbiddenEdgesForShortestPath(observations),
                                                           candidates);
    if (shortestPath.isEmpty()) {
        return false;
    }
    this.destinationPositionID = shortestPath.get().isEmpty() ? currentPositionID : shortestPath.get().getLast();
    this.shortestPathIter = shortestPath.get().iterator();
    return true;
  }
  
  public Optional<String> getNextPositionID(List<Couple<Location, List<Couple<Observation, String>>>> observations) {
    if (shortestPathIter == null || !shortestPathIter.hasNext()) {
      if (!computeShortestPathToDestination(observations)) {
        return Optional.empty();
      }
    }
    
    if (!shortestPathIter.hasNext()) {
      return Optional.empty();
    }
    var nextPositionID = shortestPathIter.next();
    setAttemptPositionID(nextPositionID);
    return Optional.of(nextPositionID);
  }
  
  public void setAttemptPositionID(String positionID) {
    this.attemptPositionID = positionID;
  }
  
  public void resetShortestPath() {
    this.shortestPathIter = null;
    this.destinationPositionID = null;
  }
  
  public boolean reachDestination() {
    return destinationPositionID != null && currentPositionID.equals(destinationPositionID);
  }
  
  
  
  
  
  private SerializableSimpleGraph<String, MapAttribute> cloneGraph(String agentID) {
    var graphNodes = dataShareTracker.getGraphNodes(agentID);
    if (graphNodes.isEmpty()) {
      return null;
    }
    return map().getSerializedParticalGraphTopology(graphNodes);
  }
  
  
  private Map<String, Treasure> cloneTreasures(String agentID) {
    var treasureNodes = dataShareTracker.getTreasureNodes(agentID);
    var treasures = new HashMap<String, Treasure>();
    for (var nodeID: treasureNodes) {
      treasures.put(nodeID, this.treasures.get(nodeID).clone());
    }
    return Map.copyOf(treasures);
  }

  private Couple<String, Couple<String, List<String>>> cloneTankerAgentKnowledge(String agentID) {
    if (!knowsTankerAgent() 
        || dataShareTracker.getTankerAgentKnowledgeSendingStatus(agentID)) {
      return null;
    }
    return new Couple<>(tankerAgentKnowledge.getLeft(),
                        new Couple<>(tankerAgentKnowledge.getRight().getLeft(),
                                     List.copyOf(tankerAgentKnowledge.getRight().getRight())));
  }
  
  private Map<String, Map<Observation, Integer>> cloneTreasureCollectionKnowledge(String agentID) {
    if (dataShareTracker.getTreasureCollectionKnowledgeSendingStatus(agentID)) {
      return null;
    }
    
    var treasureCollectionKnowledge = new HashMap<String, Map<Observation, Integer>>();
    for (var agentIdentifier: this.treasureCollectionKnowledge.keySet()) {
      var knowledge = Map.copyOf(this.treasureCollectionKnowledge.get(agentIdentifier));
      treasureCollectionKnowledge.put(agentIdentifier, knowledge);
    }
    return Map.copyOf(treasureCollectionKnowledge);
  }
  
  private Map<String, Observation> cloneAgentPreferences(String agentID) {
    if (dataShareTracker.getAgentPreferencesSendingStatus(agentID)) {
      return null;
    }
    return Map.copyOf(this.agentPreferences);
  }
  
  public DataContainer getDataToBeSent(String agentID) {
    validateAgentIdentifier(agentID);
    /*
    if (!dataShareTracker.sendingRequired(agentID)) {
      return Optional.empty();
    }*/
    /*
    DataContainer dataContainer = null;
    switch (mode) {
      case IMMOBILE:
        dataContainer = new DataContainer(cloneGraph(agentID),
                                          cloneTreasures(agentID),
                                          cloneTankerAgentKnowledge(agentID),
                                          cloneTreasureCollectionKnowledge(agentID),
                                          cloneAgentPreferences(agentID));
        break;
      case MAP_EXPLORE:
        dataContainer = new DataContainer(cloneGraph(agentID),
                                          cloneTreasures(agentID),
                                          cloneTankerAgentKnowledge(agentID),
                                          null,
                                          cloneAgentPreferences(agentID));
        break;
      default:
        dataContainer = new DataContainer(cloneGraph(agentID),
                                          cloneTreasures(agentID),
                                          null,
                                          cloneTreasureCollectionKnowledge(agentID),
                                          cloneAgentPreferences(agentID));
        break;

    }*/
    //return Optional.of(dataContainer);
    return new DataContainer(cloneGraph(agentID),
                             cloneTreasures(agentID),
                             cloneTankerAgentKnowledge(agentID),
                             cloneTreasureCollectionKnowledge(agentID),
                             cloneAgentPreferences(agentID));
  }
  
  public void cleanDataTracker(String agentID) {
    Objects.requireNonNull(agentID);
    validateAgentIdentifier(agentID);
    
    dataShareTracker.cleanTracker(agentID);
  }
  
  private void updateGraph(String agentID, Optional<SerializableSimpleGraph<String, MapAttribute>> graphContainer) {
    if (graphContainer.isEmpty()) {
      return;
    }
    var graph = graphContainer.get();
    var map = map();
    for (var node: graph.getAllNodes()) {
      var nodeID = node.getNodeId();
      if (map.containsNode(nodeID)) {
        if (node.getNodeContent().toString().equals(MapAttribute.closed.toString())) {
          map.addNode(nodeID, MapAttribute.closed);
        }
      }
      else {
        map.addNode(nodeID, node.getNodeContent());
        dataShareTracker.addGraphNode(nodeID);
      }
      
      for (var neighbourNodeID: graph.getEdges(nodeID)) {
        map.addEdge(nodeID, neighbourNodeID);
      }
    }
    System.out.println(agent.getLocalName() + " update graph from " + agentID);
  }
  
  private void updateTreasures(String agentID, Optional<Map<String, Treasure>> treasuresContainer) {
    if (treasuresContainer.isEmpty()) {
      return;
    }
    var treasures = treasuresContainer.get();
    for (var entry: treasures.entrySet()) {
      var treasureNodeID = entry.getKey();
      if (!this.treasures.containsKey(treasureNodeID)) {
        this.treasures.put(treasureNodeID, entry.getValue().clone());
        dataShareTracker.addTreasureNode(agentID, treasureNodeID);
      }
      else {
        var currentTreasure = this.treasures.get(treasureNodeID);
        if (currentTreasure.timeStamp() < entry.getValue().timeStamp()) {
          this.treasures.put(treasureNodeID, entry.getValue().clone());
          dataShareTracker.addTreasureNode(agentID, treasureNodeID);
        }
      }
    }
    System.out.println(agent.getLocalName() + " update treasures from " + agentID);
  }
  
  private void updateTankerAgentKnowledge(String agentID, Optional<Couple<String, Couple<String, List<String>>>> tankerAgentKnowledgeContainer) {
    if (tankerAgentKnowledgeContainer.isEmpty() || isTankerAgent || knowsTankerAgent()) {
      return;
    }
    var tankerAgentKnowledge = tankerAgentKnowledgeContainer.get();
    this.tankerAgentKnowledge = new Couple<>(tankerAgentKnowledge.getLeft(),
                                             new Couple<>(tankerAgentKnowledge.getRight().getLeft(),
                                                          List.copyOf(tankerAgentKnowledge.getRight().getRight())));
    dataShareTracker.setTankerAgentKnowledgeSendingStatus(agentID, false);
    System.out.println(agent.getLocalName() + " knows tanker agent from " + agentID);
    //System.out.println(tankerAgentKnowledge);
  }
  
  private void updateTreasureCollectionKnowledge(String agentID, Optional<Map<String, Map<Observation, Integer>>> treasureCollectionKnowledgeContainer) {
    if (treasureCollectionKnowledgeContainer.isEmpty()) {
      return;
    }
    var treasureCollectionKnowledge = treasureCollectionKnowledgeContainer.get();
    if (this.treasureCollectionKnowledge.equals(treasureCollectionKnowledge)) {
      return;
    }
    for (var agentIdentifier: this.treasureCollectionKnowledge.keySet()) {
      var currentKnowledge = this.treasureCollectionKnowledge.get(agentIdentifier);
      var receivedKnowledge = treasureCollectionKnowledge.get(agentIdentifier);
      
      currentKnowledge.merge(Observation.GOLD, receivedKnowledge.get(Observation.GOLD), Math::max);
      currentKnowledge.merge(Observation.DIAMOND, receivedKnowledge.get(Observation.DIAMOND), Math::max);
    }
    dataShareTracker.setTreasureCollectionKnowledgeSendingStatus(agentID, false);
    System.out.println(agent.getLocalName() + " learns knowledge of treasure collection status from " + agentID);
  }
  
  private void updatAgentPreferences(String agentID, Optional<Map<String, Observation>> agentPreferencesContainer) {
    if (agentPreferencesContainer.isEmpty()) {
      return;
    }
    var agentPreferences = agentPreferencesContainer.get();
    if (this.agentPreferences.equals(agentPreferences)) {
      return;
    }
    this.agentPreferences.putAll(agentPreferences);
    dataShareTracker.setAgentPreferencesSendingStatus(agentID, false);
    System.out.println(agent.getLocalName() + " learns agent preferences from " + agentID);
  }
  
  public void updateReceivedData(String agentID, DataContainer dataContainer) {
    Objects.requireNonNull(agentID);
    Objects.requireNonNull(dataContainer);
    validateAgentIdentifier(agentID);
    
    updateGraph(agentID, dataContainer.graph());
    updateTreasures(agentID, dataContainer.treasures());
    updateTankerAgentKnowledge(agentID, dataContainer.tankerAgentKnowledge());
    updateTreasureCollectionKnowledge(agentID, dataContainer.treasureCollectionKnowledge());
    updatAgentPreferences(agentID, dataContainer.agentPreferences());
  }
  
  public boolean sendingRequired(String agentID) {
    Objects.requireNonNull(agentID);
    validateAgentIdentifier(agentID);
    
    return dataShareTracker.sendingRequired(agentID);
  }
  
  public boolean isDeadlock() {
    return this.attemptPositionID != null && !currentPositionID.equals(attemptPositionID);
  }
  
  public boolean knowsTankerAgent() {
    return tankerAgentKnowledge != null;
  }
  
  public Set<String> agentIdentifiers() {
    return this.agentIdentifiers;
  }
  
  public boolean isTankerAgent() {
    return this.isTankerAgent;
  }
  
  public WaitingProtocolsList waitingProtocolsList() {
    return this.waitingProtocolsList;
  }
  
  public MapRepresentation map() {
    if (this.map == null) {
      this.map = new MapRepresentation();
    }
    return this.map;
  }
  
  public String currentPositionID() {
    return this.currentPositionID;
  }
  
  public String attemptPositionID() {
    return this.attemptPositionID;
  }
  
  public String destinationPositionID() {
    return this.destinationPositionID;
  }
  
  
  public boolean allAgentsTerminate() {
    return agentTerminationsRegister.values().stream()
        .allMatch(terminate -> terminate);
  }
  
  
  public void setAgentTermination(String agentID) {
    Objects.requireNonNull(agentID);
    validateAgentIdentifier(agentID);
    if (!isTankerAgent) {
      throw new IllegalStateException("Only the tanker agent contains the knowledge of the agent terminations");
    }
    agentTerminationsRegister.put(agentID, true);
  }
  
  
  public void initializeTankerAgentKnowledge(String tankerAgentName,
                                             String tankerAgentPosition,
                                             List<String> edges) {
    Objects.requireNonNull(tankerAgentName);
    Objects.requireNonNull(tankerAgentPosition);
    Objects.requireNonNull(edges);
    if (!isTankerAgent) {
      throw new IllegalStateException("Only the tanker agent can intialize the tanker agent's knowledge");
    } 
    if (tankerAgentKnowledge != null) {
      throw new IllegalStateException("The knowledge of the tanker agent has already been initialized");
    }
    this.tankerAgentKnowledge = new Couple<>(tankerAgentName,
                                             new Couple<>(tankerAgentPosition, edges));
    dataShareTracker.setTankerAgentKnowledgeSendingStatus(false);
  }
  
  
  public String getTankerAgentName() {
    if (!knowsTankerAgent()) {
      throw new IllegalStateException("Agent " + agent.getLocalName() + " is not aware of the existence of the tanker agent");
    }
    return tankerAgentKnowledge.getLeft();
  }
  
  public AgentMode mode() {
    return this.mode;
  }
  
  public void setAgentMode(AgentMode mode) {
    if (isTankerAgent) {
      throw new IllegalStateException("The tanker agent operating mode cannot be changed");
    }
    this.mode = mode;
  }
  
  
  public void computeTreasureNumberRegister() {
    var numberOfGold = treasures.values().stream()
        .filter(v -> v.type().equals(Observation.GOLD))
        .count();
    treasureNumberRegister.put(Observation.GOLD, (int)numberOfGold);
    treasureNumberRegister.put(Observation.DIAMOND, (int)(treasures.size() - numberOfGold));
  }
  
  
  public void increaseMyTreasureCollectionCounter() {
    treasureCollectionKnowledge.get(agent.getLocalName()).merge(agent.getMyTreasureType(), 1, Integer::sum);
    dataShareTracker.setTreasureCollectionKnowledgeSendingStatus(false);
  }
  
  
  public int numberOfCompletedTask() {
    return treasureCollectionKnowledge.values().stream()
        .flatMap(map -> map.values().stream())
        .reduce(0, Integer::sum);
  }
  
  
  public int numberOfCompletedTaskOfMyType() {
    return treasureCollectionKnowledge.values().stream()
        .mapToInt(map -> map.get(agent.getMyTreasureType()))
        .reduce(0, Integer::sum);
  }
  
  
  public boolean isTreasureCollectionTaskCompleted() {
    var totalNumberOfTreasures = treasureNumberRegister.values().stream()
        .reduce(0, Integer::sum);
    if (totalNumberOfTreasures < 0) {
      return false;
    }
    if (numberOfCompletedTask() == totalNumberOfTreasures) {
      return true;
    }
    var isAnotherTypeOfAgent = agentPreferences.entrySet().stream()
        .filter(e -> !e.getKey().equals(tankerAgentKnowledge.getLeft())
            && !e.getValue().equals(agent.getMyTreasureType()))
        .findAny()
        .isPresent();
    return agentPreferences.size() == agentIdentifiers.size()
        && !isAnotherTypeOfAgent
        && treasureNumberRegister.get(agent.getMyTreasureType()) == numberOfCompletedTaskOfMyType();
  }
  
  public int initialPackCapacity() {
    return this.initialPackCapacity;
  }
  
  public int currentPackCapacity() {
    if (isTankerAgent) {
      throw new IllegalStateException("The tanker agent does not have packaging capacity");
    }
    return agent.getBackPackFreeSpace().stream()
        .filter(couple -> couple.getLeft().equals(agent.getMyTreasureType()))
        .mapToInt(Couple::getRight)
        .findFirst()
        .getAsInt();
  }
  
  public boolean isPackFree() {
    return currentPackCapacity() == initialPackCapacity();
  }
  
  public void removeRandomSelectedPosition(String positionID) {
    Objects.requireNonNull(positionID);
    if (!mode.equals(AgentMode.RANDOM_SEARCH)) {
      throw new IllegalStateException("Only RANDOM-SEARCH mode is authorized to remove element in random selected positions");
    }
    randomSelectedPositions.remove(positionID);
  }
  
  public void cleanRandomSelectedPositions() {
    this.randomSelectedPositions = null;
  }
  
  public void print() {
    System.err.println(treasures);
    System.err.println(treasureCollectionKnowledge);
    System.err.println(treasureNumberRegister);
    System.err.println(agentPreferences);
  }
}
