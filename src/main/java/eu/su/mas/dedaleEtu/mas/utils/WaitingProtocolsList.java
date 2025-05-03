package eu.su.mas.dedaleEtu.mas.utils;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class WaitingProtocolsList implements Serializable {
  
  private static final long serialVersionUID = -1861194147315239120L;
  
  private final List<WaitingTask> waitingTasks = new LinkedList<>();
  
  public boolean add(String waitingAgentName, String sentProtocol, String waitForProtocol, long waitingDuration) {
    Objects.requireNonNull(waitingAgentName);
    Objects.requireNonNull(sentProtocol);
    Objects.requireNonNull(waitForProtocol);
    if (waitingDuration <= 0) {
      throw new IllegalArgumentException("Waiting duration cannot be negative or zero");
    }
    return waitingTasks.add(new WaitingTask(waitingAgentName, sentProtocol, waitForProtocol, waitingDuration));
  }
  
  public boolean allCompleted() {
    return waitingTasks.isEmpty() || waitingTasks.stream().allMatch(WaitingTask::isDone);
  }
  
  public void clear() {
    waitingTasks.clear();
  }
  
  public boolean remove(String waitingAgentName, String waitForProtocol) {
    Objects.requireNonNull(waitingAgentName);
    Objects.requireNonNull(waitForProtocol);
    return waitingTasks.removeIf(task -> task.waitingAgentName.equals(waitingAgentName) && 
        task.waitForProtocol.equals(waitForProtocol));
  }
  
  public long getMaxRemainingTime() {
    return waitingTasks.stream()
        .mapToLong(WaitingTask::getRemainingTime)
        .max()
        .orElse(0L);
  }
  
  @Override
  public String toString() {
    return waitingTasks.stream()
        .filter(WaitingTask::isNotDone)
        .map(WaitingTask::toString)
        .collect(Collectors.joining("\n"));
  }
  
  private final class WaitingTask {
    
    private final String waitingAgentName;
    
    private final String sentProtocol;
    
    private final String waitForProtocol;
    
    private final long startTime;
    
    private final long waitingDuration;
    
    public WaitingTask(String waitingAgentName, String sentProtocol, String waitForProtocol, long waitingDuration) {
      this.waitingAgentName = waitingAgentName;
      this.sentProtocol = sentProtocol;
      this.waitForProtocol = waitForProtocol;
      this.waitingDuration = waitingDuration;
      
      this.startTime = System.currentTimeMillis();
    }
    
    public boolean isDone() {
      return (System.currentTimeMillis() - startTime) >= waitingDuration;
    }
    
    public boolean isNotDone() {
      return (System.currentTimeMillis() - startTime) < waitingDuration;
    }
    
    public long getRemainingTime() {
      return waitingDuration - (System.currentTimeMillis() - startTime);
    }
    
    @Override
    public String toString() {
      return "Wait for agent: " + waitingAgentName + "(" + waitingDuration + " ms start at " + startTime + "),"
          + " sent protocol: " + sentProtocol + " and wait for protocol: " + waitForProtocol;
    }
  }
}
