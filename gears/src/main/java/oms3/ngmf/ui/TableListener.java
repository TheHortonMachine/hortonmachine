/*
 * This class provides listeners that
 * allow the user to use CNTRL_V to 
 * paste data to the JTable.
 */
package oms3.ngmf.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.swing.JTable;

/**
 *
 * @author Brandon Daniel
 */
public class TableListener {

    JTable table;//the table

    public TableListener(JTable table) {

        this.table = table;
        table.getTableHeader().addKeyListener(new PasteKeyListener(this));
        table.addKeyListener(new PasteKeyListener(this));

    }

    /**
     * turns the clipboard into a list of tokens
     * each array list is a line, each string in the list is a token in the line
     * @param text
     * @return
     */
    private ArrayList<ArrayList<String>> parseString(String text) {

        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        StringTokenizer linetoken = new StringTokenizer(text, "\n");
        StringTokenizer token;
        String current;
        while (linetoken.hasMoreTokens()) {
            current = linetoken.nextToken();
            if (current.contains(",")) {
                token = new StringTokenizer(current, ",");
            } else {
                token = new StringTokenizer(current);
            }
            ArrayList<String> line = new ArrayList<String>();
            while (token.hasMoreTokens()) {
                line.add(token.nextToken());
            }
            result.add(line);
        }
        return result;
    }

    /**
     * this adds the text to the jtable
     * @param text
     */
    private void addContents(String text) {

        int firstColSelected = table.getSelectedColumn();
        int firstRowSelected = table.getSelectedRow();
        int temp = firstColSelected;

        if (firstColSelected == -1 || firstRowSelected == -1) {
            return;
        }
        ArrayList<ArrayList<String>> clipboard = parseString(text);
        for (int i = 0; i < clipboard.size(); i++) {
            for (int j = 0; j < clipboard.get(i).size(); j++) {
                try {
                    table.getModel().setValueAt(clipboard.get(i).get(j), firstRowSelected, temp++);
                } catch (Exception e) {
                }
            }
            temp = firstColSelected;
            firstRowSelected++;
        }
    }

    //this is the function that adds the clipboard contents to the table
    public void pasteClipboard() {
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        try {
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                addContents((String) t.getTransferData(DataFlavor.stringFlavor));
                table.repaint();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

//this listens for the CNTRL_V key
class PasteKeyListener implements KeyListener {

    boolean pressed = false;
    TableListener adaptee;

    public PasteKeyListener(TableListener adaptee) {
        this.adaptee = adaptee;
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            pressed = true;
        }
        if (pressed && e.getKeyCode() == KeyEvent.VK_V) {
            adaptee.pasteClipboard();
        }
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            pressed = false;
        }
    }

    public void keyTyped(KeyEvent e) {
    }
}
