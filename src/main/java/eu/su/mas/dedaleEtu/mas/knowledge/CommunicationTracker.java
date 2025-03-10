package eu.su.mas.dedaleEtu.mas.knowledge;

import java.util.*;


public class CommunicationTracker{
	private List<String> knownAgents;
	private Map<String, Set<String>> pendingNodesToSend;
	
	
	public CommunicationTracker(){
		this.knownAgents = new ArrayList<>();
		this.pendingNodesToSend = new HashMap<>();
	}
	
	public void registerAgent(String agentId) {
		if(!knownAgents.contains(agentId)) {
			knownAgents.add(agentId);
			pendingNodesToSend.put(agentId, new HashSet<>());
		}
	}
	
	public void addNewNodeToPending(String nodeId) {
		for(String agentId : pendingNodesToSend.keySet()) {
			pendingNodesToSend.get(agentId).add(nodeId);
		}
	}
	
	public Set<String> getUnSentNodes(String agentId){
		return new HashSet<>(pendingNodesToSend.getOrDefault(agentId, new HashSet<>()));
	}
	
	public void markNodesAsSent(String agentId) {
		pendingNodesToSend.put(agentId, new HashSet<>());
	}
	
	public boolean hasCommunicatedWith(String agentId) {
		return knownAgents.contains(agentId);
	}
}
