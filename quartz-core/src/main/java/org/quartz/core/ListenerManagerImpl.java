package org.quartz.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.Matcher;
import org.quartz.SchedulerListener;
import org.quartz.TriggerListener;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.utils.Key;

public class ListenerManagerImpl implements ListenerManager {

    private Map<String, JobListener> globalJobListeners = new LinkedHashMap<String, JobListener>(10);

    private Map<String, TriggerListener> globalTriggerListeners = new LinkedHashMap<String, TriggerListener>(10);

//    private Map<String, List<Matcher<JobKey>>> globalJobListenersMatchers = new LinkedHashMap<String, List<Matcher<JobKey>>>(10);
    private Map<String, List<Matcher<Key<?>>>> globalJobListenersMatchers = new LinkedHashMap<String, List<Matcher<Key<?>>>>(10);

//    private Map<String, List<Matcher<TriggerKey>>> globalTriggerListenersMatchers = new LinkedHashMap<String, List<Matcher<TriggerKey>>>(10);
    private Map<String, List<Matcher<Key<?>>>> globalTriggerListenersMatchers = new LinkedHashMap<String, List<Matcher<Key<?>>>>(10);

    private ArrayList<SchedulerListener> schedulerListeners = new ArrayList<SchedulerListener>(10);

    @Override
    public void addJobListener(JobListener jobListener, Matcher<Key<?>> ... matchers) {
        addJobListener(jobListener, Arrays.asList(matchers));
    }
    @Override
    public void addJobListener(JobListener jobListener, List<Matcher<Key<?>>> matchers) {
        if (jobListener.getName() == null || jobListener.getName().length() == 0) {
            throw new IllegalArgumentException("JobListener name cannot be empty.");
        }
        synchronized (globalJobListeners) {
            globalJobListeners.put(jobListener.getName(), jobListener);
            LinkedList<Matcher<Key<?>>> matchersL = new  LinkedList<Matcher<Key<?>>>();
            if(matchers != null && matchers.size() > 0){
                matchersL.addAll(matchers);
            }
            else{
                matchersL.add(EverythingMatcher.allJobs());
            }
            globalJobListenersMatchers.put(jobListener.getName(), matchersL);
        }
    }

    @Override
    public void addJobListener(JobListener jobListener) {
        addJobListener(jobListener, EverythingMatcher.allJobs());
    }
    @Override
    public void addJobListener(JobListener jobListener, Matcher<Key<?>> matcher) {
        if (jobListener.getName() == null || jobListener.getName().length() == 0) {
            throw new IllegalArgumentException("JobListener name cannot be empty.");
        }
        synchronized (globalJobListeners) {
            globalJobListeners.put(jobListener.getName(), jobListener);
            LinkedList<Matcher<Key<?>>> matchersL = new  LinkedList<Matcher<Key<?>>>();
            if(matcher != null){
                matchersL.add(matcher);
            }
            else{
                matchersL.add(EverythingMatcher.allJobs());
            }
            globalJobListenersMatchers.put(jobListener.getName(), matchersL);
        }
    }

    @Override
    public boolean addJobListenerMatcher(String listenerName, Matcher<Key<?>> matcher) {
        if(matcher == null){
            throw new IllegalArgumentException("Null value not acceptable.");
        }
        synchronized (globalJobListeners) {
            List<Matcher<Key<?>>> matchers = globalJobListenersMatchers.get(listenerName);
            if(matchers == null){
                return false;
            }
            matchers.add(matcher);
            return true;
        }
    }
    @Override
    public boolean removeJobListenerMatcher(String listenerName, Matcher<Key<?>> matcher) {
        if(matcher == null){
            throw new IllegalArgumentException("Non-null value not acceptable.");
        }
        synchronized (globalJobListeners) {
            List<Matcher<Key<?>>> matchers = globalJobListenersMatchers.get(listenerName);
            if(matchers == null){
                return false;
            }
            return matchers.remove(matcher);
        }
    }
    @Override
    public List<Matcher<Key<?>>> getJobListenerMatchers(String listenerName) {
        synchronized (globalJobListeners) {
            List<Matcher<Key<?>>> matchers = globalJobListenersMatchers.get(listenerName);
            if(matchers == null){
                return null;
            }
            return Collections.unmodifiableList(matchers);
        }
    }
    @Override
    public boolean setJobListenerMatchers(String listenerName, List<Matcher<Key<?>>> matchers)  {
        if(matchers == null){
            throw new IllegalArgumentException("Non-null value not acceptable.");
        }
        synchronized (globalJobListeners) {
            List<Matcher<Key<?>>> oldMatchers = globalJobListenersMatchers.get(listenerName);
            if(oldMatchers == null){
                return false;
            }
            globalJobListenersMatchers.put(listenerName, matchers);
            return true;
        }
    }
    @Override
    public boolean removeJobListener(String name) {
        synchronized (globalJobListeners) {
            return (globalJobListeners.remove(name) != null);
        }
    }
    @Override
    public List<JobListener> getJobListeners() {
        synchronized (globalJobListeners) {
            return java.util.Collections.unmodifiableList(new LinkedList<JobListener>(globalJobListeners.values()));
        }
    }
    @Override
    public JobListener getJobListener(String name) {
        synchronized (globalJobListeners) {
            return globalJobListeners.get(name);
        }
    }
    @Override
    public void addTriggerListener(TriggerListener triggerListener, Matcher<Key<?>> ... matchers) {
        addTriggerListener(triggerListener, Arrays.asList(matchers));
    }
    @Override
    public void addTriggerListener(TriggerListener triggerListener, List<Matcher<Key<?>>> matchers) {
        if (triggerListener.getName() == null || triggerListener.getName().length() == 0) {
            throw new IllegalArgumentException("TriggerListener name cannot be empty.");
        }
        synchronized (globalTriggerListeners) {
            globalTriggerListeners.put(triggerListener.getName(), triggerListener);
            LinkedList<Matcher<Key<?>>> matchersL = new  LinkedList<Matcher<Key<?>>>();
            if(matchers != null && matchers.size() > 0){
                matchersL.addAll(matchers);
            }else{
                matchersL.add(EverythingMatcher.allTriggers());
            }
            globalTriggerListenersMatchers.put(triggerListener.getName(),matchersL);
        }
    }
    @Override
    public void addTriggerListener(TriggerListener triggerListener) {
        addTriggerListener(triggerListener, EverythingMatcher.allTriggers());
    }
    @Override
    public void addTriggerListener(TriggerListener triggerListener,Matcher<Key<?>> matcher) {
        if(matcher == null){
            throw new IllegalArgumentException("Null value not acceptable for matcher.");
        }
        if (triggerListener.getName() == null || triggerListener.getName().length() == 0) {
            throw new IllegalArgumentException("TriggerListener name cannot be empty.");
        }
        synchronized (globalTriggerListeners) {
            globalTriggerListeners.put(triggerListener.getName(), triggerListener);
            List<Matcher<Key<?>>> matchers = new LinkedList<Matcher<Key<?>>>();
            matchers.add(matcher);
            globalTriggerListenersMatchers.put(triggerListener.getName(),matchers);
        }
    }
    @Override
    public boolean addTriggerListenerMatcher(String listenerName, Matcher<Key<?>> matcher) {
        if(matcher == null){
            throw new IllegalArgumentException("Non-null value not acceptable.");
        }
        synchronized (globalTriggerListeners) {
            List<Matcher<Key<?>>> matchers = globalTriggerListenersMatchers.get(listenerName);
            if(matchers == null){
                return false;
            }
            matchers.add(matcher);
            return true;
        }
    }
    @Override
    public boolean removeTriggerListenerMatcher(String listenerName, Matcher<Key<?>> matcher) {
        if(matcher == null){
            throw new IllegalArgumentException("Non-null value not acceptable.");
        }
        synchronized (globalTriggerListeners) {
            List<Matcher<Key<?>>> matchers = globalTriggerListenersMatchers.get(listenerName);
            if(matchers == null){
                return false;
            }
            return matchers.remove(matcher);
        }
    }
    @Override
    public List<Matcher<Key<?>>> getTriggerListenerMatchers(String listenerName) {
        synchronized (globalTriggerListeners) {
            List<Matcher<Key<?>>> matchers = globalTriggerListenersMatchers.get(listenerName);
            if(matchers == null){
                return null;
            }
            return Collections.unmodifiableList(matchers);
        }
    }
    @Override
    public boolean setTriggerListenerMatchers(String listenerName, List<Matcher<Key<?>>> matchers)  {
        if(matchers == null){
            throw new IllegalArgumentException("Non-null value not acceptable.");
        }
        synchronized (globalTriggerListeners) {
            List<Matcher<Key<?>>> oldMatchers = globalTriggerListenersMatchers.get(listenerName);
            if(oldMatchers == null){
                return false;
            }
            globalTriggerListenersMatchers.put(listenerName, matchers);
            return true;
        }
    }
    @Override
    public boolean removeTriggerListener(String name) {
        synchronized (globalTriggerListeners) {
            return (globalTriggerListeners.remove(name) != null);
        }
    }
    
    @Override
    public List<TriggerListener> getTriggerListeners() {
        synchronized (globalTriggerListeners) {
            return java.util.Collections.unmodifiableList(new LinkedList<TriggerListener>(globalTriggerListeners.values()));
        }
    }
    @Override
    public TriggerListener getTriggerListener(String name) {
        synchronized (globalTriggerListeners) {
            return globalTriggerListeners.get(name);
        }
    }
    
    @Override
    public void addSchedulerListener(SchedulerListener schedulerListener) {
        synchronized (schedulerListeners) {
            schedulerListeners.add(schedulerListener);
        }
    }
    @Override
    public boolean removeSchedulerListener(SchedulerListener schedulerListener) {
        synchronized (schedulerListeners) {
            return schedulerListeners.remove(schedulerListener);
        }
    }
    @Override
    public List<SchedulerListener> getSchedulerListeners() {
        synchronized (schedulerListeners) {
            return java.util.Collections.unmodifiableList(new ArrayList<SchedulerListener>(schedulerListeners));
        }
    }
}
