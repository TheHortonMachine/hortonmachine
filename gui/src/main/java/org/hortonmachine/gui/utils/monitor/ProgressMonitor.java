package org.hortonmachine.gui.utils.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

/** 
 * MySwing: Advanced Swing Utilites 
 * Copyright (C) 2005  Santhosh Kumar T 
 * <p/> 
 * This library is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version. 
 * <p/> 
 * This library is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU 
 * Lesser General Public License for more details. 
 */
public class ProgressMonitor implements IHMProgressMonitor {
    int total, current = -1;
    boolean indeterminate;
    int milliSecondsToWait = 500; // half second
    String status;

    public ProgressMonitor( int total, boolean indeterminate, int milliSecondsToWait ) {
        this.total = total;
        this.indeterminate = indeterminate;
        this.milliSecondsToWait = milliSecondsToWait;
    }

    public ProgressMonitor( int total, boolean indeterminate ) {
        this.total = total;
        this.indeterminate = indeterminate;
    }

    public int getTotal() {
        return total;
    }

    public void start( String status ) {
        if (current != -1)
            throw new IllegalStateException("not started yet");
        this.status = status;
        current = 0;
        fireChangeEvent();
    }

    @Override
    public void beginTask( String name, int totalWork ) {
        // this can be seen as a start and setting of totalwork
        this.total = totalWork;
        start(name);
    }

    public int getMilliSecondsToWait() {
        return milliSecondsToWait;
    }

    public int getCurrent() {
        return current;
    }

    public String getStatus() {
        return status;
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public void setCurrent( String status, int current ) {
        if (current == -1)
            throw new IllegalStateException("not started yet");
        this.current = current;
        if (status != null)
            this.status = status;
        fireChangeEvent();
    }

    /*--------------------------------[ ListenerSupport ]--------------------------------*/

    private List<ChangeListener> listeners = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private ChangeEvent ce = new ChangeEvent(this);

    public void addChangeListener( ChangeListener listener ) {
        lock.lock();
        try {
            listeners.add(listener);
        } finally {
            lock.unlock();
        }
    }

    public void removeChangeListener( ChangeListener listener ) {
        lock.lock();
        try {
            listeners.remove(listener);
        } finally {
            lock.unlock();
        }
    }

    private synchronized void fireChangeEvent() {
        lock.lock();
        try {
            for( ChangeListener changeListener : listeners ) {
                changeListener.stateChanged(ce);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void message( String message ) {
        if (message != null) {
            this.status = message;
            fireChangeEvent();
        }
    }

    @Override
    public void errorMessage( String message ) {
        // TODO Auto-generated method stub

    }

    @Override
    public void exceptionThrown( String message ) {
        if (message != null) {
            this.status = message;
            fireChangeEvent();
        }
    }

    @Override
    public void done() {
        setCurrent("Done.", total);
    }

    @Override
    public void internalWorked( double work ) {
        throw new RuntimeException("Not implemented yet...");
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void setCanceled( boolean value ) {
        throw new RuntimeException("Not implemented yet...");
    }

    @Override
    public void setTaskName( String name ) {
        throw new RuntimeException("Not implemented yet...");
    }

    @Override
    public void subTask( String name ) {
        throw new RuntimeException("Not implemented yet...");
    }

    @Override
    public void worked( int work ) {
        this.current += work;
        fireChangeEvent();
    }

    @Override
    public <T> T adapt( Class<T> adaptee ) {
        throw new RuntimeException("Not implemented yet...");
    }

    @Override
    public void onModuleExit() {
        throw new RuntimeException("Not implemented yet...");
    }
}
