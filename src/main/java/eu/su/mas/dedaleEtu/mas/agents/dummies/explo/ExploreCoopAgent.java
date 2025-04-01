package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;

import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsm.Broadcast;
import eu.su.mas.dedaleEtu.mas.behaviours.fsm.EndExplore;
import eu.su.mas.dedaleEtu.mas.behaviours.fsm.Explore;
import eu.su.mas.dedaleEtu.mas.behaviours.fsm.MergeMap;
import eu.su.mas.dedaleEtu.mas.behaviours.fsm.ShareMap;
import eu.su.mas.dedaleEtu.mas.behaviours.fsm.ShareReceiveManagement;
import eu.su.mas.dedaleEtu.mas.knowledge.DataShare;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.utils.MapContainer;
import eu.su.mas.dedaleEtu.mas.utils.WaitingList;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;

/**
 * <pre>
 * ExploreCoop agent. 
 * Basic example of how to "collaboratively" explore the map
 *  - It explore the map using a DFS algorithm and blindly tries to share the topology with the agents within reach.
 *  - The shortestPath computation is not optimized
 *  - Agents do not coordinate themselves on the node(s) to visit, thus progressively creating a single file. It's bad.
 *  - The agent sends all its map, periodically, forever. Its bad x3.
 *  - You should give him the list of agents'name to send its map to in parameter when creating the agent.
 *   Object [] entityParameters={"Name1","Name2};
 *   ag=createNewDedaleAgent(c, agentName, ExploreCoopAgent.class.getName(), entityParameters);
 *  
 * It stops when all nodes have been visited.
 * 
 * 
 *  </pre>
 *  
 * @author hc
 *
 */


public class ExploreCoopAgent extends AbstractDedaleAgent {

	private static final long serialVersionUID = -7969469610241668140L;
	private MapRepresentation myMap;
	

	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){

		super.setup();
		
		//get the parameters added to the agent at creation (if any)
		final Object[] args = getArguments();
		
		List<String> list_agentNames=new ArrayList<String>();
		
		if(args.length==0){
			System.err.println("Error while creating the agent, names of agent to contact expected");
			System.exit(-1);
		}else{
			int i=2;// WARNING YOU SHOULD ALWAYS START AT 2. This will be corrected in the next release.
			while (i<args.length) {
				list_agentNames.add((String)args[i]);
				i++;
			}
		}

		List<Behaviour> lb=new ArrayList<Behaviour>();
		
		/************************************************
		 * 
		 * ADD the behaviours of the Dummy Moving Agent
		 * 
		 ************************************************/
		
		// lb.add(new ExploCoopBehaviour(this,this.myMap,list_agentNames));
    var fsm = new FSMBehaviour();
    var mapContainer = new MapContainer();
    var waitingList = new WaitingList();
    var receivedTopos = new HashMap<String, DataShare>();
    var shareWith = new ArrayList<String>();
    
    fsm.registerFirstState(new Explore(this, mapContainer), "EXPLORE");
    fsm.registerState(new Broadcast(this, list_agentNames, waitingList), "BROADCAST");
    fsm.registerState(new ShareReceiveManagement(this, waitingList, receivedTopos, shareWith, mapContainer), "SHARE-RECEIVE-MANAGEMENT");
    fsm.registerState(new ShareMap(this, mapContainer, shareWith), "SHARE");
    fsm.registerState(new MergeMap(this, mapContainer, receivedTopos), "MERGE");
    fsm.registerLastState(new EndExplore(this), "END-EXPLORE");
    
    fsm.registerTransition("EXPLORE", "BROADCAST", 1);
    fsm.registerTransition("EXPLORE", "END-EXPLORE", 0);
    fsm.registerDefaultTransition("BROADCAST", "SHARE-RECEIVE-MANAGEMENT");
    fsm.registerTransition("SHARE-RECEIVE-MANAGEMENT", "EXPLORE", 0);
    fsm.registerTransition("SHARE-RECEIVE-MANAGEMENT", "SHARE", 1);
    fsm.registerTransition("SHARE-RECEIVE-MANAGEMENT", "MERGE", 2);
    fsm.registerDefaultTransition("SHARE", "SHARE-RECEIVE-MANAGEMENT");
    fsm.registerDefaultTransition("MERGE", "SHARE-RECEIVE-MANAGEMENT");
    
    lb.add(fsm);

		
		
		/***
		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 */
		
		
		addBehaviour(new StartMyBehaviours(this,lb));
		
		System.out.println("the  agent "+this.getLocalName()+ " is started");

	}
	
	
	/**
	 * This method is automatically called after doDelete()
	 */
	protected void takeDown(){
		super.takeDown();
	}

	protected void beforeMove(){
		super.beforeMove();
		//System.out.println("I migrate");
	}

	protected void afterMove(){
		super.afterMove();
		//System.out.println("I migrated");
	}

}
