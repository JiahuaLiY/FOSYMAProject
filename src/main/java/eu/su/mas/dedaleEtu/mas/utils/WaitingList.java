package eu.su.mas.dedaleEtu.mas.utils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class WaitingList implements Serializable {
  private static final long serialVersionUID = -1861194147315239120L;
  private final List<WaitingTask> waitingTasks = new LinkedList<>();
  
  public void add(String name, String sentProtocol, String waitedProtocol, long waitDuration) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(sentProtocol);
    Objects.requireNonNull(waitedProtocol);
    if (waitDuration <= 0) {
      throw new IllegalArgumentException("wait duration must not be negative or null");
    }
    waitingTasks.add(new WaitingTask(name, sentProtocol, waitedProtocol, waitDuration));
  }
  
  public boolean allTasksCompleted() {
    return waitingTasks.isEmpty() ||
        waitingTasks.stream()
        .allMatch(WaitingTask::isDone);
  }
  
  public void clearAllTasks() {
    waitingTasks.clear();
  }
  
  public void remove(String name, String waitForProtocol) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(waitForProtocol);
    waitingTasks.removeIf(task -> task.name.equals(name) 
        && task.waitedProtocol.equals(waitForProtocol));
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
  
  private class WaitingTask {
    private final String name;
    private final String sentProtocol;
    private final String waitedProtocol;
    private final long startTime;
    private final long waitDuration;
    
    public WaitingTask(
        String name,
        String sentProtocol,
        String waitForProtocol,
        long waitDuration) {
      this.name = name;
      this.sentProtocol = sentProtocol;
      this.waitedProtocol = waitForProtocol;
      this.waitDuration = waitDuration;
      this.startTime = System.currentTimeMillis();
    }
    
    public long getRemainingTime() {
      return waitDuration - (System.currentTimeMillis() - startTime);
    }
    
    public boolean isDone() {
      return (System.currentTimeMillis() - startTime) >= waitDuration;
    }
    
    public boolean isNotDone() {
      return (System.currentTimeMillis() - startTime) < waitDuration;
    }
    
    public String name() {
      return name;
    }
    
    public String waitedProtocol() {
      return waitedProtocol;
    }
    
    @Override
    public String toString() {
      return name + ": " + waitDuration + " ms (started at " + startTime + 
          "); sent protocol: " + sentProtocol + 
          "; waited protocol: " + waitedProtocol + ";";
    }
  }
}
