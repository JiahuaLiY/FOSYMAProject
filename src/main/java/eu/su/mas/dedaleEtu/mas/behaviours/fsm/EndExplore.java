package eu.su.mas.dedaleEtu.mas.behaviours.fsm;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;

public class EndExplore extends OneShotBehaviour {

  private static final long serialVersionUID = 6749834804789478471L;
  
  public EndExplore(AbstractDedaleAgent agent) {
    super(agent);
  }
  @Override
  public void action() {
    System.out.println(myAgent.getLocalName() + " finished");
  }
}
