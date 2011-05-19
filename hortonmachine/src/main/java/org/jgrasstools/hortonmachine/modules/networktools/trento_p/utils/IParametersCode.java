package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;

public interface IParametersCode {
    public int getCode();
    public String getKey(); 
    public String getDescription();
    public IParametersCode forCode( int i ); 
}
