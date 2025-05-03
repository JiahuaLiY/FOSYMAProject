package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import eu.su.mas.dedaleEtu.mas.utils.UnorderedCouple;
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
		agent,
		open,
		closed;
	}
	
	private static final long serialVersionUID = -1333959882640838272L;

	/*********************************
	 * Parameters for graph rendering
	 ********************************/

	private String defaultNodeStyle = "node {" + "fill-color: black;" + " size-mode:fit;text-alignment:under; text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";
	private String nodeStyle_open = "node.agent {" + "fill-color: forestgreen;" + "}";
	private String nodeStyle_agent = "node.open {" + "fill-color: blue;" + "}";
	private String nodeStyle = defaultNodeStyle+nodeStyle_agent+nodeStyle_open;
	

	private Graph graph; //data structure non serializable
	private Viewer viewer; //ref to the display,  non serializable
	private Integer nbEdges;//used to generate the edges ids

	private SerializableSimpleGraph<String, MapAttribute> sg;//used as a temporary dataStructure during migration
	
	private final Map<UnorderedCouple<String>, String> edges = new HashMap<>();
	
	private final Map<String, MapAttribute> nodes = new HashMap<>();

	public MapRepresentation() {
		//System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.graph = new SingleGraph("My world vision");
		this.graph.setAttribute("ui.stylesheet",nodeStyle);

		Platform.runLater(() -> {
			openGui();
		});
		//this.viewer = this.g.display();
		this.nbEdges=0;
	}
	
	
	/**
	 * Add or replace a node and its attribute 
	 * @param id unique identifier of the node
	 * @param mapAttribute attribute to process
	 */
	public synchronized boolean addNode(String nodeID, MapAttribute mapAttribute){
	  Objects.requireNonNull(nodeID);
	  Objects.requireNonNull(mapAttribute);
	  
		Node node;
		var isNewNode = false;
		if (graph.getNode(nodeID) == null) {
      isNewNode = true;
			node = graph.addNode(nodeID);
		}
		else {
			node = graph.getNode(nodeID);
		}
		nodes.put(nodeID, mapAttribute);
		node.clearAttributes();
		node.setAttribute("ui.class", mapAttribute.toString());
		node.setAttribute("ui.label", nodeID);
		return isNewNode;
	}

	/**
	 * Add a node to the graph. Do nothing if the node already exists.
	 * If new, it is labeled as open (non-visited)
	 * @param id id of the node
	 * @return true if added
	 */
	public synchronized boolean addNewNode(String nodeID) {
	  Objects.requireNonNull(nodeID);
		if (graph.getNode(nodeID) == null) {
			addNode(nodeID, MapAttribute.open);
			return true;
		}
		return false;
	}

	/**
	 * Add an undirect edge if not alreasentProtocoldy existing.
	 * @param idNode1 unique identifier of node1
	 * @param idNode2 unique identifier of node2
	 */
	public synchronized String addEdge(String nodeID1, String nodeID2) {
	  Objects.requireNonNull(nodeID1);
	  Objects.requireNonNull(nodeID2);
		this.nbEdges++;
		try {
			this.graph.addEdge(this.nbEdges.toString(), nodeID1, nodeID2);
			edges.put(new UnorderedCouple<String>(nodeID1, nodeID2), nbEdges.toString());
			return nbEdges.toString();
		}catch (IdAlreadyInUseException e1) {
			System.err.println("ID existing");
			System.exit(1);
		}catch (EdgeRejectedException e2) {
			this.nbEdges--;
		} catch(ElementNotFoundException e3){

		}
		return null;
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
		dijkstra.init(graph);
		dijkstra.setSource(graph.getNode(idFrom));
		dijkstra.compute();//compute the distance to all nodes from idFrom
		List<Node> path=dijkstra.getPath(graph.getNode(idTo)).getNodePath(); //the shortest path from idFrom to idTo
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
		return this.graph.nodes()
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

		this.graph = null;
	}

	/**
	 * Before sending the agent knowledge of the map it should be serialized.
	 */
	private synchronized void serializeGraphTopology() {
		this.sg= new SerializableSimpleGraph<String,MapAttribute>();
		Iterator<Node> iter=this.graph.iterator();
		while(iter.hasNext()){
			Node n=iter.next();
			sg.addNode(n.getId(),MapAttribute.valueOf((String)n.getAttribute("ui.class")));
		}
		Iterator<Edge> iterE=this.graph.edges().iterator();
		while (iterE.hasNext()){
			Edge e=iterE.next();
			Node sn=e.getSourceNode();
			Node tn=e.getTargetNode();
			sg.addEdge(e.getId(), sn.getId(), tn.getId());
		}
	}
	
	public synchronized SerializableSimpleGraph<String,MapAttribute> getSerializableGraph(String agentId) {
	  serializeGraphTopology();
	  return this.sg;
	}

	/**
	 * After migration we load the serialized data and recreate the non serializable components (Gui,..)
	 */
	public synchronized void loadSavedData(){

		this.graph= new SingleGraph("My world vision");
		this.graph.setAttribute("ui.stylesheet",nodeStyle);

		openGui();

		Integer nbEd=0;
		for (SerializableNode<String, MapAttribute> n: this.sg.getAllNodes()){
			this.graph.addNode(n.getNodeId()).setAttribute("ui.class", n.getNodeContent().toString());
			for(String s:this.sg.getEdges(n.getNodeId())){
				this.graph.addEdge(nbEd.toString(),n.getNodeId(),s);
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
		this.viewer =new FxViewer(this.graph, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);//GRAPH_IN_GUI_THREAD)
		viewer.enableAutoLayout();
		viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);
		viewer.addDefaultView(true);

		graph.display();
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
				newnode=this.graph.addNode(n.getNodeId());
			}	catch(IdAlreadyInUseException e) {
				alreadyIn=true;
				//System.out.println("Already in"+n.getNodeId());
			}
			if (!alreadyIn) {
				newnode.setAttribute("ui.label", newnode.getId());
				newnode.setAttribute("ui.class", n.getNodeContent().toString());
			}else{
				newnode=this.graph.getNode(n.getNodeId());
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

	/**
	 * 
	 * @return true if there exist at least one openNode on the graph 
	 */
	public boolean hasOpenNode() {
		return (this.graph.nodes()
				.filter(n -> n.getAttribute("ui.class")==MapAttribute.open.toString())
				.findAny()).isPresent();
	}
	
	
	
	// Added methods.
	public boolean containsNode(String nodeID) {
	  Objects.requireNonNull(nodeID);
	  return nodes.containsKey(nodeID);
	}
	
	public boolean isOpenNode(String nodeID) {
	  Objects.requireNonNull(nodeID);
	  return nodes.get(nodeID).equals(MapAttribute.open);
	}
	
	public MapAttribute getNodeAttribute(String nodeID) {
	  Objects.requireNonNull(nodeID);
	  return nodes.get(nodeID);
	}
	
	public String getEdgeIdentifier(UnorderedCouple<String> unorderedCouple) {
	  Objects.requireNonNull(unorderedCouple);
	  return edges.get(unorderedCouple);
	}
	
	public String getEdgeIdentifier(String nodeID1, String nodeID2) {
	  Objects.requireNonNull(nodeID1);
	  Objects.requireNonNull(nodeID2);
	  
	  var unorderedCouple = new UnorderedCouple<String>(nodeID1, nodeID2);
	  return edges.get(unorderedCouple);
	}
	
	public List<String> getOpenNodeIdentifiers() {
	  return graph.nodes()
	      .filter(node -> isOpenNode(node.getId()))
	      .map(Node::getId)
	      .collect(Collectors.toList());
	}
	
 	public synchronized Optional<List<String>> getShortestPathFromCondidates(String currentPositionID,
	                                                                         Collection<UnorderedCouple<String>> forbiddenEdges,
	                                                                         Collection<String> condidates) {
	  Objects.requireNonNull(currentPositionID);
	  Objects.requireNonNull(forbiddenEdges);
	  Objects.requireNonNull(condidates);
	  
	  var dijkstra = new Dijkstra();
	  // We temporarily remove all forbidden edges in order to compute the shortest path.
	  for (var edge: forbiddenEdges) {
	    try {
	      graph.removeEdge(getEdgeIdentifier(edge));
	    } catch (ElementNotFoundException e) {
        e.printStackTrace();
        System.out.println(graph.edges().map(Edge::getId).toList());
        System.out.println(edges);
        System.out.println(forbiddenEdges);
        System.out.println(edge);
        System.exit(-1);
      }
	  }
	  
	  // Compute the shortest path.
    dijkstra.init(graph);
    dijkstra.setSource(graph.getNode(currentPositionID));
    dijkstra.compute();
    
    // We add all deleted edges.
    for (var edge: forbiddenEdges) {
      graph.addEdge(getEdgeIdentifier(edge), edge.left(), edge.right());
    }
    
    var bestCondidate = condidates.stream()
        .min(Comparator.comparingDouble(nodeID -> dijkstra.getPathLength(graph.getNode(nodeID))));
    
    if (bestCondidate.isPresent()) {
      var shortestPath = new ArrayList<String>();
      for (var node: dijkstra.getPath(graph.getNode(bestCondidate.get())).getNodePath()) {
        shortestPath.add(node.getId());
      }
      if (!shortestPath.isEmpty()) {
        dijkstra.clear();
        shortestPath.removeFirst();
        return Optional.of(shortestPath);
      }
    }
    dijkstra.clear();
    return Optional.empty();
	}
  
  public synchronized SerializableSimpleGraph<String, MapAttribute> getSerializedParticalGraphTopology(Set<String> targetNodes) {
    Objects.requireNonNull(targetNodes);
    
    var serializedParticalGraph = new SerializableSimpleGraph<String, MapAttribute>();
    for (var nodeID: targetNodes) {
      //System.out.println(nodeID);
      serializedParticalGraph.addNode(nodeID, getNodeAttribute(nodeID));
    }
    //System.out.println(targetNodes);
    var edgesIterator = graph.edges().iterator();
    while (edgesIterator.hasNext()) {
      var edge = edgesIterator.next();
      var srcNode = edge.getSourceNode();
      var tarNode = edge.getTargetNode();
      
      var srcNodeID = srcNode.getId();
      var tarNodeID = tarNode.getId();
      if (targetNodes.contains(srcNodeID) || targetNodes.contains(tarNodeID)) {
        if (!targetNodes.contains(srcNodeID)) {
          serializedParticalGraph.addNode(srcNodeID, getNodeAttribute(srcNodeID));
        }
        if (!targetNodes.contains(tarNodeID)) {
          serializedParticalGraph.addNode(tarNodeID, getNodeAttribute(tarNodeID));
        }
        //System.out.println(edge.getId() + " " + srcNodeID + " " + tarNodeID);
        serializedParticalGraph.addEdge(edge.getId(), srcNodeID, tarNodeID);
      }
    }
    return serializedParticalGraph;
  }
  
  public List<UnorderedCouple<String>> getAllEdges() {
    return edges.keySet().stream()
        .collect(Collectors.toList());
  }
  
  public List<String> getAllNodes() {
    return nodes.keySet().stream()
        .toList();
  }
}

