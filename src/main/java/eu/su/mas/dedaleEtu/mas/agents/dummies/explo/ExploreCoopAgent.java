package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;

import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.BroadcastBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.EndBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.ExploreBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.RandomTerminationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.TerminationInformBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.CollectBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.EmptyPackBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.ShareReceiveBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours.SolveDeadlockBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.utils.WaitingProtocolsList;
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
		
		List<String> listAgentNames=new ArrayList<String>();
		
		if(args.length==0){
			System.err.println("Error while creating the agent, names of agent to contact expected");
			System.exit(-1);
		}else{
			int i=2;// WARNING YOU SHOULD ALWAYS START AT 2. This will be corrected in the next release.
			while (i<args.length) {
				listAgentNames.add((String)args[i]);
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
    
    var agentKnowledge = new AgentKnowledge(this, listAgentNames, false);
    
    fsm.registerFirstState(new ExploreBehaviour(this, agentKnowledge), "EXPLORE");
    fsm.registerState(new BroadcastBehaviour(this, agentKnowledge), "BROADCAST");
    fsm.registerState(new ShareReceiveBehaviour(this, agentKnowledge), "SHARE-RECEIVE-MANAGEMENT");
    //fsm.registerState(new ShareReceiveBehaviour(this, waitingList, receivedTopos, shareWith, mapContainer), "SHARE-RECEIVE-MANAGEMENT");
    //fsm.registerState(new ShareMap(this, mapContainer, shareWith), "SHARE");
    fsm.registerState(new SolveDeadlockBehaviour(this, agentKnowledge), "SOLVE-DEADLOCK");
    fsm.registerLastState(new EndBehaviour(this, agentKnowledge), "END-EXPLORE");
    
    fsm.registerState(new CollectBehaviour(this, agentKnowledge), "RT");
    fsm.registerState(new BroadcastBehaviour(this, agentKnowledge), "BROADCAST2");
    fsm.registerState(new ShareReceiveBehaviour(this, agentKnowledge), "SHARE-RECEIVE-MANAGEMENT2");
    fsm.registerState(new SolveDeadlockBehaviour(this, agentKnowledge), "SOLVE-DEADLOCK2");
    
    fsm.registerState(new EmptyPackBehaviour(this, agentKnowledge), "T");
    fsm.registerState(new BroadcastBehaviour(this, agentKnowledge), "BROADCAST3");
    fsm.registerState(new ShareReceiveBehaviour(this, agentKnowledge), "SHARE-RECEIVE-MANAGEMENT3");
    fsm.registerState(new SolveDeadlockBehaviour(this, agentKnowledge), "SOLVE-DEADLOCK3");
    
    
    fsm.registerState(new TerminationInformBehaviour(this, agentKnowledge), "A");
    
    fsm.registerState(new SolveDeadlockBehaviour(this, agentKnowledge), "SOLVE-DEADLOCK5");
    fsm.registerState(new RandomTerminationBehaviour(this, agentKnowledge), "RTB");
    fsm.registerState(new SolveDeadlockBehaviour(this, agentKnowledge), "SOLVE-DEADLOCK4");
    
    
    fsm.registerTransition("EXPLORE", "BROADCAST", 1);
    fsm.registerTransition("EXPLORE", "RT", 0);
    fsm.registerTransition("EXPLORE", "SOLVE-DEADLOCK", 2);
    fsm.registerDefaultTransition("BROADCAST", "SHARE-RECEIVE-MANAGEMENT");
    
    fsm.registerTransition("RT", "BROADCAST2", 1);
    fsm.registerTransition("RT", "T", 0);
    fsm.registerTransition("RT", "SOLVE-DEADLOCK2", 2);
    fsm.registerTransition("RT", "A", 3);
    fsm.registerDefaultTransition("BROADCAST2", "SHARE-RECEIVE-MANAGEMENT2");
    fsm.registerDefaultTransition("SHARE-RECEIVE-MANAGEMENT2", "RT");
    fsm.registerDefaultTransition("SOLVE-DEADLOCK2", "BROADCAST2");
    //fsm.registerTransition("SHARE-RECEIVE-MANAGEMENT", "EXPLORE", 0);
    //fsm.registerTransition("SHARE-RECEIVE-MANAGEMENT", "SHARE", 1);
    //fsm.registerTransition("SHARE-RECEIVE-MANAGEMENT", "MERGE", 2);
    fsm.registerDefaultTransition("SHARE-RECEIVE-MANAGEMENT", "EXPLORE");
    fsm.registerDefaultTransition("SOLVE-DEADLOCK", "BROADCAST");
    //fsm.registerDefaultTransition("SHARE", "SHARE-RECEIVE-MANAGEMENT");
    //fsm.registerDefaultTransition("MERGE", "SHARE-RECEIVE-MANAGEMENT");
    
    fsm.registerTransition("T", "BROADCAST3", 1);
    fsm.registerTransition("T", "BROADCAST2", 0);
    fsm.registerTransition("T", "SOLVE-DEADLOCK3", 2);
    fsm.registerDefaultTransition("BROADCAST3", "SHARE-RECEIVE-MANAGEMENT3");
    fsm.registerDefaultTransition("SHARE-RECEIVE-MANAGEMENT3", "T");
    fsm.registerDefaultTransition("SOLVE-DEADLOCK3", "BROADCAST3");
    
    

    fsm.registerTransition("A", "A", 1);
    fsm.registerTransition("A", "RTB", 0);
    fsm.registerTransition("A", "SOLVE-DEADLOCK5", 2);
    fsm.registerDefaultTransition("SOLVE-DEADLOCK5", "A");
    
    fsm.registerTransition("RTB", "END-EXPLORE", 0);
    fsm.registerTransition("RTB", "RTB", 1);
    fsm.registerTransition("RTB", "SOLVE-DEADLOCK4", 2);
    fsm.registerDefaultTransition("SOLVE-DEADLOCK4", "RTB");
    
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
