package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.Objects;

import org.jdom2.IllegalAddException;

import eu.su.mas.dedale.env.Observation;

public final class Treasure implements Serializable {
  
  private static final long serialVersionUID = -8466455814694543142L;
  
  private Observation type;
  
  private int lockPicking;
  
  private int strength;
  
  private int amount;
  
  private boolean locked;
  
  private long timeStamp;
  
  private Treasure(Observation type, int lockPicking, int strength, int amount, boolean locked, long timeStamp) {
    this.type = type;
    this.lockPicking = lockPicking;
    this.strength = strength;
    this.amount = amount;
    this.locked = locked;
    this.timeStamp = timeStamp;
  }
  
  private Treasure() {
    this(Observation.ANY_TREASURE, 0, 0, 0, false, System.currentTimeMillis());
  }
  
  public Treasure clone() {
    return new Treasure(this.type, this.lockPicking, this.strength, this.amount, this.locked, this.timeStamp);
  }
  
  public static Treasure createEmptyTreasure() {
    return new Treasure();
  }
  
  public void setTreasureType(Observation type) {
    Objects.requireNonNull(type);
    this.type = type;
  }

  public void setTreasureLockPicking(int lockPicking) {
    if (lockPicking < 0) {
      throw new IllegalArgumentException("The value of treasure lock picking cannot be negative");
    }
    this.lockPicking = lockPicking;
  }
  
  public void setTreasureStrength(int strenght) {
    if (strenght < 0) {
      throw new IllegalAddException("The value of treasure strength cannot be negative");
    }
    this.strength = strenght;
  }
  
  public void setTreasureAmount(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("The value of treasure amount cannot be negative");
    }
    this.amount = amount;
  }
  
  public void setTreasureLocked(boolean locked) {
    this.locked = locked;
  }
  
  public void updatTimeStamp() {
    this.timeStamp = System.currentTimeMillis();
  }
  
  public Observation type() {
    return this.type;
  }
  
  public int lockPicking() {
    return this.lockPicking;
  }
  
  public int strength() {
    return this.strength;
  }
  
  public int amount() {
    return this.amount;
  }
  
  public boolean locked() {
    return this.locked;
  }
  
  public long timeStamp() {
    return this.timeStamp;
  }
  
  public boolean isEmptyTreasure() {
    return this.amount == 0;
  }
  
  @Override
  public boolean equals(Object obj) {
    return obj instanceof Treasure treasureKnowledge
        && this.type.equals(treasureKnowledge.type)
        && this.lockPicking == treasureKnowledge.lockPicking
        && this.strength == treasureKnowledge.strength
        && this.amount == treasureKnowledge.amount
        && this.locked == treasureKnowledge.locked;
  }
  @Override
  public String toString() {
    return "treasure:{type:" + type
        + ", amount:" + amount
        + ", lock picking:" + lockPicking
        + ", strength: " + strength
        + ", locked: " + locked + "};";
  }
}
