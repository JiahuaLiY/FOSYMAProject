package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * The agent periodically share its map.
 * It blindly tries to send all its graph to its friend(s)  	
 * If it was written properly, this sharing action would NOT be in a ticker behaviour and only a subgraph would be shared.

 * @author hc
 *
 */
public class ShareMapBehaviour extends TickerBehaviour{
	
	private MapRepresentation myMap;
	private List<String> receivers;

	/**
	 * The agent periodically share its map.
	 * It blindly tries to send all its graph to its friend(s)  	
	 * If it was written properly, this sharing action would NOT be in a ticker behaviour and only a subgraph would be shared.

	 * @param a the agent
	 * @param period the periodicity of the behaviour (in ms)
	 * @param mymap (the map to share)
	 * @param receivers the list of agents to send the map to
	 */
	public ShareMapBehaviour(Agent a, long period,MapRepresentation mymap, List<String> receivers) {
		super(a, period);
		this.myMap=mymap;
		this.receivers=receivers;	
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -568863390879327961L;

	@Override
	protected void onTick() {
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setProtocol("SHARE-TOPO");
		msg.setSender(this.myAgent.getAID());
		for (String agentName : receivers) {
	        msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
	        System.out.println("----------\n");
	        
	        
	        SerializableSimpleGraph<String, MapAttribute> partialGraph = this.myMap.getSerializableGraph(agentName);

	        System.out.println("----------\n");
	        try {
	            msg.setContentObject(partialGraph);
	        } catch (IOException e) {
	            e.printStackTrace();
	            continue;
	        }
	        
	        
	        
	        ((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
	        //this.myMap.cleanSet(agentName);
	    }
		
		System.out.println("end of send");

		
	}


}
