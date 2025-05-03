package eu.su.mas.dedaleEtu.mas.utils;

import java.io.Serializable;
import java.util.Objects;

public final class UnorderedCouple<T> implements Serializable {

  private static final long serialVersionUID = -3264846633856661570L;

  private final T left;
  private final T right;
  
  public UnorderedCouple(T left, T right) {
    Objects.requireNonNull(left);
    Objects.requireNonNull(right);
    this.left = left;
    this.right = right;
  }
  
  public T left() {
    return this.left;
  }
  
  public T right() {
    return this.right;
  }
  
  @Override
  public boolean equals(Object obj) {
    return obj instanceof UnorderedCouple unorderedCouple
        && ((this.left.equals(unorderedCouple.left) && this.right.equals(unorderedCouple.right)) 
            || (this.left.equals(unorderedCouple.right) && this.right.equals(unorderedCouple.left)));
  }
  
  @Override
  public int hashCode() {
    return left.hashCode() ^ right.hashCode();
  }
  
  @Override
  public String toString() {
    return "{" + left + ", " + right + "}";
  }
}
