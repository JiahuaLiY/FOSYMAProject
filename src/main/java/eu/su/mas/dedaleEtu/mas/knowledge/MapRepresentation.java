package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.CloseFramePolicy;

import dataStructures.serializableGraph.*;
import dataStructures.tuple.Couple;
import javafx.application.Platform;

/**
 * This simple topology representation only deals with the graph, not its content.</br>
 * The knowledge representation is not well written (at all), it is just given as a minimal example.</br>
 * The viewer methods are not independent of the data structure, and the dijkstra is recomputed every-time.
 * 
 * @author hc
 */
public class MapRepresentation implements Serializable {

	/**
	 * A node is open, closed, or agent
	 * @author hc
	 *
	 */

	public enum MapAttribute {	
		agent,open,closed;

	}
	
	public class Tresor implements Serializable{
		private static final long serialVersionUID = -904881439693109677L;
		public enum Type{
			or, diamond, empty
		}
		private Type type;
		private boolean isLocked;
		private boolean isCollected;
		
		public Tresor(String tresorType) {
			if(tresorType == "or")
				this.type = Type.or;
			else
				this.type = Type.diamond;
			this.isLocked = true;
			this.isCollected = false;
		}
		
		public Tresor(String tresorType, boolean isLocked, boolean isCollected) {
			this(tresorType);
			this.isLocked = isLocked;
			this.isCollected = isCollected;
		}
		
		public void collectTresor() {
			this.isCollected = true;
		}
		
		public void unLockTresor() {
			this.isLocked = false;
		}
		
		
		public Tresor copyTresor(Tresor tresor) {
			return new Tresor(tresor.type.toString(), tresor.isLocked, tresor.isCollected);
		}
		
		
		
	}

	private static final long serialVersionUID = -1333959882640838272L;

	/*********************************
	 * Parameters for graph rendering
	 ********************************/

	private String defaultNodeStyle= "node {"+"fill-color: black;"+" size-mode:fit;text-alignment:under; text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";
	private String nodeStyle_open = "node.agent {"+"fill-color: forestgreen;"+"}";
	private String nodeStyle_agent = "node.open {"+"fill-color: blue;"+"}";
	private String nodeStyle=defaultNodeStyle+nodeStyle_agent+nodeStyle_open;
	

	private Graph g; //data structure non serializable
	private Viewer viewer; //ref to the display,  non serializable
	private Integer nbEdges;//used to generate the edges ids

	private SerializableSimpleGraph<String, MapAttribute> sg;//used as a temporary dataStructure during migration

	private CommunicationTracker communicationTracker;
	
	private HashMap<String, Tresor> tresor;
	private long timestamp;

	public MapRepresentation() {
		//System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.g= new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet",nodeStyle);

		Platform.runLater(() -> {
			openGui();
		});
		//this.viewer = this.g.display();

		this.nbEdges=0;
		
		this.communicationTracker = new CommunicationTracker();
		
		
		this.tresor = new HashMap<String, Tresor>();
		this.timestamp = System.currentTimeMillis();
	}

	
	
	/**
	 * Add or replace a node and its attribute 
	 * @param id unique identifier of the node
	 * @param mapAttribute attribute to process
	 */
	public synchronized void addNode(String id,MapAttribute mapAttribute){
		Node n;
		if (this.g.getNode(id)==null){
			n=this.g.addNode(id);
		}else{
			n=this.g.getNode(id);
		}
		n.clearAttributes();
		n.setAttribute("ui.class", mapAttribute.toString());
		n.setAttribute("ui.label",id);
		communicationTracker.addNewNodeToPending(id); // add for send the particle graph
	}

	/**
	 * Add a node to the graph. Do nothing if the node already exists.
	 * If new, it is labeled as open (non-visited)
	 * @param id id of the node
	 * @return true if added
	 */
	public synchronized boolean addNewNode(String id) {
		if (this.g.getNode(id)==null){
			addNode(id,MapAttribute.open);
			return true;
		}
		return false;
	}

	/**
	 * Add an undirect edge if not already existing.
	 * @param idNode1 unique identifier of node1
	 * @param idNode2 unique identifier of node2
	 */
	public synchronized void addEdge(String idNode1,String idNode2){
		this.nbEdges++;
		try {
			this.g.addEdge(this.nbEdges.toString(), idNode1, idNode2);
		}catch (IdAlreadyInUseException e1) {
			System.err.println("ID existing");
			System.exit(1);
		}catch (EdgeRejectedException e2) {
			this.nbEdges--;
		} catch(ElementNotFoundException e3){

		}
	}
	
	public void addTresor(String idNode, Tresor tresorType) {
		this.tresor.put(idNode, tresorType);
	}

	/**
	 * Compute the shortest Path from idFrom to IdTo. The computation is currently not very efficient
	 * 
	 * 
	 * @param idFrom id of the origin node
	 * @param idTo id of the destination node
	 * @return the list of nodes to follow, null if the targeted node is not currently reachable
	 */
	public synchronized List<String> getShortestPath(String idFrom,String idTo){
		List<String> shortestPath=new ArrayList<String>();

		Dijkstra dijkstra = new Dijkstra();//number of edge
		dijkstra.init(g);
		dijkstra.setSource(g.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		List<Node> path=dijkstra.getPath(g.getNode(idTo)).getNodePath(); //the shortest path from idFrom to idTo
		Iterator<Node> iter=path.iterator();
		while (iter.hasNext()){
			shortestPath.add(iter.next().getId());
		}
		dijkstra.clear();
		if (shortestPath.isEmpty()) {//The openNode is not currently reachable
			return null;
		}else {
			shortestPath.remove(0);//remove the current position
		}
		return shortestPath;
	}

	public List<String> getShortestPathToClosestOpenNode(String myPosition) {
		//1) Get all openNodes
		List<String> opennodes=getOpenNodes();

		//2) select the closest one
		List<Couple<String,Integer>> lc=
				opennodes.stream()
				.map(on -> (getShortestPath(myPosition,on)!=null)? new Couple<String, Integer>(on,getShortestPath(myPosition,on).size()): new Couple<String, Integer>(on,Integer.MAX_VALUE))//some nodes my be unreachable if the agents do not share at least one common node.
				.collect(Collectors.toList());

		Optional<Couple<String,Integer>> closest=lc.stream().min(Comparator.comparing(Couple::getRight));
		//3) Compute shorterPath

		return getShortestPath(myPosition,closest.get().getLeft());
	}



	public List<String> getOpenNodes(){
		return this.g.nodes()
				.filter(x ->x .getAttribute("ui.class")==MapAttribute.open.toString()) 
				.map(Node::getId)
				.collect(Collectors.toList());
	}


	/**
	 * Before the migration we kill all non serializable components and store their data in a serializable form
	 */
	public void prepareMigration(){
		serializeGraphTopology();

		closeGui();

		this.g=null;
	}

	/**
	 * Before sending the agent knowledge of the map it should be serialized.
	 */
	private synchronized void serializeGraphTopology() {
		this.sg= new SerializableSimpleGraph<String,MapAttribute>();
		Iterator<Node> iter=this.g.iterator();
		while(iter.hasNext()){
			Node n=iter.next();
			sg.addNode(n.getId(),MapAttribute.valueOf((String)n.getAttribute("ui.class")));
		}
		Iterator<Edge> iterE=this.g.edges().iterator();
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			sg.addEdge(e.getId(), sn.getId(), tn.getId());
		}	
		
		var a = g.nodes().map(Node::toString).collect(Collectors.joining("\n"));
		System.out.println(a);
	}
	
	
	//version modified for generating the partical graph
	private synchronized SerializableSimpleGraph<String,MapAttribute> serializeParticalGraphTopology(String agentId) {
		SerializableSimpleGraph<String,MapAttribute> spg= new SerializableSimpleGraph<String,MapAttribute>();
		
		Set<String> unsentNodes = this.communicationTracker.getUnSentNodes(agentId);
		
		System.out.println("HashCode : " + System.identityHashCode(this.g));
		if(unsentNodes.isEmpty())
			return spg;
		
		for(String nodeId : unsentNodes) {
			Node n = this.g.getNode(nodeId);
			spg.addNode(n.getId(),MapAttribute.valueOf((String)n.getAttribute("ui.class")));
			System.out.println(n.getId()+"node's class"+n.getAttribute("ui.class"));
		}
		System.out.println("to " + agentId + " : " + unsentNodes);
		
		Iterator<Edge> iterE=this.g.edges().iterator();
		System.out.println("adding edges");
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			if(unsentNodes.contains(sn.getId()) || unsentNodes.contains(tn.getId())) {
				if(!unsentNodes.contains(sn)) {
					spg.addNode(sn.getId(), MapAttribute.valueOf((String)tn.getAttribute("ui.class")));
					System.out.println(sn.getId()+"node's class"+sn.getAttribute("ui.class"));
				}
				
				if(!unsentNodes.contains(tn)) {
					spg.addNode(tn.getId(), MapAttribute.valueOf((String)tn.getAttribute("ui.class")));
					System.out.println(tn.getId()+"node's class"+tn.getAttribute("ui.class"));
				}
				
				spg.addEdge(e.getId(), sn.getId(), tn.getId());
				System.out.print("edges : " + e.getId() );
			}
		}	
		
		return spg;
	}


	public synchronized SerializableSimpleGraph<String,MapAttribute> getSerializableGraph(String agentId){
		

			 System.out.println("sending total graph to agent :"+ agentId); registerAgent(agentId);
			 serializeGraphTopology(); return this.sg; 

		 
		
		
	}
	
	public DataShare getSerializedData(String agenntId) {
		return new DataShare(this.getSerializableGraph(agenntId),
				this.tresor);
	}

	/**
	 * After migration we load the serialized data and recreate the non serializable components (Gui,..)
	 */
	public synchronized void loadSavedData(){

		this.g= new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet",nodeStyle);

		openGui();

		Integer nbEd=0;
		for (SerializableNode<String, MapAttribute> n: this.sg.getAllNodes()){
			this.g.addNode(n.getNodeId()).setAttribute("ui.class", n.getNodeContent().toString());
			for(String s:this.sg.getEdges(n.getNodeId())){
				this.g.addEdge(nbEd.toString(),n.getNodeId(),s);
				nbEd++;
			}
		}
		System.out.println("Loading done");
	}

	/**
	 * Method called before migration to kill all non serializable graphStream components
	 */
	private synchronized void closeGui() {
		//once the graph is saved, clear non serializable components
		if (this.viewer!=null){
			//Platform.runLater(() -> {
			try{
				this.viewer.close();
			}catch(NullPointerException e){
				System.err.println("Bug graphstream viewer.close() work-around - https://github.com/graphstream/gs-core/issues/150");
			}
			//});
			this.viewer=null;
		}
	}

	/**
	 * Method called after a migration to reopen GUI components
	 */
	private synchronized void openGui() {
		this.viewer =new FxViewer(this.g, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);//GRAPH_IN_GUI_THREAD)
		viewer.enableAutoLayout();
		viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);
		viewer.addDefaultView(true);

		g.display();
	}

	public void mergeMap(SerializableSimpleGraph<String, MapAttribute> sgreceived) {
		//System.out.println("You should decide what you want to save and how");
		//System.out.println("We currently blindy add the topology");
		System.out.println("merging, size: "+sgreceived.getAllNodes().size());
		for (SerializableNode<String, MapAttribute> n: sgreceived.getAllNodes()){
			//System.out.println(n);
			boolean alreadyIn =false;
			//1 Add the node
			Node newnode=null;
			System.out.println("adding node : " + n.getNodeId());
			try {
				newnode=this.g.addNode(n.getNodeId());
			}	catch(IdAlreadyInUseException e) {
				alreadyIn=true;
				//System.out.println("Already in"+n.getNodeId());
			}
			if (!alreadyIn) {
				newnode.setAttribute("ui.label", newnode.getId());
				newnode.setAttribute("ui.class", n.getNodeContent().toString());
			}else{
				newnode=this.g.getNode(n.getNodeId());
				//3 check its attribute. If it is below the one received, update it.
				if (((String) newnode.getAttribute("ui.class"))==MapAttribute.closed.toString() || n.getNodeContent().toString()==MapAttribute.closed.toString()) {
					System.out.println("modifiy node class to closed");
					newnode.setAttribute("ui.class",MapAttribute.closed.toString());
				}
			}
			
			System.out.println("node class : " + newnode.getAttribute("ui.class"));
		}

		//4 now that all nodes are added, we can add edges
		for (SerializableNode<String, MapAttribute> n: sgreceived.getAllNodes()){
			for(String s:sgreceived.getEdges(n.getNodeId())){
				addEdge(n.getNodeId(),s);
			}
		}
		//System.out.println("Merge done");
	}
	
	public void addTresor(String nodeId, String tresorType) {
		this.tresor.put(nodeId, new Tresor(tresorType));
	}
	
	public void updateTresorData(String nodeId, boolean locked, boolean collected) {
		
	}
	
	public void mergeTresor(HashMap<String, Tresor> tresors) {
		for(Map.Entry<String, Tresor> entry : tresors.entrySet()) {
			this.tresor.put(entry.getKey(), entry.getValue());
		}
	}
	
	public void mergeAllData(DataShare ds) {
		mergeMap(ds.getMap());
		if(ds.isNewData(timestamp)) {
			mergeTresor(ds.getTresors());
		}
	}

	/**
	 * 
	 * @return true if there exist at least one openNode on the graph 
	 */
	public boolean hasOpenNode() {
		return (this.g.nodes()
				.filter(n -> n.getAttribute("ui.class")==MapAttribute.open.toString())
				.findAny()).isPresent();
	}
	
	
	//new functions for communicationTracker
	public synchronized void registerAgent(String agentId) {
		this.communicationTracker.registerAgent(agentId);
	}
	
	public synchronized void cleanSet(String agentId) {
		this.communicationTracker.markNodesAsSent(agentId);
	}
	

	public long nbNodes() {
	  return g.nodes().count();
	}

}