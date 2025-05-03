package eu.su.mas.dedaleEtu.mas.behaviours.fsmBehaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.AgentKnowledge;
import jade.core.behaviours.OneShotBehaviour;

public class EndBehaviour extends OneShotBehaviour {

  private static final long serialVersionUID = 6749834804789478471L;
  
  private final AgentKnowledge agentKnowledge;
  
  public EndBehaviour(AbstractDedaleAgent agent, AgentKnowledge agentKnowledge) {
    super(agent);
    this.agentKnowledge = agentKnowledge;
  }
  
  @Override
  public void action() {
    var agent = (AbstractDedaleAgent)myAgent;
    System.out.println(myAgent.getLocalName() + " finished");
    System.out.println(agent.getBackPackFreeSpace());
  }
}
