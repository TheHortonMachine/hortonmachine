package org.hortonmachine.trentop.wizards.projectpage;
import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.geotools.swing.wizard.JPage;
import org.hortonmachine.trentop.wizards.intropage.IntroView;

public class OpenProjectPage extends JPage {

    public static final String ID = "projectmodepage";

    private IntroView introView;

    public OpenProjectPage( String id ) {
        super(id);
    }

    @Override
    public JPanel createPanel() {
        JPanel page = new JPanel(new BorderLayout());
        introView = new IntroView();
        page.add(introView, BorderLayout.CENTER);

        return page;
    }

    @Override
    public void preDisplayPanel() {
    };

    @Override
    public void preClosePanel() {
    };

    @Override
    public boolean isValid() {
        return true;
    }
}