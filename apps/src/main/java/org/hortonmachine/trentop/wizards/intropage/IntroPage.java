package org.hortonmachine.trentop.wizards.intropage;
import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.geotools.swing.wizard.JPage;
import org.hortonmachine.trentop.wizards.filesmodepage.FilesModePage;
import org.hortonmachine.trentop.wizards.projectpage.OpenProjectPage;

public class IntroPage extends JPage {

    public static final String ID = "intropage";

    private IntroView introView;

    public IntroPage( String id ) {
        super(id);
    }

    @Override
    public JPanel createPanel() {
        JPanel page = new JPanel(new BorderLayout());
        introView = new IntroView();
        page.add(introView, BorderLayout.CENTER);

        introView._createFilesButton.addActionListener(( e ) -> {
            setNextPageIdentifier(FilesModePage.ID);
            getJWizard().getController().syncButtonsToPage();
        });
        introView._openProjectButton.addActionListener(( e ) -> {
            setNextPageIdentifier(OpenProjectPage.ID);
            getJWizard().getController().syncButtonsToPage();
        });

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
        return introView._createFilesButton.isSelected() || introView._openProjectButton.isSelected();
    }
}