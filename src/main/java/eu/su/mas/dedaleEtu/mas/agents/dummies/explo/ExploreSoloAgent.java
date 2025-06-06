package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import java.util.ArrayList;
import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploSoloBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.Behaviour;

/**
 * <pre>
 * ExploreSolo agent. 
 * It explore the map using a DFS algorithm.
 * It stops when all nodes have been visited.
 *  </pre>
 *  
 * @author hc
 *
 */

public class ExploreSoloAgent extends AbstractDedaleAgent {

	private static final long serialVersionUID = -6431752665590433727L;
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
		

		List<Behaviour> lb=new ArrayList<Behaviour>();
		
		/************************************************
		 * 
		 * ADD the initial behaviours of the Agent here
		 * 
		 ************************************************/
		
		lb.add(new ExploSoloBehaviour(this,this.myMap));
		
		
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
