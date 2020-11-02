package org.hortonmachine.trentop;

import java.awt.HeadlessException;

import org.geotools.swing.wizard.JPage;
import org.geotools.swing.wizard.JWizard;
import org.hortonmachine.trentop.wizards.filesmodepage.FilesModePage;
import org.hortonmachine.trentop.wizards.intropage.IntroPage;

public class TrentopWizards {

    public static void runCreateFilesWIzard() {
        TrentopWizard w = new TrentopWizard();
        w.setSize(600, 600);
        
        int result = w.showModalDialog();
        
    }
    
    
    
    public static void main( String[] args ) {
        TrentopWizards.runCreateFilesWIzard();
    }
    
    
    
    static class TrentopWizard extends JWizard {

        public TrentopWizard( ) throws HeadlessException {
            super("Trento P");
            
            
            JPage page1 = new IntroPage(IntroPage.ID);
            page1.setBackPageIdentifier(null);
            page1.setNextPageIdentifier(null);
            registerWizardPanel(page1);

            JPage page2 = new IntroPage(FilesModePage.ID);
            page2.setBackPageIdentifier(null);
            page2.setNextPageIdentifier(null);
            registerWizardPanel(page2);
            
            setCurrentPanel(IntroPage.ID);
        }
            
        
    }
}