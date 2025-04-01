package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.HashMap;

import dataStructures.serializableGraph.*;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.Tresor;


public class DataShare implements Serializable {
	private static final long serialVersionUID = -4398879521367771647L;
	
	private SerializableSimpleGraph<String, MapAttribute> map;
	private HashMap<String, Tresor> tresorMap;
	private long timestamp;
	
	public DataShare(SerializableSimpleGraph<String, MapAttribute> map,
			HashMap<String, Tresor> tresorMap) {
		this.map = map;
		this.tresorMap = tresorMap;
		this.timestamp = System.currentTimeMillis();
	}
	
	public SerializableSimpleGraph<String, MapAttribute> getMap(){
		return this.map;
	}
	
	public HashMap<String, Tresor> getTresors(){
		return this.tresorMap;
	}
	
	public boolean isNewData(long timestamp) {
		return this.timestamp > timestamp;
	
	}
	
}
